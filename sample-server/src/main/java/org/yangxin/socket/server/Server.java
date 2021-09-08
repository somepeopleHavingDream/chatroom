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
public class Server {

    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("server");
        // 启动输入输出上下文，监听处理
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .start();

        // 启动tcp服务端，监听注册
        TcpServer server = new TcpServer(TcpConstants.PORT_SERVER, cachePath);
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
            str = reader.readLine();
            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }

            // 发送字符串
            server.broadcast(str);
        } while (true);

        // 关闭相关资源
        UdpProvider.stop();
        server.stop();

        IoContext.close();
    }
}
