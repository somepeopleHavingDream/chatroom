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
public class ClientHandler {

    private final Connection connection;
    private final SocketChannel channel;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback handlerCallback;
    private final String clientInfo;

    public ClientHandler(SocketChannel channel, ClientHandlerCallback handlerCallback) throws IOException {
        // 客户端通道
        this.channel = channel;

        // 为该客户端设置连接
        connection = new Connection() {

            @Override
            public void onChannelClosed(SocketChannel channel) {
                super.onChannelClosed(channel);
                exitBySelf();
            }

            @Override
            protected void onReceiveNewMessage(String str) {
                super.onReceiveNewMessage(str);
                handlerCallback.onNewMessageArrived(ClientHandler.this, str);
            }
        };
        connection.setup(channel);

        // 注册写事件，并设置写事件处理者
        Selector writeSelector = Selector.open();
        channel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);

        this.handlerCallback = handlerCallback;
        this.clientInfo = channel.getRemoteAddress().toString();

        System.out.println("新客户端连接：" + clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void exit() {
        CloseUtils.close(connection);
        writeHandler.exit();
        CloseUtils.close(channel);

        System.out.println("客户端已退出：" + clientInfo);
    }

    public void send(String msg) {
        writeHandler.send(msg);
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

    private class ClientWriteHandler {

        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer buffer;
        private final ExecutorService executorService;

        private ClientWriteHandler(Selector selector) {
            this.selector = selector;
            this.buffer = ByteBuffer.allocate(256);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        private void exit() {
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }

        private void send(String msg) {
            if (done) {
                return;
            }

            executorService.execute(new WriteRunnable(msg));
        }

        private class WriteRunnable implements Runnable {

            private final String msg;

            public WriteRunnable(String msg) {
                this.msg = msg + '\n';
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }

                buffer.clear();
                buffer.put(msg.getBytes());
                // 反转操作，重点
                buffer.flip();

                while (!done && buffer.hasRemaining()) {
                    try {
                        int length = channel.write(buffer);
                        // length = 0 合法
                        if (length < 0) {
                            System.out.println("客户端已无法发送数据！");
                            ClientHandler.this.exitBySelf();
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
