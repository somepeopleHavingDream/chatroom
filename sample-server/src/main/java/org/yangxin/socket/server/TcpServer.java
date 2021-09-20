package org.yangxin.socket.server;

import lombok.Setter;
import org.yangxin.socket.lib.utils.CloseUtils;
import org.yangxin.socket.server.handle.ClientHandler;

import java.io.File;
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

    /**
     * tcp服务端用于监听客户端连接的端口
     */
    private final int port;

    private final File cachePath;
    private final ExecutorService forwardingExecutor;

    /**
     * 用于该tcp服务端的客户端监听者
     */
    private ClientListener listener;

    /**
     * 该tcp服务端的所有客户端处理者
     */
    private final List<ClientHandler> handlers = new ArrayList<>();

    /**
     * 用于监听客户端连接的选择器
     */
    private Selector selector;

    private ServerSocketChannel channel;

    public TcpServer(int port, File cachePath) {
        // 设置端口和缓存路径
        this.port = port;
        this.cachePath = cachePath;

        // 转发线程池
        this.forwardingExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * 启动Tcp服务端
     *
     * @return 是否启动成功
     */
    public boolean start() {
        try {
            // 获得并设置选择器，用于监听客户端的连接
            selector = Selector.open();

            // 打开一个服务套接字通道
            ServerSocketChannel channel = ServerSocketChannel.open();
            // 设置为非阻塞
            channel.configureBlocking(false);
            // 绑定本地端口
            channel.socket().bind(new InetSocketAddress(port));
            // 注册客户端就绪事件到达监听
            channel.register(selector, SelectionKey.OP_ACCEPT);
            // 设置通道
            this.channel = channel;

            // 打印服务端信息
            System.out.println("服务器信息：" + channel.getLocalAddress().toString());

            // 获取客户端监听，并设置和启动客户端监听
            ClientListener listener = new ClientListener();
            this.listener = listener;
            Thread thread = new Thread(listener);
            listener.setThread(thread);
            thread.start();
        } catch (IOException e) {
            // 若在tcp服务端启动的过程中捕获到了输入输出异常，则打印堆栈信息，并返回tcp服务端启动失败
            e.printStackTrace();
            return false;
        }

        // 返回tcp服务端启动成功
        return true;
    }

    public void stop() {
        // 退出客户端监听者
        if (listener != null) {
            listener.exit();
        }

        // 关闭服务端套接字通道和用于监听客户端连接的选择器
        CloseUtils.close(channel);
        CloseUtils.close(selector);

        // 退出所有的客户端处理者
        synchronized (TcpServer.this) {
            for (ClientHandler handler : handlers) {
                handler.exit();
            }

            handlers.clear();
        }

        // 停止线程池
        forwardingExecutor.shutdownNow();
    }

    /**
     * 广播来自服务端的消息
     *
     * @param msg 来自服务端的消息
     */
    public synchronized void broadcast(String msg) {
        // 所有的客户端处理者发送来自服务端的消息
        handlers.forEach(handler -> handler.send(msg));
    }

    @Override
    public void onSelfClose(ClientHandler handler) {
        handlers.remove(handler);
    }

    @Override
    public void onNewMessageArrived(ClientHandler handler, String msg) {
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

        /**
         * 用于执行该客户端监听者的线程
         */
        private Thread thread;

        @Override
        public void run() {
            // 该选择器用于监听客户端接入事件
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

                    // 处理所有客户端就绪事件，即客户端接收事件
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        // 如果当前线程被打断，则退出循环
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }

                        // 拿到一个选择键，并将该选择键从集合中移除
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        // 检查当前key的状态是否是我们关注的
                        // 客户端到达状态
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                            // 非阻塞状态拿到客户端连接
                            SocketChannel clientChannel = serverChannel.accept();

                            try {
                                // 为客户端构建异步线程
                                ClientHandler handler = new ClientHandler(clientChannel,
                                        TcpServer.this,
                                        cachePath);

                                // 添加同步处理，将此客户端处理者放入到处理者集合中
                                synchronized (TcpServer.this) {
                                    handlers.add(handler);
                                }
                            } catch (IOException e) {
                                // 若捕获到输入输出异常，则打印堆栈信息
                                e.printStackTrace();
                                System.out.println("客户端连接异常：" + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    // 若捕获到输入输出异常，则打印堆栈信息
                    e.printStackTrace();
                }
            } while (!Thread.currentThread().isInterrupted());

            // 打印日志：服务端已关闭
            System.out.println("服务器已关闭！");
        }

        public void exit() {
            // 中断客户端监听者循环线程
            thread.interrupt();
            // 唤醒当前的阻塞
            selector.wakeup();
        }
    }
}
