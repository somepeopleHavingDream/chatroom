package org.yangxin.socket.lib.impl.async;

import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.ReceivePacket;

import java.io.Closeable;
import java.io.IOException;

/**
 * 写数据到Packet中
 *
 * @author yangxin
 * 2021/9/15 下午9:04
 */
class AsyncPacketWriter implements Closeable {

    private final PacketProvider provider;

    public AsyncPacketWriter(PacketProvider provider) {
        this.provider = provider;
    }

    void consumeIoArgs(IoArgs args) {

    }

    IoArgs takeIoArgs() {
        return null;
    }

    @Override
    public void close() throws IOException {

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
}
