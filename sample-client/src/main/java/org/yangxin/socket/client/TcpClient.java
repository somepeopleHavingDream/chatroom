package org.yangxin.socket.client;

import org.yangxin.socket.client.bean.ServerInfo;
import org.yangxin.socket.foo.Foo;
import org.yangxin.socket.lib.core.Connection;
import org.yangxin.socket.lib.core.Packet;
import org.yangxin.socket.lib.core.ReceivePacket;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author yangxin
 * 2021/8/23 下午8:59
 */
@SuppressWarnings("AlibabaAvoidManuallyCreateThread")
public class TcpClient extends Connection {

    private final File cachePath;

    public TcpClient(SocketChannel socketChannel, File cachePath) throws IOException {
        this.cachePath = cachePath;
        // 设置一些参数
        setup(socketChannel);
    }

    public void exit() {
        CloseUtils.close(this);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        System.out.println("连接已关闭，无法读取数据。");
    }

    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivedPacket(ReceivePacket<?, ?> packet) {
        super.onReceivedPacket(packet);

        if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            String string = (String) packet.entity();
            System.out.println(key + ":" + string);
        }
    }

    public static TcpClient startWith(ServerInfo info, File cachePath) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();

        // 连接本地，端口2000；超时时间3000ms
        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("已发起服务器连接，并进入后续流程~");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress());
        System.out.println("服务端信息：" + socketChannel.getRemoteAddress());

        try {
            return new TcpClient(socketChannel, cachePath);
        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socketChannel);
        }

        return null;
    }
}
