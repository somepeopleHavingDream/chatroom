package org.yangxin.socket.lib.frames;

import org.yangxin.socket.lib.core.Frame;
import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.SendPacket;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Packet头帧
 *
 * @author yangxin
 * 2021/9/15 下午9:07
 */
public class SendHeaderFrame extends AbstractSendPacketFrame {

    static final int PACKET_HEADER_FRAME_MIN_LENGTH = 6;
    private final byte[] body;

    public SendHeaderFrame(short identifier, SendPacket<?> packet) {
        super(PACKET_HEADER_FRAME_MIN_LENGTH,
                Frame.TYPE_PACKET_HEADER,
                Frame.FLAG_NONE,
                identifier,
                packet);

        final long packetLength = packet.length();
        final byte packetType = packet.type();
        final byte[] packetHeaderInfo = packet.headerInfo();

        // 头部对应的数据信息长度
        body = new byte[bodyRemaining];

        // 头5字节存储长度信息低5字节（40位）数据
        // 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000
        body[0] = (byte) (packetLength >> 32);
        body[1] = (byte) (packetLength >> 24);
        body[2] = (byte) (packetLength >> 16);
        body[3] = (byte) (packetLength >> 8);
        body[4] = (byte) (packetLength);

        body[5] = packetType;

        if (packetHeaderInfo != null) {
            System.arraycopy(packetHeaderInfo, 0, body, PACKET_HEADER_FRAME_MIN_LENGTH, packetHeaderInfo.length);
        }
    }

    @Override
    protected int consumeBody(IoArgs args) {
        int count = bodyRemaining;
        int offset = body.length - count;

        return args.readFrom(body, offset, count);
    }

    @Override
    public Frame buildNextFrame() {
        // 从包处，打开一个输入流
        InputStream stream = packet.open();
        // 通过输入流，打开一个可读字节通道
        ReadableByteChannel channel = Channels.newChannel(stream);

        // 实例化一个发送实体帧
        return new SendEntityFrame(getBodyIdentifier(), packet.length(), channel, packet);
    }
}
