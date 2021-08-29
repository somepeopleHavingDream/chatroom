package org.yangxin.socket.server.handle;

import org.yangxin.socket.lib.core.Connection;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yangxin
 * 2021/8/23 11:18
 */
@SuppressWarnings("AlibabaAvoidManuallyCreateThread")
public class ClientHandler extends Connection {

    private final ClientHandlerCallback handlerCallback;
    private final String clientInfo;

    public ClientHandler(SocketChannel channel, ClientHandlerCallback handlerCallback) throws IOException {
        // 客户端通道
        this.handlerCallback = handlerCallback;
        this.clientInfo = channel.getRemoteAddress().toString();

        System.out.println("新客户端连接：" + clientInfo);
        setup(channel);
    }

    /**
     * 客户端处理者退出
     */
    public void exit() {
        // 退出连接
        CloseUtils.close(this);
        System.out.println("客户端已退出：" + clientInfo);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        exitBySelf();
    }

    private void exitBySelf() {
        exit();
        handlerCallback.onSelfClose(this);
    }

    @Override
    protected void onReceiveNewMessage(String str) {
        super.onReceiveNewMessage(str);
        handlerCallback.onNewMessageArrived(this, str);
    }

    public interface ClientHandlerCallback {

        /**
         * 自身关闭通知
         *
         * @param handler 客户端处理者
         */
        void onSelfClose(ClientHandler handler);

        /**
         * 收到消息时通知
         *
         * @param handler 客户端处理者
         * @param msg 收到的消息
         */
        void onNewMessageArrived(ClientHandler handler, String msg);
    }
}
