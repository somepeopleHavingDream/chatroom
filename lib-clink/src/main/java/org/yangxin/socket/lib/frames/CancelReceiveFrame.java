package org.yangxin.socket.lib.frames;

import org.yangxin.socket.lib.core.IoArgs;

/**
 * 取消传输帧，接收实现
 *
 * @author yangxin
 * 2021/9/19 上午10:47
 */
public class CancelReceiveFrame extends AbstractReceiveFrame {

    public CancelReceiveFrame(byte[] header) {
        super(header);
    }

    @Override
    protected int consumeBody(IoArgs args) {
        return 0;
    }
}
