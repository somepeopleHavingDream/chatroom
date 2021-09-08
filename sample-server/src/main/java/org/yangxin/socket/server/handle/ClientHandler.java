package org.yangxin.socket.server.handle;

import org.yangxin.socket.foo.Foo;
import org.yangxin.socket.lib.core.Connection;
import org.yangxin.socket.lib.core.Packet;
import org.yangxin.socket.lib.core.ReceivePacket;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author yangxin
 * 2021/8/23 11:18
 */
@SuppressWarnings("AlibabaAvoidManuallyCreateThread")
public class ClientHandler extends Connection {

    private final File cachePath;

    /**
     * 客户端处理者的回调
     */
    private final ClientHandlerCallback handlerCallback;

    /**
     * 客户端信息
     */
    private final String clientInfo;

    public ClientHandler(SocketChannel channel, ClientHandlerCallback handlerCallback, File cachePath) throws IOException {
        // 客户端通道
        this.handlerCallback = handlerCallback;
        this.clientInfo = channel.getRemoteAddress().toString();
        this.cachePath = cachePath;

        System.out.println("新客户端连接：" + clientInfo);

        // 设置一些参数
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

    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivedPacket(ReceivePacket<?, ?> packet) {
        super.onReceivedPacket(packet);

        if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            String string = (String) packet.entity();
            System.out.println(key + ":" + string);
            handlerCallback.onNewMessageArrived(this, string);
        }
    }

    private void exitBySelf() {
        exit();
        handlerCallback.onSelfClose(this);
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
