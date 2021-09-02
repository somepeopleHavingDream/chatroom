package org.yangxin.socket.lib.box;

import org.yangxin.socket.lib.core.SendPacket;

import java.io.*;

/**
 * @author yangxin
 * 2021/8/28 下午1:16
 */
public class FileSendPacket extends SendPacket<FileInputStream> {

    public FileSendPacket(File file) {
        // 此属性来自于Packet
        this.length = file.length();
    }

    @Override
    protected FileInputStream createStream() {
        return null;
    }
}
