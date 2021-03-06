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

    /**
     * 用于此发送包帧的发送包
     */
    protected volatile SendPacket<?> packet;

    public AbstractSendPacketFrame(int length, byte type, byte flag, short identifier, SendPacket<?> packet) {
        super(length, type, flag, identifier);
        this.packet = packet;
    }

    /**
     * 获取当前对应的发送Packet
     *
     * @return SendPacket
     */
    public synchronized SendPacket<?> getPacket() {
        return packet;
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        if (packet == null && !isSending()) {
            // 已取消，并且未发送任何数据，直接返回结束，发送下一帧
            return true;
        }

        return super.handle(args);
    }

    /**
     * 下一帧
     *
     * @return 下一帧
     */
    @Override
    public final synchronized Frame nextFrame() {
        return packet == null ? null : buildNextFrame();
    }

    /**
     * 终止当前帧，
     * 需要在当前方法中做一些操作，以及状态的维护，
     * 后续可以扩展{@link #fillDirtyDataOnAbort()}方法对数据进行填充操作
     *
     * @return True: 完美终止，可以顺利地移除当前帧；False：已发送部分数据
     */
    public final synchronized boolean abort() {
        // True，当前帧没有发送任何数据
        // 1234, 12,34
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
     * @return NULL: 下一帧
     */
    protected abstract Frame buildNextFrame();
}
