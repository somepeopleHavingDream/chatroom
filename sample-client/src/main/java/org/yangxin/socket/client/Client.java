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

        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .start();

        ServerInfo info = UdpSearcher.searchServer(10000);
        System.out.println("Server: " + info);

        if (info != null) {
            TcpClient client = null;

            try {
                client = TcpClient.startWith(info, cachePath);
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

        IoContext.close();
    }

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
