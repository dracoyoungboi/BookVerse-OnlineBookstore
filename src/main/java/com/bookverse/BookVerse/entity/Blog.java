package com.bookverse.BookVerse.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "blogs")
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long blogId;

    private String title;
    private String thumbnail;
    private String author;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "blog")
    private List<BlogDetail> blogDetails;

    public Blog() {}

    public Long getBlogId() {
        return blogId;
    }

    public void setBlogId(Long blogId) {
        this.blogId = blogId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<BlogDetail> getBlogDetails() {
        return blogDetails;
    }

    public void setBlogDetails(List<BlogDetail> blogDetails) {
        this.blogDetails = blogDetails;
    }
}
