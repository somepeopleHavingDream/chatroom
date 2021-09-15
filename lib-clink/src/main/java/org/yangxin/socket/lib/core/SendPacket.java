package org.yangxin.socket.lib.core;

import java.io.InputStream;

/**
 * 发送的包定义
 *
 * @author yangxin
 * 2021/8/28 下午1:09
 */
public abstract class SendPacket<T extends InputStream> extends Packet<T> {

    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * 设置取消发送标记
     */
    public void cancel() {
        isCanceled = true;
    }
}
