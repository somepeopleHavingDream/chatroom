package org.yangxin.socket.lib.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * @author yangxin
 * 2021/8/24 11:35
 */
@SuppressWarnings("DuplicatedCode")
public class IoArgs {

    /**
     * 初始容量为256个字节
     */
    private int limit = 256;

    /**
     * 底层字节缓冲实例
     */
    private final ByteBuffer buffer = ByteBuffer.allocate(limit);

    /**
     * 从bytes数组进行消费
     */
    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count, buffer.remaining());
        if (size <= 0) {
            return 0;
        }
        buffer.put(bytes, offset, size);

        return size;
    }

    /**
     * 写入数据到bytes中
     */
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    /**
     * 从可读字节通道中读取数据
     */
    public int readFrom(ReadableByteChannel channel) throws IOException {
        // 开始写数据到底层缓冲
        startWriting();

        int bytes = 0;
        while (buffer.hasRemaining()) {
            // 从通道中读取字节到底层缓冲
            int length = channel.read(buffer);
            if (length < 0) {
                throw new EOFException();
            }
            bytes += length;
        }

        // 结束写数据到底层缓冲
        finishWriting();

        // 返回写入了多少个字节
        return bytes;
    }

    /**
     * 写数据到bytes中
     */
    public int writeTo(WritableByteChannel channel) throws IOException {
        int bytes = 0;
        while (buffer.hasRemaining()) {
            int length = channel.write(buffer);
            if (length < 0) {
                throw new EOFException();
            }
            bytes += length;
        }
        return bytes;
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
        int bytes = 0;
        while (buffer.hasRemaining()) {
            int length = channel.write(buffer);
            if (length < 0) {
                throw new EOFException();
            }
            bytes += length;
        }
        return bytes;
    }

    /**
     * 开始写入数据到IoArgs
     */
    public void startWriting() {
        // 清理缓冲
        buffer.clear();
        // 定义容纳区间
        buffer.limit(limit);
    }

    /**
     * 写完数据后调用，用于读写
     */
    public void finishWriting() {
        // 缓冲区翻转
        buffer.flip();
    }

    /**
     * 设置单次写操作的容纳区间
     *
     * @param limit 区间大小
     */
    public void limit(int limit) {
        this.limit = Math.min(limit, buffer.capacity());
    }

    public void writeLength(int total) {
        // 开始写
        startWriting();

        // 放入长度
        buffer.putInt(total);

        // 结束写
        finishWriting();
    }

    public int readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public boolean remained() {
        return buffer.remaining() > 0;
    }

    /**
     * IoArgs提供者、处理者；
     * 数据的生产或消费者
     */
    public interface IoArgsEventProcessor {

        /**
         * 提供一份可消费的输入输出参数
         *
         * @return IoArgs
         */
        IoArgs provideIoArgs();

        /**
         * 消费失败时的回调
         *
         * @param args IoArgs
         * @param e 异常信息
         */
        void onConsumeFailed(IoArgs args, Exception e);

        /**
         * 消费成功的回调
         *
         * @param args IoArgs
         */
        void onConsumeCompleted(IoArgs args);
    }
}
