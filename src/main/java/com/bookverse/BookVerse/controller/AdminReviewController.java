package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Review;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.ReviewRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.repository.BookRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    private boolean ensureAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() &&
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().contains("ADMIN"));
    }

    private void attachCurrentUserToModel(HttpSession session, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
                session.setAttribute("username", currentUser.getUsername());
                session.setAttribute("fullName", currentUser.getFullName());
            }
        }
        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }
    }

    /**
     * Lists reviews with pagination for admin moderation.
     * 
     * PAGINATION: Reviews are paginated (default 10 per page) for admin panel.
     * Can filter by bookId or userId. Sorted by newest first.
     * Shows all reviews (visible and hidden) for moderation purposes.
     */
    @GetMapping
    public String listReviews(Model model,
                              HttpSession session,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Authentication authentication,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) Long bookId,
                              @RequestParam(required = false) Long userId) {
        if (!ensureAdmin(authentication)) {
            return "redirect:/login";
        }
        attachCurrentUserToModel(session, userDetails, model);

        // Pagination: default 10 reviews per page, sorted by newest first
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage;
        if (bookId != null) {
            // Filter by book (all reviews for a specific book)
            reviewPage = reviewRepository.findByBookBookId(bookId, pageable);
        } else if (userId != null) {
            // Filter by user (all reviews by a specific user)
            reviewPage = reviewRepository.findByUserUserId(userId, pageable);
        } else {
            // Show all reviews across all books
            reviewPage = reviewRepository.findAll(pageable);
        }

        int startPage = Math.max(0, page - 1);
        int endPage = Math.min(reviewPage.getTotalPages() - 1, page + 1);
        boolean showFirstPage = page > 2;
        boolean showLastPage = page < reviewPage.getTotalPages() - 2 && reviewPage.getTotalPages() > 1;
        boolean showFirstEllipsis = page > 3;
        boolean showLastEllipsis = page < reviewPage.getTotalPages() - 3;

        model.addAttribute("reviews", reviewPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviewPage.getTotalPages());
        model.addAttribute("totalItems", reviewPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showFirstPage", showFirstPage);
        model.addAttribute("showLastPage", showLastPage);
        model.addAttribute("showFirstEllipsis", showFirstEllipsis);
        model.addAttribute("showLastEllipsis", showLastEllipsis);
        model.addAttribute("bookId", bookId);
        model.addAttribute("userId", userId);
        return "admin/reviews-list";
    }

    /**
     * Toggles review visibility (admin moderation).
     * 
     * MODERATION: Admin can hide/show reviews to moderate content.
     * - Hidden reviews (visible=false) are not displayed on book detail page
     * - Hidden reviews are excluded from average rating calculation
     * - Admin can toggle visibility to hide inappropriate/spam reviews
     */
    @PostMapping("/toggle/{id}")
    public String toggleVisibility(@PathVariable("id") Long id,
                                   RedirectAttributes redirectAttributes,
                                   Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return "redirect:/login";
        }
        Optional<Review> opt = reviewRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Review not found!");
            return "redirect:/admin/reviews";
        }
        Review r = opt.get();
        // Toggle visibility: if visible, hide it; if hidden, show it
        r.setVisible(r.getVisible() == null || !r.getVisible() ? true : false);
        reviewRepository.save(r);
        redirectAttributes.addFlashAttribute("success", "Review visibility updated.");
        return "redirect:/admin/reviews";
    }

    @PostMapping("/delete/{id}")
    public String deleteReview(@PathVariable("id") Long id,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return "redirect:/login";
        }
        try {
            if (!reviewRepository.existsById(id)) {
                redirectAttributes.addFlashAttribute("error", "Review not found!");
                return "redirect:/admin/reviews";
            }
            reviewRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Review deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting review: " + e.getMessage());
        }
        return "redirect:/admin/reviews";
    }
}
