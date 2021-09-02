package org.yangxin.socket.lib.box;

import org.yangxin.socket.lib.core.ReceivePacket;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

/**
 * @author yangxin
 * 2021/8/28 下午1:18
 */
public class StringReceivePacket extends ReceivePacket<ByteArrayOutputStream> {

    private String string;

    public StringReceivePacket(int length) {
        this.length = length;
    }

    public String string() {
        return string;
    }

    @Override
    protected void closeStream(ByteArrayOutputStream stream) throws IOException {
        super.closeStream(stream);
        string = new String(stream.toByteArray());
    }

    @Override
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }
}
