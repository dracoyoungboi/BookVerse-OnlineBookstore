package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Find all orders with user and pagination
    // Note: Sorting is handled by Pageable, not hardcoded in query
    @Query(value = "SELECT o FROM Order o LEFT JOIN o.user",
           countQuery = "SELECT COUNT(o) FROM Order o")
    Page<Order> findAllWithUserPaged(Pageable pageable);
    
    // Search orders by order ID, user username, email, or full name with pagination
    // Note: Sorting is handled by Pageable, not hardcoded in query
    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN o.user WHERE " +
           "CAST(o.orderId AS string) LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(o.user.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o LEFT JOIN o.user WHERE " +
           "CAST(o.orderId AS string) LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(o.user.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Order> searchOrdersWithUser(@Param("search") String search, Pageable pageable);
    
    // Find order by ID with user and order items
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.orderItems WHERE o.orderId = :id")
    Optional<Order> findByIdWithUserAndItems(@Param("id") Long id);
    
    // Find all orders with user and status filter with pagination
    // Note: Sorting is handled by Pageable, not hardcoded in query
    @Query(value = "SELECT o FROM Order o LEFT JOIN o.user WHERE " +
           "(COALESCE(:status, '') = '' OR o.status = :status)",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE " +
           "(COALESCE(:status, '') = '' OR o.status = :status)")
    Page<Order> findAllWithUserAndStatusPaged(@Param("status") String status, Pageable pageable);
    
    // Search orders with status filter
    // Note: Sorting is handled by Pageable, not hardcoded in query
    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN o.user WHERE " +
           "(COALESCE(:status, '') = '' OR o.status = :status) AND (" +
           "CAST(o.orderId AS string) LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(o.user.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o LEFT JOIN o.user WHERE " +
           "(COALESCE(:status, '') = '' OR o.status = :status) AND (" +
           "CAST(o.orderId AS string) LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(o.user.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrdersWithUserAndStatus(@Param("search") String search, 
                                              @Param("status") String status, 
                                              Pageable pageable);
    
    // Find orders by user ID with pagination
    // Note: Sorting is handled by Pageable, not hardcoded in query
    @Query(value = "SELECT o FROM Order o WHERE o.user.userId = :userId",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.userId = :userId")
    Page<Order> findByUserUserId(@Param("userId") Long userId, Pageable pageable);
    
    // Find orders by user ID and status with pagination
    // Note: Sorting is handled by Pageable, not hardcoded in query
    @Query(value = "SELECT o FROM Order o WHERE o.user.userId = :userId AND o.status = :status",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.userId = :userId AND o.status = :status")
    Page<Order> findByUserUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status, Pageable pageable);
    
    // Find all distinct user IDs from orders
    @Query("SELECT DISTINCT o.user.userId FROM Order o WHERE o.user IS NOT NULL")
    java.util.List<Long> findDistinctUserIds();
    
    // Find all distinct user IDs from orders with status filter
    @Query("SELECT DISTINCT o.user.userId FROM Order o WHERE o.user IS NOT NULL " +
           "AND (COALESCE(:status, '') = '' OR o.status = :status)")
    java.util.List<Long> findDistinctUserIdsByStatus(@Param("status") String status);
    
    // Find all distinct user IDs from orders with search and status filter
    @Query("SELECT DISTINCT o.user.userId FROM Order o WHERE o.user IS NOT NULL " +
           "AND (COALESCE(:status, '') = '' OR o.status = :status) " +
           "AND (LOWER(o.user.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    java.util.List<Long> findDistinctUserIdsBySearchAndStatus(@Param("search") String search, @Param("status") String status);
    
    // Find all orders by user ID
    java.util.List<Order> findByUserUserId(Long userId);
    
    // Find pending orders older than specified days with order items
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.status = 'pending' AND o.createdAt < :cutoffDate")
    java.util.List<Order> findPendingOrdersOlderThan(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}

