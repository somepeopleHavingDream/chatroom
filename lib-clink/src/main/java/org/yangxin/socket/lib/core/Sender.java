package org.yangxin.socket.lib.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author yangxin
 * 2021/8/24 11:34
 */
@SuppressWarnings("unused")
public interface Sender extends Closeable {

    void setSendListener(IoArgs.IoArgsEventProcessor processor);

    /**
     * 异步发送
     *
     * @return 是否异步发送成功
     * @throws IOException io异常
     */
    boolean postSendAsync() throws IOException;
}
