package org.yangxin.socket.lib.box;

import org.yangxin.socket.lib.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author yangxin
 * 2021/8/28 下午1:16
 */
public class StringSendPacket extends SendPacket<ByteArrayInputStream> {

    private final byte[] bytes;

    public StringSendPacket(String msg) {
        this.bytes = msg.getBytes();

        // 此length属性来自于Packet
        this.length = bytes.length;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        // 实例化并返回字节数组输入流
        return new ByteArrayInputStream(bytes);
    }
}
