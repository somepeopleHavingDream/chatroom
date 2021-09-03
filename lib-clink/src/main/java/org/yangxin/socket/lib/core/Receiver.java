package org.yangxin.socket.lib.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * 接收者
 *
 * @author yangxin
 * 2021/8/24 11:44
 */
@SuppressWarnings("UnusedReturnValue")
public interface Receiver extends Closeable {

    /**
     * 设置接收监听
     *
     * @param processor 接收监听
     */
    void setReceiveListener(IoArgs.IoArgsEventProcessor processor);

    /**
     * 异步接收
     *
     * @return 是否接收成功
     * @throws IOException 输入输出异常
     */
    boolean postReceiveAsync() throws IOException;
}
