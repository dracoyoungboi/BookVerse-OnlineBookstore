package com.bookverse.BookVerse.config;

import com.bookverse.BookVerse.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ControllerAdvice để tự động thêm cart info vào model cho tất cả các request
 */
@ControllerAdvice
public class CartModelAttribute {
    
    @Autowired
    private CartService cartService;
    
    @ModelAttribute
    public void addCartAttributes(Model model, HttpSession session) {
        // Chỉ lấy cart item count (không cần cart items nữa vì đã xóa mini cart)
        int cartItemCount = cartService.getCartItemCount(session);
        
        // Thêm vào model để sử dụng trong template
        model.addAttribute("cartItemCount", cartItemCount);
    }
}

