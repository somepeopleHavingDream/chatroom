package org.yangxin.socket.lib.core;

import org.yangxin.socket.lib.impl.SocketChannelAdapter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @author yangxin
 * 2021/8/24 11:33
 */
public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;

    public void setup(SocketChannel channel) throws IOException {
        this.channel = channel;

        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);

        this.sender = adapter;
        this.receiver = adapter;

        readNextMessage();
    }

    private void readNextMessage() {
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

    private IoArgs.IoArgsEventListener echoReceiveListener = new IoArgs.IoArgsEventListener() {

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
        System.out.println(key.toString() + ":" + str);
    }
}
