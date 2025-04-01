package com.saqib.fxengine;

/**
 * Benchmarking utility for testing the performance of {@link LowLatencyMatchingEngine}.
 * <p>
 * This class executes a multi-threaded test to measure orders processed per second, targeting over 1 million orders/sec
 * with sub-microsecond latency. It includes a warmup phase to optimize JVM performance and reports throughput metrics.
 * Optimized for 4 threads to match physical cores on m6a.2xlarge, outperforming 8 threads due to reduced contention.
 * </p>
 * @author Saqib
 * @version 0.0.1-SNAPSHOT
 */
public class LowLatencyMatchingEngineTest {
    /**
     * Runs the performance benchmark with warmup and multi-threaded order submission.
     * <p>
     * Configures 4 threads to process 1 million orders total, with a 100k-order warmup. Measures execution time and
     * calculates throughput in orders per second. Uses 4 threads to align with m6a.2xlarge’s 4 physical cores for
     * optimal performance (1.26M orders/sec achieved vs. 0.87M with 8 threads).
     * </p>
     * @param args command-line arguments (unused)
     * @throws InterruptedException if thread joining is interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        LowLatencyMatchingEngine engine = LowLatencyMatchingEngine.getInstance();
        
        // Warmup phase to optimize JIT and Aeron
        System.out.println("Warming up...");
        engine.warmUp(100_000, CurrencyPair.EUR_USD, 1);

        // Benchmark configuration
        int threads = 4;  // Matches m6a.2xlarge’s 4 physical cores—faster than 8 vCPUs
        int totalOrders = 1_000_000;  // 1M orders for scale
        int ordersPerThread = totalOrders / threads;  // 250k per thread
        Thread[] workers = new Thread[threads];
        System.out.println("Starting timed run...");
        long start = System.nanoTime();  // High-precision timing

        // Launch threads to submit orders concurrently
        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            workers[t] = new Thread(() -> {
                LowLatencyMatchingEngine threadEngine = LowLatencyMatchingEngine.getInstance();
                long basePrice = 123450 + threadId * 250;  // Unique price per thread, wider spread for 4
                for (int i = 0; i < ordersPerThread; i++) {
                    byte side = (i % 2 == 0) ? Constants.BUY : Constants.SELL;  // Alternates buy/sell
                    threadEngine.submitOrder(CurrencyPair.EUR_USD, side, basePrice + i, 100, 1);
                }
            });
            workers[t].start();
        }

        // Wait for all threads to complete
        for (Thread w : workers) w.join();
        long end = System.nanoTime();
        double ms = (end - start) / 1_000_000.0;  // Convert ns to ms
        double ordersPerSec = totalOrders * 1000.0 / ms;  // Orders/sec
        System.out.printf("Processed %d orders in %.2f ms (%.0f orders/sec)%n", totalOrders, ms, ordersPerSec);
    }
}