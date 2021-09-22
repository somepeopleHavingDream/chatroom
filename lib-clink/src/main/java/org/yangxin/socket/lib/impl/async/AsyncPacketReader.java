package org.yangxin.socket.lib.impl.async;

import org.yangxin.socket.lib.core.Frame;
import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.SendPacket;
import org.yangxin.socket.lib.core.ds.BytePriorityNode;
import org.yangxin.socket.lib.frames.AbstractSendPacketFrame;
import org.yangxin.socket.lib.frames.CancelSendFrame;
import org.yangxin.socket.lib.frames.SendEntityFrame;
import org.yangxin.socket.lib.frames.SendHeaderFrame;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author yangxin
 * 2021/9/15 下午9:04
 */
public class AsyncPacketReader implements Closeable {

    /**
     * 用于此异步包阅读者的包提供者
     */
    private final PacketProvider provider;

    /**
     * 底层数据结构：输入输出参数
     */
    private volatile IoArgs args = new IoArgs();

    /**
     * 帧队列
     */
    private volatile BytePriorityNode<Frame> node;

    /**
     * 当前帧链表的节点长度
     */
    private volatile int nodeSize = 0;

    /**
     * 1,2,3...255
     */
    private short lastIdentifier = 0;

    AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 请求从{@link #provider}队列中拿一份Packet进行发送，
     * 并不是返回一个用于发送的包，而是将包封装成发送帧，并将该帧置入队列
     *
     * @return 如果当前Reader中有可以用于网络发送的数据，则返回True
     */
    boolean requestTakePacket() {
        // 锁住当前实例
        synchronized (this) {
            // 如果当前帧队列长度大于1，则直接返回真
            if (nodeSize >= 1) {
                return true;
            }
        }

        // 从包提供者处拿到一个发送包
        SendPacket<?> packet = provider.takePacket();
        if (packet != null) {
            // 生成标识符
            short identifier = generateIdentifier();
            // 实例化一个发送头帧
            SendHeaderFrame frame = new SendHeaderFrame(identifier, packet);
            // 将发送头帧追加到帧链表中
            appendNewFrame(frame);
        }

        synchronized (this) {
            return nodeSize != 0;
        }
    }

    /**
     * 填充数据到输入输出参数中
     *
     * @return 如果当前有可用于发送的帧，则填充数据并返回，如果填充失败可返回null
     */
    IoArgs fillData() {
        // 生成当前帧
        Frame currentFrame = generateCurrentFrame();
        if (currentFrame == null) {
            return null;
        }

        try {
            // 如果当前帧已被处理完
            if (currentFrame.handle(args)) {
                // 消费完本帧
                // 尝试基于本帧构建后续帧
                Frame nextFrame = currentFrame.nextFrame();
                if (nextFrame != null) {
                    // 如果下一帧存在，则追加新帧
                    appendNewFrame(nextFrame);
                } else if (currentFrame instanceof SendEntityFrame) {
                    // 当前帧被处理完，且当前帧是发送实体帧，则当前帧是末尾实体帧
                    // 通知完成
                    provider.completedPacket(((SendEntityFrame) currentFrame).getPacket(), true);
                }

                // 从链头弹出
                popCurrentFrame();
            }

            // 返回输入输出参数
            return args;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 取消Packet对应的帧发送，
     * 如果当前Packet已发送部分数据（就算只是头数据），
     * 也应该在当前帧队列中发送一份取消发送的标志{@link org.yangxin.socket.lib.frames.CancelSendFrame}
     *
     * @param packet 待取消的packet
     */
    synchronized void cancel(SendPacket<?> packet) {
        if (nodeSize == 0) {
            return;
        }

        for (BytePriorityNode<Frame> x = node, before = null; x != null; before = x, x = x.next) {
            Frame frame = x.item;
            if (frame instanceof AbstractSendPacketFrame) {
                AbstractSendPacketFrame packetFrame = (AbstractSendPacketFrame) frame;
                if (packetFrame.getPacket() == packet) {
                    boolean removable = packetFrame.abort();
                    if (removable) {
                        // A B C
                        removeFrame(x, before);
                        if (packetFrame instanceof SendHeaderFrame) {
                            // 头帧，并且未被发送任何数据，直接取消后不需要添加取消发送帧
                            break;
                        }
                    }

                    // 添加终止帧，通知到接收方
                    CancelSendFrame cancelSendFrame = new CancelSendFrame(packetFrame.getBodyIdentifier());
                    appendNewFrame(cancelSendFrame);

                    // 意外终止，返回失败
                    provider.completedPacket(packet, false);

                    break;
                }
            }
        }
    }

    /**
     * 关闭当前Reader，关闭时应关闭所有Frame对应的Packet
     *
     * @throws IOException 关闭时出现的异常
     */
    @Override
    public synchronized void close() throws IOException {
        while (node != null) {
            Frame frame = node.item;
            if (frame instanceof AbstractSendPacketFrame) {
                SendPacket<?> packet = ((AbstractSendPacketFrame) frame).getPacket();
                provider.completedPacket(packet, false);
            }
        }

        nodeSize = 0;
        node = null;
    }

    /**
     * 追加一个新的帧
     *
     * @param frame 新帧
     */
    private synchronized void appendNewFrame(Frame frame) {
        // 实例化一个新的字节优先级节点
        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if (node != null) {
            // 使用优先级别添加到链表
            node.appendWithPriority(newNode);
        } else {
            node = newNode;
        }
        nodeSize++;
    }

    /**
     * 生成当前帧
     *
     * @return 帧
     */
    private synchronized Frame generateCurrentFrame() {
        // 说明帧队列为null，直接返回null
        if (node == null) {
            return null;
        }

        return node.item;
    }

    /**
     * 弹出当前帧
     */
    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if (node == null) {
            // 请求拿出一个包
            requestTakePacket();
        }
    }

    private synchronized void removeFrame(BytePriorityNode<Frame> removeNode, BytePriorityNode<Frame> before) {
        if (before == null) {
            // A B C
            // B C
            node = removeNode.next;
        } else {
            // A B C
            // A C
            before.next = removeNode.next;
        }

        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }

    /**
     * 生成标识符
     *
     * @return 标识符
     */
    private short generateIdentifier() {
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }

    /**
     * 包提供者
     */
    interface PacketProvider {

        /**
         * 拿到发送包
         *
         * @return 发送包
         */
        SendPacket<?> takePacket();

        /**
         * 完成包
         *
         * @param packet    发送包
         * @param isSucceed 是否成功
         */
        void completedPacket(SendPacket<?> packet, boolean isSucceed);
    }
}
