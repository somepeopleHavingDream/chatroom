package org.yangxin.socket.lib.impl.async;

import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.SendDispatcher;
import org.yangxin.socket.lib.core.SendPacket;
import org.yangxin.socket.lib.core.Sender;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yangxin
 * 2021/8/28 下午10:12
 */
public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventProcessor {

    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * 数据将从包的抽象转成输入输出参数的抽象
     */
    private final IoArgs ioArgs = new IoArgs();

    /**
     * 当前处理的发送包
     */
    private SendPacket<?> packetTemp;

    private ReadableByteChannel packetChannel;

    /**
     * 当前发送的packet的大小
     */
    private long total;

    /**
     * 当前发送的packet的进度
     */
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        // 设置发送者
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {
        // 将发送包入队
        queue.offer(packet);

        // 设置发送状态，并实际地发送下一个包（isSending状态在发送完一个包后未关闭）
        if (isSending.compareAndSet(false, true)) {
            sendNextPacket();
        }
    }

    @Override
    public void cancel(SendPacket packet) {

    }

    /**
     * 取出一个包，用于发送
     *
     * @return 发送包
     */
    private SendPacket takePacket() {
        // 从发送队列中取出一个发送包
        SendPacket packet = queue.poll();
        if (packet != null && packet.isCanceled()) {
            // 已取消，不用发送
            return takePacket();
        }

        // 返回取出来的一个发送包
        return packet;
    }

    private void sendNextPacket() {
        // 如果上一个发送包因为某些原因未被发送完，则关闭上一个包
        SendPacket temp = packetTemp;
        if (temp != null) {
            CloseUtils.close(temp);
        }

        // 取出一个发送包
        SendPacket packet = takePacket();
        packetTemp = packet;
        if (packet == null) {
            // 队列为空，取消状态发送，然后返回
            isSending.set(false);
            return;
        }

        // 当前发送包的总字节数
        total = packet.length();
        // 当前包发送到第几个字节
        position = 0;

        // 发送当前包
        sendCurrentPacket();
    }

    /**
     * 发送当前包
     */
    private void sendCurrentPacket() {
        if (position >= total) {
            completePacket(position == total);
            sendNextPacket();
            return;
        }

        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    /**
     * 完成Packet发送
     *
     * @param isSucceed 是否成功
     */
    private void completePacket(boolean isSucceed) {
        SendPacket packet = this.packetTemp;
        if (packet == null) {
            return;
        }

        CloseUtils.close(packet);
        CloseUtils.close(packetChannel);

        packetTemp = null;
        packetChannel = null;
        total = 0;
        position = 0;
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            // 异常关闭导致的完成
            completePacket(false);
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs args = ioArgs;
        if (packetChannel == null) {
            packetChannel = Channels.newChannel(packetTemp.open());
            args.limit(4);
            args.writeLength((int) packetTemp.length());
        } else {
            args.limit((int) Math.min(args.capacity(), total - position));

            try {
                int count = args.readFrom(packetChannel);
                position += count;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        // 继续发送当前包
        sendCurrentPacket();
    }
}
