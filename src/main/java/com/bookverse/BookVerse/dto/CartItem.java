package com.bookverse.BookVerse.dto;

import com.bookverse.BookVerse.entity.Book;
import java.io.Serializable;

/**
 * DTO class để lưu thông tin cart item trong session
 * Không phải entity, chỉ để lưu tạm trong session
 */
public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Book book;
    private int quantity;
    
    public CartItem() {
    }
    
    public CartItem(Book book, int quantity) {
        this.book = book;
        this.quantity = quantity;
    }
    
    public Book getBook() {
        return book;
    }
    
    public void setBook(Book book) {
        this.book = book;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    /**
     * Tính tổng giá của item này (giá đã giảm * số lượng)
     */
    public Double getSubtotal() {
        if (book == null) {
            return 0.0;
        }
        Double price = book.getDiscountPrice();
        if (price == null) {
            price = book.getPrice();
        }
        if (price == null) {
            return 0.0;
        }
        return Math.round((price * quantity) * 100.0) / 100.0;
    }
    
    /**
     * Tính tổng giá gốc của item này (chưa giảm giá)
     */
    public Double getOriginalSubtotal() {
        if (book == null || book.getPrice() == null) {
            return 0.0;
        }
        return Math.round((book.getPrice() * quantity) * 100.0) / 100.0;
    }
}

