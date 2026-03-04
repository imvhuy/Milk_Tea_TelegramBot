package com.milktea.bot.model;

import java.time.LocalDateTime;
import java.util.List;

public class Order {

    private final String orderId;
    private final long customerChatId;
    private final String customerName;
    private final String customerPhone;
    private final List<CartItem> items;
    private final int totalPrice;
    private final LocalDateTime createdAt;
    private OrderStatus status;

    public Order(String orderId, long customerChatId, String customerName,
                 String customerPhone, List<CartItem> items) {
        this.orderId = orderId;
        this.customerChatId = customerChatId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.items = List.copyOf(items);
        this.totalPrice = items.stream().mapToInt(CartItem::getSubtotal).sum();
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }

    public String getOrderId() { return orderId; }
    public long getCustomerChatId() { return customerChatId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public List<CartItem> getItems() { return items; }
    public int getTotalPrice() { return totalPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
