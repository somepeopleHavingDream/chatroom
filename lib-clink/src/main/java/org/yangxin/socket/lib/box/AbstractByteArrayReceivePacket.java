package org.yangxin.socket.lib.box;

import org.yangxin.socket.lib.core.ReceivePacket;

import java.io.ByteArrayOutputStream;

/**
 * 定义最基础的基于{@link ByteArrayOutputStream}的输出接收包
 *
 * @param <Entity> 对应的实体规范，需定义{@link ByteArrayOutputStream}流最终转化为什么数据实体
 *
 * @author yangxin
 * 2021/9/6 下午8:50
 */
public abstract class AbstractByteArrayReceivePacket<Entity> extends ReceivePacket<ByteArrayOutputStream, Entity> {

    public AbstractByteArrayReceivePacket(long length) {
        super(length);
    }

    /**
     * 创建流操作，直接返回一个@{link ByteArrayOutputStream}流
     *
     * @return {@link ByteArrayOutputStream}
     */
    @Override
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }
}
