package org.yangxin.socket.lib.frames;

import org.yangxin.socket.lib.core.Frame;
import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.SendPacket;

import java.nio.channels.ReadableByteChannel;

/**
 * @author yangxin
 * 2021/9/15 下午9:07
 */
public class SendEntityFrame extends AbstractSendPacketFrame {

    public SendEntityFrame(short identifier,
                           long entityLength,
                           ReadableByteChannel channel,
                           SendPacket<?> packet) {
        super((int) Math.min(entityLength, Frame.MAX_CAPACITY),
                Frame.TYPE_PACKET_ENTITY,
                Frame.FLAG_NONE,
                identifier,
                packet);
    }

    @Override
    public Frame nextFrame() {
        return null;
    }

    @Override
    protected int consumeBody(IoArgs args) {
        return 0;
    }
}
