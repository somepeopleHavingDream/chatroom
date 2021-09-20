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

    /**
     * 用于文件传输的缓存路径
     */
    private final File cachePath;

    public TcpClient(SocketChannel socketChannel, File cachePath) throws IOException {
        // 设置用于文件传输的缓存路径
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

    /**
     * tcp连接服务端
     *
     * @param info 服务端的tcp信息
     * @param cachePath 用于文件传输的缓存目录
     * @return tcp客户端
     * @throws IOException 输入输出异常
     */
    public static TcpClient startWith(ServerInfo info, File cachePath) throws IOException {
        // 打开一个套接字通道
        SocketChannel socketChannel = SocketChannel.open();

        // 连接本地，端口2000；超时时间3000ms
        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        // 打印客户端和服务端的信息
        System.out.println("已发起服务器连接，并进入后续流程~");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress());
        System.out.println("服务端信息：" + socketChannel.getRemoteAddress());

        try {
            // 实例化并返回tcp客户端实例
            return new TcpClient(socketChannel, cachePath);
        } catch (Exception e) {
            System.out.println("连接异常");
            // 若捕获异常，则关闭该套接字通道
            CloseUtils.close(socketChannel);
        }

        return null;
    }
}
