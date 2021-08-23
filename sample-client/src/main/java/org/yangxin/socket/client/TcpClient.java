package org.yangxin.socket.client;

import lombok.Setter;
import org.yangxin.socket.client.bean.ServerInfo;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.*;
import java.net.*;

/**
 * @author yangxin
 * 2021/8/23 下午8:59
 */
@SuppressWarnings("AlibabaAvoidManuallyCreateThread")
public class TcpClient {

    private final Socket socket;
    private final ReadHandler readHandler;
    private final PrintStream printStream;

    public TcpClient(Socket socket, ReadHandler readHandler) throws IOException {
        this.socket = socket;
        this.readHandler = readHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public void exit() {
        readHandler.exit();
        CloseUtils.close(printStream);
        CloseUtils.close(socket);
    }

    public void send(String msg) {
        printStream.println(msg);
    }

    public static TcpClient startWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();
        // 超时时间
        socket.setSoTimeout(3000);

        // 连接本地，端口2000；超时时间3000ms
        socket.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()), 3000);

        System.out.println("已发起服务器连接，并进入后续流程~");
        System.out.println("客户端信息：" + socket.getLocalAddress() + " P: " + socket.getLocalPort());
        System.out.println("服务端信息：" + socket.getInetAddress() + " P: " + socket.getPort());

        try {
            ReadHandler handler = new ReadHandler(socket.getInputStream());
            Thread thread = new Thread(handler);
            handler.setThread(thread);
            thread.start();

            return new TcpClient(socket, handler);
        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socket);
        }

        return null;
    }

    @Setter
    private static class ReadHandler implements Runnable {

//        private boolean done = false;
        private final InputStream inputStream;
        private Thread thread;

        private ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                // 得到输入流，用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    String msg;
                    try {
                        // 客户端拿到一条数据
                        msg = socketInput.readLine();
                    } catch (IOException e) {
                        continue;
                    }

                    if (msg == null) {
                        System.out.println("连接已关闭，无法读取数据！");
                        break;
                    }
                    // 打印到屏幕
                    System.out.println(msg);
                } while (!Thread.currentThread().isInterrupted());
            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.out.println("连接异常断开：" + e.getMessage());
                }
            } finally {
                // 连接关闭
                CloseUtils.close(inputStream);
            }
        }

        private void exit() {
            thread.interrupt();
            CloseUtils.close(inputStream);
        }
    }
}
