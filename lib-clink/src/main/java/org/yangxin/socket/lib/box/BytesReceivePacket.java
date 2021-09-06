package org.yangxin.socket.lib.box;

import java.io.ByteArrayOutputStream;

/**
 * 纯Byte数组接收包
 *
 * @author yangxin
 * 2021/9/6 下午9:04
 */
public class BytesReceivePacket extends AbstractByteArrayReceivePacket<byte[]> {

    public BytesReceivePacket(long length) {
        super(length);
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_BYTES;
    }

    @Override
    protected byte[] buildEntity(ByteArrayOutputStream stream) {
        return stream.toByteArray();
    }
}
