package com.saqib.fxengine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PriceLevel {
    private long p1, p2, p3, p4, p5, p6, p7, p8;
    private final long price; // Pips (x10^5)
    private long p9, p10, p11, p12, p13, p14, p15, p16;
    private final ConcurrentLinkedQueue<Order> orders = new ConcurrentLinkedQueue<>();
    private final Map<Long, Order> orderMap = new ConcurrentHashMap<>();
    private long p17, p18, p19, p20, p21, p22, p23, p24;
    private volatile int totalQuantity = 0;
    private long p25, p26, p27, p28, p29, p30, p31, p32;
    
    public PriceLevel(long price) {
        this.price = price;
    }
    
    public void addOrder(Order order) {
        orders.add(order);
        orderMap.put(order.orderId, order);
        totalQuantity += order.remainingQuantity;
    }
    
    public void removeOrder(long orderId) {
        Order order = orderMap.remove(orderId);
        if (order != null) {
            orders.remove(order);
            totalQuantity -= order.remainingQuantity;
        }
    }
    
    public Order getFirstOrder() {
        return orders.peek();
    }
    
    public boolean isEmpty() {
        return orders.isEmpty();
    }
    
    public int getTotalQuantity() {
        return totalQuantity;
    }
    
    public int getOrderCount() {
        return orders.size();
    }
    
    public long getPrice() {
        return price;
    }
}