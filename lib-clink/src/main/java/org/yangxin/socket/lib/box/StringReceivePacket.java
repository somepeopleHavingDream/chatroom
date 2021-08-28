package org.yangxin.socket.lib.box;

import org.yangxin.socket.lib.core.ReceivePacket;

/**
 * @author yangxin
 * 2021/8/28 下午1:18
 */
public class StringReceivePacket extends ReceivePacket {

    private final byte[] buffer;
    private int position;

    public StringReceivePacket(int length) {
        buffer = new byte[length];
        this.length = length;
    }

    @Override
    public void save(byte[] bytes, int count) {
        System.arraycopy(bytes, 0, buffer, position, count);
        position += count;
    }

    public String string() {
        return new String(buffer);
    }
}
