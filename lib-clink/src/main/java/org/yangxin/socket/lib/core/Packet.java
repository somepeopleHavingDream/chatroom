package org.yangxin.socket.lib.core;

import lombok.Getter;

import java.io.Closeable;

/**
 * 公共数据的封装，
 * 提供了类型以及基本的长度定义
 *
 * @author yangxin
 * 2021/8/28 下午1:06
 */
public abstract class Packet implements Closeable {

    private byte type;

    protected int length;

    public byte type() {
        return type;
    }

    public int length() {
        return length;
    }
}
