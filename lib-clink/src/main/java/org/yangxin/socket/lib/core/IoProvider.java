package org.yangxin.socket.lib.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

/**
 * @author yangxin
 * 2021/8/24 11:48
 */
public interface IoProvider extends Closeable {

    /**
     * 注册输入
     *
     * @param channel 通道
     * @param callback 处理输入回调
     * @return 注册输入是否成功
     */
    boolean registerInput(SocketChannel channel, HandleInputCallback callback);

    /**
     * 注册输出
     *
     * @param channel 通道
     * @param callback 处理输出回调
     * @return 注册输出是否成功
     */
    boolean registerOutput(SocketChannel channel, HandleOutputCallback callback);

    /**
     * 取消注册输入
     *
     * @param channel 通道
     */
    void unRegisterInput(SocketChannel channel);

    /**
     * 取消注册输出
     *
     * @param channel 通道
     */
    void unRegisterOutput(SocketChannel channel);

    abstract class HandleInputCallback implements Runnable {

        @Override
        public void run() {
            canProviderInput();
        }

        /**
         * 能够提供输入
         */
        protected abstract void canProviderInput();
    }

    /**
     * 处理输出回调
     */
    abstract class HandleOutputCallback implements Runnable {

        private Object attach;

        @Override
        public void run() {
            canProviderOutput(attach);
        }

        public final void setAttach(Object attach) {
            this.attach = attach;
        }

        @SuppressWarnings("unchecked")
        public final <T> T getAttach() {
            return (T) this.attach;
        }

        /**
         * 能够提供输出
         *
         * @param attach 附加信息
         */
        protected abstract void canProviderOutput(Object attach);
    }
}
