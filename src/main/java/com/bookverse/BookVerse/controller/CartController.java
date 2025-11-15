package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.dto.CartItem;
import com.bookverse.BookVerse.entity.Category;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.service.BookService;
import com.bookverse.BookVerse.service.CartService;
import com.bookverse.BookVerse.service.CouponService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private CouponService couponService;
    
    /**
     * Hiển thị trang cart
     */
    @GetMapping
    public String cartPage(Model model, HttpSession session, Authentication authentication) {
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
        // Lấy cart items
        List<CartItem> cartItems = cartService.getCartItems(session);
        
        // Tính tổng
        Double cartTotal = cartService.getCartTotal(session);
        Double cartOriginalTotal = cartService.getCartOriginalTotal(session);
        Double cartSavings = cartService.getCartSavings(session);
        int cartItemCount = cartService.getCartItemCount(session);
        
        // Lấy coupon info
        String couponCode = cartService.getCouponCode(session);
        Double couponDiscount = cartService.getCouponDiscount(session);
        Double cartTotalAfterCoupon = cartService.getCartTotalAfterCoupon(session);
        String couponDescription = null;
        
        // Lấy coupon description nếu có
        if (couponCode != null && !couponCode.isEmpty()) {
            CouponService.CouponValidationResult result = couponService.validateCoupon(couponCode, cartTotal);
            if (result.isValid() && result.getCoupon() != null) {
                couponDescription = result.getCoupon().getDescription();
            }
        }
        
        // Lấy categories để hiển thị trong header
        List<Category> categories = bookService.getAllCategories();
        
        // Thêm vào model
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("cartOriginalTotal", cartOriginalTotal);
        model.addAttribute("cartSavings", cartSavings);
        model.addAttribute("cartItemCount", cartItemCount);
        model.addAttribute("categories", categories);
        model.addAttribute("couponCode", couponCode);
        model.addAttribute("couponDiscount", couponDiscount);
        model.addAttribute("cartTotalAfterCoupon", cartTotalAfterCoupon);
        model.addAttribute("couponDescription", couponDescription);
        
        return "user/cart";
    }
    
    /**
     * Thêm sách vào cart (AJAX)
     */
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestParam Long bookId,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (quantity <= 0) {
                quantity = 1;
            }
            
            boolean added = cartService.addToCart(session, bookId, quantity);
            
            if (added) {
                response.put("success", true);
                response.put("message", "Book added to cart");
                response.put("cartItemCount", cartService.getCartItemCount(session));
                response.put("cartTotal", cartService.getCartTotal(session));
            } else {
                response.put("success", false);
                response.put("message", "Failed to add book to cart. Book may be out of stock or not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cập nhật quantity của sách trong cart (AJAX)
     */
    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCartItem(
            @RequestParam Long bookId,
            @RequestParam int quantity,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean updated = cartService.updateCartItem(session, bookId, quantity);
            
            if (updated) {
                CartItem item = cartService.getCartItems(session).stream()
                        .filter(i -> i.getBook().getBookId().equals(bookId))
                        .findFirst()
                        .orElse(null);
                
                response.put("success", true);
                response.put("message", "Cart updated");
                response.put("cartItemCount", cartService.getCartItemCount(session));
                response.put("cartTotal", cartService.getCartTotal(session));
                response.put("cartOriginalTotal", cartService.getCartOriginalTotal(session));
                response.put("cartSavings", cartService.getCartSavings(session));
                
                if (item != null) {
                    response.put("itemSubtotal", item.getSubtotal());
                    response.put("itemQuantity", item.getQuantity());
                }
            } else {
                response.put("success", false);
                response.put("message", "Failed to update cart. Book may be out of stock or not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Xóa sách khỏi cart (AJAX)
     */
    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromCart(
            @RequestParam Long bookId,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean removed = cartService.removeFromCart(session, bookId);
            
            if (removed) {
                response.put("success", true);
                response.put("message", "Book removed from cart");
                response.put("cartItemCount", cartService.getCartItemCount(session));
                response.put("cartTotal", cartService.getCartTotal(session));
                response.put("cartOriginalTotal", cartService.getCartOriginalTotal(session));
                response.put("cartSavings", cartService.getCartSavings(session));
            } else {
                response.put("success", false);
                response.put("message", "Failed to remove book from cart");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lấy thông tin cart (AJAX) - để cập nhật header
     */
    @GetMapping("/info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCartInfo(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int cartItemCount = cartService.getCartItemCount(session);
            Double cartTotal = cartService.getCartTotal(session);
            List<CartItem> cartItems = cartService.getCartItemsLimit(session, 3); // Lấy 3 items đầu tiên cho mini cart
            
            response.put("success", true);
            response.put("cartItemCount", cartItemCount);
            response.put("cartTotal", cartTotal);
            response.put("cartItems", cartItems);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Xóa tất cả items trong cart
     */
    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearCart(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            cartService.clearCart(session);
            cartService.removeCoupon(session); // Xóa coupon khi clear cart
            response.put("success", true);
            response.put("message", "Cart cleared");
            response.put("cartItemCount", 0);
            response.put("cartTotal", 0.0);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Áp dụng coupon (AJAX)
     */
    @PostMapping("/apply-coupon")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyCoupon(
            @RequestParam String couponCode,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Double cartTotal = cartService.getCartTotal(session);
            CouponService.CouponValidationResult result = couponService.validateCoupon(couponCode, cartTotal);
            
            if (result.isValid() && result.getCoupon() != null) {
                // Lưu coupon vào session
                cartService.setCouponCode(session, couponCode);
                
                // Tính toán lại giá
                Double couponDiscount = result.getCoupon().calculateDiscount(cartTotal);
                Double cartTotalAfterCoupon = cartTotal - couponDiscount;
                
                response.put("success", true);
                response.put("message", result.getMessage());
                response.put("couponCode", couponCode);
                response.put("couponDiscount", couponDiscount);
                response.put("cartTotalAfterCoupon", cartTotalAfterCoupon);
                response.put("couponDescription", result.getCoupon().getDescription());
            } else {
                response.put("success", false);
                response.put("message", result.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Xóa coupon (AJAX)
     */
    @PostMapping("/remove-coupon")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeCoupon(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            cartService.removeCoupon(session);
            Double cartTotal = cartService.getCartTotal(session);
            
            response.put("success", true);
            response.put("message", "Coupon removed");
            response.put("cartTotal", cartTotal);
            response.put("cartTotalAfterCoupon", cartTotal);
            response.put("couponDiscount", 0.0);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}

