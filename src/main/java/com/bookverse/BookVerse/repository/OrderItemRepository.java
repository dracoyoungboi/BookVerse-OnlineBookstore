package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderOrderId(Long orderId);
    List<OrderItem> findByBookBookId(Long bookId);
    
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order WHERE oi.book.bookId = :bookId")
    List<OrderItem> findByBookBookIdWithOrder(@Param("bookId") Long bookId);
}






