package org.yangxin.socket.lib.core;

import java.io.IOException;

/**
 * @author yangxin
 * 2021/8/24 17:31
 */
public class IoContext {

    private static IoContext instance;
    private final IoProvider ioProvider;

    private IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }

    public static IoContext get() {
        return instance;
    }

    public static StartedBoot setup() {
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

    public static class StartedBoot {

        private IoProvider provider;

        private StartedBoot() {

        }

        public StartedBoot ioProvider(IoProvider provider) {
            this.provider = provider;
            return this;
        }

        public IoContext start() {
            instance = new IoContext(provider);
            return instance;
        }
    }
}
