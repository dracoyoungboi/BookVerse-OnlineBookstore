package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    
    // Tìm tất cả transactions của một sách
    Page<InventoryTransaction> findByBookBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);
    
    // Tìm transactions theo loại
    Page<InventoryTransaction> findByTransactionTypeOrderByCreatedAtDesc(String transactionType, Pageable pageable);
    
    // Tìm transactions theo sách và loại
    Page<InventoryTransaction> findByBookBookIdAndTransactionTypeOrderByCreatedAtDesc(
            Long bookId, String transactionType, Pageable pageable);
    
    // Tìm transactions trong khoảng thời gian
    @Query("SELECT it FROM InventoryTransaction it WHERE it.createdAt BETWEEN :startDate AND :endDate ORDER BY it.createdAt DESC")
    Page<InventoryTransaction> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    // Tìm transactions theo sách và khoảng thời gian
    @Query("SELECT it FROM InventoryTransaction it WHERE it.book.bookId = :bookId AND it.createdAt BETWEEN :startDate AND :endDate ORDER BY it.createdAt DESC")
    Page<InventoryTransaction> findByBookIdAndDateRange(
            @Param("bookId") Long bookId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    // Tìm tất cả transactions với book và user (cho admin)
    @Query("SELECT it FROM InventoryTransaction it LEFT JOIN FETCH it.book LEFT JOIN FETCH it.createdBy ORDER BY it.createdAt DESC")
    Page<InventoryTransaction> findAllWithDetails(Pageable pageable);
    
    // Tìm tất cả transactions với book và user (không phân trang - cho export)
    @Query("SELECT it FROM InventoryTransaction it LEFT JOIN FETCH it.book LEFT JOIN FETCH it.createdBy ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findAllWithDetailsList();
    
    // Tìm transactions theo sách (không phân trang)
    @Query("SELECT it FROM InventoryTransaction it LEFT JOIN FETCH it.book LEFT JOIN FETCH it.createdBy WHERE it.book.bookId = :bookId ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByBookBookIdWithDetails(@Param("bookId") Long bookId);
    
    // Tìm transactions theo loại (không phân trang)
    @Query("SELECT it FROM InventoryTransaction it LEFT JOIN FETCH it.book LEFT JOIN FETCH it.createdBy WHERE it.transactionType = :transactionType ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByTransactionTypeWithDetails(@Param("transactionType") String transactionType);
    
    // Tìm transactions theo sách và loại (không phân trang)
    @Query("SELECT it FROM InventoryTransaction it LEFT JOIN FETCH it.book LEFT JOIN FETCH it.createdBy WHERE it.book.bookId = :bookId AND it.transactionType = :transactionType ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByBookBookIdAndTransactionTypeWithDetails(
            @Param("bookId") Long bookId, 
            @Param("transactionType") String transactionType);
    
    // Tìm transactions trong khoảng thời gian (không phân trang)
    @Query("SELECT it FROM InventoryTransaction it LEFT JOIN FETCH it.book LEFT JOIN FETCH it.createdBy WHERE it.createdAt BETWEEN :startDate AND :endDate ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByDateRangeWithDetails(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // Tìm transactions theo reference (ví dụ: order)
    List<InventoryTransaction> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    
    // Đếm số lượng nhập kho của một sách trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(it.quantity), 0) FROM InventoryTransaction it WHERE it.book.bookId = :bookId AND it.transactionType = 'IMPORT' AND it.createdAt BETWEEN :startDate AND :endDate")
    int sumImportQuantityByBookIdAndDateRange(
            @Param("bookId") Long bookId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // Đếm số lượng xuất kho của một sách trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(ABS(it.quantity)), 0) FROM InventoryTransaction it WHERE it.book.bookId = :bookId AND it.transactionType = 'EXPORT' AND it.createdAt BETWEEN :startDate AND :endDate")
    int sumExportQuantityByBookIdAndDateRange(
            @Param("bookId") Long bookId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

