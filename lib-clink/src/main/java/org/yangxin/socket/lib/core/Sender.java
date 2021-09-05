package org.yangxin.socket.lib.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author yangxin
 * 2021/8/24 11:34
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Sender extends Closeable {

    /**
     * 设置发送监听
     *
     * @param processor 输入输出参数事件处理器
     */
    void setSendListener(IoArgs.IoArgsEventProcessor processor);

    /**
     * 异步发送
     *
     * @return 是否异步发送成功
     * @throws IOException io异常
     */
    boolean postSendAsync() throws IOException;
}
