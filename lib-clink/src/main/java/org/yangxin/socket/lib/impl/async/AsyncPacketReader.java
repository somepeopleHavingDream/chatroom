package org.yangxin.socket.lib.impl.async;

import org.yangxin.socket.lib.core.Frame;
import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.SendPacket;
import org.yangxin.socket.lib.core.ds.BytePriorityNode;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author yangxin
 * 2021/9/15 下午9:04
 */
public class AsyncPacketReader implements Closeable {

    private final PacketProvider provider;
    private volatile IoArgs args = new IoArgs();

    private volatile BytePriorityNode<Frame> node;
    private volatile int nodeSize = 0;

    /**
     * 1,2,3...255
     */
    private short lastIdentifier = 0;

    AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    void cancel(SendPacket<?> packet) {
    }

    boolean requestTakePacket() {
        return false;
    }

    IoArgs fillData() {
        return null;
    }

    @Override
    public void close() throws IOException {
    }

    private short generateIdentifier() {
        short identifier = ++ lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }

    interface PacketProvider {

        /**
         * 拿到发送包
         *
         * @return 发送包
         */
        SendPacket<?> takePacket();

        /**
         * 完成包
         *
         * @param packet 发送包
         * @param isSucceed 是否成功
         */
        void completedPacket(SendPacket<?> packet, boolean isSucceed);
    }
}
