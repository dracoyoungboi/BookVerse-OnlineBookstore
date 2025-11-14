package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    java.util.List<Review> findAllByOrderByCreatedAtDesc();
    java.util.List<Review> findByBookBookIdOrderByCreatedAtDesc(Long bookId);
    java.util.List<Review> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    org.springframework.data.domain.Page<Review> findByBookBookId(Long bookId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Review> findByUserUserId(Long userId, org.springframework.data.domain.Pageable pageable);
    java.util.List<Review> findByBookBookIdAndVisibleTrueOrderByCreatedAtDesc(Long bookId);
}
