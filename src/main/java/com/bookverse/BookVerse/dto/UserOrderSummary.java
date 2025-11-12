package com.bookverse.BookVerse.dto;

import com.bookverse.BookVerse.entity.User;

public class UserOrderSummary {
    private User user;
    private Long orderCount;
    private Double totalAmount;

    public UserOrderSummary() {
    }

    public UserOrderSummary(User user, Long orderCount, Double totalAmount) {
        this.user = user;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}

