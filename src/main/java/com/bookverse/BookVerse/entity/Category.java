package com.bookverse.BookVerse.entity;

import jakarta.persistence.*;
import java.util.List;

/**
 * Category entity representing a book category (e.g., Fiction, Non-Fiction, Science, etc.).
 * 
 * This entity is used for organizing books into groups. Each book belongs to one category,
 * and each category can have multiple books (One-to-Many relationship).
 * 
 * CATEGORY FILTERING RELATIONSHIP:
 * - Category has a OneToMany relationship with Book (one category, many books)
 * - Book has a ManyToOne relationship with Category (many books, one category)
 * - When filtering by category, we query books where book.category.categoryId matches
 * 
 * DATABASE STRUCTURE:
 * - Table: categories
 * - Primary Key: category_id (auto-generated)
 * - Foreign Key: books.category_id references categories.category_id
 */
@Entity
@Table(name = "categories")
public class Category {

    /**
     * Unique identifier for the category.
     * This is the primary key used in category filtering queries.
     * When user selects a category, this ID is passed as categoryId parameter.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    /**
     * Category name (e.g., "Fiction", "Science", "History").
     * Displayed in the category filter menu and page headers.
     */
    private String name;
    
    /**
     * Optional description of the category.
     * Can be used for category detail pages or tooltips.
     */
    private String description;

    /**
     * List of all books belonging to this category.
     * This is the inverse side of the Book-Category relationship.
     * 
     * NOTE: This list is lazy-loaded and may not be populated unless explicitly fetched.
     * For category filtering, we typically query books by categoryId rather than
     * loading this entire list, for better performance.
     */
    @OneToMany(mappedBy = "category")
    private List<Book> books;

    public Category() {}

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }
}
