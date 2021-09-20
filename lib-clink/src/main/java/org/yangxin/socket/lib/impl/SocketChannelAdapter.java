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
    private IoArgs.IoArgsEventProcessor receiveIoEventProcessor;

    /**
     * 发送的输入输出参数事件监听器
     */
    private IoArgs.IoArgsEventProcessor sendIoEventProcessor;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        // 配置当前通道非阻塞
        channel.configureBlocking(false);
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) {
        receiveIoEventProcessor = processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        // 如果该套接字通道适配器已关闭，则抛出当前通道已被关闭的输入输出异常
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }

        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public void setSendListener(IoArgs.IoArgsEventProcessor processor) {
        sendIoEventProcessor = processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        // 如果当前套接字通道适配者已关闭，则抛出当前通道已关闭的输入输出异常
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }

        // 向输入输出提供者注册输出事件
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

    /**
     * 处理输入的回调
     */
    private final IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {

        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }

            IoArgs.IoArgsEventProcessor processor = receiveIoEventProcessor;
            IoArgs args = processor.provideIoArgs();

            try {
                // 具体的读取操作
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null."));
                } else if (args.readFrom(channel) > 0) {
                    // 读取完成回调
                    processor.onConsumeCompleted(args);
                } else {
                    processor.onConsumeFailed(args, new IOException("Cannot read any data!"));
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    /**
     * 输出回调，即当选择器监听到写事件时，并做处理时，最终会调用此回调方法
     */
    private final IoProvider.HandleOutputCallback outputCallback = new IoProvider.HandleOutputCallback() {

        @Override
        protected void canProviderOutput() {
            // 如果当前套接字通道适配者已经关闭，则直接返回
            if (isClosed.get()) {
                return;
            }

            // 获得发送输入输出事件处理器
            IoArgs.IoArgsEventProcessor processor = sendIoEventProcessor;
            // 从输入输出参数事件处理器中拿到输入输出参数
            IoArgs args = processor.provideIoArgs();

            try {
                // 具体的写操作
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null."));
                } else if (args.writeTo(channel) > 0) {
                    // 消费完成时回调
                    processor.onConsumeCompleted(args);
                } else {
                    // 消费失败时回调
                    processor.onConsumeFailed(args, new IOException("Cannot write any data!"));
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    public interface OnChannelStatusChangedListener {

        /**
         * 当通道关闭时
         *
         * @param channel 通道
         */
        void onChannelClosed(SocketChannel channel);
    }
}
