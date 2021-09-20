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
            args.limit(headerRemaining + bodyRemaining);
            args.startWriting();

            if (headerRemaining > 0 && args.remained()) {
                headerRemaining -= consumeHeader(args);
            }

            if (headerRemaining == 0 && args.remained() && bodyRemaining > 0) {
                bodyRemaining -= consumeBody(args);
            }

            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            args.finishWriting();
        }
    }

    @Override
    public int getConsumableLength() {
        return headerRemaining + bodyRemaining;
    }

    private byte consumeHeader(IoArgs args) {
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte) args.readFrom(header, offset, count);
    }

    /**
     * 消费消息体
     *
     * @param args 输入输出参数
     * @return 消费了多少个字节
     * @throws IOException 输入输出异常
     */
    protected abstract int consumeBody(IoArgs args) throws IOException;

    protected synchronized boolean isSending() {
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }
}
