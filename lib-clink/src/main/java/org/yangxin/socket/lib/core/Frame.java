package org.yangxin.socket.lib.core;

import java.io.IOException;

/**
 * @author yangxin
 * 2021/9/15 下午9:03
 */
public abstract class Frame {

    public static final int FRAME_HEADER_LENGTH = 6;
    public static final int MAX_CAPACITY = 64 * 1024 - 1;

    public static final byte TYPE_PACKET_HEADER = 11;
    public static final byte TYPE_PACKET_ENTITY = 12;

    public static final byte TYPE_COMMAND_SEND_CANCEL = 41;
    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;

    public static final byte FLAG_NONE = 0;

    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];

    public Frame(int length, byte type, byte flag, short identifier) {
        if (length < 0 || length > MAX_CAPACITY) {
            throw new RuntimeException("");
        }

        if (identifier < 1 || identifier > 255) {
            throw new RuntimeException("");
        }

        // 00000000 00000000 00000000 00000000
        header[0] = (byte) (length >> 8);
        header[1] = (byte) length;

        header[2] = type;
        header[3] = flag;

        header[4] = (byte) identifier;
        header[5] = 0;
    }

    public int getBodyLength() {
        return ((((int) header[0]) & 0xff) << 8) | (((int) header[1]) & 0xff);
    }

    public byte getBodyType() {
        return header[2];
    }

    public short getBodyIdentifier() {
        return (short) (((short) header[4]) & 0xff);
    }

    public abstract boolean handle(IoArgs args) throws IOException;

    public abstract Frame nextFrame();
}
