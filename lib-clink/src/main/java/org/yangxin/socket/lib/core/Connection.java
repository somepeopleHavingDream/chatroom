package org.yangxin.socket.lib.core;

import org.yangxin.socket.lib.impl.SocketChannelAdapter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * 连接
 *
 * @author yangxin
 * 2021/8/24 11:33
 */
@SuppressWarnings("FieldCanBeLocal")
public class Connection implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    /**
     * 该连接的唯一标识
     */
    private final UUID key = UUID.randomUUID();

    /**
     * 该连接对应的通道
     */
    private SocketChannel channel;

    /**
     * 该连接对应的发送者
     */
    private Sender sender;

    /**
     * 该连接对应的接收者
     */
    private Receiver receiver;

    /**
     * 为当前连接设置一些参数
     *
     * @param channel 通道
     * @throws IOException io异常
     */
    public void setup(SocketChannel channel) throws IOException {
        // 设置通道
        this.channel = channel;

        // 通过输入输出上下文获得套接字通道适配器
        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);

        // 设置发送者和接收者
        this.sender = adapter;
        this.receiver = adapter;

        // 读取下一条消息
        readNextMessage();
    }

    /**
     * 读取下一条消息
     */
    private void readNextMessage() {
        // 如果接收者不为null，则接收者开始异步地接收消息
        if (receiver != null) {
            try {
                receiver.receiveAsync(echoReceiveListener);
            } catch (IOException e) {
                System.out.println("开始接收数据异常：" + e.getMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }

    /**
     * 回声接收监听器
     */
    private final IoArgs.IoArgsEventListener echoReceiveListener = new IoArgs.IoArgsEventListener() {

        @Override
        public void onStarted(IoArgs args) {
        }

        @Override
        public void onCompleted(IoArgs args) {
            // 打印
            onReceiveNewMessage(args.bufferString());
            // 读取下一条数据
            readNextMessage();
        }
    };

    protected void onReceiveNewMessage(String str) {
        System.out.println(key + ":" + str);
    }
}
