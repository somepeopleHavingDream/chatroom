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
public class AsyncSendDispatcher implements SendDispatcher {

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
    private SendPacket packetTemp;

    /**
     * 当前发送的packet的大小
     */
    private int total;

    /**
     * 当前发送的packet的进度
     */
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        // 设置发送者
        this.sender = sender;
    }

    @Override
    public void send(SendPacket packet) {
        // 将发送包入队
        queue.offer(packet);

        // 设置发送状态，并实际地发送下一个包
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

        total = packet.length();
        position = 0;

        // 发送当前包
        sendCurrentPacket();
    }

    /**
     * 发送当前包
     */
    private void sendCurrentPacket() {
        IoArgs args = ioArgs;

        // 开始，清理
        args.startWriting();

        if (position >= total) {
            // 说明已经写完了一个包，接着发送下一个包
            sendNextPacket();
            return;
        } else if (position == 0) {
            // 首包，需要携带长度信息
            args.writeLength(total);
        }

        byte[] bytes = packetTemp.bytes();
        // 把bytes的数据写入到IoArgs
        int count = args.readFrom(bytes, position);

        // 更新当前位置
        position += count;

        // 完成封装
        args.finishWriting();

        try {
            // 异步发送（提供了一个回调，最终交给写线程池执行）
            sender.sendAsync(args, ioArgsEventListener);
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
            SendPacket packet = this.packetTemp;
            if (packet != null) {
                packetTemp = null;
                CloseUtils.close(packet);
            }
        }
    }

    private final IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {

        @Override
        public void onStarted(IoArgs args) {
            // 暂且是空实现
        }

        @Override
        public void onCompleted(IoArgs args) {
            // 继续发送当前包
            sendCurrentPacket();
        }
    };
}
