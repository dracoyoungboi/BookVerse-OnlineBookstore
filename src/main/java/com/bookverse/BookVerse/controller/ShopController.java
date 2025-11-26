package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.Review;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.entity.Wishlist;
import com.bookverse.BookVerse.service.BookService;
import com.bookverse.BookVerse.service.WishlistService;
import com.bookverse.BookVerse.repository.ReviewRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private final BookService bookService;
    private final WishlistService wishlistService;
    private final ReviewRepository reviewRepository;

    public ShopController(BookService bookService, WishlistService wishlistService, ReviewRepository reviewRepository) {
        this.bookService = bookService;
        this.wishlistService = wishlistService;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Displays the book list page for users with filtering, sorting, and pagination capabilities.
     * This is the main shop page where users can browse all available books.
     * 
     * @param page Current page number (0-indexed, default: 0)
     * @param size Number of books per page (default: 12)
     * @param categoryId Optional category filter - shows only books in this category
     * @param search Optional search keyword - filters books by title matching this keyword
     * @param minPrice Optional minimum price filter - used with maxPrice for price range filtering
     * @param maxPrice Optional maximum price filter - used with minPrice for price range filtering
     * @param sortBy Field to sort by (default: "title") - options: title, price, createdAt, etc.
     * @param sortDir Sort direction (default: "asc") - "asc" for ascending, "desc" for descending
     * @param view Display view type (default: "grid") - "grid" for grid view, "list" for list view
     * @param saleOnly If true, shows only books currently on sale (discountPercent > 0)
     * @param model Spring MVC model to pass data to the view
     * @param session HTTP session to access current user information
     * @param authentication Spring Security authentication object to check user role
     * @return Thymeleaf template name "user/shop" to render the book list page
     */
    @GetMapping
    public String shopPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false, defaultValue = "grid") String view,
            @RequestParam(required = false, defaultValue = "false") boolean saleOnly,
            Model model,
            HttpSession session,
            Authentication authentication) {
        
        // Prevent admin users from accessing the user shop page - redirect them to admin dashboard
        // This ensures role separation and prevents admins from accidentally using the customer interface
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> {
                        String auth = authority.getAuthority().toUpperCase();
                        return auth.equals("ROLE_ADMIN") || auth.contains("ADMIN");
                    });
            
            // Double-check admin status from session if not found in authorities
            if (!isAdmin) {
                User currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null && currentUser.getRole() != null) {
                    String roleName = currentUser.getRole().getName();
                    if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                        isAdmin = true;
                    }
                }
            }
            
            if (isAdmin) {
                return "redirect:/demo/admin";
            }
        }
        
        // Initialize the book list - will be populated based on active filters
        Page<Book> books;

        // ====================================================================
        // CATEGORY FILTERING LOGIC
        // ====================================================================
        // The categoryId parameter comes from the URL query string when user clicks
        // a category link (e.g., /shop?categoryId=1). This is the highest priority
        // filter - if a category is selected, it takes precedence over other filters.
        // 
        // Filter priority order: category > search > price range > sale only > all books
        // This ensures only one filter type is active at a time for clear user experience
        // ====================================================================
        if (categoryId != null) {
            // CATEGORY FILTER ACTIVE:
            // User has selected a specific category from the sidebar or category menu.
            // Filter the book list to show only books that belong to this category.
            // The categoryId is used to query the database for books with matching category.
            // Pagination and sorting are still applied to the filtered results.
            books = bookService.getBooksByCategory(categoryId, page, size, sortBy, sortDir);
        }
        // ====================================================================
        // TITLE/AUTHOR KEYWORD SEARCH FILTERING LOGIC
        // ====================================================================
        // The search parameter comes from the search form in the shop page header.
        // User types a keyword in the search box and submits the form via GET request.
        // The search queries BOTH book titles AND author names, providing comprehensive results.
        // 
        // SEARCH FORM SUBMISSION:
        // - Form action: /shop (GET method)
        // - Input field name: "search"
        // - Example URLs:
        //   * /shop?search=harry+potter (searches title and author)
        //   * /shop?search=stephen+king (searches title and author)
        //   * /shop?search=rowling (searches title and author)
        // 
        // SEARCH PRIORITY:
        // - Search has second priority (after category filter)
        // - If both categoryId and search are provided, category takes precedence
        // - Search only activates when categoryId is null
        // 
        // SEARCH BEHAVIOR - TITLE AND AUTHOR:
        // - Searches BOTH title AND author fields (OR logic)
        // - Case-insensitive: "STEPHEN" matches "Stephen", "stephen", "STEPHEN"
        // - Partial matching: "king" matches "Stephen King" (author) and "The King's Speech" (title)
        // - Substring matching: "rowling" matches "J.K. Rowling" (author)
        // - Multiple words: "harry potter" matches "Harry Potter" (title) and authors named "Harry Potter"
        // - Whitespace handling: Leading/trailing spaces are trimmed
        // - Empty search: If search is null or empty after trimming, search is skipped
        // 
        // SEARCH EXAMPLES:
        // - "harry potter" → Finds books with "Harry Potter" in title (e.g., Harry Potter series)
        // - "stephen king" → Finds all books by author "Stephen King" (The Shining, It, etc.)
        // - "rowling" → Finds all books by "J.K. Rowling" (Harry Potter series)
        // - "tolkien" → Finds all books by "J.R.R. Tolkien" (Lord of the Rings, The Hobbit)
        // - "king" → Finds books with "king" in title AND books by authors with "king" in name
        // ====================================================================
        else if (search != null && !search.trim().isEmpty()) {
            // TITLE/AUTHOR SEARCH ACTIVE:
            // User has entered a search keyword in the search box and submitted the form.
            // The search keyword is trimmed to remove leading/trailing whitespace.
            // Only non-empty keywords trigger the search - empty strings are ignored.
            // 
            // The search performs a case-insensitive partial match on BOTH:
            // 1. Book titles - finds books whose title contains the keyword
            // 2. Author names - finds books whose author name contains the keyword
            // 
            // Results include books matching in EITHER field (OR logic).
            // 
            // EXAMPLES OF WHAT GETS MATCHED:
            // - Searching "java" matches:
            //   * Titles: "Java Programming", "Advanced Java", "JavaScript Basics"
            //   * Authors: Books by authors with "java" in their name (rare)
            // 
            // - Searching "stephen king" matches:
            //   * Titles: Books with "stephen king" in title (rare)
            //   * Authors: All books by "Stephen King" (The Shining, It, The Stand, etc.)
            // 
            // - Searching "rowling" matches:
            //   * Titles: Books with "rowling" in title (rare)
            //   * Authors: All books by "J.K. Rowling" (entire Harry Potter series)
            // 
            // Pagination and sorting are still applied to search results.
            // User can navigate through multiple pages of search results and sort them.
            books = bookService.searchBooks(search, page, size, sortBy, sortDir);
        }
        else if (minPrice != null && maxPrice != null) {
            // Filter: Show books within the specified price range
            books = bookService.getBooksByPriceRange(minPrice, maxPrice, page, size, sortBy, sortDir);
        }
        else if (saleOnly) {
            // Filter: Show only books currently on sale (have active discount)
            books = bookService.getOnSaleBooks(page, size, sortBy, sortDir);
        }
        else {
            // Default: Show all books with pagination and sorting
            books = bookService.getAllBooks(page, size, sortBy, sortDir);
        }

        // ====================================================================
        // CATEGORY SIDEBAR MENU POPULATION
        // ====================================================================
        // Fetch all available categories from the database to populate the sidebar
        // filter menu. This allows users to see all categories and quickly switch
        // between them by clicking category links.
        var categories = bookService.getAllCategories();

        // ====================================================================
        // SELECTED CATEGORY HIGHLIGHTING
        // ====================================================================
        // If a category filter is active (categoryId is not null), find the full
        // Category object from the list. This object contains category details
        // (name, description) that can be displayed in the UI to show which category
        // is currently selected. The template uses this to:
        // 1. Highlight the active category in the sidebar menu
        // 2. Display the category name in the page header/breadcrumb
        // 3. Show category-specific information or styling
        // ====================================================================
        com.bookverse.BookVerse.entity.Category selectedCategoryObj = null;
        if (categoryId != null) {
            // Search through all categories to find the one matching the selected categoryId
            // This gives us access to the full Category entity (name, description, etc.)
            // instead of just the ID, which is useful for display purposes
            selectedCategoryObj = categories.stream()
                    .filter(c -> c.getCategoryId().equals(categoryId))
                    .findFirst()
                    .orElse(null);
        }

        // Get random books for sidebar recommendations
        // These are displayed to help users discover new books outside their current filter
        var randomBooks = bookService.getRandomBooks(6);

        // Prepare wishlist books grouped for carousel display
        // If user is logged in, fetch their wishlist and organize books into groups of 3
        // This allows the carousel to display multiple books per slide
        List<List<Book>> wishlistBooksGroups = new java.util.ArrayList<>();
        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser != null) {
                // Fetch all wishlist items for the current user
                List<Wishlist> allWishlistItems = wishlistService.getUserWishlist(currentUser.getUserId());
                
                // Extract book entities from wishlist items
                List<Book> allWishlistBooks = allWishlistItems.stream()
                        .map(Wishlist::getBook)
                        .collect(Collectors.toList());

                // Group books into chunks of 3 for carousel slides
                // Each slide will display up to 3 books from the wishlist
                int itemsPerSlide = 3;
                for (int i = 0; i < allWishlistBooks.size(); i += itemsPerSlide) {
                    int endIndex = Math.min(i + itemsPerSlide, allWishlistBooks.size());
                    List<Book> group = new java.util.ArrayList<>(allWishlistBooks.subList(i, endIndex));
                    wishlistBooksGroups.add(group);
                }
            }
        }

        // ====================================================================
        // MODEL ATTRIBUTES FOR TEMPLATE RENDERING
        // ====================================================================
        // Add all data to the model for Thymeleaf template rendering.
        // The template uses these attributes to display the book list, filters, and controls.
        // ====================================================================
        model.addAttribute("books", books);                    // Paginated book list (or search results if search is active)
        model.addAttribute("categories", categories);          // All categories for sidebar
        model.addAttribute("randomBooks", randomBooks);        // Random recommendations
        model.addAttribute("wishlistBooksGroups", wishlistBooksGroups); // Wishlist carousel data
        model.addAttribute("currentPage", page);               // Current page number
        model.addAttribute("totalPages", books.getTotalPages()); // Total number of pages
        model.addAttribute("totalItems", books.getTotalElements()); // Total number of books (or search results count)
        model.addAttribute("selectedCategory", categoryId);    // Selected category ID
        model.addAttribute("selectedCategoryObj", selectedCategoryObj); // Selected category object
        
        // ====================================================================
        // SEARCH KEYWORD PRESERVATION
        // ====================================================================
        // Store the search keyword in the model so the template can:
        // 1. Display it in the search input field (th:value="${searchKeyword}")
        //    - User sees what they searched for when viewing results
        // 2. Preserve it in pagination links (hidden input: name="search")
        //    - When user clicks "Next Page", search keyword is maintained
        // 3. Preserve it in sorting links (hidden input: name="search")
        //    - When user changes sort order, search keyword is maintained
        // 4. Preserve it in view type switches (hidden input: name="search")
        //    - When user switches grid/list view, search keyword is maintained
        // 5. Display search results header (e.g., "Search results for: 'harry potter'")
        // 
        // This ensures the search context persists across all user interactions
        // on the shop page, providing a seamless search experience.
        // ====================================================================
        model.addAttribute("searchKeyword", search);           // Current search keyword (preserved in all links)
        model.addAttribute("sortBy", sortBy);                  // Current sort field
        model.addAttribute("sortDir", sortDir);                // Current sort direction
        model.addAttribute("viewType", view);                  // Current view type (grid/list)
        model.addAttribute("saleOnly", saleOnly);              // Sale filter flag

        // Return the Thymeleaf template that renders the shop page
        return "user/shop";
    }

    @GetMapping("/product/{id}")
    public String productDetails(
            @PathVariable Long id,
            Model model,
            HttpSession session,
            Authentication authentication) {
        
        // Block admin from accessing user pages
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> {
                        String auth = authority.getAuthority().toUpperCase();
                        return auth.equals("ROLE_ADMIN") || auth.contains("ADMIN");
                    });
            
            if (!isAdmin) {
                User currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null && currentUser.getRole() != null) {
                    String roleName = currentUser.getRole().getName();
                    if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                        isAdmin = true;
                    }
                }
            }
            
            if (isAdmin) {
                return "redirect:/demo/admin";
            }
        }
        Optional<Book> bookOpt = bookService.getBookByIdWithDetails(id);

        if (bookOpt.isEmpty()) {
            return "redirect:/shop";
        }

        Book book = bookOpt.get();

        // Lấy related books (cùng category, tối đa 4 sách)
        List<Book> relatedBooks = Collections.emptyList();
        if (book.getCategory() != null) {
            relatedBooks = bookService.getRelatedBooks(book.getBookId(), book.getCategory().getCategoryId(), 4);
        }

        // Nếu không đủ related books, lấy thêm sách ngẫu nhiên
        if (relatedBooks.size() < 4) {
            List<Book> randomBooks = bookService.getRandomBooks(4 - relatedBooks.size());
            // Loại trừ sách hiện tại
            randomBooks = randomBooks.stream()
                    .filter(b -> !b.getBookId().equals(book.getBookId()))
                    .collect(java.util.stream.Collectors.toList());
            relatedBooks.addAll(randomBooks);
        }

        // Lấy reviews hiển thị từ DB và tính rating trung bình
        List<Review> visibleReviews = reviewRepository.findByBookBookIdAndVisibleTrueOrderByCreatedAtDesc(book.getBookId());
        double avgRating = 0.0;
        if (visibleReviews != null && !visibleReviews.isEmpty()) {
            avgRating = visibleReviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
        }

        // Lấy tất cả wishlist items và chia thành các nhóm 3 items cho carousel
        List<List<Book>> wishlistBooksGroups = new java.util.ArrayList<>();
        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser != null) {
                List<Wishlist> allWishlistItems = wishlistService.getUserWishlist(currentUser.getUserId());
                // Loại trừ sách hiện tại khỏi wishlist
                List<Book> allWishlistBooks = allWishlistItems.stream()
                        .map(Wishlist::getBook)
                        .filter(b -> !b.getBookId().equals(book.getBookId())) // Loại trừ sách hiện tại
                        .collect(Collectors.toList());

                // Chia thành các nhóm 3 items
                int itemsPerSlide = 3;
                for (int i = 0; i < allWishlistBooks.size(); i += itemsPerSlide) {
                    int endIndex = Math.min(i + itemsPerSlide, allWishlistBooks.size());
                    List<Book> group = new java.util.ArrayList<>(allWishlistBooks.subList(i, endIndex));
                    wishlistBooksGroups.add(group);
                }
            }
        }

        model.addAttribute("book", book);
        model.addAttribute("relatedBooks", relatedBooks);
        model.addAttribute("wishlistBooksGroups", wishlistBooksGroups);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("visibleReviews", visibleReviews);
        model.addAttribute("visibleReviewCount", visibleReviews.size());
        model.addAttribute("categories", bookService.getAllCategories());

        return "user/product-details";
    }

    @PostMapping("/product/{id}/reviews")
    public String submitReview(
            @PathVariable("id") Long bookId,
            @RequestParam("rating") int rating,
            @RequestParam(value = "comment", required = false) String comment,
            HttpSession session) {
        // Validate rating bounds
        if (rating < 1) rating = 1;
        if (rating > 5) rating = 5;

        Optional<Book> bookOpt = bookService.getBookById(bookId);
        if (bookOpt.isEmpty()) {
            return "redirect:/shop";
        }

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Review review = new Review();
        review.setBook(bookOpt.get());
        review.setUser(currentUser);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());
        review.setVisible(true);
        reviewRepository.save(review);

        return "redirect:/shop/product/" + bookId + "#Reviews";
    }
}

