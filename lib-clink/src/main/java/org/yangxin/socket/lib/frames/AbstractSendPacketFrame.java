package org.yangxin.socket.lib.frames;

import org.yangxin.socket.lib.core.Frame;
import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.SendPacket;

import java.io.IOException;

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

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        if (packet == null && !isSending()) {
            // 已取消，并且未发送任何数据，直接返回结束，发送下一帧
            return true;
        }

        return super.handle(args);
    }

    @Override
    public final synchronized Frame nextFrame() {
        return packet == null ? null : buildNextFrame();
    }

    /**
     * True，当前帧没有发送任何数据
     *
     * 1234, 12,34
     */
    public final synchronized boolean abort() {
        boolean isSending = isSending();
        if (isSending) {
            fillDirtyDataOnAbort();
        }

        packet = null;
        return !isSending;
    }

    protected void fillDirtyDataOnAbort() {

    }

    /**
     * 创建下一帧
     *
     * @return 下一帧
     */
    protected abstract Frame buildNextFrame();
}
