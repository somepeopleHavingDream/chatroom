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
     * 异步接收
     *
     * @param listener 输入输出参数事件监听者
     * @return 是否异步接收成功
     * @throws IOException io异常
     */
    boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException;
}
