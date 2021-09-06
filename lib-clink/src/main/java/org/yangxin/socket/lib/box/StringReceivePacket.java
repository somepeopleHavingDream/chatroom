package org.yangxin.socket.lib.box;

import org.yangxin.socket.lib.core.ReceivePacket;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

/**
 * 字符串接收包
 *
 * @author yangxin
 * 2021/8/28 下午1:18
 */
public class StringReceivePacket extends AbstractByteArrayReceivePacket<String> {

    public StringReceivePacket(int length) {
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
