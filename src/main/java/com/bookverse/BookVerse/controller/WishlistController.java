package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.entity.Wishlist;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/wishlist")
public class WishlistController {
    
    @Autowired
    private WishlistService wishlistService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Hiển thị trang wishlist với phân trang
     */
    @GetMapping
    public String wishlistPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model, 
            HttpSession session,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Please login to view your wishlist");
            return "redirect:/login";
        }
        
        // Lấy user từ session hoặc authentication
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }
        
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/login";
        }
        
        // Lấy wishlist items với phân trang (mặc định sort theo addedAt desc)
        Page<Wishlist> wishlistPage = wishlistService.getUserWishlistPaged(
            currentUser.getUserId(), 
            page, 
            size, 
            "addedAt", 
            "desc"
        );
        
        // Tính toán pagination info
        int startPage = Math.max(0, page - 1);
        int endPage = Math.min(wishlistPage.getTotalPages() - 1, page + 1);
        boolean showFirstPage = page > 2;
        boolean showLastPage = page < wishlistPage.getTotalPages() - 2 && wishlistPage.getTotalPages() > 1;
        boolean showFirstEllipsis = page > 3;
        boolean showLastEllipsis = page < wishlistPage.getTotalPages() - 3;
        
        model.addAttribute("wishlistItems", wishlistPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", wishlistPage.getTotalPages());
        model.addAttribute("totalItems", wishlistPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showFirstPage", showFirstPage);
        model.addAttribute("showLastPage", showLastPage);
        model.addAttribute("showFirstEllipsis", showFirstEllipsis);
        model.addAttribute("showLastEllipsis", showLastEllipsis);
        model.addAttribute("wishlistCount", wishlistPage.getTotalElements());
        
        return "user/wishlist";
    }
    
    /**
     * Thêm sách vào wishlist (AJAX)
     */
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToWishlist(@RequestParam Long bookId,
                                                              HttpSession session,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Please login to add items to wishlist");
            response.put("redirect", "/login");
            return ResponseEntity.ok(response);
        }
        
        // Lấy user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }
        
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.ok(response);
        }
        
        // Thêm vào wishlist
        boolean added = wishlistService.addToWishlist(currentUser.getUserId(), bookId);
        
        if (added) {
            response.put("success", true);
            response.put("message", "Book added to wishlist");
            response.put("wishlistCount", wishlistService.getWishlistCount(currentUser.getUserId()));
        } else {
            response.put("success", false);
            response.put("message", "Book already in wishlist or not found");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Xóa sách khỏi wishlist
     */
    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromWishlist(@RequestParam Long bookId,
                                                                   HttpSession session,
                                                                   Authentication authentication,
                                                                   @RequestParam(required = false, defaultValue = "false") boolean redirect) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Please login");
            return ResponseEntity.ok(response);
        }
        
        // Lấy user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }
        
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.ok(response);
        }
        
        // Xóa khỏi wishlist
        boolean removed = wishlistService.removeFromWishlist(currentUser.getUserId(), bookId);
        
        if (removed) {
            response.put("success", true);
            response.put("message", "Book removed from wishlist");
            response.put("wishlistCount", wishlistService.getWishlistCount(currentUser.getUserId()));
            
            if (redirect) {
                response.put("redirect", "/wishlist");
            }
        } else {
            response.put("success", false);
            response.put("message", "Failed to remove book from wishlist");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Toggle wishlist (thêm nếu chưa có, xóa nếu đã có)
     */
    @PostMapping("/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleWishlist(@RequestParam Long bookId,
                                                               HttpSession session,
                                                               Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Please login to add items to wishlist");
            response.put("redirect", "/login");
            return ResponseEntity.ok(response);
        }
        
        // Lấy user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }
        
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.ok(response);
        }
        
        // Toggle wishlist
        boolean wasInWishlist = wishlistService.isInWishlist(currentUser.getUserId(), bookId);
        boolean toggled = wishlistService.toggleWishlist(currentUser.getUserId(), bookId);
        
        if (toggled) {
            response.put("success", true);
            response.put("inWishlist", !wasInWishlist);
            response.put("message", wasInWishlist ? "Book removed from wishlist" : "Book added to wishlist");
            response.put("wishlistCount", wishlistService.getWishlistCount(currentUser.getUserId()));
        } else {
            response.put("success", false);
            response.put("message", "Failed to update wishlist");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Kiểm tra xem sách có trong wishlist không (AJAX)
     */
    @GetMapping("/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkWishlist(@RequestParam Long bookId,
                                                              HttpSession session,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("inWishlist", false);
            return ResponseEntity.ok(response);
        }
        
        // Lấy user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }
        
        if (currentUser != null) {
            boolean inWishlist = wishlistService.isInWishlist(currentUser.getUserId(), bookId);
            response.put("inWishlist", inWishlist);
        } else {
            response.put("inWishlist", false);
        }
        
        return ResponseEntity.ok(response);
    }
}

