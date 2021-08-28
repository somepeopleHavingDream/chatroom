package org.yangxin.socket.lib.core;

import lombok.Getter;

/**
 * 公共数据的封装，
 * 提供了类型以及基本的长度定义
 *
 * @author yangxin
 * 2021/8/28 下午1:06
 */
@Getter
public class Packet {

    private byte type;
    protected int length;
}
