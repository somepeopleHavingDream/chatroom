package org.yangxin.socket.client;

import org.yangxin.socket.client.bean.ServerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author yangxin
 * 2021/8/23 下午8:55
 */
@SuppressWarnings("AlibabaUndefineMagicConstant")
public class Client {

    public static void main(String[] args) {
        ServerInfo info = UdpSearcher.searchServer(10000);
        System.out.println("Server: " + info);

        if (info != null) {
            TcpClient client = null;

            try {
                client = TcpClient.startWith(info);
                if (client == null) {
                    return;
                }

                write(client);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (client != null) {
                    client.exit();
                }
            }
        }
    }

    private static void write(TcpClient client) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String msg = input.readLine();
            // 发送到服务器
            client.send(msg);

            if ("00bye00".equalsIgnoreCase(msg)) {
                break;
            }
        } while (true);
    }
}
