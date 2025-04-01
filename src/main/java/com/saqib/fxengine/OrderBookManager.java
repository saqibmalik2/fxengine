package com.saqib.fxengine;

import java.util.concurrent.ConcurrentHashMap;

public class OrderBookManager {
    private final ConcurrentHashMap<CurrencyPair, OrderBook> orderBooks = new ConcurrentHashMap<>();
    
    public OrderBook getOrCreateOrderBook(CurrencyPair symbol) {
        return orderBooks.computeIfAbsent(symbol, OrderBook::new);
    }
    
    public Iterable<OrderBook> getAllOrderBooks() {
        return orderBooks.values();
    }
}