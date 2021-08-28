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
     * @param listener 接收监听
     */
    void setReceiveListener(IoArgs.IoArgsEventListener listener);

    /**
     * 异步接收
     *
     * @param args 输入输出参数
     * @return 是否接收成功
     * @throws IOException 输入输出异常
     */
    boolean receiveAsync(IoArgs args) throws IOException;
}
