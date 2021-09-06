package org.yangxin.socket.lib.core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 接收包的定义
 *
 * @author yangxin
 * 2021/8/28 下午1:13
 */
public abstract class ReceivePacket<Stream extends OutputStream, Entity> extends Packet<Stream> {

    /**
     * 定义当前接收包最终的实体
     */
    private Entity entity;

    public ReceivePacket(long length) {
        this.length = length;
    }

    /**
     * 得到最终接收到的数据实体
     *
     * @return 数据实体
     */
    public Entity entity() {
        return entity;
    }

    /**
     * 根据接收到的流转化为对应的实体
     *
     * @param stream {@link OutputStream}
     * @return 实体
     */
    protected abstract Entity buildEntity(Stream stream);

    /**
     * 先关闭流，随后将流的内容转化为对应的实体
     *
     * @param stream 待关闭的流
     * @throws IOException IO异常
     */
    @Override
    protected void closeStream(Stream stream) throws IOException {
        super.closeStream(stream);
        entity = buildEntity(stream);
    }
}
