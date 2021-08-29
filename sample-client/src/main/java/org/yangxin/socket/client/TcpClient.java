package org.yangxin.socket.client;

import lombok.Setter;
import org.yangxin.socket.client.bean.ServerInfo;
import org.yangxin.socket.lib.core.Connection;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;

/**
 * @author yangxin
 * 2021/8/23 下午8:59
 */
@SuppressWarnings("AlibabaAvoidManuallyCreateThread")
public class TcpClient extends Connection {

    public TcpClient(SocketChannel socketChannel) throws IOException {
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

    public static TcpClient startWith(ServerInfo info) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();

        // 连接本地，端口2000；超时时间3000ms
        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("已发起服务器连接，并进入后续流程~");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress());
        System.out.println("服务端信息：" + socketChannel.getRemoteAddress());

        try {
            return new TcpClient(socketChannel);
        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socketChannel);
        }

        return null;
    }
}
