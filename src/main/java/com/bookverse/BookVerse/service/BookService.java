package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.Category;
import com.bookverse.BookVerse.repository.BookRepository;
import com.bookverse.BookVerse.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {
    
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    
    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Retrieves all books with pagination and sorting.
     * Used when no filters are applied - displays the complete book catalog.
     * 
     * @param page Page number (0-indexed)
     * @param size Number of books per page
     * @param sortBy Field to sort by (e.g., "title", "price", "createdAt")
     * @param sortDir Sort direction ("asc" or "desc")
     * @return Page of books matching the pagination and sorting criteria
     */
    public Page<Book> getAllBooks(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findAll(pageable);
    }
    
    /**
     * Retrieves books filtered by category with pagination and sorting.
     * 
     * This method is called when a user selects a category from the sidebar filter menu.
     * It filters the book catalog to show only books that belong to the specified category,
     * while maintaining pagination and sorting capabilities.
     * 
     * HOW CATEGORY FILTERING WORKS:
     * 1. The categoryId parameter identifies which category to filter by
     * 2. Spring Data JPA uses the method name "findByCategoryCategoryId" to automatically
     *    generate a query that joins the Book and Category tables
     * 3. Only books where book.category.categoryId matches the provided categoryId are returned
     * 4. Results are paginated (split into pages) and sorted according to user preferences
     * 
     * EXAMPLE:
     * - If categoryId = 1 (e.g., "Fiction"), only books in the Fiction category are returned
     * - If user is on page 2 with 12 items per page, books 13-24 from Fiction category are shown
     * - If sorted by "price" ascending, Fiction books are ordered from cheapest to most expensive
     * 
     * @param categoryId ID of the category to filter by (must exist in categories table)
     * @param page Page number (0-indexed, e.g., 0 = first page, 1 = second page)
     * @param size Number of books per page (e.g., 12 books per page)
     * @param sortBy Field to sort by (e.g., "title", "price", "createdAt", "discountPercent")
     * @param sortDir Sort direction - "asc" for ascending (A-Z, low-high) or "desc" for descending (Z-A, high-low)
     * @return Page object containing books in the specified category, with pagination metadata
     *         (total pages, total elements, current page, etc.)
     */
    public Page<Book> getBooksByCategory(Long categoryId, int page, int size, String sortBy, String sortDir) {
        // Create a Sort object based on the sort field and direction
        // This tells Spring Data JPA how to order the results
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        
        // Create a Pageable object that combines pagination (page, size) with sorting
        // This is passed to the repository method to control which books are returned
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Query the database for books matching the category ID
        // Spring Data JPA automatically generates the SQL query:
        // SELECT * FROM books WHERE category_id = ? ORDER BY ? LIMIT ? OFFSET ?
        return bookRepository.findByCategoryCategoryId(categoryId, pageable);
    }
    
    /**
     * Searches books by title OR author keyword with pagination and sorting.
     * 
     * This method is called when a user enters a search keyword in the shop page search box
     * and submits the search form. It performs a case-insensitive partial match search on
     * BOTH book titles AND author names, meaning it will find books where either the title
     * or the author contains the keyword anywhere, regardless of case.
     * 
     * HOW TITLE/AUTHOR SEARCH WORKS:
     * 1. User types a keyword in the search box (e.g., "java", "Stephen King", "rowling")
     * 2. Form submits the keyword as a URL parameter: /shop?search=stephen+king
     * 3. Controller receives the search parameter and calls this method
     * 4. This method creates pagination and sorting parameters
     * 5. Repository queries database for books where title OR author contains the keyword
     * 6. Results are paginated and sorted according to user preferences
     * 
     * SEARCH MATCHING BEHAVIOR:
     * - Searches BOTH title AND author fields (OR logic)
     * - Case-insensitive: "STEPHEN" matches "Stephen", "stephen", "STEPHEN"
     * - Partial matching: "king" matches "Stephen King" (author) and "The King's Speech" (title)
     * - Substring matching: "rowling" matches "J.K. Rowling" (author)
     * - Multiple words: "harry potter" matches "Harry Potter" (title) and authors named "Harry Potter"
     * 
     * SEARCH EXAMPLES BY TITLE:
     * - Keyword: "java"
     *   Matches in Title: "Java Programming", "Advanced Java Techniques", "JavaScript Basics"
     *   Matches in Author: Books by authors with "java" in their name (rare)
     *   Returns: All matching books from either field
     * 
     * - Keyword: "harry potter"
     *   Matches in Title: "Harry Potter and the Philosopher's Stone", "Harry Potter Collection"
     *   Matches in Author: None (unless author name contains "harry potter")
     *   Returns: All Harry Potter series books
     * 
     * SEARCH EXAMPLES BY AUTHOR:
     * - Keyword: "stephen king"
     *   Matches in Title: None (unless title contains "stephen king")
     *   Matches in Author: All books by "Stephen King"
     *   Returns: The Shining, It, The Stand, etc.
     * 
     * - Keyword: "rowling"
     *   Matches in Title: None (unless title contains "rowling")
     *   Matches in Author: All books by "J.K. Rowling"
     *   Returns: All Harry Potter books and other books by J.K. Rowling
     * 
     * - Keyword: "tolkien"
     *   Matches in Title: None (unless title contains "tolkien")
     *   Matches in Author: All books by "J.R.R. Tolkien"
     *   Returns: The Hobbit, Lord of the Rings trilogy, etc.
     * 
     * SEARCH EXAMPLES - MATCHES BOTH:
     * - Keyword: "king"
     *   Matches in Title: "The King's Speech", "King Arthur", "The Lion King"
     *   Matches in Author: Books by "Stephen King", "Martin Luther King Jr."
     *   Returns: All books matching in either field
     * 
     * WHY SEARCH BOTH TITLE AND AUTHOR:
     * - Users often search by author name to find all books by that author
     * - Users may not remember exact book titles but remember the author
     * - Provides more comprehensive search results than title-only search
     * - Better user experience: one search box works for both titles and authors
     * 
     * PAGINATION & SORTING:
     * - Search results are paginated (e.g., 12 books per page)
     * - User can navigate through multiple pages of search results
     * - Results can be sorted by title, price, date, etc.
     * - Sorting applies to the filtered search results only
     * 
     * PERFORMANCE NOTES:
     * - The search uses SQL LIKE query with OR condition on two fields
     * - Searches both title and author fields, which may be slower than single-field search
     * - For large databases, consider adding full-text search indexes on both title and author columns
     * - DISTINCT is used to avoid duplicate results
     * 
     * @param keyword Search keyword to match against book titles OR author names (should be trimmed before calling)
     * @param page Page number (0-indexed, e.g., 0 = first page, 1 = second page)
     * @param size Number of books per page (e.g., 12 books per page)
     * @param sortBy Field to sort by (e.g., "title", "price", "createdAt", "discountPercent")
     * @param sortDir Sort direction - "asc" for ascending (A-Z, low-high) or "desc" for descending (Z-A, high-low)
     * @return Page object containing books whose title OR author contains the keyword, with pagination metadata
     *         (total pages, total elements, current page, etc.)
     */
    public Page<Book> searchBooks(String keyword, int page, int size, String sortBy, String sortDir) {
        // Create a Sort object based on the sort field and direction
        // This tells Spring Data JPA how to order the search results
        // Example: Sort.by("title").ascending() orders results A-Z by title
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        
        // Create a Pageable object that combines pagination (page, size) with sorting
        // This is passed to the repository method to control which search results are returned
        // Example: PageRequest.of(0, 12, sort) = first page, 12 items, with sorting
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Query the database for books whose titles OR authors contain the keyword
        // This searches both the title and author fields, providing a more comprehensive search.
        // Spring Data JPA generates the SQL query:
        // SELECT DISTINCT b.* FROM books b 
        // WHERE LOWER(b.title) LIKE LOWER('%keyword%') OR LOWER(b.author) LIKE LOWER('%keyword%')
        // ORDER BY ? LIMIT ? OFFSET ?
        // 
        // SEARCHING BOTH TITLE AND AUTHOR:
        // - User can search by book title: "harry potter" → finds Harry Potter books
        // - User can search by author name: "stephen king" → finds all Stephen King books
        // - User can search by partial author: "rowling" → finds J.K. Rowling books
        // - Provides better results than title-only search
        return bookRepository.findByTitleOrAuthorContainingIgnoreCase(keyword, pageable);
    }
    
    /**
     * Retrieves books within a specified price range with pagination and sorting.
     * Used when user applies a price filter (e.g., $10-$50).
     * 
     * @param minPrice Minimum price (inclusive)
     * @param maxPrice Maximum price (inclusive)
     * @param page Page number (0-indexed)
     * @param size Number of books per page
     * @param sortBy Field to sort by
     * @param sortDir Sort direction ("asc" or "desc")
     * @return Page of books within the specified price range
     */
    public Page<Book> getBooksByPriceRange(Double minPrice, Double maxPrice, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByPriceBetween(minPrice, maxPrice, pageable);
    }
    
    // Lấy sách theo ID
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
    
    /**
     * Retrieves a book by ID along with its reviews for the detail page.
     * 
     * This method is used to fetch a complete book entity with all its reviews
     * for display on the book detail page. It uses eager loading to fetch
     * reviews in the same database query to avoid N+1 query problems.
     * 
     * HOW IT WORKS:
     * 1. Uses a JOIN FETCH query to load book and reviews in one database call
     * 2. This prevents lazy loading exceptions when accessing reviews in the template
     * 3. Returns Optional<Book> to handle cases where book doesn't exist
     * 
     * BOOK DATA RETRIEVED:
     * - All book fields: title, author, description, price, imageUrl, stock, etc.
     * - Category information (ManyToOne relationship)
     * - All reviews associated with the book (OneToMany relationship)
     * 
     * PERFORMANCE:
     * - Uses @Transactional(readOnly = true) for read-only optimization
     * - JOIN FETCH prevents multiple queries (one for book, one for reviews)
     * - Single database round-trip instead of N+1 queries
     * 
     * USAGE:
     * - Called by productDetails controller method
     * - Used when displaying book detail page (/shop/product/{id})
     * - Ensures all book information and reviews are available for template rendering
     * 
     * @param id The book ID to retrieve
     * @return Optional containing the book with reviews if found, empty Optional if not found
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Optional<Book> getBookByIdWithDetails(Long id) {
        // Fetch book with reviews using JOIN FETCH query
        // This single query loads both the book and all its reviews
        // SQL equivalent: SELECT b.*, r.* FROM books b LEFT JOIN reviews r ON b.id = r.book_id WHERE b.id = ?
        Optional<Book> bookOpt = bookRepository.findByIdWithReviews(id);
        if (bookOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Book book = bookOpt.get();
        
        // Ensure reviews are loaded (trigger lazy loading if needed)
        // This is a safety check - with JOIN FETCH, reviews should already be loaded
        // Accessing the collection ensures it's initialized in the current transaction
        if (book.getReviews() != null) {
            book.getReviews().size(); // Trigger lazy loading if needed
        }
        
        return Optional.of(book);
    }
    
    /**
     * Retrieves all categories from the database.
     * 
     * This method is used to populate the category filter menu in the shop page sidebar.
     * It returns all available categories so users can see and select from them.
     * 
     * USAGE IN CATEGORY FILTERING:
     * - Called by the controller to build the category sidebar menu
     * - Each category in the list becomes a clickable filter option
     * - When user clicks a category, the categoryId is passed as a URL parameter
     * - The controller then uses that categoryId to filter books
     * 
     * EXAMPLE:
     * - Returns: [Category(id=1, name="Fiction"), Category(id=2, name="Science"), ...]
     * - These are displayed as clickable links: "Fiction", "Science", etc.
     * - Clicking "Fiction" navigates to: /shop?categoryId=1
     * - Controller receives categoryId=1 and filters books accordingly
     * 
     * @return List of all categories in the database, used for the filter menu
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    // Lấy sách còn hàng
    public Page<Book> getAvailableBooks(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByStockGreaterThan(0, pageable);
    }
    
    /**
     * Retrieves books currently on sale (with active discount) with pagination and sorting.
     * A book is considered "on sale" if its discountPercent is greater than 0.
     * Used when user clicks "Sale Only" filter to see discounted books.
     * 
     * @param page Page number (0-indexed)
     * @param size Number of books per page
     * @param sortBy Field to sort by (if "discount", maps to "discountPercent")
     * @param sortDir Sort direction ("asc" or "desc")
     * @return Page of books that are currently on sale
     */
    public Page<Book> getOnSaleBooks(int page, int size, String sortBy, String sortDir) {
        // Map 'discount' alias to actual field name 'discountPercent' for sorting
        if ("discount".equalsIgnoreCase(sortBy)) {
            sortBy = "discountPercent";
            sortDir = "desc"; // Default: show highest discount first
        }
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByDiscountPercentGreaterThan(0.0, pageable);
    }
    
    /**
     * Counts the number of books in a specific category.
     * Used for displaying book counts in category filters.
     * 
     * @param categoryId ID of the category
     * @return Total number of books in the category
     */
    public long countBooksByCategory(Long categoryId) {
        return bookRepository.findByCategoryCategoryId(categoryId).size();
    }
    
    /**
     * Retrieves a random selection of books.
     * Used for sidebar recommendations and featured book sections.
     * Fetches all books, shuffles them, and returns the requested number.
     * 
     * @param limit Maximum number of random books to return
     * @return List of randomly selected books (up to the limit)
     */
    public List<Book> getRandomBooks(int limit) {
        List<Book> allBooks = bookRepository.findAll();
        Collections.shuffle(allBooks);
        return allBooks.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Retrieves related books from the same category, excluding the current book.
     * 
     * This method is used on the book detail page to show "Related Books" or
     * "You May Also Like" recommendations. It finds books in the same category
     * as the current book, which helps users discover similar titles.
     * 
     * HOW IT WORKS:
     * 1. Queries database for books in the same category
     * 2. Excludes the current book (so it doesn't recommend itself)
     * 3. Shuffles results for variety (different books shown on each page load)
     * 4. Limits to specified number (typically 4 books)
     * 
     * USAGE ON DETAIL PAGE:
     * - User views "Harry Potter" book (category: Fantasy)
     * - This method finds other Fantasy books
     * - Returns 4 random Fantasy books (excluding Harry Potter)
     * - Displayed in "Related Books" section at bottom of detail page
     * 
     * SHUFFLING:
     * - Results are shuffled to show different books on each page load
     * - Provides variety and helps users discover different titles
     * - Without shuffling, same books would always appear first
     * 
     * EXAMPLE:
     * - Current book: "Harry Potter" (categoryId = 1, Fantasy)
     * - Method finds: "Lord of the Rings", "The Hobbit", "Game of Thrones", "Chronicles of Narnia"
     * - Returns: 4 shuffled books from Fantasy category
     * 
     * @param bookId ID of the current book (excluded from results)
     * @param categoryId Category ID to find related books from
     * @param limit Maximum number of related books to return
     * @return List of related books (shuffled, limited to specified count)
     */
    public List<Book> getRelatedBooks(Long bookId, Long categoryId, int limit) {
        // Find books in the same category, excluding the current book
        List<Book> relatedBooks = bookRepository.findRelatedBooks(categoryId, bookId);
        
        // Shuffle results to show different books on each page load
        // This provides variety in recommendations
        Collections.shuffle(relatedBooks);
        
        // Limit to the specified number of books
        return relatedBooks.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }
    
    // Lấy best seller books (random hoặc mới nhất)
    public List<Book> getBestSellerBooks(int limit) {
        return getRandomBooks(limit);
    }
    
    // Lấy new arrival books (sách mới nhất)
    public List<Book> getNewArrivalBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return bookRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    // Lấy on sale books (không phân trang, chỉ lấy list)
    public List<Book> getOnSaleBooksList(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("discountPercent").descending());
        Page<Book> page = bookRepository.findByDiscountPercentGreaterThan(0.0, pageable);
        return page.getContent();
    }
    
    // Lấy featured books (random)
    public List<Book> getFeaturedBooks(int limit) {
        return getRandomBooks(limit);
    }
    
    // Lấy sách theo category (không phân trang, chỉ lấy list)
    public List<Book> getBooksByCategoryList(Long categoryId, int limit) {
        List<Book> books = bookRepository.findByCategoryCategoryId(categoryId);
        Collections.shuffle(books);
        return books.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }
}

