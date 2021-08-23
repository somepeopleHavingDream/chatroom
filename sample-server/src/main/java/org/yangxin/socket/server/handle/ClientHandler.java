package org.yangxin.socket.server.handle;

import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yangxin
 * 2021/8/23 11:18
 */
@SuppressWarnings("AlibabaAvoidManuallyCreateThread")
public class ClientHandler {

    private final SocketChannel channel;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback handlerCallback;
    private final String clientInfo;

    public ClientHandler(SocketChannel channel, ClientHandlerCallback handlerCallback) throws IOException {
        this.channel = channel;

        // 设置非阻塞模式
        channel.configureBlocking(false);

        Selector readSelector = Selector.open();
        channel.register(readSelector, SelectionKey.OP_READ);
        this.readHandler = new ClientReadHandler(readSelector);

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
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(channel);

        System.out.println("客户端已退出：" + clientInfo);
    }

    public void send(String msg) {
        writeHandler.send(msg);
    }

    public void readToPrint() {
        Thread thread = new Thread(readHandler);
        thread.start();
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

    private class ClientReadHandler implements Runnable {

        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer buffer;

        private ClientReadHandler(Selector selector) {
            this.selector = selector;
            this.buffer = ByteBuffer.allocate(256);
        }

        @Override
        public void run() {
            try {
                do {
                    // 客户端拿到一条数据
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }

                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }

                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();

                            // 清空操作
                            buffer.clear();

                            // 读取
                            int read = client.read(buffer);
                            if (read > 0) {
                                // 丢弃换行符
                                String msg = new String(buffer.array(), 0, read - 1);
                                // 通知到TcpServer
                                handlerCallback.onNewMessageArrived(ClientHandler.this, msg);
                            } else {
                                System.out.println("客户端已无法读取数据！");

                                // 退出当前客户端
                                ClientHandler.this.exitBySelf();
                                break;
                            }
                        }
                    }
                } while (!done);
            } catch (IOException e) {
                if (!done) {
                    System.out.println("连接异常断开！");
                    ClientHandler.this.exitBySelf();
                }
            } finally {
                // 连接关闭
                CloseUtils.close(selector);
            }
        }

        public void exit() {
            done = true;
            selector.wakeup();
            CloseUtils.close(selector);
        }
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
                this.msg = msg +'\n';
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
