package com.saqib.fxengine;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class AeronMessaging {
    private final Aeron aeron;
    private final Publication inputPublication;
    private final Subscription inputSubscription;
    private final Publication outputPublication;
    private final ExecutorService matchingEnginePool;
    private final ExecutorService marketDataPublisherPool;
    private final OrderBookManager orderBookManager;
    private final AtomicCounter processedOrders;
    
    public AeronMessaging(OrderBookManager orderBookManager, AtomicCounter processedOrders) {
        this.orderBookManager = orderBookManager;
        this.processedOrders = processedOrders;
        
        MediaDriver driver = MediaDriver.launchEmbedded();
        Aeron.Context context = new Aeron.Context()
            .aeronDirectoryName(driver.aeronDirectoryName());
        aeron = Aeron.connect(context);
        
        String inputChannel = "aeron:ipc";
        String outputChannel = "aeron:ipc?alias=market-data";
        int streamId = 1001;
        
        inputPublication = aeron.addPublication(inputChannel, streamId);
        inputSubscription = aeron.addSubscription(inputChannel, streamId);
        outputPublication = aeron.addPublication(outputChannel, streamId + 1);
        
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setPriority(Thread.MAX_PRIORITY);
                t.setDaemon(true);
                return t;
            }
        };
        
        matchingEnginePool = Executors.newSingleThreadExecutor(threadFactory);
        marketDataPublisherPool = Executors.newFixedThreadPool(2, threadFactory);
        
        startProcessingThreads();
    }
    
    private void startProcessingThreads() {
        matchingEnginePool.submit(() -> {
            while (true) {
                processMessages();
                Thread.onSpinWait();
            }
        });
        
        marketDataPublisherPool.submit(this::publishMarketData);
    }
    
    private void processMessages() {
        inputSubscription.poll((buffer, offset, length, header) -> {
            int msgTypeId = buffer.getInt(offset);
            switch (msgTypeId) {
                case Constants.NEW_ORDER:
                    processNewOrder(buffer, offset + 4, length - 4);
                    break;
                case Constants.CANCEL_ORDER:
                    processCancelOrder(buffer, offset + 4, length - 4);
                    break;
                case Constants.MODIFY_ORDER:
                    processModifyOrder(buffer, offset + 4, length - 4);
                    break;
                default:
                    System.err.println("Unknown message type: " + msgTypeId);
                    break;
            }
        }, 10);
    }
    
    private void processNewOrder(DirectBuffer buffer, int offset, int length) {
        ByteBuffer byteBuffer = ((UnsafeBuffer) buffer).byteBuffer();
        byteBuffer.position(offset);
        
        CurrencyPair symbol = CurrencyPair.values()[byteBuffer.getInt()];
        byte side = byteBuffer.get();
        long price = byteBuffer.getLong();
        int quantity = byteBuffer.getInt();
        long userId = byteBuffer.getLong();
        
        Order order = new Order(
            processedOrders.nextOrderId(),
            symbol,
            side,
            price,
            quantity,
            userId,
            System.nanoTime()
        );
        
        OrderBook orderBook = orderBookManager.getOrCreateOrderBook(symbol);
        orderBook.addOrder(order);
        
        processedOrders.increment();
    }
    
    private void processCancelOrder(DirectBuffer buffer, int offset, int length) {
        ByteBuffer byteBuffer = ((UnsafeBuffer) buffer).byteBuffer();
        byteBuffer.position(offset);
        
        CurrencyPair symbol = CurrencyPair.values()[byteBuffer.getInt()];
        long orderId = byteBuffer.getLong();
        
        OrderBook orderBook = orderBookManager.getOrCreateOrderBook(symbol);
        orderBook.cancelOrder(orderId);
    }
    
    private void processModifyOrder(DirectBuffer buffer, int offset, int length) {
        ByteBuffer byteBuffer = ((UnsafeBuffer) buffer).byteBuffer();
        byteBuffer.position(offset);
        
        CurrencyPair symbol = CurrencyPair.values()[byteBuffer.getInt()];
        long orderId = byteBuffer.getLong();
        long newPrice = byteBuffer.getLong();
        int newQuantity = byteBuffer.getInt();
        
        OrderBook orderBook = orderBookManager.getOrCreateOrderBook(symbol);
        orderBook.modifyOrder(orderId, newPrice, newQuantity);
    }
    
    private void publishMarketData() {
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(1024));
        while (true) {
            for (OrderBook book : orderBookManager.getAllOrderBooks()) {
                MarketDataSnapshot snapshot = book.createMarketDataSnapshot();
                int size = serializeSnapshot(buffer, snapshot);
                while (outputPublication.offer(buffer, 0, size) < 0) {
                    Thread.onSpinWait();
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
    
    private int serializeSnapshot(UnsafeBuffer buffer, MarketDataSnapshot snapshot) {
        int offset = 0;
        buffer.putInt(offset, snapshot.getSymbol().ordinal()); offset += 4;
        buffer.putLong(offset, snapshot.getTimestamp()); offset += 8;
        return offset; // Stub - expand later
    }
    
    public long submitOrder(CurrencyPair symbol, byte side, long price, int quantity, long userId) {
        long orderId = processedOrders.nextOrderId();
        int messageSize = 29; // Fixed: msgType(4) + ordinal(4) + side(1) + price(8) + qty(4) + userId(8)
        
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(messageSize));
        int offset = 0;
        
        buffer.putInt(offset, Constants.NEW_ORDER); offset += 4;
        buffer.putInt(offset, symbol.ordinal()); offset += 4;
        buffer.putByte(offset, side); offset += 1;
        buffer.putLong(offset, price); offset += 8;
        buffer.putInt(offset, quantity); offset += 4;
        buffer.putLong(offset, userId); offset += 8;
        
        while (inputPublication.offer(buffer, 0, messageSize) < 0) {
            Thread.onSpinWait();
        }
        return orderId;
    }
}