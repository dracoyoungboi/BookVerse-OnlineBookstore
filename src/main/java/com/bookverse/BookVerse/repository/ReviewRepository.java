package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    java.util.List<Review> findAllByOrderByCreatedAtDesc();
    java.util.List<Review> findByBookBookIdOrderByCreatedAtDesc(Long bookId);
    java.util.List<Review> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Finds reviews by book with pagination (for admin panel).
     * Returns all reviews (visible and hidden) for moderation.
     */
    org.springframework.data.domain.Page<Review> findByBookBookId(Long bookId, org.springframework.data.domain.Pageable pageable);
    
    /**
     * Finds reviews by user with pagination (for admin panel).
     * Returns all reviews (visible and hidden) for moderation.
     */
    org.springframework.data.domain.Page<Review> findByUserUserId(Long userId, org.springframework.data.domain.Pageable pageable);
    
    /**
     * Finds only visible reviews for a book, ordered by newest first.
     * Used on book detail page - excludes hidden reviews (admin moderation).
     * Only these reviews are displayed and included in average rating calculation.
     */
    java.util.List<Review> findByBookBookIdAndVisibleTrueOrderByCreatedAtDesc(Long bookId);
    
    // TODO: Add method for anti-spam check:
    // Optional<Review> findByBookBookIdAndUserUserId(Long bookId, Long userId);
    // This would check if user already reviewed this book (prevent duplicates)
}
