package org.yangxin.socket.lib.impl.async;

import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.ReceiveDispatcher;
import org.yangxin.socket.lib.core.ReceivePacket;
import org.yangxin.socket.lib.core.Receiver;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yangxin
 * 2021/8/29 下午2:39
 */
public class AsyncReceiveDispatcher implements ReceiveDispatcher,
        IoArgs.IoArgsEventProcessor, AsyncPacketWriter.PacketProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private final AsyncPacketWriter writer = new AsyncPacketWriter(this);

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {
        // 设置接收者
        this.receiver = receiver;

        // 设置接收监听器（用于处理IoArgs）
        this.receiver.setReceiveListener(this);
        // 设置接收包回调
        this.callback = callback;
    }

    @Override
    public void start() {
        // 注册接收
        registerReceive();
    }

    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            writer.close();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    /**
     * 注册接收
     */
    private void registerReceive() {
        try {
            // 异步接收
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        return writer.takeIoArgs();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        do {
            writer.consumeIoArgs(args);
        } while (args.remained());

        registerReceive();
    }

    @Override
    public ReceivePacket<?, ?> takePacket(byte type, long length, byte[] headerInfo) {
        return callback.onArrivedNewPacket(type, length);
    }

    @Override
    public void completedPacket(ReceivePacket<?, ?> packet, boolean isSucceed) {
        CloseUtils.close(packet);
        callback.onReceivePacketCompleted(packet);
    }
}
