package org.yangxin.socket.lib.frames;

import org.yangxin.socket.lib.core.Frame;
import org.yangxin.socket.lib.core.IoArgs;

import java.io.IOException;

/**
 * @author yangxin
 * 2021/9/15 下午9:06
 */
public abstract class AbstractReceiveFrame extends Frame {

    /**
     * 帧体可读写区域大小
     */
    volatile int bodyRemaining;

    public AbstractReceiveFrame(byte[] header) {
        super(header);
        bodyRemaining = getBodyLength();
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        if (bodyRemaining == 0) {
            // 已读取所有数据
            return true;
        }

        bodyRemaining -= consumeBody(args);

        return bodyRemaining == 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }

    /**
     * 消费帧体
     *
     * @param args 输入输出参数
     * @return 消费了多少个字节
     * @throws IOException 输入输出异常
     */
    protected abstract int consumeBody(IoArgs args) throws IOException;
}
