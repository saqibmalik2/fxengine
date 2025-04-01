package com.saqib.fxengine;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

public class OrderBook {
    private final CurrencyPair symbol;
    private final NavigableMap<Long, PriceLevel> buyLevels = new ConcurrentSkipListMap<>(Collections.reverseOrder());
    private final NavigableMap<Long, PriceLevel> sellLevels = new ConcurrentSkipListMap<>();
    private final ConcurrentHashMap<Long, Order> ordersById = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Trade> tradeQueue = new ConcurrentLinkedQueue<>();
    
    public OrderBook(CurrencyPair symbol) {
        this.symbol = symbol;
    }
    
    public void addOrder(Order order) {
        ordersById.put(order.orderId, order);
        boolean fullyMatched = matchOrder(order);
        if (!fullyMatched && order.remainingQuantity > 0) {
            NavigableMap<Long, PriceLevel> levels = (order.side == Constants.BUY) ? buyLevels : sellLevels;
            PriceLevel level = levels.computeIfAbsent(order.price, k -> new PriceLevel(order.price));
            level.addOrder(order);
        }
    }
    
    private boolean matchOrder(Order incomingOrder) {
        NavigableMap<Long, PriceLevel> opposingLevels = 
            (incomingOrder.side == Constants.BUY) ? sellLevels : buyLevels;
        
        if (opposingLevels.isEmpty()) return false;
        
        boolean isMatch = true;
        while (isMatch && incomingOrder.remainingQuantity > 0) {
            isMatch = false;
            Map.Entry<Long, PriceLevel> bestEntry = opposingLevels.firstEntry();
            if (bestEntry == null) break;
            
            PriceLevel bestLevel = bestEntry.getValue();
            long bestPrice = bestEntry.getKey();
            
            if ((incomingOrder.side == Constants.BUY && incomingOrder.price >= bestPrice) ||
                (incomingOrder.side == Constants.SELL && incomingOrder.price <= bestPrice)) {
                isMatch = true;
                while (!bestLevel.isEmpty() && incomingOrder.remainingQuantity > 0) {
                    Order restingOrder = bestLevel.getFirstOrder();
                    if (restingOrder == null) break;
                    
                    int matchQuantity = Math.min(incomingOrder.remainingQuantity, restingOrder.remainingQuantity);
                    incomingOrder.remainingQuantity -= matchQuantity;
                    restingOrder.remainingQuantity -= matchQuantity;
                    
                    Trade trade = new Trade(
                        incomingOrder.orderId,
                        restingOrder.orderId,
                        incomingOrder.side == Constants.BUY ? restingOrder.userId : incomingOrder.userId,
                        incomingOrder.side == Constants.BUY ? incomingOrder.userId : restingOrder.userId,
                        symbol,
                        bestPrice,
                        matchQuantity,
                        System.nanoTime()
                    );
                    tradeQueue.offer(trade);
                    
                    if (restingOrder.remainingQuantity == 0) {
                        bestLevel.removeOrder(restingOrder.orderId);
                        ordersById.remove(restingOrder.orderId);
                    }
                    if (bestLevel.isEmpty()) {
                        opposingLevels.remove(bestPrice);
                        break;
                    }
                }
            }
        }
        return incomingOrder.remainingQuantity == 0;
    }
    
    public void cancelOrder(long orderId) {
        Order order = ordersById.remove(orderId);
        if (order != null) {
            NavigableMap<Long, PriceLevel> levels = (order.side == Constants.BUY) ? buyLevels : sellLevels;
            PriceLevel level = levels.get(order.price);
            if (level != null) {
                level.removeOrder(orderId);
                if (level.isEmpty()) {
                    levels.remove(order.price);
                }
            }
        }
    }
    
    public void modifyOrder(long orderId, long newPrice, int newQuantity) {
        Order existingOrder = ordersById.get(orderId);
        if (existingOrder != null) {
            if (existingOrder.price != newPrice) {
                cancelOrder(orderId);
                Order newOrder = new Order(
                    existingOrder.orderId,
                    existingOrder.symbol,
                    existingOrder.side,
                    newPrice,
                    newQuantity,
                    existingOrder.userId,
                    System.nanoTime()
                );
                addOrder(newOrder);
            } else {
                if (newQuantity > existingOrder.remainingQuantity) {
                    existingOrder.remainingQuantity = newQuantity;
                } else {
                    existingOrder.remainingQuantity = newQuantity;
                    if (newQuantity == 0) {
                        cancelOrder(orderId);
                    }
                }
            }
        }
    }
    
    public MarketDataSnapshot createMarketDataSnapshot() {
        MarketDataSnapshot snapshot = new MarketDataSnapshot(symbol);
        int levelsToInclude = 5;
        
        int buyLevelCount = 0;
        for (Map.Entry<Long, PriceLevel> entry : buyLevels.entrySet()) {
            if (buyLevelCount >= levelsToInclude) break;
            PriceLevel level = entry.getValue();
            snapshot.addBuyLevel(level.getPrice(), level.getTotalQuantity(), level.getOrderCount());
            buyLevelCount++;
        }
        
        int sellLevelCount = 0;
        for (Map.Entry<Long, PriceLevel> entry : sellLevels.entrySet()) {
            if (sellLevelCount >= levelsToInclude) break;
            PriceLevel level = entry.getValue();
            snapshot.addSellLevel(level.getPrice(), level.getTotalQuantity(), level.getOrderCount());
            sellLevelCount++;
        }
        return snapshot;
    }
}