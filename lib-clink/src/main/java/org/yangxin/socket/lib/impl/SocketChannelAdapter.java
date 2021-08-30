package org.yangxin.socket.lib.impl;

import org.yangxin.socket.lib.core.IoArgs;
import org.yangxin.socket.lib.core.IoProvider;
import org.yangxin.socket.lib.core.Receiver;
import org.yangxin.socket.lib.core.Sender;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 客户端通道适配器
 *
 * @author yangxin
 * 2021/8/24 11:34
 */
@SuppressWarnings("CloneableClassWithoutClone")
public class SocketChannelAdapter implements Sender, Receiver, Cloneable {

    /**
     * 当前套接字通道适配者是否关闭
     */
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * 当前套接字通道适配者所处的通道
     */
    private final SocketChannel channel;

    /**
     * 输入输出提供者
     */
    private final IoProvider ioProvider;

    /**
     * 当通道状态发生改变时的监听
     */
    private final OnChannelStatusChangedListener listener;

    /**
     * 接收的输入输出事件监听器
     */
    private IoArgs.IoArgsEventListener receiveIoEventListener;

    /**
     * 发送的输入输出事件监听器
     */
    private IoArgs.IoArgsEventListener sendIoEventListener;

    /**
     * 临时的用于接收的输入输出参数
     */
    private IoArgs receiveArgsTemp;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        // 配置当前通道非阻塞
        channel.configureBlocking(false);
    }

    /**
     * 处理输入的回调
     */
    private final IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {

        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }

            // 获得临时用于接收的参数
            IoArgs args = receiveArgsTemp;

            // 接收输入输出事件的监听器
            IoArgs.IoArgsEventListener listener = SocketChannelAdapter.this.receiveIoEventListener;
            // 调用监听器的开始回调方法
            listener.onStarted(args);

            try {
                // 具体的读取操作
                if (args.readFrom(channel) > 0) {
                    // 读取完成回调
                    listener.onCompleted(args);
                } else {
                    throw new IOException("Cannot read any data!");
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    /**
     * 输出回调，即当选择器监听到写事件时，最终会调用此回调方法。
     */
    private final IoProvider.HandleOutputCallback outputCallback = new IoProvider.HandleOutputCallback() {

        @Override
        protected void canProviderOutput(Object attach) {
            if (isClosed.get()) {
                return;
            }

            // 获得需要发送的附加参数和对应的发送输入输出事件监听器
            IoArgs args = getAttach();
            IoArgs.IoArgsEventListener listener = sendIoEventListener;

            // 调用监听器的开始发送回调
            listener.onStarted(args);

            try {
                // 具体的写操作
                if (args.writeTo(channel) > 0) {
                    // 读取完成回调
                    listener.onCompleted(args);
                } else {
                    throw new IOException("Cannot write any data!");
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventListener listener) {
        // 设置接收输入输出事件监听器
        receiveIoEventListener = listener;
    }

    @Override
    public boolean receiveAsync(IoArgs args) throws IOException {
        // 如果该套接字通道适配器已关闭，则抛出输入输出异常
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }

        // 临时的，用于接收数据的输入输出参数
        receiveArgsTemp = args;

        // 向输入输出提供者注册输入回调
        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }

        sendIoEventListener = listener;

        // 当前发送的数据附加到回调中
        outputCallback.setAttach(args);

        return ioProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            // 解除注册回调
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);

            // 关闭
            CloseUtils.close(channel);

            // 回调当前Channel已关闭
            listener.onChannelClosed(channel);
        }
    }

    public interface OnChannelStatusChangedListener {

        /**
         * 当通道关闭时
         *
         * @param channel 通道
         */
        void onChannelClosed(SocketChannel channel);
    }
}
