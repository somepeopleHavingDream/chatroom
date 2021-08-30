package org.yangxin.socket.lib.impl.async;

import org.yangxin.socket.lib.box.StringReceivePacket;
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
public class AsyncReceiveDispatcher implements ReceiveDispatcher {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private final IoArgs ioArgs = new IoArgs();
    private ReceivePacket packetTemp;
    private byte[] buffer;
    private int total;
    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {
        // 设置接收者
        this.receiver = receiver;
        // 设置接收监听器
        this.receiver.setReceiveListener(ioArgsEventListener);
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

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    private void registerReceive() {
        try {
            // 异步接收
            receiver.receiveAsync(ioArgs);
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            ReceivePacket packet = packetTemp;
            if (packet != null) {
                packetTemp = null;
                CloseUtils.close(packet);
            }
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completePacket() {
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);
        callback.onReceivePacketCompleted(packet);
    }

    private final IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {

        @Override
        public void onStarted(IoArgs args) {
            int receiveSize;
            if (packetTemp == null) {
                receiveSize = 4;
            } else {
                receiveSize = Math.min(total - position, args.capacity());
            }

            // 设置本次接收数据大小
            args.limit(receiveSize);
        }

        @Override
        public void onCompleted(IoArgs args) {
            assemblePacket(args);
            // 继续接收下一条数据
            registerReceive();
        }
    };

    /**
     * 解析数据到Packet
     *
     * @param args 需要解析的数据
     */
    private void assemblePacket(IoArgs args) {
        if (packetTemp == null) {
            int length = args.readLength();
            packetTemp = new StringReceivePacket(length);
            buffer = new byte[length];
            total = length;
            position = 0;
        }

        int count = args.writeTo(buffer, 0);
        if (count > 0) {
            packetTemp.save(buffer, count);
            position += count;

            // 检查是否已完成一份Packet的接收
            if (position == total) {
                completePacket();
                packetTemp = null;
            }
        }
    }
}
