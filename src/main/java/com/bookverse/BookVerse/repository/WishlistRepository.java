package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    // Tìm wishlist theo user và book
    Optional<Wishlist> findByUserUserIdAndBookBookId(Long userId, Long bookId);
    
    // Lấy tất cả wishlist items của một user (không phân trang)
    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.book b LEFT JOIN FETCH b.category WHERE w.user.userId = :userId ORDER BY w.addedAt DESC")
    List<Wishlist> findByUserUserIdWithBook(@Param("userId") Long userId);
    
    // Lấy wishlist items của một user với phân trang
    // Sử dụng JOIN thông thường (không FETCH) để có thể sort theo book fields
    @Query(value = "SELECT w FROM Wishlist w JOIN w.book b JOIN b.category c WHERE w.user.userId = :userId",
           countQuery = "SELECT COUNT(w) FROM Wishlist w WHERE w.user.userId = :userId")
    Page<Wishlist> findByUserUserIdWithBookPaged(@Param("userId") Long userId, Pageable pageable);
    
    // Lấy wishlist items với book và category đã load (dùng sau khi có page result)
    @Query("SELECT DISTINCT w FROM Wishlist w LEFT JOIN FETCH w.book b LEFT JOIN FETCH b.category WHERE w.wishlistId IN :wishlistIds")
    List<Wishlist> findByWishlistIdInWithBook(@Param("wishlistIds") List<Long> wishlistIds);
    
    // Kiểm tra xem book đã có trong wishlist chưa
    boolean existsByUserUserIdAndBookBookId(Long userId, Long bookId);
    
    // Đếm số lượng wishlist items của user
    long countByUserUserId(Long userId);
    
    // Xóa wishlist item theo user và book
    void deleteByUserUserIdAndBookBookId(Long userId, Long bookId);
}

