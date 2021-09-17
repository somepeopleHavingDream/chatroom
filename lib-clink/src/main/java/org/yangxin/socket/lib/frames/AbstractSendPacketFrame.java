package org.yangxin.socket.lib.frames;

import org.yangxin.socket.lib.core.SendPacket;

/**
 * @author yangxin
 * 2021/9/17 下午8:24
 */
public abstract class AbstractSendPacketFrame extends AbstractSendFrame {

    protected SendPacket<?> packet;

    public AbstractSendPacketFrame(int length, byte type, byte flag, short identifier, SendPacket<?> packet) {
        super(length, type, flag, identifier);
        this.packet = packet;
    }
}
