package org.yangxin.socket.lib.core;

import java.io.Closeable;

/**
 * 接收的数据调度封装，
 * 把一份或者多份IoArgs组合成一份Packet
 *
 * @author yangxin
 * 2021/8/28 下午1:28
 */
public interface ReceiveDispatcher extends Closeable {

    /**
     * 开始接收
     */
    void start();

    /**
     * 停止接收
     */
    void stop();

    interface ReceivePacketCallback {

        /**
         * 都接收包完成时
         *
         * @param packet 接收到的包
         */
        void onReceivePacketCompleted(ReceivePacket<?, ?> packet);
    }
}
