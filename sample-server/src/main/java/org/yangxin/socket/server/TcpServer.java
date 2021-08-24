package org.yangxin.socket.server;

import lombok.Setter;
import org.yangxin.socket.lib.utils.CloseUtils;
import org.yangxin.socket.server.handle.ClientHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yangxin
 * 2021/8/23 16:55
 */
@SuppressWarnings({"AlibabaThreadPoolCreation", "AlibabaAvoidManuallyCreateThread"})
public class TcpServer implements ClientHandler.ClientHandlerCallback {

    private final int port;
    private ClientListener listener;
    private final List<ClientHandler> handlers = new ArrayList<>();
    private final ExecutorService forwardingExecutor;
    private Selector selector;
    private ServerSocketChannel channel;

    public TcpServer(int port) {
        this.port = port;
        // 转发线程池
        this.forwardingExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            selector = Selector.open();

            ServerSocketChannel channel = ServerSocketChannel.open();
            // 设置为非阻塞
            channel.configureBlocking(false);
            // 绑定本地端口
            channel.socket().bind(new InetSocketAddress(port));
            // 注册客户端连接到达监听
            channel.register(selector, SelectionKey.OP_ACCEPT);
            this.channel = channel;

            System.out.println("服务器信息：" + channel.getLocalAddress().toString());

            // 启动客户端监听
            ClientListener listener = new ClientListener();
            this.listener = listener;
            Thread thread = new Thread(listener);
            listener.setThread(thread);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void stop() {
        if (listener != null) {
            listener.exit();
        }

        CloseUtils.close(channel);
        CloseUtils.close(selector);

        synchronized (TcpServer.this) {
            for (ClientHandler handler : handlers) {
                handler.exit();
            }

            handlers.clear();
        }

        // 停止线程池
        forwardingExecutor.shutdownNow();
    }

    public synchronized void broadcast(String msg) {
        handlers.forEach(handler -> handler.send(msg));
    }

    @Override
    public void onSelfClose(ClientHandler handler) {
        handlers.remove(handler);
    }

    @Override
    public void onNewMessageArrived(ClientHandler handler, String msg) {
        // 打印到屏幕
        System.out.println("Received-" + handler.getClientInfo() + ":" + msg);
        // 异步提交转发任务
        forwardingExecutor.execute(() -> {
            synchronized (TcpServer.this) {
                for (ClientHandler clientHandler : handlers) {
                    if (Objects.equals(clientHandler, handler)) {
                        // 跳过自己
                        continue;
                    }

                    // 对其他客户端发送消息
                    clientHandler.send(msg);
                }
            }
        });
    }

    /**
     * 客户端监听者
     */
    @Setter
    private class ClientListener implements Runnable {

//        private boolean done = false;
        private Thread thread;

        @Override
        public void run() {
            Selector selector = TcpServer.this.selector;
            System.out.println("服务器准备就绪~");

            // 等待客户端连接
            do {
                // 得到客户端
                try {
                    if (selector.select() == 0) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        continue;
                    }

                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }

                        SelectionKey key = iterator.next();
                        iterator.remove();

                        // 检查当前key的状态是否是我们关注的
                        // 客户端到达状态
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                            // 非阻塞状态拿到客户端连接
                            SocketChannel clientChannel = serverChannel.accept();

                            try {
                                // 客户端构建异步线程
                                ClientHandler handler = new ClientHandler(clientChannel, TcpServer.this);
                                // 添加同步处理
                                synchronized (TcpServer.this) {
                                    handlers.add(handler);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常：" + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (!Thread.currentThread().isInterrupted());

            System.out.println("服务器已关闭！");
        }

        public void exit() {
            thread.interrupt();
            // 唤醒当前的阻塞
            selector.wakeup();
        }
    }
}
