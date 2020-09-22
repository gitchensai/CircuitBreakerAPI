package com.baidu.crm.cloudwarefs.CircuitBreaker;

import lombok.Data;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chensai
 * @version 1.0
 * @description
 * @date 2020-09-22
 */
@Data
public class CircuitBreaker {

    /**
     * 当前失败次数
     */
    private AtomicInteger currentFailNum;

    /**
     * 失败数阈值
     */
    private Integer failNum = 10;

    /**
     * 请求总数
     */
    private Integer totalNum = 50;

    private ConcurrentLinkedQueue<Integer> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
    /**
     * true:断线状态；false:正常状态
     */
    private AtomicBoolean state = new AtomicBoolean(false);

    /**
     * 断开时间
     */
    private Long timeDelay = 1000L;

    /**
     * 补偿方法
     */
    public void fallback() {
        System.out.println("this is fallback method");
    }

}
