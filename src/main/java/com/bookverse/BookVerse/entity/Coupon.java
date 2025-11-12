package com.bookverse.BookVerse.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(unique = true, nullable = false, length = 50)
    private String code; // Mã coupon (ví dụ: "SAVE10", "WELCOME20")

    @Column(nullable = false, length = 20)
    private String discountType; // "PERCENTAGE" hoặc "FIXED_AMOUNT"

    @Column(nullable = false)
    private Double discountValue; // Giá trị giảm giá (phần trăm hoặc số tiền)

    @Column(nullable = false)
    private Double minPurchaseAmount = 0.0; // Số tiền tối thiểu để áp dụng coupon

    private Double maxDiscountAmount; // Số tiền giảm tối đa (chỉ áp dụng với PERCENTAGE, null = không giới hạn)

    @Column(nullable = false)
    private LocalDateTime expiryDate; // Ngày hết hạn

    private Integer usageLimit; // Số lần sử dụng tối đa (null = không giới hạn)

    @Column(nullable = false)
    private Integer usedCount = 0; // Số lần đã sử dụng

    @Column(nullable = false)
    private Boolean active = true; // Trạng thái hoạt động

    private LocalDateTime createdAt;

    private String description; // Mô tả coupon

    // Constructor
    public Coupon() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public Double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        this.discountValue = discountValue;
    }

    public Double getMinPurchaseAmount() {
        return minPurchaseAmount;
    }

    public void setMinPurchaseAmount(Double minPurchaseAmount) {
        this.minPurchaseAmount = minPurchaseAmount;
    }

    public Double getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(Double maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Helper methods
    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isUsageLimitReached() {
        return usageLimit != null && usedCount >= usageLimit;
    }

    public boolean isValid() {
        return active && !isExpired() && !isUsageLimitReached();
    }

    /**
     * Tính toán số tiền giảm giá dựa trên tổng giá trị đơn hàng
     * @param cartTotal Tổng giá trị đơn hàng
     * @return Số tiền giảm giá
     */
    public Double calculateDiscount(Double cartTotal) {
        if (cartTotal == null || cartTotal <= 0) {
            return 0.0;
        }

        if (cartTotal < minPurchaseAmount) {
            return 0.0; // Không đủ điều kiện áp dụng coupon
        }

        Double discount = 0.0;

        if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
            // Giảm giá theo phần trăm
            discount = cartTotal * (discountValue / 100.0);
            // Áp dụng giới hạn tối đa nếu có
            if (maxDiscountAmount != null && discount > maxDiscountAmount) {
                discount = maxDiscountAmount;
            }
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(discountType)) {
            // Giảm giá cố định
            discount = discountValue;
            // Không được giảm quá tổng giá trị đơn hàng
            if (discount > cartTotal) {
                discount = cartTotal;
            }
        }

        return Math.round(discount * 100.0) / 100.0; // Làm tròn đến 2 chữ số thập phân
    }
}

