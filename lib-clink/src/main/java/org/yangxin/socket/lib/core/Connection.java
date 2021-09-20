package org.yangxin.socket.lib.core;

import org.yangxin.socket.lib.box.BytesReceivePacket;
import org.yangxin.socket.lib.box.FileReceivePacket;
import org.yangxin.socket.lib.box.StringReceivePacket;
import org.yangxin.socket.lib.box.StringSendPacket;
import org.yangxin.socket.lib.impl.SocketChannelAdapter;
import org.yangxin.socket.lib.impl.async.AsyncReceiveDispatcher;
import org.yangxin.socket.lib.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * 连接，实现了当通道状态发生改变时的回调
 *
 * @author yangxin
 * 2021/8/24 11:33
 */
@SuppressWarnings({"FieldCanBeLocal", "DuplicateBranchesInSwitch"})
public abstract class Connection implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    /**
     * 该连接的唯一标识
     */
    protected final UUID key = UUID.randomUUID();

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

        // 通过输入输出上下文获得套接字通道适配器，
        // （输入输出上下文环境获取到的输入输出提供者具体是哪个服务端的实例还是客户端的实例这并不重要，因为两个实例起到的作用是一样的）
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

    /**
     * 发送消息
     *
     * @param msg 消息
     */
    public void send(String msg) {
        // 每次都是实例化一个字符串发送包，即发送的消息只支持字符串消息
        StringSendPacket packet = new StringSendPacket(msg);

        // 然后将该包交给发送调度者来发送（异步处理）
        sendDispatcher.send(packet);
    }

    public void send(SendPacket<?> packet) {
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

    protected void onReceivedPacket(ReceivePacket<?, ?> packet) {
        System.out.println(key + ":[New Packet]-Type:" + packet.type() + ",Length:" + packet.length());
    }

    /**
     * 创建新的接收文件
     *
     * @return 新的文件
     */
    protected abstract File createNewReceiveFile();

    /**
     * 接收调度者的接收包回调
     */
    private final ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {

        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile());
                case Packet.TYPE_STREAM_DIRECT:
                    return new BytesReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("Unsupported packet type: " + type);
            }
        }

        @Override
        public void onReceivePacketCompleted(ReceivePacket<?, ?> packet) {
            onReceivedPacket(packet);
        }
    };
}
