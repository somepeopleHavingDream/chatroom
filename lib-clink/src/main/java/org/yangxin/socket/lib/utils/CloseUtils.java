package org.yangxin.socket.lib.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author yangxin
 * 2021/8/23 11:39
 */
public class CloseUtils {

    public static void close(Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        for (Closeable closeable : closeables) {
            if (closeable ==null) {
                continue;
            }

            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
