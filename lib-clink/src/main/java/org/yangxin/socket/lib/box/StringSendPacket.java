package org.yangxin.socket.lib.box;

import org.yangxin.socket.lib.core.SendPacket;

/**
 * @author yangxin
 * 2021/8/28 下午1:16
 */
public class StringSendPacket extends SendPacket {

    private final byte[] bytes;

    public StringSendPacket(String msg) {
        this.bytes = msg.getBytes();
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }
}
