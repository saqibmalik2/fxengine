package com.saqib.fxengine;

import java.util.ArrayList;
import java.util.List;

public class MarketDataSnapshot {
    private final CurrencyPair symbol;
    private final long timestamp;
    private final List<PriceLevelDTO> buyLevels = new ArrayList<>();
    private final List<PriceLevelDTO> sellLevels = new ArrayList<>();
    
    public MarketDataSnapshot(CurrencyPair symbol) {
        this.symbol = symbol;
        this.timestamp = System.nanoTime();
    }
    
    public void addBuyLevel(long price, int quantity, int orderCount) {
        buyLevels.add(new PriceLevelDTO(price, quantity, orderCount));
    }
    
    public void addSellLevel(long price, int quantity, int orderCount) {
        sellLevels.add(new PriceLevelDTO(price, quantity, orderCount));
    }
    
    public CurrencyPair getSymbol() {
        return symbol;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public static class PriceLevelDTO {
        public final long price; // Pips
        public final int quantity;
        public final int orderCount;
        
        public PriceLevelDTO(long price, int quantity, int orderCount) {
            this.price = price;
            this.quantity = quantity;
            this.orderCount = orderCount;
        }
    }
}