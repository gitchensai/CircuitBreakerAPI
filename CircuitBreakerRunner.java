package com.baidu.crm.cloudwarefs.CircuitBreaker;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author chensai
 * @version 1.0
 * @description
 * @date 2020-09-22
 */
public class CircuitBreakerRunner {

    /**
     * 请求成功标记
     */
    public static final int SUCC_FLAG = 0;

    /**
     * 请求失败标记
     */
    public static final int FAIL_FLAG = 1;

    public static Timer timer = new Timer();

    public static Object lock = new Object();

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
            (Runtime.getRuntime().availableProcessors() * 2) + 1, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000), new ThreadPoolExecutor.AbortPolicy());

    public static void run(CircuitBreaker circuitBreaker, Callable callable) {

        // 直接进入fallback逻辑
        if (circuitBreaker.getState().get()) {
            circuitBreaker.fallback();
            return;
        }
        synchronized (lock) {
            try {
                if (!circuitBreaker.getState().get()) {
                    Future submit = threadPoolExecutor.submit(callable);
                    if (submit.isDone()) {
                        updateQueue(circuitBreaker, SUCC_FLAG);
                    }
                    return;
                }
            } catch (Exception e) {
                circuitBreaker.getCurrentFailNum().incrementAndGet();
                updateQueue(circuitBreaker, FAIL_FLAG);
                // 如果满足断线条件
                if (circuitBreaker.getConcurrentLinkedQueue().size() == circuitBreaker.getTotalNum() &&
                        circuitBreaker.getCurrentFailNum().get() == circuitBreaker.getFailNum()) {
                    updateState(circuitBreaker);
                }
            }
        }
    }

    /**
     * 修改断线状态
     *
     * @param circuitBreaker
     */
    public static void updateState(CircuitBreaker circuitBreaker) {
        // 进入断线状态
        circuitBreaker.getState().compareAndSet(false, true);
        // 定时任务，退出断线状态
        timer.schedule(new TimerTask() {
            public void run() {
                circuitBreaker.getState().compareAndSet(true, false);
            }
        }, circuitBreaker.getTimeDelay());
        // 清空记录
        circuitBreaker.getCurrentFailNum().compareAndSet(circuitBreaker.getFailNum(), 0);
        circuitBreaker.getConcurrentLinkedQueue().clear();
    }

    public static void updateQueue(CircuitBreaker circuitBreaker, int flag) {
        if (circuitBreaker.getConcurrentLinkedQueue().size() == circuitBreaker.getTotalNum()) {
            Integer poll = circuitBreaker.getConcurrentLinkedQueue().poll();
            if (poll == FAIL_FLAG) {
                circuitBreaker.getCurrentFailNum().decrementAndGet();
            }
        }
        circuitBreaker.getConcurrentLinkedQueue().add(flag);
    }
}
