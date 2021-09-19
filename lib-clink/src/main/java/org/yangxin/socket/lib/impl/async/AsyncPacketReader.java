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

    private final PacketProvider provider;
    private volatile IoArgs args = new IoArgs();

    /**
     * Frame队列
     */
    private volatile BytePriorityNode<Frame> node;

    private volatile int nodeSize = 0;

    /**
     * 1,2,3...255
     */
    private short lastIdentifier = 0;

    AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 请求从{@link #provider}队列中拿一份Packet进行发送
     *
     * @return 如果当前Reader中有可以用于网络发送的数据，则返回True
     */
    boolean requestTakePacket() {
        synchronized (this) {
            if (nodeSize >= 1) {
                return true;
            }
        }

        SendPacket<?> packet = provider.takePacket();
        if (packet != null) {
            short identifier = generateIdentifier();
            SendHeaderFrame frame = new SendHeaderFrame(identifier, packet);
            appendNewFrame(frame);
        }

        synchronized (this) {
            return nodeSize != 0;
        }
    }

    /**
     * 填充数据到IoArgs中
     *
     * @return 如果当前有可用于发送的帧，则填充数据并返回，如果填充失败可返回null
     */
    IoArgs fillData() {
        Frame currentFrame = generateCurrentFrame();
        if (currentFrame == null) {
            return null;
        }

        try {
            if (currentFrame.handle(args)) {
                // 消费完本帧
                // 尝试基于本帧构建后续帧
                Frame nextFrame = currentFrame.nextFrame();
                if (nextFrame != null) {
                    appendNewFrame(nextFrame);
                } else if (currentFrame instanceof SendEntityFrame) {
                    // 末尾实体帧
                    // 通知完成
                    provider.completedPacket(((SendEntityFrame) currentFrame).getPacket(), true);
                }

                // 从链头弹出
                popCurrentFrame();
            }

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

    private synchronized void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if (node != null) {
            // 使用优先级别添加到链表
            node.appendWithPriority(newNode);
        } else {
            node = newNode;
        }
        nodeSize++;
    }

    private synchronized Frame generateCurrentFrame() {
        if (node == null) {
            return null;
        }
        return node.item;
    }

    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if (node == null) {
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

    private short generateIdentifier() {
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }

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
