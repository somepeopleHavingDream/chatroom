package org.yangxin.socket.lib.core;

import org.yangxin.socket.lib.box.StringReceivePacket;
import org.yangxin.socket.lib.box.StringSendPacket;
import org.yangxin.socket.lib.impl.SocketChannelAdapter;
import org.yangxin.socket.lib.impl.async.AsyncReceiveDispatcher;
import org.yangxin.socket.lib.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * 连接，实现了当通道状态发生改变时的回调
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

        // 设置发送调度者和接收调度者（异步的发送调度者和异步的接收调度者）
        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);

        // 启动接收调度者（实际就是注册读事件和该事件对应的回调）
        receiveDispatcher.start();
    }

    public void send(String msg) {
        // 每次都是实例化一个字符串发送包
        StringSendPacket packet = new StringSendPacket(msg);

        // 然后将该包交给发送调度者来发送
        sendDispatcher.send(packet);
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }

    protected void onReceiveNewMessage(String str) {
        System.out.println(key + ":" + str);
    }

    /**
     * 接收调度者的接收包回调
     */
    private final ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = packet -> {
        // 如果包类型是字符串接收包
        if (packet instanceof StringReceivePacket) {
            // 转换类型后，获取包中的字符串
            String msg = ((StringReceivePacket) packet).string();

            // 调用当接收到新消息时方法
            onReceiveNewMessage(msg);
        }
    };
}
