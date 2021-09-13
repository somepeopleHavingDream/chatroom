package org.yangxin.socket.lib.core;

import java.io.IOException;

/**
 * 输入输出上下文
 *
 * @author yangxin
 * 2021/8/24 17:31
 */
public class IoContext {

    /**
     * 当前输入输出上下文实例
     */
    private static IoContext instance;

    /**
     * 输入输出提供者
     */
    private final IoProvider ioProvider;

    private IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }

    /**
     * 返回当前输入输出上下文实例
     *
     * @return 当前输入输出上下文实例
     */
    public static IoContext get() {
        return instance;
    }

    public static StartedBoot setup() {
        // 实例化一个启动引导实例
        return new StartedBoot();
    }

    public static void close() throws IOException {
        if (instance != null) {
            instance.callClose();
        }
    }

    private void callClose() throws IOException {
        ioProvider.close();
    }

    /**
     * 输入输出上下文的启动引导。
     */
    @SuppressWarnings("UnusedReturnValue")
    public static class StartedBoot {

        private IoProvider provider;

        private StartedBoot() {
        }

        public StartedBoot ioProvider(IoProvider provider) {
            this.provider = provider;
            return this;
        }

        /**
         * 启动，返回输入输出上下文
         *
         * @return 输入输出上下文
         */
        public IoContext start() {
            instance = new IoContext(provider);
            return instance;
        }
    }
}
