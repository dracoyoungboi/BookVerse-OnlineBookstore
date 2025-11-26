package com.bookverse.BookVerse.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    /**
     * Rating value (1-5 stars).
     * Used to calculate average rating displayed on book detail page.
     */
    private int rating;
    
    /**
     * Optional review comment/text.
     * Can be null if user only provides rating without written review.
     */
    private String comment;
    
    /**
     * Timestamp when review was created.
     * Used for sorting reviews (newest first) and pagination.
     */
    private LocalDateTime createdAt;
    
    /**
     * Visibility flag for admin moderation.
     * - true: Review is visible to users (default)
     * - false: Review is hidden by admin (inappropriate content, spam, etc.)
     * Only visible reviews are displayed and included in average rating calculation.
     */
    private Boolean visible = true;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    public Review() {}

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getVisible() {
        return visible != null ? visible : true;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible != null ? visible : true;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }
}
