package org.yangxin.socket.foo;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author yangxin
 * 2021/9/8 下午9:28
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class Foo {

    private static final String CACHE_DIR = "cache";

    /**
     * 获得一个指定的缓存目录
     *
     * @param dir 目录
     * @return 指定的缓存目录
     */
    public static File getCacheDir(String dir) {
        String path = System.getProperty("user.dir")
                + (File.separator + CACHE_DIR)
                + (File.separator + dir);
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("Create path error: " + path);
            }
        }
        return file;
    }

    public static File createRandomTemp(File parent) {
        String s = UUID.randomUUID() + ".tmp";
        File file = new File(parent, s);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
