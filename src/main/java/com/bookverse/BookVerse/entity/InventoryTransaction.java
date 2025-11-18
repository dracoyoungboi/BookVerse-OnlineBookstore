package com.bookverse.BookVerse.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false, length = 20)
    private String transactionType; // "IMPORT", "EXPORT", "ADJUSTMENT"

    @Column(nullable = false)
    private int quantity; // Số lượng thay đổi (+ hoặc -)

    @Column(nullable = false)
    private int stockBefore; // Stock trước khi thay đổi

    @Column(nullable = false)
    private int stockAfter; // Stock sau khi thay đổi

    private String reason; // Lý do (tùy chọn)

    @Column(columnDefinition = "TEXT")
    private String note; // Ghi chú

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy; // Người thực hiện

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Long referenceId; // ID đơn hàng hoặc reference khác (nếu có)

    @Column(length = 50)
    private String referenceType; // "ORDER", "MANUAL", etc.

    public InventoryTransaction() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getStockBefore() {
        return stockBefore;
    }

    public void setStockBefore(int stockBefore) {
        this.stockBefore = stockBefore;
    }

    public int getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(int stockAfter) {
        this.stockAfter = stockAfter;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
}

