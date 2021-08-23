package org.yangxin.socket.server;

import org.yangxin.socket.foo.constants.TcpConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author yangxin
 * 2021/8/12 15:52
 */
public class Server {

    public static void main(String[] args) throws IOException {
        // 启动tcp服务端
        TcpServer server = new TcpServer(TcpConstants.PORT_SERVER);
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
            server.broadcast(str);
        } while (!"00bye00".equalsIgnoreCase(str));

        // 关闭相关资源
        UdpProvider.stop();
        server.stop();
    }
}
