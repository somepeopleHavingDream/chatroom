package org.yangxin.socket.lib.impl.async;

import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.SendDispatcher;
import org.yangxin.socket.lib.core.SendPacket;
import org.yangxin.socket.lib.core.Sender;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yangxin
 * 2021/8/28 下午10:12
 */
public class AsyncSendDispatcher implements SendDispatcher,
        IoArgs.IoArgsEventProcessor,
        AsyncPacketReader.PacketProvider {

    /**
     * 实际用于发送数据的发送者
     */
    private final Sender sender;

    /**
     * 存储发送包的队列
     */
    private final Queue<SendPacket<?>> queue = new ConcurrentLinkedQueue<>();

    /**
     * 当前异步发送调度者是否处于发送状态中
     */
    private final AtomicBoolean isSending = new AtomicBoolean();

    /**
     * 当前异步发送调度者是否处于关闭状态中
     */
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * 用于此异步发送调度者的异步包阅读者
     */
    private final AsyncPacketReader reader = new AsyncPacketReader(this);

    /**
     * 队列锁
     */
    private final Object queueLock = new Object();

    public AsyncSendDispatcher(Sender sender) {
        // 设置发送者
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket<?> packet) {
        // 拿到队列锁
        synchronized (queueLock) {
            // 将发送包入队
            queue.offer(packet);

            // 设置发送状态，并实际地发送下一个包
            if (isSending.compareAndSet(false, true)) {
                // 向阅读者请求拿出一个包，用于发送
                if (reader.requestTakePacket()) {
                    // 请求发送
                    requestSend();
                }
            }

        }
    }

    @Override
    public void cancel(SendPacket<?> packet) {
        boolean ret;
        synchronized (queueLock) {
            ret = queue.remove(packet);
        }
        if (ret) {
            packet.cancel();
            return;
        }

        reader.cancel(packet);
    }

    /**
     * 取出一个包，用于发送
     *
     * @return 发送包
     */
    @Override
    public SendPacket<?> takePacket() {
        // 记录发送包
        SendPacket<?> packet;

        synchronized (queueLock) {
            // 从发送队列中取出一个发送包
            packet = queue.poll();
            if (packet == null) {
                // 队列为空，取消发送状态，并返回null
                isSending.set(false);
                return null;
            }
        }

        // 已经取出来了一个发送包，查看该发送包是否被取消
        if (packet.isCanceled()) {
            // 已取消，不用发送，继续拿下一个包用于发送
            return takePacket();
        }

        // 返回取出来的一个发送包
        return packet;
    }

    /**
     * 完成Packet发送
     *
     * @param isSucceed 是否成功
     */
    @Override
    public void completedPacket(SendPacket<?> packet, boolean isSucceed) {
        // 关闭此包
        CloseUtils.close(packet);
    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {
        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            // reader关闭
            reader.close();
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        // 阅读者填充数据
        return reader.fillData();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        if (args != null) {
            e.printStackTrace();
        } else {
            // todo
        }
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        // 继续发送当前包
        if (reader.requestTakePacket()) {
            // 请求发送
            requestSend();
        }
    }
}
