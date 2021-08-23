package org.yangxin.socket.foo.constants;

/**
 * @author yangxin
 * 2021/8/12 16:38
 */
public class UdpConstants {

    /**
     * 公用头部
     */
    public static byte[] header = new byte[]{7, 7, 7, 7, 7, 7, 7, 7};

    /**
     * 服务器固话Udp接收端口
     */
    public static Integer portServer = 30201;

    /**
     * 客户端回送端口
     */
    public static Integer portClientResponse = 30202;
}
