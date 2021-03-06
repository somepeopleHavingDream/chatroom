package org.yangxin.socket.server;

import org.yangxin.socket.foo.Foo;
import org.yangxin.socket.foo.constants.TcpConstants;
import org.yangxin.socket.lib.core.IoContext;
import org.yangxin.socket.lib.impl.IoSelectorProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author yangxin
 * 2021/8/12 15:52
 */
@SuppressWarnings("AlibabaUndefineMagicConstant")
public class Server {

    public static void main(String[] args) throws IOException {
        // 获得一个缓存目录
        File cachePath = Foo.getCacheDir("server");

        // 启动输入输出上下文，监听处理
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .start();

        // 启动tcp服务端，监听注册
        TcpServer server = new TcpServer(TcpConstants.PORT_SERVER, cachePath);
        // tcp服务端启动
        boolean isSucceed = server.start();
        if (!isSucceed) {
            System.out.println("Start tcp server failed!");
            return;
        }

        // 以udp的方式广播tcp服务端的监听端口
        UdpProvider.start(TcpConstants.PORT_SERVER);

        // 监听键盘输入
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            // 从控制台读取一行，若读取到的字符串为“00bye00”，则退出当前键盘事件的循环监听
            str = reader.readLine();
            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }

            // 发送从控制台读取到的字符串
            server.broadcast(str);
        } while (true);

        // 关闭相关资源（udp提供者和tcp服务端）
        UdpProvider.stop();
        server.stop();

        // 关闭输入输出上下文
        IoContext.close();
    }
}
