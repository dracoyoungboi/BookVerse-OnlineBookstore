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
        
        // Make the storefront inaccessible to admins so they stay in the dashboard UI
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
        Page<Book> books;

        // Select the correct dataset for the grid/list view based on active filters
        if (categoryId != null) {
            books = bookService.getBooksByCategory(categoryId, page, size, sortBy, sortDir);
        }
        else if (search != null && !search.trim().isEmpty()) {
            books = bookService.searchBooks(search, page, size, sortBy, sortDir);
        }
        else if (minPrice != null && maxPrice != null) {
            books = bookService.getBooksByPriceRange(minPrice, maxPrice, page, size, sortBy, sortDir);
        }
        else if (saleOnly) {
            books = bookService.getOnSaleBooks(page, size, sortBy, sortDir);
        }
        else {
            books = bookService.getAllBooks(page, size, sortBy, sortDir);
        }

        // Populate sidebar filters so the user can pivot between categories
        var categories = bookService.getAllCategories();

        // Highlight the active category (if any) to keep UI state in sync
        com.bookverse.BookVerse.entity.Category selectedCategoryObj = null;
        if (categoryId != null) {
            selectedCategoryObj = categories.stream()
                    .filter(c -> c.getCategoryId().equals(categoryId))
                    .findFirst()
                    .orElse(null);
        }

        // Surface a small random set to spotlight titles outside the current filters
        var randomBooks = bookService.getRandomBooks(6);

        // Split the user's wishlist into groups of three so the carousel renders clean slides
        List<List<Book>> wishlistBooksGroups = new java.util.ArrayList<>();
        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser != null) {
                List<Wishlist> allWishlistItems = wishlistService.getUserWishlist(currentUser.getUserId());
                // Lấy tất cả books từ wishlist
                List<Book> allWishlistBooks = allWishlistItems.stream()
                        .map(Wishlist::getBook)
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

        // Attach everything the Thymeleaf template needs to render the book list UI
        model.addAttribute("books", books);
        model.addAttribute("categories", categories);
        model.addAttribute("randomBooks", randomBooks);
        model.addAttribute("wishlistBooksGroups", wishlistBooksGroups);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", books.getTotalPages());
        model.addAttribute("totalItems", books.getTotalElements());
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("selectedCategoryObj", selectedCategoryObj);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("viewType", view);
        model.addAttribute("saleOnly", saleOnly);

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

