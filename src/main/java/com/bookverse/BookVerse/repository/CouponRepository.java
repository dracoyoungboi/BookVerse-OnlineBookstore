package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    // Tìm coupon theo code
    Optional<Coupon> findByCode(String code);
    
    // Tìm coupon theo code và active
    Optional<Coupon> findByCodeAndActiveTrue(String code);
    
    // Tìm tất cả coupon active
    List<Coupon> findByActiveTrue();
    
    // Tìm coupon hợp lệ (active, chưa hết hạn, chưa hết số lần sử dụng)
    @Query("SELECT c FROM Coupon c WHERE c.active = true " +
           "AND (c.expiryDate IS NULL OR c.expiryDate > :now) " +
           "AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    List<Coupon> findValidCoupons(@Param("now") LocalDateTime now);
    
    // Tìm coupon hợp lệ theo code
    @Query("SELECT c FROM Coupon c WHERE c.code = :code " +
           "AND c.active = true " +
           "AND (c.expiryDate IS NULL OR c.expiryDate > :now) " +
           "AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    Optional<Coupon> findValidCouponByCode(@Param("code") String code, @Param("now") LocalDateTime now);
}

