package org.yangxin.socket.lib.frames;

import org.yangxin.socket.lib.core.Frame;
import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * @author yangxin
 * 2021/9/15 下午9:07
 */
public class SendEntityFrame extends AbstractSendPacketFrame {

    private final ReadableByteChannel channel;
    private final long unConsumeEntityLength;

    SendEntityFrame(short identifier,
                    long entityLength,
                    ReadableByteChannel channel,
                    SendPacket<?> packet) {
        super((int) Math.min(entityLength, Frame.MAX_CAPACITY),
                Frame.TYPE_PACKET_ENTITY,
                Frame.FLAG_NONE,
                identifier,
                packet);

        // 1234567890
        // 1234 5678 90
        // 10 4,6 4,2 2

        // 设置未消费的实体长度
        unConsumeEntityLength = entityLength - bodyRemaining;
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        if (packet == null) {
            // 已终止当前帧，则填充假数据
            return args.fillEmpty(bodyRemaining);
        }

        return args.readFrom(channel);
    }

    @Override
    public Frame buildNextFrame() {
        if (unConsumeEntityLength == 0) {
            return null;
        }

        return new SendEntityFrame(getBodyIdentifier(),
                unConsumeEntityLength, channel, packet);
    }
}
