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
    
    // Optimized queries for book inactive validation
    
    // Check if book has processing orders (for blocking inactive)
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi JOIN oi.order o WHERE oi.book.bookId = :bookId AND o.status = 'processing'")
    boolean hasProcessingOrders(@Param("bookId") Long bookId);
    
    // Check if book has shipped orders (for blocking inactive)
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi JOIN oi.order o WHERE oi.book.bookId = :bookId AND o.status = 'shipped'")
    boolean hasShippedOrders(@Param("bookId") Long bookId);
    
    // Get only pending orders for a book (for deletion when inactive)
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE oi.book.bookId = :bookId AND o.status = 'pending'")
    List<OrderItem> findPendingOrderItemsByBookId(@Param("bookId") Long bookId);
    
    // Get order IDs and status for a book (lightweight query)
    @Query("SELECT DISTINCT o.orderId, o.status FROM OrderItem oi JOIN oi.order o WHERE oi.book.bookId = :bookId")
    List<Object[]> findOrderIdsAndStatusByBookId(@Param("bookId") Long bookId);
}






