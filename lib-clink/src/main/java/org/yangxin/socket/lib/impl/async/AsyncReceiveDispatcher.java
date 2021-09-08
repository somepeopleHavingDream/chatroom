package org.yangxin.socket.lib.impl.async;

import org.yangxin.socket.lib.core.*;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yangxin
 * 2021/8/29 下午2:39
 */
public class AsyncReceiveDispatcher implements ReceiveDispatcher, IoArgs.IoArgsEventProcessor {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    /**
     * 用于接收的底层数据结构输入输出参数
     */
    private final IoArgs ioArgs = new IoArgs();

    /**
     * 临时的接收包
     */
    private ReceivePacket<?, ?> packetTemp;

    private WritableByteChannel writablePacketChannel;

    /**
     * 总共需要接收多少个字节
     */
    private long total;

    /**
     * 当前接收到第几个字节
     */
    private long position;

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
            completePacket(false);
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    private void registerReceive() {
        try {
            // 异步接收
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completePacket(boolean isSucceed) {
        // 关闭此临时接收包
        ReceivePacket<?, ?> packet = this.packetTemp;
        CloseUtils.close(packet);
        packetTemp = null;

        WritableByteChannel channel = this.writablePacketChannel;
        CloseUtils.close(channel);
        writablePacketChannel = null;

        // 调用回调的完整接收包方法
        if (packet != null) {
            callback.onReceivePacketCompleted(packet);
        }
    }

    /**
     * 解析数据到Packet
     *
     * @param args 需要解析的数据
     */
    private void assemblePacket(IoArgs args) {
        // 如果临时接收包为null，说明当前的接收包是首包
        if (packetTemp == null) {
            // 获得当前包的总字节长度
            int length = args.readLength();
            byte type = length > 200 ? Packet.TYPE_STREAM_FILE : Packet.TYPE_MEMORY_STRING;

            // 实例化字符串接收包
            packetTemp = callback.onArrivedNewPacket(type, length);
            writablePacketChannel = Channels.newChannel(packetTemp.open());

            total = length;
            position = 0;
        }

        // 将接收到的数据从入参输入输出参数，写到当前底层字节缓冲区中
        try {
            int count = args.writeTo(writablePacketChannel);
            position += count;

            // 检查是否已完成一份Packet的接收
            if (position == total) {
                // 如果当前接收进度到达重点，则完成调用完成包方法，并置临时接收包的引用为null
                completePacket(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            completePacket(false);
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs args = ioArgs;

        int receiveSize;
        if (packetTemp == null) {
            // 如果当前接收的包为null，说明即将要接收了一个新包，先读取包长度
            receiveSize = 4;
        } else {
            receiveSize = (int) Math.min(total - position, args.capacity());
        }

        // 设置本次接收数据大小
        args.limit(receiveSize);

        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        assemblePacket(args);
        registerReceive();
    }
}
