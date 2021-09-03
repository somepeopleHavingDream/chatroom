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

    /**
     * 用于接收的底层数据结构输入输出参数
     */
    private final IoArgs ioArgs = new IoArgs();

    /**
     * 临时的接收包
     */
    private ReceivePacket packetTemp;

    /**
     * 用于接收数据的字节数组
     */
    private byte[] buffer;

    /**
     * 总共需要接收多少个字节
     */
    private int total;

    /**
     * 当前接收到第几个字节
     */
    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {
        // 设置接收者
        this.receiver = receiver;

        // 设置接收监听器（用于处理IoArgs）
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
            receiver.postReceiveAsync(ioArgs);
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
        // 关闭此临时接收包
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);

        // 调用回调的完整接收包方法
        callback.onReceivePacketCompleted(packet);
    }

    private final IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {

        @Override
        public void onStarted(IoArgs args) {
            // 取得此次接收的字节大小
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
            // 从入参输入输出参数中解析组装成包
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
        // 如果临时接收包为null，说明当前的接收包是首包
        if (packetTemp == null) {
            // 获得当前包的总字节长度
            int length = args.readLength();

            // 实例化字符串接收包
            packetTemp = new StringReceivePacket(length);

            buffer = new byte[length];
            total = length;
            position = 0;
        }

        // 将接收到的数据从入参输入输出参数，写到当前底层字节缓冲区中
        int count = args.writeTo(buffer, 0);
        if (count > 0) {
            // 保存到接收包实例的底层缓冲数组中
            packetTemp.save(buffer, count);
            // 更新当前接收进度
            position += count;

            // 检查是否已完成一份Packet的接收
            if (position == total) {
                // 如果当前接收进度到达重点，则完成调用完成包方法，并置临时接收包的引用为null
                completePacket();
                packetTemp = null;
            }
        }
    }
}
