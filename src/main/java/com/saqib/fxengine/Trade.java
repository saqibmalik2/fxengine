package com.saqib.fxengine;

public class Trade {
    private long p1, p2, p3, p4, p5, p6, p7, p8;
    public final long takerOrderId;
    public final long makerOrderId;
    private long p9, p10, p11, p12, p13, p14, p15, p16;
    public final long sellSideUserId;
    public final long buySideUserId;
    private long p17, p18, p19, p20, p21, p22, p23, p24;
    public final CurrencyPair symbol;
    public final long price; // Pips
    public final int quantity;
    public final long timestamp;
    private long p25, p26, p27, p28, p29, p30, p31, p32;
    
    public Trade(long takerOrderId, long makerOrderId, long sellSideUserId, long buySideUserId, 
                 CurrencyPair symbol, long price, int quantity, long timestamp) {
        this.takerOrderId = takerOrderId;
        this.makerOrderId = makerOrderId;
        this.sellSideUserId = sellSideUserId;
        this.buySideUserId = buySideUserId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }
}