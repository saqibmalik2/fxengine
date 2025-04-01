package com.saqib.fxengine;

import java.util.concurrent.TimeUnit;

/**
 * A high-performance FX trading engine that coordinates order books and messaging for ultra-low-latency order processing.
 * <p>
 * This singleton class integrates {@link OrderBookManager} for matching orders and {@link AeronMessaging} for high-speed
 * trade communication. Designed to process over 1 million orders per second with sub-microsecond latency, it leverages
 * Aeron for messaging and ScyllaDB for persistence (pending implementation).
 * </p>
 * @author Saqib
 * @version 0.0.1-SNAPSHOT
 */
public class LowLatencyMatchingEngine {
    private static final LowLatencyMatchingEngine INSTANCE = new LowLatencyMatchingEngine();
    
    private final OrderBookManager orderBookManager;
    private final AeronMessaging aeronMessaging;
    private final AtomicCounter counter;
    
    private LowLatencyMatchingEngine() {
        counter = new AtomicCounter();  // Tracks processed orders
        orderBookManager = new OrderBookManager();  // Manages order books per currency pair
        aeronMessaging = new AeronMessaging(orderBookManager, counter);  // Handles trade messaging
    }
    
    /**
     * Retrieves the singleton instance of the trading engine.
     * @return the single instance of {@link LowLatencyMatchingEngine}
     */
    public static LowLatencyMatchingEngine getInstance() {
        return INSTANCE;
    }
    
    /**
     * Submits an order to the engine for processing and matching.
     * <p>
     * Delegates to {@link AeronMessaging#submitOrder} to publish the order via Aeron, which then interacts with the
     * appropriate {@link OrderBook} for matching. Returns a unique order ID.
     * </p>
     * @param symbol the currency pair (e.g., EUR_USD)
     * @param side the order side (Constants.BUY or Constants.SELL)
     * @param price the order price in pips (scaled by 10^5, e.g., 1.23450 = 123450)
     * @param quantity the order volume in lots
     * @param userId the submitting user’s ID
     * @return the generated order ID
     */
    public long submitOrder(CurrencyPair symbol, byte side, long price, int quantity, long userId) {
        return aeronMessaging.submitOrder(symbol, side, price, quantity, userId);
    }
    
    /**
     * Warms up the JVM and engine components with dummy orders to optimize JIT compilation and Aeron buffers.
     * <p>
     * Executes a series of orders to prime the system, ensuring consistent performance during benchmark runs.
     * Resets the counter post-warmup for accurate metrics.
     * </p>
     * @param warmUpCount number of warmup orders to process
     * @param symbol the currency pair for warmup orders
     * @param userId the user ID for warmup orders
     */
    public void warmUp(int warmUpCount, CurrencyPair symbol, long userId) {
        System.out.println("Warming up JVM...");
        for (int i = 0; i < warmUpCount; i++) {
            byte side = (i % 2 == 0) ? Constants.BUY : Constants.SELL;  // Alternates buy/sell
            long price = 123450 + (i % 20 - 10);  // Price ±0.0010 around 1.23450
            submitOrder(symbol, side, price, 100, userId);
        }
        try {
            counter.getProcessingLatch().await(1, TimeUnit.SECONDS);  // Waits for orders to clear
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Restores interrupt status
        }
        counter.reset();  // Clears counter for test run
    }
    
    /**
     * Provides access to the engine’s order counter for performance metrics.
     * @return the {@link AtomicCounter} tracking processed orders
     */
    public AtomicCounter getCounter() {
        return counter;
    }
    
    public static void main(String[] args) {
        LowLatencyMatchingEngine engine = getInstance();
        engine.warmUp(10_000, CurrencyPair.EUR_USD, 1);  // Basic warmup for demo
    }
}