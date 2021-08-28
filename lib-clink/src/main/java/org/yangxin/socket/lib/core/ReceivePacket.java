package org.yangxin.socket.lib.core;

/**
 * 接收包的定义
 *
 * @author yangxin
 * 2021/8/28 下午1:13
 */
public abstract class ReceivePacket extends Packet {

    /**
     * 保存接收到的数据
     *
     * @param bytes 把接收到的字节存入到该数组中
     * @param count 需要保存多少个字节
     */
    public abstract void save(byte[] bytes, int count);
}
