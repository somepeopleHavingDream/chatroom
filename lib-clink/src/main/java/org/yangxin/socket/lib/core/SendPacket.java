package org.yangxin.socket.lib.core;

/**
 * 发送的包定义
 *
 * @author yangxin
 * 2021/8/28 下午1:09
 */
public abstract class SendPacket extends Packet {

    private boolean isCanceled;

    /**
     * 获得该发送包的用户发送的实际字节数组
     *
     * @return 该发送包的用户发送的实际字节数组
     */
    public abstract byte[] bytes();

    public boolean isCanceled() {
        return isCanceled;
    }
}
