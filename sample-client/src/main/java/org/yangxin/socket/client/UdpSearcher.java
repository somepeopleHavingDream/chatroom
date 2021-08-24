package org.yangxin.socket.client;

import org.yangxin.socket.client.bean.ServerInfo;
import org.yangxin.socket.foo.constants.UdpConstants;
import org.yangxin.socket.lib.utils.ByteUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author yangxin
 * 2021/8/12 17:37
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "AlibabaAvoidManuallyCreateThread"})
public class UdpSearcher {

    public static final Integer LISTEN_PORT = UdpConstants.portClientResponse;

    public static ServerInfo searchServer(Integer timeout) {
        System.out.println("UdpSearcher started.");

        // 成功收到回送的栅栏
        CountDownLatch receiveLatch = new CountDownLatch(1);
        Listener listener = null;
        try {
            // 开启监听
            listener = listen(receiveLatch);
            // 发送广播
            sendBroadcast();
            // 计时等待，直到监听到一份服务端信息
            receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 完成
        System.out.println("UdpSearcher finished.");
        if (listener == null) {
            return null;
        }

        // 从收到的服务端信息中，返回一份服务端信息
        List<ServerInfo> devices = listener.getServerAndClose();
        if (devices.size() > 0) {
            return devices.get(0);
        }

        return null;
    }

    private static Listener listen(CountDownLatch receiveLatch) throws InterruptedException {
        System.out.println("UdpSearcher start listen.");

        // 开启监听
        CountDownLatch startDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, startDownLatch, receiveLatch);
        Thread thread = new Thread(listener);
        thread.start();

        // 等待监听开启
        startDownLatch.await();
        return listener;
    }

    private static void sendBroadcast() throws IOException {
        System.out.println("UdpSearcher sendBroadcast started.");

        // 作为搜索方，让系统自动分配端口
        DatagramSocket datagramSocket = new DatagramSocket();

        // 构建一份请求数据
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        // 头部
        byteBuffer.put(UdpConstants.header);
        // cmd命名
        byteBuffer.putShort((short) 1);
        // 回送端口信息
        byteBuffer.putInt(LISTEN_PORT);
        // 直接构建packet
        DatagramPacket requestPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);
        // 广播地址
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        // 设置服务端端口
        requestPacket.setPort(UdpConstants.portServer);

        // 发送
        datagramSocket.send(requestPacket);
        datagramSocket.close();

        // 完成
        System.out.println("UdpSearcher sendBroadcast finished.");
    }

    private static class Listener implements Runnable {
        private final Integer listenPort;
        private final CountDownLatch startDownLatch;
        private final CountDownLatch receiveDownLatch;
        private final List<ServerInfo> serverInfoList = new ArrayList<>();
        private final byte[] buffer = new byte[128];
        private final Integer minLength = UdpConstants.header.length + 2 + 4;
        private boolean done = false;
        private DatagramSocket datagramSocket = null;

        private Listener(Integer listenPort, CountDownLatch startDownLatch, CountDownLatch receiveDownLatch) {
            this.listenPort = listenPort;
            this.startDownLatch = startDownLatch;
            this.receiveDownLatch = receiveDownLatch;
        }


        @Override
        public void run() {
            // 通知已启动
            startDownLatch.countDown();

            try {
                // 监听回送端口
                datagramSocket = new DatagramSocket(listenPort);
                // 构建接收实体
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                while (!done) {
                    // 接收
                    datagramSocket.receive(receivePacket);

                    // 打印接收到的信息与发送者的信息
                    // 发送者的ip地址
                    String ip = receivePacket.getAddress().getHostAddress();
                    int port = receivePacket.getPort();
                    int dataLength = receivePacket.getLength();
                    byte[] data = receivePacket.getData();
                    boolean isValid = dataLength >= minLength
                            && ByteUtils.startsWith(data, UdpConstants.header);

                    System.out.println("UdpSearcher receive from ip: " + ip
                            + "\tport: " + port
                            + "\tdataValid: " + isValid);

                    if (!isValid) {
                        // 无效继续
                        continue;
                    }

                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, UdpConstants.header.length, dataLength);
                    final short cmd = byteBuffer.getShort();
                    final int serverPort = byteBuffer.getInt();
                    if (cmd != 2 || serverPort <= 0) {
                        System.out.println("UdpSearcher receive cmd: " + "\tserverPort: " + serverPort);
                        continue;
                    }

                    // 从服务端的udp提供者那获得返回的sn
                    String sn = new String(buffer, minLength, dataLength - minLength);

                    // 构建服务端信息，并存储服务端信息（可能会有多个服务端）
                    ServerInfo info = new ServerInfo(serverPort, ip, sn);
                    serverInfoList.add(info);

                    // 成功接收到一份服务端信息
                    receiveDownLatch.countDown();
                }
            } catch (Exception ignored) {
            } finally {
                close();
            }

            System.out.println("UdpSearcher listener finished.");
        }

        private void close() {
            if (datagramSocket != null) {
                datagramSocket.close();
                datagramSocket = null;
            }
        }

        public List<ServerInfo> getServerAndClose() {
            done = true;
            close();
            return serverInfoList;
        }
    }
}
