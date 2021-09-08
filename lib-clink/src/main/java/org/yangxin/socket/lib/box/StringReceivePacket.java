package org.yangxin.socket.lib.box;

import java.io.ByteArrayOutputStream;

/**
 * 字符串接收包
 *
 * @author yangxin
 * 2021/8/28 下午1:18
 */
public class StringReceivePacket extends AbstractByteArrayReceivePacket<String> {

    public StringReceivePacket(long length) {
        super(length);
    }

    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return stream.toString();
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
