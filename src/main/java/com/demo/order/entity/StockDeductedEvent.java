package com.demo.order.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class StockDeductedEvent implements Serializable {
    private String        messageId;
    private String        userId;
    private String        productId;
    private String        productName;
    private Integer       quantity;
    private LocalDateTime deductedAt;

    public StockDeductedEvent() {}
    public String getMessageId() { return messageId; }
    public void setMessageId(String m) { this.messageId = m; }
    public String getUserId() { return userId; }
    public void setUserId(String u) { this.userId = u; }
    public String getProductId() { return productId; }
    public void setProductId(String p) { this.productId = p; }
    public String getProductName() { return productName; }
    public void setProductName(String p) { this.productName = p; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer q) { this.quantity = q; }
    public LocalDateTime getDeductedAt() { return deductedAt; }
    public void setDeductedAt(LocalDateTime t) { this.deductedAt = t; }
    @Override public String toString() {
        return "StockDeductedEvent{msgId=" + messageId + ", productId=" + productId + "}";
    }
}
