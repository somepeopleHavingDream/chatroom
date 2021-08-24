package org.yangxin.socket.lib.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author yangxin
 * 2021/8/24 11:34
 */
public interface Sender extends Closeable {

    /**
     * 异步发送
     *
     * @param args io参数
     * @param listener io参数事件监听者
     * @return 是否异步发送成功
     * @throws IOException io异常
     */
    boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException;
}
