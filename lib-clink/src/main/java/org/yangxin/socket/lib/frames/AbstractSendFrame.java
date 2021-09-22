package org.yangxin.socket.lib.frames;

import org.yangxin.socket.lib.core.Frame;
import org.yangxin.socket.lib.core.IoArgs;

import java.io.IOException;

/**
 * @author yangxin
 * 2021/9/15 下午9:06
 */
public abstract class AbstractSendFrame extends Frame {

    /**
     * 帧头剩余的长度
     */
    volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;

    /**
     * 帧体剩余的长度
     */
    volatile int bodyRemaining;

    public AbstractSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);

        // 设置帧体剩余的长度
        bodyRemaining = length;
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        try {
            // 设置输入输出参数写的字节数
            args.limit(headerRemaining + bodyRemaining);
            args.startWriting();

            // 如果帧头还未被消费完，并且输入输出参数仍有剩余
            if (headerRemaining > 0 && args.remained()) {
                // 消费头部，并且更新头部剩余
                headerRemaining -= consumeHeader(args);
            }

            // 如果帧头已被消费完，并且输入输出参数仍有剩余，并且帧体还未被消费完
            if (headerRemaining == 0 && args.remained() && bodyRemaining > 0) {
                // 消费帧体
                bodyRemaining -= consumeBody(args);
            }

            // 返回当前帧是否已被消费完
            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            args.finishWriting();
        }
    }

    @Override
    public int getConsumableLength() {
        return headerRemaining + bodyRemaining;
    }

    /**
     * 消费帧头
     *
     * @param args 输入输出参数
     * @return 消费了多少个字节
     */
    private byte consumeHeader(IoArgs args) {
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte) args.readFrom(header, offset, count);
    }

    /**
     * 消费帧体体
     *
     * @param args 输入输出参数
     * @return 消费了多少个字节
     * @throws IOException 输入输出异常
     */
    protected abstract int consumeBody(IoArgs args) throws IOException;

    /**
     * 当前帧是否已处于发送状态
     *
     * @return 帧是否发送
     */
    protected synchronized boolean isSending() {
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }
}
