package com.bookverse.BookVerse.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    private String title;
    private String author;
    private Double price;
    private int stock;
    private String description;
    private String imageUrl;
    private LocalDateTime createdAt;

    // Discount (sale)
    private Double discountPercent = 0.0; // percent (0 = no sale)
    private LocalDateTime discountStart;
    private LocalDateTime discountEnd;
    
    // Giá đã giảm (tính động)
    public Double getDiscountPrice() {
        if (discountPercent != null && discountPercent > 0) {
            return Math.round((price * (1 - discountPercent / 100)) * 100.0) / 100.0;
        } else {
            return price;
        }
    }
    public Double getDiscountPercent() {
        return discountPercent;
    }
    public void setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
    }
    public LocalDateTime getDiscountStart() {
        return discountStart;
    }
    public void setDiscountStart(LocalDateTime discountStart) {
        this.discountStart = discountStart;
    }
    public LocalDateTime getDiscountEnd() {
        return discountEnd;
    }
    public void setDiscountEnd(LocalDateTime discountEnd) {
        this.discountEnd = discountEnd;
    }

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "book")
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "book")
    private List<Wishlist> wishlists;

    @OneToMany(mappedBy = "book")
    private List<Review> reviews;

    @OneToMany(mappedBy = "book")
    private List<BookImage> bookImages;

    public Book() {}

    // Getters & Setters
    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public List<Wishlist> getWishlists() {
        return wishlists;
    }

    public void setWishlists(List<Wishlist> wishlists) {
        this.wishlists = wishlists;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public List<BookImage> getBookImages() {
        return bookImages;
    }

    public void setBookImages(List<BookImage> bookImages) {
        this.bookImages = bookImages;
    }
}
