package org.yangxin.socket.lib.core;

import java.io.OutputStream;

/**
 * 接收包的定义
 *
 * @author yangxin
 * 2021/8/28 下午1:13
 */
public abstract class ReceivePacket<T extends OutputStream> extends Packet<T> {
}
