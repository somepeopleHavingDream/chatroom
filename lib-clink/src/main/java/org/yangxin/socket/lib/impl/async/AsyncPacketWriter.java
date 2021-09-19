package org.yangxin.socket.lib.impl.async;

import org.yangxin.socket.lib.core.Frame;
import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.ReceivePacket;
import org.yangxin.socket.lib.frames.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 写数据到Packet中
 *
 * @author yangxin
 * 2021/9/15 下午9:04
 */
class AsyncPacketWriter implements Closeable {

    private final PacketProvider provider;

    private final Map<Short, PacketModel> packetMap = new HashMap<>();
    private final IoArgs args = new IoArgs();
    private volatile Frame frameTemp;

    public AsyncPacketWriter(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 构建一份数据容纳封装，
     * 当前帧如果没有则返回至少6字节长度的IoArgs，
     * 如果当前帧有，则返回当前帧未消费完成的区间
     *
     * @return 输入输出参数
     */
    synchronized IoArgs takeIoArgs() {
        args.limit(frameTemp == null
        ? Frame.FRAME_HEADER_LENGTH
                : frameTemp.getConsumableLength());
        return args;
    }

    /**
     * 消费输入输出参数中的数据
     *
     * @param args 输入输出参数
     */
    synchronized void consumeIoArgs(IoArgs args) {
        if (frameTemp == null) {
            Frame temp;
            do {
                temp = buildNewFrame(args);
            } while (temp == null && args.remained());

            if (temp == null) {
                return;
            }

            frameTemp = temp;
            if (!args.remained()) {
                return;
            }
        }

        Frame currentFrame = frameTemp;
        do {
            try {
                if (currentFrame.handle(args)) {
                    if (currentFrame instanceof ReceiveHeaderFrame) {
                        ReceiveHeaderFrame headerFrame = (ReceiveHeaderFrame) currentFrame;
                        ReceivePacket<?, ?> packet = provider.takePacket(headerFrame.getPacketType(),
                                headerFrame.getPacketLength(),
                                headerFrame.getPacketHeaderInfo());
                        appendNewPacket(headerFrame.getBodyIdentifier(), packet);
                    } else if (currentFrame instanceof ReceiveEntityFrame) {
                        completeEntityFrame((ReceiveEntityFrame) currentFrame);
                    }

                    frameTemp = null;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (args.remained());
    }

    private Frame buildNewFrame(IoArgs args) {
        AbstractReceiveFrame frame = ReceiveFrameFactory.createInstance(args);
        if (frame instanceof CancelReceiveFrame) {
            cancelReceivePacket(frame.getBodyIdentifier());
            return null;
        } else if (frame instanceof ReceiveEntityFrame) {
            WritableByteChannel channel = getPacketChannel(frame.getBodyIdentifier());
            ((ReceiveEntityFrame) frame).bindPacketChannel(channel);
        }

        return frame;
    }

    private void completeEntityFrame(ReceiveEntityFrame frame) {
        synchronized (packetMap) {
            short identifier = frame.getBodyIdentifier();
            int length = frame.getBodyLength();

            PacketModel model = packetMap.get(identifier);
            model.unreceivedLength -= length;
            if (model.unreceivedLength <= 0) {
                provider.completedPacket(model.packet, true);
                packetMap.remove(identifier);
            }
        }
    }

    private void appendNewPacket(short identifier, ReceivePacket<?, ?> packet) {
        synchronized (packetMap) {
            PacketModel model = new PacketModel(packet);
            packetMap.put(identifier, model);
        }
    }

    private WritableByteChannel getPacketChannel(short identifier) {
        synchronized (packetMap) {
            PacketModel model = packetMap.get(identifier);
            return model == null ? null : model.channel;
        }
    }

    private void cancelReceivePacket(short identifier) {
        synchronized (packetMap) {
            PacketModel model = packetMap.get(identifier);
            if (model != null) {
                ReceivePacket<?, ?> packet = model.packet;
                provider.completedPacket(packet, false);
            }
        }
    }

    /**
     * 关闭操作，关闭时若当前还有正在接收到Packet，则尝试停止对应的Packet的接收
     *
     * @throws IOException 输入输出异常
     */
    @Override
    public synchronized void close() throws IOException {
        synchronized (packetMap) {
            Collection<PacketModel> values = packetMap.values();
            for (PacketModel value : values) {
                provider.completedPacket(value.packet, false);
            }
            packetMap.clear();
        }
    }

    /**
     * Packet提供者
     */
    interface PacketProvider {

        ReceivePacket<?, ?> takePacket(byte type, long length, byte[] headerInfo);

        /**
         * 结束一份Packet
         *
         * @param packet 接收包
         * @param isSucceed 是否成功发送完成
         */
        void completedPacket(ReceivePacket<?, ?> packet, boolean isSucceed);
    }

    static class PacketModel {

        final ReceivePacket<?, ?> packet;
        final WritableByteChannel channel;
        volatile long unreceivedLength;

        PacketModel(ReceivePacket<?, ?> packet) {
            this.packet = packet;
            this.channel = Channels.newChannel(packet.open());
            this.unreceivedLength = packet.length();
        }
    }
}
