package org.yangxin.socket.client;

import org.yangxin.socket.client.bean.ServerInfo;
import org.yangxin.socket.foo.Foo;
import org.yangxin.socket.lib.box.FileSendPacket;
import org.yangxin.socket.lib.core.IoContext;
import org.yangxin.socket.lib.impl.IoSelectorProvider;

import java.io.*;

/**
 * @author yangxin
 * 2021/8/23 下午8:55
 */
@SuppressWarnings("AlibabaUndefineMagicConstant")
public class Client {

    public static void main(String[] args) throws IOException {
        // 获得缓存路径
        File cachePath = Foo.getCacheDir("client");

        // 设置并启动输入输出上下文环境
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .start();

        // udp搜寻服务端信息，收到服务端信息的响应
        ServerInfo info = UdpSearcher.searchServer(10000);
        System.out.println("Server: " + info);

        // 如果服务端信息存在
        if (info != null) {
            TcpClient client = null;

            try {
                // tcp连接服务端，获得tcp客户端实例
                client = TcpClient.startWith(info, cachePath);
                if (client == null) {
                    return;
                }

                // 写入数据
                write(client);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (client != null) {
                    client.exit();
                }
            }
        }

        IoContext.close();
    }

    /**
     * 写入数据
     *
     * @param client 要写入数据的tcp客户端
     * @throws IOException 输入输出异常
     */
    private static void write(TcpClient client) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String msg = input.readLine();
            if ("00bye00".equalsIgnoreCase(msg)) {
                break;
            }

            // asdwqrqwrqwrqwe
            // --f url
            if (msg.startsWith("--f")) {
                String[] array = msg.split(" ");
                if (array.length >= 2) {
                    String filePath = array[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        FileSendPacket packet = new FileSendPacket(file);
                        client.send(packet);
                        continue;
                    }
                }
            }

            // 发送字符串
            client.send(msg);
        } while (true);
    }
}
