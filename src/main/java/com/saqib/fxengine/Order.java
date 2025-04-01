package com.saqib.fxengine;

public class Order {
    private long p1, p2, p3, p4, p5, p6, p7, p8;
    public final long orderId;
    private long p9, p10, p11, p12, p13, p14, p15, p16;
    public final CurrencyPair symbol; // Enum - int opt possible
    public final byte side;
    public final long price; // Pips (x10^5)
    public final int originalQuantity;
    private long p17, p18, p19, p20, p21, p22, p23, p24;
    public final long userId;
    public final long timestamp;
    private long p25, p26, p27, p28, p29, p30, p31, p32;
    public volatile int remainingQuantity;
    private long p33, p34, p35, p36, p37, p38, p39, p40;
    
    public Order(long orderId, CurrencyPair symbol, byte side, long price, int quantity, long userId, long timestamp) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.originalQuantity = quantity;
        this.remainingQuantity = quantity;
        this.userId = userId;
        this.timestamp = timestamp;
    }
}