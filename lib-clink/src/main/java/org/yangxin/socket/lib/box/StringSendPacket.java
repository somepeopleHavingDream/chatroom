package org.yangxin.socket.lib.box;

import org.yangxin.socket.lib.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * 字符串发送包
 *
 * @author yangxin
 * 2021/8/28 下午1:16
 */
public class StringSendPacket extends BytesSendPacket {

    /**
     * 字符串发送时就是Byte数组，所以直接得到Byte数组，并按照Byte的发送方式发送即可。
     *
     * @param msg 字符串
     */
    public StringSendPacket(String msg) {
        super(msg.getBytes());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
