package org.yangxin.socket.lib.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author yangxin
 * 2021/8/24 11:35
 */
public class IoArgs {

    private final byte[] byteBuffer = new byte[256];
    private final ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    /**
     * 从通道中读数据
     *
     * @param channel 通道
     * @return 读了多少个字节
     * @throws IOException 输入输出异常
     */
    public int read(SocketChannel channel) throws IOException {
        buffer.clear();
        return channel.read(buffer);
    }

    /**
     * 写数据到通道中
     *
     * @param channel 通道
     * @return 写了多少个字节
     * @throws IOException 输入输出异常
     */
    public int write(SocketChannel channel) throws IOException {
        return channel.write(buffer);
    }

    public String bufferString() {
        // 丢弃换行符
        return new String(byteBuffer, 0, buffer.position() - 1);
    }

    public interface IoArgsEventListener {

        /**
         * 启动时
         *
         * @param args io参数
         */
        void onStarted(IoArgs args);

        /**
         * 完成时
         *
         * @param args io参数
         */
        void onCompleted(IoArgs args);
    }
}
