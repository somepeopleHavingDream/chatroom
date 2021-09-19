package org.yangxin.socket.lib.frames;

import org.yangxin.socket.lib.core.IoArgs;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * @author yangxin
 * 2021/9/15 下午9:07
 */
public class ReceiveEntityFrame extends AbstractReceiveFrame {

    private WritableByteChannel channel;

    public ReceiveEntityFrame(byte[] header) {
        super(header);
    }

    public void bindPacketChannel(WritableByteChannel channel) {
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return args.writeTo(channel);
    }
}
