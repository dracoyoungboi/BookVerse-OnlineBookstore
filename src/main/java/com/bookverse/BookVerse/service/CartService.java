package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.dto.CartItem;
import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.repository.BookRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CartService {
    
    private static final String CART_SESSION_KEY = "cart";
    private static final String COUPON_SESSION_KEY = "cartCoupon";
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private CouponService couponService;
    
    /**
     * Lấy cart từ session
     */
    @SuppressWarnings("unchecked")
    private Map<Long, CartItem> getCart(HttpSession session) {
        Map<Long, CartItem> cart = (Map<Long, CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ConcurrentHashMap<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }
    
    /**
     * Lấy tất cả cart items
     */
    public List<CartItem> getCartItems(HttpSession session) {
        Map<Long, CartItem> cart = getCart(session);
        return new ArrayList<>(cart.values());
    }
    
    /**
     * Thêm sách vào cart
     */
    public boolean addToCart(HttpSession session, Long bookId, int quantity) {
        try {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null || book.getDeleted() != null && book.getDeleted()) {
                return false; // Sách không tồn tại hoặc đã bị xóa
            }
            
            if (book.getStock() < quantity) {
                return false; // Không đủ hàng
            }
            
            Map<Long, CartItem> cart = getCart(session);
            CartItem existingItem = cart.get(bookId);
            
            if (existingItem != null) {
                // Nếu đã có trong cart, cộng thêm quantity
                int newQuantity = existingItem.getQuantity() + quantity;
                if (newQuantity > book.getStock()) {
                    return false; // Không đủ hàng
                }
                existingItem.setQuantity(newQuantity);
            } else {
                // Nếu chưa có trong cart, tạo mới
                CartItem newItem = new CartItem(book, quantity);
                cart.put(bookId, newItem);
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Cập nhật quantity của sách trong cart
     */
    public boolean updateCartItem(HttpSession session, Long bookId, int quantity) {
        try {
            if (quantity <= 0) {
                return removeFromCart(session, bookId);
            }
            
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null || book.getDeleted() != null && book.getDeleted()) {
                return false;
            }
            
            if (book.getStock() < quantity) {
                return false; // Không đủ hàng
            }
            
            Map<Long, CartItem> cart = getCart(session);
            CartItem item = cart.get(bookId);
            if (item != null) {
                item.setQuantity(quantity);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Xóa sách khỏi cart
     */
    public boolean removeFromCart(HttpSession session, Long bookId) {
        try {
            Map<Long, CartItem> cart = getCart(session);
            CartItem removed = cart.remove(bookId);
            return removed != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Xóa tất cả items trong cart
     */
    public void clearCart(HttpSession session) {
        Map<Long, CartItem> cart = getCart(session);
        cart.clear();
    }
    
    /**
     * Đếm tổng số lượng items trong cart
     */
    public int getCartItemCount(HttpSession session) {
        Map<Long, CartItem> cart = getCart(session);
        return cart.values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
    
    /**
     * Đếm số lượng loại sách khác nhau trong cart
     */
    public int getCartSize(HttpSession session) {
        Map<Long, CartItem> cart = getCart(session);
        return cart.size();
    }
    
    /**
     * Tính tổng giá của cart (đã giảm giá)
     */
    public Double getCartTotal(HttpSession session) {
        List<CartItem> items = getCartItems(session);
        Double total = items.stream()
                .mapToDouble(item -> item.getSubtotal() != null ? item.getSubtotal() : 0.0)
                .sum();
        return Math.round(total * 100.0) / 100.0;
    }
    
    /**
     * Tính tổng giá gốc của cart (chưa giảm giá)
     */
    public Double getCartOriginalTotal(HttpSession session) {
        List<CartItem> items = getCartItems(session);
        Double total = items.stream()
                .mapToDouble(item -> item.getOriginalSubtotal() != null ? item.getOriginalSubtotal() : 0.0)
                .sum();
        return Math.round(total * 100.0) / 100.0;
    }
    
    /**
     * Tính tổng tiết kiệm được (giá gốc - giá đã giảm)
     */
    public Double getCartSavings(HttpSession session) {
        Double originalTotal = getCartOriginalTotal(session);
        Double total = getCartTotal(session);
        return Math.round((originalTotal - total) * 100.0) / 100.0;
    }
    
    /**
     * Kiểm tra xem sách có trong cart không
     */
    public boolean isInCart(HttpSession session, Long bookId) {
        Map<Long, CartItem> cart = getCart(session);
        return cart.containsKey(bookId);
    }
    
    /**
     * Lấy quantity của sách trong cart
     */
    public int getCartItemQuantity(HttpSession session, Long bookId) {
        Map<Long, CartItem> cart = getCart(session);
        CartItem item = cart.get(bookId);
        return item != null ? item.getQuantity() : 0;
    }
    
    /**
     * Lấy một số cart items đầu tiên (cho mini cart)
     */
    public List<CartItem> getCartItemsLimit(HttpSession session, int limit) {
        List<CartItem> items = getCartItems(session);
        return items.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy coupon code từ session
     */
    public String getCouponCode(HttpSession session) {
        return (String) session.getAttribute(COUPON_SESSION_KEY);
    }
    
    /**
     * Lưu coupon code vào session
     */
    public void setCouponCode(HttpSession session, String couponCode) {
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            session.setAttribute(COUPON_SESSION_KEY, couponCode.trim().toUpperCase());
        } else {
            session.removeAttribute(COUPON_SESSION_KEY);
        }
    }
    
    /**
     * Xóa coupon khỏi session
     */
    public void removeCoupon(HttpSession session) {
        session.removeAttribute(COUPON_SESSION_KEY);
    }
    
    /**
     * Tính tổng giá của cart sau khi áp dụng coupon
     */
    public Double getCartTotalAfterCoupon(HttpSession session) {
        Double cartTotal = getCartTotal(session);
        String couponCode = getCouponCode(session);
        
        if (couponCode != null && !couponCode.isEmpty()) {
            CouponService.CouponValidationResult result = couponService.validateCoupon(couponCode, cartTotal);
            if (result.isValid() && result.getCoupon() != null) {
                Double discount = result.getCoupon().calculateDiscount(cartTotal);
                Double totalAfterCoupon = cartTotal - discount;
                return Math.max(0.0, Math.round(totalAfterCoupon * 100.0) / 100.0);
            }
        }
        
        return cartTotal;
    }
    
    /**
     * Tính số tiền giảm giá từ coupon
     */
    public Double getCouponDiscount(HttpSession session) {
        Double cartTotal = getCartTotal(session);
        String couponCode = getCouponCode(session);
        
        if (couponCode != null && !couponCode.isEmpty()) {
            CouponService.CouponValidationResult result = couponService.validateCoupon(couponCode, cartTotal);
            if (result.isValid() && result.getCoupon() != null) {
                return result.getCoupon().calculateDiscount(cartTotal);
            }
        }
        
        return 0.0;
    }
}

