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

    private final Sender sender;
    private final Queue<SendPacket<?>> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final AsyncPacketReader reader = new AsyncPacketReader(this);
    private final Object queueLock = new Object();

    public AsyncSendDispatcher(Sender sender) {
        // 设置发送者
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket<?> packet) {
        synchronized (queueLock) {
            // 将发送包入队
            queue.offer(packet);

            // 设置发送状态，并实际地发送下一个包（isSending状态在发送完一个包后未关闭）
            if (isSending.compareAndSet(false, true)) {
                if (reader.requestTakePacket()) {
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
        SendPacket<?> packet;

        synchronized (queueLock) {
            // 从发送队列中取出一个发送包
            packet = queue.poll();
            if (packet == null) {
                // 队列为空，取消发送状态
                isSending.set(false);
                return null;
            }
        }

        if (packet.isCanceled()) {
            // 已取消，不用发送
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
            requestSend();
        }
    }
}
