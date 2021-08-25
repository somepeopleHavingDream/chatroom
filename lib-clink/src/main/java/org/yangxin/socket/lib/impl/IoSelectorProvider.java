package org.yangxin.socket.lib.impl;

import org.yangxin.socket.lib.core.IoProvider;
import org.yangxin.socket.lib.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yangxin
 * 2021/8/24 下午8:09
 */
@SuppressWarnings({"AlibabaThreadPoolCreation", "AlibabaAvoidManuallyCreateThread"})
public class IoSelectorProvider implements IoProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /*
        是否处于某个过程
     */

    /**
     * 是否处于注册输入过程
     */
    private final AtomicBoolean isRegInput = new AtomicBoolean(false);

    /**
     * 是否处于注册输出过程
     */
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;

    private final Map<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final Map<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    private final ExecutorService inputHandlePool;
    private final ExecutorService outputHandlePool;

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();

        inputHandlePool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Output-Thread-"));

        // 开始输入输出的监听
        startRead();
    }

    private void startRead() {
        Thread thread = new Thread("Clink IoSelectorProvider ReadSelector Thread") {

            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            waitSelection(isRegInput);
                            continue;
                        }

                        Set<SelectionKey> set = writeSelector.selectedKeys();
                        for (SelectionKey key : set) {
                            if (key.isValid()) {
                                handleSelection(key, SelectionKey.OP_WRITE, outputCallbackMap, outputHandlePool);
                            }
                        }
                        set.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        return registerSelection(channel, readSelector, SelectionKey.OP_READ, isRegInput, inputCallbackMap, callback)
                != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap, callback)
                != null;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap);
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writeSelector, outputCallbackMap);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlePool.shutdown();
            outputHandlePool.shutdown();

            inputCallbackMap.clear();
            outputCallbackMap.clear();

            readSelector.wakeup();
            writeSelector.wakeup();

            CloseUtils.close(readSelector, writeSelector);
        }
    }

    private void waitSelection(AtomicBoolean locker) {
        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 注册选择
     *
     * @param channel 通道
     * @param selector 选择器
     * @param registerOps 被注册的操作
     * @param locker 锁
     * @param map SelectionKey -> Runnable
     * @param runnable 可运行实例
     * @return 注册成功之后的选择键
     */
    private static SelectionKey registerSelection(SocketChannel channel,
                                                  Selector selector,
                                                  int registerOps,
                                                  AtomicBoolean locker,
                                                  Map<SelectionKey, Runnable> map,
                                                  Runnable runnable) {
        synchronized (locker) {
            // 设置锁定状态
            // 标志当前输入输出选择器提供者处于注册服务过程或者处于注册输出过程
            locker.set(true);

            try {
                // 唤醒当前的selector，让selector不处于select()状态
                selector.wakeup();

                SelectionKey key = null;
                if (channel.isRegistered()) {
                    // 查询通道是否已经被注册过
                    key = channel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }

                // 如果该通道未被注册过
                if (key == null) {
                    // 注册selector得到Key
                    key = channel.register(selector, registerOps);
                    // 注册回调
                    map.put(key, runnable);
                }

                // 返回选择键
                return key;
            } catch (ClosedChannelException e) {
                return null;
            } finally {
                // 解除锁定状态
                locker.set(false);
                try {
                    // 通知
                    locker.notify();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void unRegisterSelection(SocketChannel channel,
                                            Selector selector,
                                            Map<SelectionKey, Runnable> map) {
        if (channel.isRegistered()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                // 取消监听的方法
                key.cancel();
                map.remove(key);
                selector.wakeup();
            }
        }
    }

    private static void handleSelection(SelectionKey key,
                                        int keyOps,
                                        Map<SelectionKey, Runnable> map,
                                        ExecutorService pool) {
        // 重点
        // 取消继续对KeyOps的监听
        key.interestOps(key.readyOps() & ~keyOps);

        Runnable runnable = null;
        try {
            runnable = map.get(key);
        } catch (Exception ignored) {
        }

        if (runnable != null && !pool.isShutdown()) {
            // 异步调度
            pool.execute(runnable);
        }
    }

    private static class IoProviderThreadFactory implements ThreadFactory {

        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        private IoProviderThreadFactory(String namePrefix) {
            SecurityManager manager = System.getSecurityManager();
            this.group = (manager != null) ? manager.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    }
}
