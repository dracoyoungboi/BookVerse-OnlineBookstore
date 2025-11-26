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
     * 
     * This is the core database query method for keyword search. Spring Data JPA
     * automatically generates the SQL query based on the method name convention.
     * 
     * METHOD NAME BREAKDOWN:
     * - "findBy" - indicates a query operation
     * - "Title" - refers to the Book entity's title property
     * - "Containing" - creates a SQL LIKE query with wildcards on both sides (%keyword%)
     * - "IgnoreCase" - makes the search case-insensitive (uses LOWER() function)
     * - Together: "find books where title contains the keyword, case-insensitive"
     * 
     * GENERATED SQL (conceptual):
     * SELECT b.* FROM books b 
     * WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', ?, '%'))
     * ORDER BY ? 
     * LIMIT ? OFFSET ?
     * 
     * SEARCH BEHAVIOR:
     * - Case-insensitive: "JAVA" matches "java", "Java", "JAVA", "jAvA"
     * - Partial matching: "potter" matches "Harry Potter", "Potter's Field"
     * - Substring matching: "java" matches "Java Programming", "Advanced Java"
     * - Wildcard matching: Uses SQL LIKE with % wildcards on both sides
     * 
     * EXAMPLES OF GENERATED QUERIES:
     * 
     * Keyword: "java"
     * SQL: WHERE LOWER(title) LIKE LOWER('%java%')
     * Matches: "Java Programming", "Advanced Java", "JavaScript Basics"
     * 
     * Keyword: "harry potter"
     * SQL: WHERE LOWER(title) LIKE LOWER('%harry potter%')
     * Matches: "Harry Potter and the Philosopher's Stone", "The Harry Potter Collection"
     * 
     * Keyword: "SCIENCE"
     * SQL: WHERE LOWER(title) LIKE LOWER('%science%')
     * Matches: "Science Fiction", "Computer Science", "science textbook"
     * 
     * PERFORMANCE CONSIDERATIONS:
     * - LIKE queries with leading wildcards (%keyword%) cannot use indexes efficiently
     * - For large databases, consider:
     *   1. Full-text search indexes (MySQL FULLTEXT, PostgreSQL tsvector)
     *   2. Search engines (Elasticsearch, Solr)
     *   3. Limiting search to beginning of title (findByTitleStartingWithIgnoreCase)
     * 
     * CURRENT LIMITATIONS:
     * - Searches only the title field, not author or description
     * - Does not support multiple keywords (AND/OR logic)
     * - Does not support phrase matching (exact phrase search)
     * - No relevance ranking (results ordered by sortBy parameter, not relevance)
     * 
     * PAGINATION:
     * - The Pageable parameter handles pagination (which page, how many items)
     * - It also handles sorting (which field, ascending/descending)
     * - Returns a Page object containing the matching books plus metadata
     * 
     * USAGE EXAMPLE:
     * - User searches for "java" (page 0, 12 per page, sorted by title ascending)
     * - Controller calls: searchBooks("java", 0, 12, "title", "asc")
     * - This method queries: "Find books where title contains 'java', page 0, 12 per page, sorted by title"
     * - Returns first 12 books matching "java", ordered alphabetically by title
     * 
     * @param title Search keyword to match against book titles (case-insensitive, partial match)
     * @param pageable Contains pagination info (page number, page size) and sorting info (field, direction)
     * @return Page object containing books whose titles contain the keyword, with pagination metadata
     */
    /**
     * Searches books by title with case-insensitive partial matching.
     * 
     * This is the core database query method for keyword search. Spring Data JPA
     * automatically generates the SQL query based on the method name convention.
     * 
     * METHOD NAME BREAKDOWN:
     * - "findBy" - indicates a query operation
     * - "Title" - refers to the Book entity's title property
     * - "Containing" - creates a SQL LIKE query with wildcards on both sides (%keyword%)
     * - "IgnoreCase" - makes the search case-insensitive (uses LOWER() function)
     * - Together: "find books where title contains the keyword, case-insensitive"
     * 
     * GENERATED SQL (conceptual):
     * SELECT b.* FROM books b 
     * WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', ?, '%'))
     * ORDER BY ? 
     * LIMIT ? OFFSET ?
     * 
     * SEARCH BEHAVIOR:
     * - Case-insensitive: "JAVA" matches "java", "Java", "JAVA", "jAvA"
     * - Partial matching: "potter" matches "Harry Potter", "Potter's Field"
     * - Substring matching: "java" matches "Java Programming", "Advanced Java"
     * - Wildcard matching: Uses SQL LIKE with % wildcards on both sides
     * 
     * EXAMPLES OF GENERATED QUERIES:
     * 
     * Keyword: "java"
     * SQL: WHERE LOWER(title) LIKE LOWER('%java%')
     * Matches: "Java Programming", "Advanced Java", "JavaScript Basics"
     * 
     * Keyword: "harry potter"
     * SQL: WHERE LOWER(title) LIKE LOWER('%harry potter%')
     * Matches: "Harry Potter and the Philosopher's Stone", "The Harry Potter Collection"
     * 
     * Keyword: "SCIENCE"
     * SQL: WHERE LOWER(title) LIKE LOWER('%science%')
     * Matches: "Science Fiction", "Computer Science", "science textbook"
     * 
     * PERFORMANCE CONSIDERATIONS:
     * - LIKE queries with leading wildcards (%keyword%) cannot use indexes efficiently
     * - For large databases, consider:
     *   1. Full-text search indexes (MySQL FULLTEXT, PostgreSQL tsvector)
     *   2. Search engines (Elasticsearch, Solr)
     *   3. Limiting search to beginning of title (findByTitleStartingWithIgnoreCase)
     * 
     * CURRENT LIMITATIONS:
     * - Searches only the title field, not author or description
     * - Does not support multiple keywords (AND/OR logic)
     * - Does not support phrase matching (exact phrase search)
     * - No relevance ranking (results ordered by sortBy parameter, not relevance)
     * 
     * PAGINATION:
     * - The Pageable parameter handles pagination (which page, how many items)
     * - It also handles sorting (which field, ascending/descending)
     * - Returns a Page object containing the matching books plus metadata
     * 
     * USAGE EXAMPLE:
     * - User searches for "java" (page 0, 12 per page, sorted by title ascending)
     * - Controller calls: searchBooks("java", 0, 12, "title", "asc")
     * - This method queries: "Find books where title contains 'java', page 0, 12 per page, sorted by title"
     * - Returns first 12 books matching "java", ordered alphabetically by title
     * 
     * @param title Search keyword to match against book titles (case-insensitive, partial match)
     * @param pageable Contains pagination info (page number, page size) and sorting info (field, direction)
     * @return Page object containing books whose titles contain the keyword, with pagination metadata
     */
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    /**
     * Searches books by title OR author with case-insensitive partial matching.
     * 
     * This method searches both the title and author fields, returning books that match
     * the keyword in either field. This provides a more comprehensive search experience
     * for users who may search by book title or author name.
     * 
     * SEARCH BEHAVIOR:
     * - Searches BOTH title AND author fields
     * - Uses OR logic: matches if keyword is found in title OR author
     * - Case-insensitive: "STEPHEN" matches "Stephen", "stephen", "STEPHEN"
     * - Partial matching: "king" matches "Stephen King" (author) and "The King's Speech" (title)
     * - Substring matching: "rowling" matches "J.K. Rowling" (author)
     * 
     * GENERATED SQL (conceptual):
     * SELECT DISTINCT b.* FROM books b 
     * WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', ?, '%')) 
     *    OR LOWER(b.author) LIKE LOWER(CONCAT('%', ?, '%'))
     * ORDER BY ? 
     * LIMIT ? OFFSET ?
     * 
     * EXAMPLES OF SEARCH RESULTS:
     * 
     * Keyword: "king"
     * Matches in Title: "The King's Speech", "King Arthur", "The Lion King"
     * Matches in Author: Books by "Stephen King", "Martin Luther King Jr."
     * Returns: All books matching in either field
     * 
     * Keyword: "rowling"
     * Matches in Title: None (unless a book title contains "rowling")
     * Matches in Author: All books by "J.K. Rowling"
     * Returns: All Harry Potter books and other books by J.K. Rowling
     * 
     * Keyword: "harry potter"
     * Matches in Title: "Harry Potter and the Philosopher's Stone", etc.
     * Matches in Author: None (unless author name contains "harry potter")
     * Returns: All Harry Potter series books
     * 
     * Keyword: "tolkien"
     * Matches in Title: None (unless title contains "tolkien")
     * Matches in Author: All books by "J.R.R. Tolkien"
     * Returns: The Hobbit, Lord of the Rings, etc.
     * 
     * WHY SEARCH BOTH FIELDS:
     * - Users may search by author name: "stephen king" → finds all Stephen King books
     * - Users may search by book title: "harry potter" → finds Harry Potter books
     * - Users may search by partial author: "rowling" → finds J.K. Rowling books
     * - Provides better user experience than title-only search
     * 
     * PERFORMANCE CONSIDERATIONS:
     * - OR queries can be slower than single-field queries
     * - Both title and author fields are searched, doubling the search space
     * - DISTINCT is used to avoid duplicate results if a book somehow matches both
     * - For large databases, consider full-text search indexes on both fields
     * 
     * PAGINATION:
     * - The Pageable parameter handles pagination (which page, how many items)
     * - It also handles sorting (which field, ascending/descending)
     * - Returns a Page object containing the matching books plus metadata
     * 
     * @param search Search keyword to match against book titles or author names (case-insensitive, partial match)
     * @param pageable Contains pagination info (page number, page size) and sorting info (field, direction)
     * @return Page object containing books whose title OR author contains the keyword, with pagination metadata
     */
    @Query(value = "SELECT DISTINCT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%'))",
           countQuery = "SELECT COUNT(DISTINCT b) FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Book> findByTitleOrAuthorContainingIgnoreCase(@Param("search") String search, Pageable pageable);
    
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

