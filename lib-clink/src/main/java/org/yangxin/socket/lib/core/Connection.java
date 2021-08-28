package org.yangxin.socket.lib.core;

import org.yangxin.socket.lib.box.StringReceivePacket;
import org.yangxin.socket.lib.box.StringSendPacket;
import org.yangxin.socket.lib.impl.SocketChannelAdapter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * 连接，实现了当通道状态发生改变时的回调。
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
     * 发送调度者
     */
    private SendDispatcher sendDispatcher;

    /**
     * 接收调度者
     */
    private ReceiveDispatcher receiveDispatcher;

    /**
     * 为当前连接设置一些参数
     *
     * @param channel 通道
     * @throws IOException io异常
     */
    public void setup(SocketChannel channel) throws IOException {
        // 设置客户端通道
        this.channel = channel;

        // 通过输入输出上下文获得套接字通道适配器
        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);

        // 设置发送者和接收者
        this.sender = adapter;
        this.receiver = adapter;
    }

    public void send(String msg) {
        StringSendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }

    protected void onReceiveNewMessage(String str) {
        System.out.println(key + ":" + str);
    }

    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = packet -> {
        if (packet instanceof StringReceivePacket) {
            String msg = ((StringReceivePacket) packet).string();
            onReceiveNewMessage(msg);
        }
    };
}
