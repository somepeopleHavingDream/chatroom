package org.yangxin.socket.lib.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * 公共数据的封装，
 * 提供了类型以及基本的长度定义
 *
 * @author yangxin
 * 2021/8/28 下午1:06
 */
public abstract class Packet<Stream extends Closeable> implements Closeable {

    /**
     * Bytes类型
     */
    public static final byte TYPE_MEMORY_BYTES = 1;

    /**
     * String类型
     */
    public static final byte TYPE_MEMORY_STRING = 2;

    /**
     * 文件类型
     */
    public static final byte TYPE_STREAM_FILE = 3;

    /**
     * 长链接流类型
     */
    public static final byte TYPE_STREAM_DIRECT = 4;

    /**
     * 包的长度
     */
    protected long length;

    /**
     * 流
     */
    private Stream stream;

    /**
     * 返回包的长度
     *
     * @return 包的长度
     */
    public long length() {
        return length;
    }

    /**
     * 对外的获取当前实例的流操作。
     * 打开一个流。
     *
     * @return 流
     */
    public final Stream open() {
        if (stream == null) {
            // 如果流为null，则创建一个流
            stream = createStream();
        }

        // 返回流
        return stream;
    }

    /**
     * 对外的关闭资源操作，如果流处于打开状态应当进行关闭。
     *
     * @throws IOException 输入输出异常
     */
    @Override
    public final void close() throws IOException {
        if (stream != null) {
            closeStream(stream);
            stream = null;
        }
    }

    /**
     * 类型，直接通过方法得到：
     * <p>
     *     {@link #TYPE_MEMORY_BYTES}
     *     {@link #TYPE_MEMORY_STRING}
     *     {@link #TYPE_STREAM_FILE}
     *     {@link #TYPE_STREAM_DIRECT}
     * </p>
     *
     * @return 类型
     */
    public abstract byte type();

    /**
     * 创建流操作，应当将当前需要传输的数据转化为流。
     * 创建一个流。
     *
     * @return 流
     */
    protected abstract Stream createStream();

    /**
     * 关闭流，当前方法会调用流的关闭操作
     *
     * @param stream 待关闭的流
     * @throws IOException IO异常
     */
    protected void closeStream(Stream stream) throws IOException {
        stream.close();
    }

    /**
     * 头部额外信息，用于携带额外的校验信息等
     *
     * @return byte数组，最大255长度
     */
    public byte[] headerInfo() {
        return null;
    }
}