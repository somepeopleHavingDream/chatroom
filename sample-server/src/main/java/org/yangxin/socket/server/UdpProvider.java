package org.yangxin.socket.server;

import lombok.Setter;
import org.yangxin.socket.foo.constants.UdpConstants;
import org.yangxin.socket.lib.utils.ByteUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author yangxin
 * 2021/8/12 16:35
 */
@SuppressWarnings("AlibabaAvoidManuallyCreateThread")
public class UdpProvider {

    /**
     * 提供者实例
     */
    private static Provider providerInstance;

    static void start(Integer port) {
        // 先停止udp提供者
        stop();

        // 实例并启动提供者
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn, port);
        Thread thread = new Thread(provider);
        provider.setThread(thread);
        thread.start();
        providerInstance = provider;
    }

    static void stop() {
        if (providerInstance != null) {
            // udp提供者实例退出
            providerInstance.exit();
        }
    }

    /**
     * 提供者
     */
    @Setter
    private static class Provider implements Runnable {

        private final byte[] sn;
        private final Integer port;
        private DatagramSocket datagramSocket = null;
        private Thread thread;

        /**
         * 存储消息的Buffer
         */
        final byte[] buffer = new byte[128];

        private Provider(String sn, Integer port) {
            this.sn = sn.getBytes();
            this.port = port;
        }

        @Override
        public void run() {
            System.out.println("UdpProvider started.");

            try {
                // 监听20000端口
                datagramSocket = new DatagramSocket(UdpConstants.portServer);
                // 接收消息的Packet
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                while (!Thread.currentThread().isInterrupted()) {
                    // 接收
                    datagramSocket.receive(receivePacket);

                    // 打印接收到的信息与发送者的信息
                    // 发送者的ip地址
                    String clientIp = receivePacket.getAddress().getHostAddress();
                    int clientPort = receivePacket.getPort();
                    int clientDataLength = receivePacket.getLength();
                    byte[] clientData = receivePacket.getData();
                    boolean isValid = clientDataLength >= (UdpConstants.header.length + 2 + 4)
                            && ByteUtils.startsWith(clientData, UdpConstants.header);

                    System.out.println("UdpProvider receive from ip: " + clientIp
                            + "\tport: " + clientPort
                            + "\tdataValid: " + isValid);

                    if (!isValid) {
                        // 无效继续
                        continue;
                    }

                    // 解析命令与回送端口
                    int index = UdpConstants.header.length;
                    short cmd = (short) ((clientData[index++] << 8 | (clientData[index++] & 0xff)));
                    int responsePort = (((clientData[index++]) << 24)
                            | ((clientData[index++] & 0xff) << 16)
                            | ((clientData[index++] & 0xff) << 8)
                            | ((clientData[index] & 0xff)));

                    // 判断合法性
                    if (cmd == 1 && responsePort > 0) {
                        // 构建一份回送数据
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UdpConstants.header)
                                .putShort((short) 2)
                                .putInt(port)
                                .put(sn);

                        int length = byteBuffer.position();
                        // 直接根据发送者构建一份回送信息
                        DatagramPacket responsePacket = new DatagramPacket(buffer,
                                length, receivePacket.getAddress(), responsePort);
                        datagramSocket.send(responsePacket);

                        System.out.println("UdpProvider response to: " + clientIp
                                + "\tport: " + responsePort
                                + "\tdataLength: " + length);
                    } else {
                        System.out.println("UdpProvider receive cmd nonsupport; cmd: " + cmd + "\tport: " + port);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                close();
            }

            // 完成
            System.out.println("UdpProvider finished.");
        }

        private void close() {
            // 关闭udp数据包套接字
            if (datagramSocket != null) {
                datagramSocket.close();
                datagramSocket = null;
            }
        }

        public void exit() {
            thread.interrupt();
            close();
        }
    }
}
