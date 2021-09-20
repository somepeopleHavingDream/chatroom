package org.yangxin.socket.lib.box;

import org.yangxin.socket.lib.core.SendPacket;

import java.io.ByteArrayInputStream;

/**
 * 纯Byte数组发送包
 *
 * @author yangxin
 * 2021/9/6 下午9:06
 */
public class BytesSendPacket extends SendPacket<ByteArrayInputStream> {

    /**
     * 用于发送的字节数组
     */
    private final byte[] bytes;

    public BytesSendPacket(byte[] bytes) {
        // 设置需要发送的字节数组和字节数组的长度
        this.bytes = bytes;
        this.length = bytes.length;
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_BYTES;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }
}
