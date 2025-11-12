package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Coupon;
import com.bookverse.BookVerse.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CouponService {
    
    @Autowired
    private CouponRepository couponRepository;
    
    /**
     * Tìm coupon theo code
     */
    public Optional<Coupon> findByCode(String code) {
        return couponRepository.findByCode(code);
    }
    
    /**
     * Validate coupon và kiểm tra xem có thể áp dụng cho cart total không
     */
    public CouponValidationResult validateCoupon(String code, Double cartTotal) {
        if (code == null || code.trim().isEmpty()) {
            return new CouponValidationResult(false, "Coupon code cannot be empty", null);
        }
        
        Optional<Coupon> couponOpt = couponRepository.findValidCouponByCode(code.trim().toUpperCase(), LocalDateTime.now());
        
        if (couponOpt.isEmpty()) {
            return new CouponValidationResult(false, "Invalid or expired coupon code", null);
        }
        
        Coupon coupon = couponOpt.get();
        
        // Kiểm tra active
        if (!coupon.getActive()) {
            return new CouponValidationResult(false, "Coupon is not active", null);
        }
        
        // Kiểm tra expiry date
        if (coupon.isExpired()) {
            return new CouponValidationResult(false, "Coupon has expired", null);
        }
        
        // Kiểm tra usage limit
        if (coupon.isUsageLimitReached()) {
            return new CouponValidationResult(false, "Coupon usage limit has been reached", null);
        }
        
        // Kiểm tra minimum purchase amount
        if (cartTotal == null || cartTotal < coupon.getMinPurchaseAmount()) {
            return new CouponValidationResult(false, 
                String.format("Minimum purchase amount of $%.2f is required", coupon.getMinPurchaseAmount()), 
                null);
        }
        
        // Tính toán discount
        Double discount = coupon.calculateDiscount(cartTotal);
        
        if (discount <= 0) {
            return new CouponValidationResult(false, "Coupon cannot be applied to this cart", null);
        }
        
        return new CouponValidationResult(true, "Coupon applied successfully", coupon);
    }
    
    /**
     * Tăng số lần sử dụng coupon (khi đặt hàng thành công)
     */
    public void incrementUsageCount(String code) {
        Optional<Coupon> couponOpt = couponRepository.findByCode(code);
        if (couponOpt.isPresent()) {
            Coupon coupon = couponOpt.get();
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }
    }
    
    /**
     * Class để trả về kết quả validate coupon
     */
    public static class CouponValidationResult {
        private boolean valid;
        private String message;
        private Coupon coupon;
        
        public CouponValidationResult(boolean valid, String message, Coupon coupon) {
            this.valid = valid;
            this.message = message;
            this.coupon = coupon;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Coupon getCoupon() {
            return coupon;
        }
    }
}

