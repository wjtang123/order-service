package com.demo.order.entity;

import java.time.LocalDateTime;

public class Order {
    private Long          id;
    private String        orderNo;
    private String        userId;
    private String        productId;
    private String        productName;
    private Integer       quantity;
    private String        status;
    private String        sourceMsgId;
    private LocalDateTime createdAt;

    public Order() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String o) { this.orderNo = o; }
    public String getUserId() { return userId; }
    public void setUserId(String u) { this.userId = u; }
    public String getProductId() { return productId; }
    public void setProductId(String p) { this.productId = p; }
    public String getProductName() { return productName; }
    public void setProductName(String p) { this.productName = p; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer q) { this.quantity = q; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getSourceMsgId() { return sourceMsgId; }
    public void setSourceMsgId(String s) { this.sourceMsgId = s; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    @Override public String toString() {
        return "Order{orderNo=" + orderNo + ", productId=" + productId + ", qty=" + quantity + "}";
    }
}
