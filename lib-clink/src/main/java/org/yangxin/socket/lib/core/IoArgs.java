package org.yangxin.socket.lib.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author yangxin
 * 2021/8/24 11:35
 */
public class IoArgs {

    private int limit = 256;
    private final byte[] byteBuffer = new byte[256];
    private final ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    /**
     * 从bytes中读取数据
     *
     * @param bytes 字节数组
     * @param offset 起始偏移
     * @return 读了多少个字节
     */
    public int readFrom(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 写数据到bytes中
     *
     * @param bytes 字节数组
     * @param offset 起始偏移
     * @return 写入了多少个字节
     */
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    /**
     * 从通道中读数据
     *
     * @param channel 通道
     * @return 读了多少个字节
     * @throws IOException 输入输出异常
     */
    public int readFrom(SocketChannel channel) throws IOException {
        startWriting();

        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int length = channel.read(buffer);
            if (length < 0) {
                throw new EOFException();
            }
            bytesProduced += length;
        }

        finishWriting();
        return bytesProduced;
    }

    /**
     * 写数据到通道中
     *
     * @param channel 通道
     * @return 写了多少个字节
     * @throws IOException 输入输出异常
     */
    public int writeTo(SocketChannel channel) throws IOException {
        int bytesConsumed = 0;
        while (buffer.hasRemaining()) {
            int length = channel.write(buffer);
            if (length < 0) {
                throw new EOFException();
            }
            bytesConsumed += length;
        }
        return bytesConsumed;
    }

    /**
     * 开始写入数据到IoArgs
     */
    private void startWriting() {
        buffer.clear();
        // 定义容纳区间
        buffer.limit(limit);
    }

    /**
     * 写完数据后调用
     */
    private void finishWriting() {
        buffer.flip();
    }

    /**
     * 设置单次写操作的容纳区间
     *
     * @param limit 区间大小
     */
    public void limit(int limit) {
        this.limit = limit;
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
