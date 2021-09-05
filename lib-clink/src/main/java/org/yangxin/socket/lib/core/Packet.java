package org.yangxin.socket.lib.core;

import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;

/**
 * 公共数据的封装，
 * 提供了类型以及基本的长度定义
 *
 * @author yangxin
 * 2021/8/28 下午1:06
 */
public abstract class Packet<T extends Closeable> implements Closeable {

    private byte type;

    /**
     * 包的长度
     */
    protected long length;

    /**
     * 流
     */
    private T stream;

    public byte type() {
        return type;
    }

    /**
     * 返回包的长度
     *
     * @return 包的长度
     */
    public long length() {
        return length;
    }

    /**
     * 打开一个流
     *
     * @return 流
     */
    public final T open() {
        if (stream == null) {
            // 如果流为null，则创建一个流
            stream = createStream();
        }

        // 返回流
        return stream;
    }

    @Override
    public final void close() throws IOException {
        if (stream != null) {
            closeStream(stream);
            stream = null;
        }
    }

    /**
     * 创建一个流
     *
     * @return 流
     */
    protected abstract T createStream();

    protected void closeStream(T stream) throws IOException {
        stream.close();
    }
}
