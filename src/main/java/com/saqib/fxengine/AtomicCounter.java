package com.saqib.fxengine;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicCounter {
    private long p1, p2, p3, p4, p5, p6, p7, p8;
    private final AtomicLong nextOrderId = new AtomicLong(1);
    private long p9, p10, p11, p12, p13, p14, p15, p16;
    private volatile int processedOrders = 0;
    private long p17, p18, p19, p20, p21, p22, p23, p24;
    private CountDownLatch processingLatch = new CountDownLatch(1);
    
    public long nextOrderId() {
        return nextOrderId.getAndIncrement();
    }
    
    public void increment() {
        processedOrders++;
        if (processedOrders >= 10_000) {
            processingLatch.countDown();
        }
    }
    
    public void reset() {
        processedOrders = 0;
        processingLatch = new CountDownLatch(1);
    }
    
    public CountDownLatch getProcessingLatch() {
        return processingLatch;
    }
}