package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    // Tìm sách theo category
    Page<Book> findByCategoryCategoryId(Long categoryId, Pageable pageable);
    
    // Tìm sách theo category
    List<Book> findByCategoryCategoryId(Long categoryId);
    
    // Tìm sách theo tên (tìm kiếm)
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    // Tìm sách theo khoảng giá
    @Query("SELECT b FROM Book b WHERE b.price BETWEEN :minPrice AND :maxPrice")
    Page<Book> findByPriceBetween(@Param("minPrice") Double minPrice, 
                                    @Param("maxPrice") Double maxPrice, 
                                    Pageable pageable);
    
    // Tìm sách còn hàng
    Page<Book> findByStockGreaterThan(int stock, Pageable pageable);
    
    // Tìm sách đang giảm giá
    Page<com.bookverse.BookVerse.entity.Book> findByDiscountPercentGreaterThan(Double percent, Pageable pageable);

    // Lấy tất cả sách có phân trang
    Page<Book> findAll(Pageable pageable);
    
    // Lấy sách cùng category nhưng loại trừ sách hiện tại
    @Query("SELECT b FROM Book b WHERE b.category.categoryId = :categoryId AND b.bookId != :bookId")
    List<Book> findRelatedBooks(@Param("categoryId") Long categoryId, @Param("bookId") Long bookId);
    
    // Lấy sách với reviews (fetch join để tránh LazyInitializationException)
    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.reviews WHERE b.bookId = :id")
    java.util.Optional<Book> findByIdWithReviews(@Param("id") Long id);
    
    // Lấy sách mới nhất (new arrival)
    List<Book> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Find all books with category (for admin panel)
    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.category ORDER BY b.bookId DESC")
    java.util.List<Book> findAllWithCategory();
    
    // Find book by ID with category (for admin panel)
    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.category WHERE b.bookId = :id")
    java.util.Optional<Book> findByIdWithCategory(@Param("id") Long id);
    
    // Find books with category and pagination (for admin panel) - including both active and inactive
    // Using LEFT JOIN instead of JOIN FETCH to avoid pagination issues
    // Category will be fetched automatically when accessed (ManyToOne relationship)
    @Query(value = "SELECT b FROM Book b LEFT JOIN b.category ORDER BY b.bookId DESC",
           countQuery = "SELECT COUNT(b) FROM Book b")
    Page<Book> findAllWithCategoryPaged(Pageable pageable);
    
    // Search books by title or author with pagination - including both active and inactive
    @Query(value = "SELECT DISTINCT b FROM Book b LEFT JOIN b.category WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY b.bookId DESC",
           countQuery = "SELECT COUNT(DISTINCT b) FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Book> searchBooksWithCategory(@Param("search") String search, Pageable pageable);
    
    // Find book by ID with category (for admin panel) - including inactive books
    @Query("SELECT b FROM Book b LEFT JOIN b.category WHERE b.bookId = :id")
    java.util.Optional<Book> findByIdWithCategoryForAdmin(@Param("id") Long id);
}

