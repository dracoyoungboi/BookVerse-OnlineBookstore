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
    
    /**
     * Finds books by category ID with pagination support.
     * 
     * This is the core database query method for category filtering. Spring Data JPA
     * automatically generates the SQL query based on the method name convention.
     * 
     * METHOD NAME BREAKDOWN:
     * - "findBy" - indicates a query operation
     * - "Category" - refers to the Book entity's category property (ManyToOne relationship)
     * - "CategoryId" - refers to the Category entity's categoryId property
     * - Together: "find books where book.category.categoryId equals the provided value"
     * 
     * GENERATED SQL (conceptual):
     * SELECT b.* FROM books b 
     * WHERE b.category_id = ? 
     * ORDER BY ? 
     * LIMIT ? OFFSET ?
     * 
     * DATABASE RELATIONSHIP:
     * - Book entity has a ManyToOne relationship with Category
     * - Each book belongs to one category (book.category_id references categories.category_id)
     * - Multiple books can belong to the same category
     * 
     * PAGINATION:
     * - The Pageable parameter handles pagination (which page, how many items)
     * - It also handles sorting (which field, ascending/descending)
     * - Returns a Page object containing the requested books plus metadata
     * 
     * USAGE EXAMPLE:
     * - User clicks "Fiction" category (categoryId = 1)
     * - Controller calls: getBooksByCategory(1, 0, 12, "title", "asc")
     * - This method queries: "Find books where category_id = 1, page 0, 12 per page, sorted by title"
     * - Returns first 12 Fiction books, ordered alphabetically by title
     * 
     * @param categoryId The category ID to filter by (foreign key in books table)
     * @param pageable Contains pagination info (page number, page size) and sorting info (field, direction)
     * @return Page object containing books in the specified category, with pagination metadata
     */
    Page<Book> findByCategoryCategoryId(Long categoryId, Pageable pageable);
    
    /**
     * Finds all books by category ID (without pagination).
     * Used for counting books in a category or getting full category lists.
     * 
     * @param categoryId The category ID to filter by
     * @return List of all books in the specified category
     */
    List<Book> findByCategoryCategoryId(Long categoryId);
    
    /**
     * Searches books by title with case-insensitive partial matching.
     * Used for the search functionality in the shop page.
     * Matches any book whose title contains the search keyword.
     * 
     * @param title Search keyword (partial match, case-insensitive)
     * @param pageable Pagination and sorting parameters
     * @return Page of books matching the search keyword
     */
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    /**
     * Finds books within a price range (inclusive).
     * Used for price filtering in the shop page.
     * 
     * @param minPrice Minimum price (inclusive)
     * @param maxPrice Maximum price (inclusive)
     * @param pageable Pagination and sorting parameters
     * @return Page of books within the specified price range
     */
    @Query("SELECT b FROM Book b WHERE b.price BETWEEN :minPrice AND :maxPrice")
    Page<Book> findByPriceBetween(@Param("minPrice") Double minPrice, 
                                    @Param("maxPrice") Double maxPrice, 
                                    Pageable pageable);
    
    /**
     * Finds books with stock greater than the specified value.
     * Used for filtering available books (stock > 0).
     * 
     * @param stock Minimum stock level (exclusive)
     * @param pageable Pagination and sorting parameters
     * @return Page of books with stock above the threshold
     */
    Page<Book> findByStockGreaterThan(int stock, Pageable pageable);
    
    /**
     * Finds books with discount percentage greater than the specified value.
     * Used for displaying books on sale (discountPercent > 0).
     * 
     * @param percent Minimum discount percentage (exclusive)
     * @param pageable Pagination and sorting parameters
     * @return Page of books with discount above the threshold
     */
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
    // Note: Sorting is handled by Pageable, not hardcoded in query
    @Query(value = "SELECT b FROM Book b LEFT JOIN b.category",
           countQuery = "SELECT COUNT(b) FROM Book b")
    Page<Book> findAllWithCategoryPaged(Pageable pageable);
    
    // Search books by title or author with pagination - including both active and inactive
    // Note: Sorting is handled by Pageable, not hardcoded in query
    @Query(value = "SELECT DISTINCT b FROM Book b LEFT JOIN b.category WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%'))",
           countQuery = "SELECT COUNT(DISTINCT b) FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Book> searchBooksWithCategory(@Param("search") String search, Pageable pageable);
    
    // Find book by ID with category (for admin panel) - including inactive books
    @Query("SELECT b FROM Book b LEFT JOIN b.category WHERE b.bookId = :id")
    java.util.Optional<Book> findByIdWithCategoryForAdmin(@Param("id") Long id);
}

