package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.dto.CartItem;
import com.bookverse.BookVerse.entity.*;
import com.bookverse.BookVerse.repository.*;
import com.bookverse.BookVerse.service.CartService;
import com.bookverse.BookVerse.service.CouponService;
import com.bookverse.BookVerse.service.NotificationService;
import com.bookverse.BookVerse.service.OrderService;
import com.bookverse.BookVerse.service.QRCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CouponService couponService;

    /**
     * Hiển thị trang checkout
     */
    @GetMapping
    public String checkoutPage(Model model,
                              HttpSession session,
                              Authentication authentication,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Please login to checkout!");
            return "redirect:/login";
        }

        // Get current user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/login";
        }

        // Check if user is admin - block admin from accessing user pages
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String auth = authority.getAuthority().toUpperCase();
                    return auth.equals("ROLE_ADMIN") || auth.contains("ADMIN");
                });
        
        // Also check role from session/database as fallback
        if (!isAdmin && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                isAdmin = true;
            }
        }
        
        if (isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Admin cannot access user checkout!");
            return "redirect:/demo/admin";
        }

        // Check if cart is empty
        List<CartItem> cartItems = cartService.getCartItems(session);
        if (cartItems == null || cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Your cart is empty!");
            return "redirect:/cart";
        }

        // Calculate totals
        Double cartTotal = cartService.getCartTotal(session);
        Double cartTotalAfterCoupon = cartService.getCartTotalAfterCoupon(session);
        Double couponDiscount = cartService.getCouponDiscount(session);
        String couponCode = cartService.getCouponCode(session);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("cartTotalAfterCoupon", cartTotalAfterCoupon);
        model.addAttribute("couponDiscount", couponDiscount);
        model.addAttribute("couponCode", couponCode);
        model.addAttribute("user", currentUser);

        return "user/checkout";
    }

    /**
     * Xử lý đặt hàng và tạo order
     */
    @PostMapping("/place-order")
    @Transactional
    public String placeOrder(@RequestParam String address,
                            @RequestParam(required = false) String note,
                            HttpSession session,
                            Authentication authentication,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes,
                            HttpServletRequest request) {
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Please login to place order!");
            return "redirect:/login";
        }

        // Get current user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/login";
        }

        // Check if user is admin - block admin from accessing user pages
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String auth = authority.getAuthority().toUpperCase();
                    return auth.equals("ROLE_ADMIN") || auth.contains("ADMIN");
                });
        
        // Also check role from session/database as fallback
        if (!isAdmin && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                isAdmin = true;
            }
        }
        
        if (isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Admin cannot access user checkout!");
            return "redirect:/demo/admin";
        }

        // Validate address
        if (address == null || address.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please enter your address!");
            return "redirect:/checkout";
        }

        // Get cart items
        List<CartItem> cartItems = cartService.getCartItems(session);
        if (cartItems == null || cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Your cart is empty!");
            return "redirect:/cart";
        }

        // Validate stock and calculate total
        Double totalAmount = cartService.getCartTotalAfterCoupon(session);
        for (CartItem cartItem : cartItems) {
            if (cartItem.getBook() == null || cartItem.getBook().getBookId() == null) {
                redirectAttributes.addFlashAttribute("error", "Invalid cart item!");
                return "redirect:/cart";
            }

            // Reload book to get latest stock
            Optional<Book> bookOpt = bookRepository.findById(cartItem.getBook().getBookId());
            if (bookOpt.isEmpty() || (bookOpt.get().getDeleted() != null && bookOpt.get().getDeleted())) {
                redirectAttributes.addFlashAttribute("error", "Book not found or unavailable!");
                return "redirect:/cart";
            }

            Book book = bookOpt.get();
            if (book.getStock() < cartItem.getQuantity()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Insufficient stock for: " + book.getTitle() + ". Available: " + book.getStock());
                return "redirect:/cart";
            }
        }

        // Create order
        Order order = new Order();
        order.setUser(currentUser);
        order.setTotalAmount(totalAmount);
        order.setStatus("pending");
        order.setAddress(address.trim());
        order.setNote(note != null ? note.trim() : null);
        order.setCreatedAt(LocalDateTime.now());
        
        // Save order to get orderId
        order = orderRepository.save(order);
        Long orderId = order.getOrderId();
        
        // Create order items
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setBook(cartItem.getBook());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getBook().getDiscountPrice() != null ? 
                cartItem.getBook().getDiscountPrice() : cartItem.getBook().getPrice());
            orderItemRepository.save(orderItem);
        }

        // Add notification for admin (no need for session parameter, but kept for compatibility)
        notificationService.addNewOrderNotification(order, session);

        // Clear cart after order is created
        cartService.clearCart(session);
        cartService.removeCoupon(session);

        // Redirect to order confirmation page with QR code
        return "redirect:/checkout/order/" + orderId;
    }

    /**
     * Hiển thị trang xác nhận order với QR code
     */
    @GetMapping("/order/{orderId}")
    public String orderConfirmation(@PathVariable Long orderId,
                                   Model model,
                                   HttpSession session,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Please login to view order!");
            return "redirect:/login";
        }

        // Get order
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found!");
            return "redirect:/my-orders";
        }

        Order order = orderOpt.get();

        // Check if order belongs to current user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login!");
            return "redirect:/login";
        }

        // Check if user is admin - block admin from accessing user pages
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String auth = authority.getAuthority().toUpperCase();
                    return auth.equals("ROLE_ADMIN") || auth.contains("ADMIN");
                });
        
        // Also check role from session/database as fallback
        if (!isAdmin && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                isAdmin = true;
            }
        }
        
        if (isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Admin cannot access user pages!");
            return "redirect:/demo/admin";
        }

        // Check if order belongs to current user
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "You don't have permission to view this order!");
            return "redirect:/my-orders";
        }

        // Generate QR code
        String qrCodeBase64 = qrCodeService.generateOrderQRCode(order.getOrderId(), order.getTotalAmount());

        model.addAttribute("order", order);
        model.addAttribute("qrCode", qrCodeBase64);
        model.addAttribute("user", currentUser);

        return "user/order-confirmation";
    }

    /**
     * Gửi request tới admin để xử lý thanh toán
     */
    @PostMapping("/send-payment-request/{orderId}")
    public String sendPaymentRequest(@PathVariable Long orderId,
                                    @RequestParam(required = false) String message,
                                    HttpSession session,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Please login!");
            return "redirect:/login";
        }

        // Get current user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login!");
            return "redirect:/login";
        }

        // Check if user is admin - block admin from accessing user pages
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String auth = authority.getAuthority().toUpperCase();
                    return auth.equals("ROLE_ADMIN") || auth.contains("ADMIN");
                });
        
        // Also check role from session/database as fallback
        if (!isAdmin && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                isAdmin = true;
            }
        }
        
        if (isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Admin cannot access user pages!");
            return "redirect:/demo/admin";
        }

        // Get order
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found!");
            return "redirect:/my-orders";
        }

        Order order = orderOpt.get();

        // Check if order belongs to current user
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "You don't have permission!");
            return "redirect:/my-orders";
        }

        // Check if order is still pending
        if (!"pending".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Order is already processed!");
            return "redirect:/my-orders/" + orderId;
        }

        // Add notification for admin about payment request
        notificationService.addPaymentRequestNotification(order, message, session);

        redirectAttributes.addFlashAttribute("success", 
            "Payment request sent to admin successfully! Admin will process your order soon.");
        return "redirect:/checkout/order/" + orderId;
    }
}







