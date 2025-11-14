package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.dto.UserOrderSummary;
import com.bookverse.BookVerse.entity.Order;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.OrderRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.service.OrderSummaryService;
import com.bookverse.BookVerse.service.QRCodeService;
import com.bookverse.BookVerse.service.OrderService;
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
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderSummaryService orderSummaryService;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private OrderService orderService;

    // List all orders
    @GetMapping
    public String listOrders(Model model,
                            HttpSession session,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Authentication authentication,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String status,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "6") int size,
                            @RequestParam(defaultValue = "userId") String sortBy,
                            @RequestParam(defaultValue = "asc") String sortDir) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info for header
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

        // Get order summaries grouped by user
        String statusFilter = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        String searchFilter = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        
        List<UserOrderSummary> summaries = orderSummaryService.getOrderSummariesByUser(searchFilter, statusFilter, sortBy, sortDir);
        
        // Manual pagination for summaries
        int totalItems = summaries.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalItems);
        
        List<UserOrderSummary> pagedSummaries = summaries.subList(startIndex, endIndex);
        
        // Calculate pagination info
        int startPage = Math.max(0, page - 1);
        int endPage = Math.min(totalPages - 1, page + 1);
        boolean showFirstPage = page > 2;
        boolean showLastPage = page < totalPages - 2 && totalPages > 1;
        boolean showFirstEllipsis = page > 3;
        boolean showLastEllipsis = page < totalPages - 3;
        
        model.addAttribute("userSummaries", pagedSummaries);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedStatus", status != null ? status : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showFirstPage", showFirstPage);
        model.addAttribute("showLastPage", showLastPage);
        model.addAttribute("showFirstEllipsis", showFirstEllipsis);
        model.addAttribute("showLastEllipsis", showLastEllipsis);
        return "admin/orders-list";
    }

    // View order details
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable("id") Long id,
                          Model model,
                          HttpSession session,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info for header
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

        // Find order with user and items
        Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found!");
            return "redirect:/admin/orders";
        }

        Order order = orderOpt.get();
        
        // Generate QR code for order
        String qrCodeBase64 = qrCodeService.generateOrderQRCode(order.getOrderId(), order.getTotalAmount());
        
        model.addAttribute("order", order);
        model.addAttribute("qrCode", qrCodeBase64);
        return "admin/order-view";
    }

    // Update order status
    @PostMapping("/{id}/update-status")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                   @RequestParam("status") String status,
                                   @RequestParam(value = "redirect", defaultValue = "list") String redirect,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Validate status - only allow processing -> shipped
        if (!status.equals("pending") && !status.equals("processing") && !status.equals("shipped")) {
            redirectAttributes.addFlashAttribute("error", "Invalid status!");
            if (redirect.equals("view")) {
                return "redirect:/admin/orders/" + id;
            }
            return "redirect:/admin/orders";
        }

        // Find order
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found!");
            if (redirect.equals("view")) {
                return "redirect:/admin/orders/" + id;
            }
            return "redirect:/admin/orders";
        }

        Order order = orderOpt.get();
        
        // Only allow processing -> shipped transition
        if (!order.getStatus().equals("processing") && status.equals("shipped")) {
            redirectAttributes.addFlashAttribute("error", "Only processing orders can be shipped!");
            if (redirect.equals("view")) {
                return "redirect:/admin/orders/" + id;
            }
            return "redirect:/admin/orders";
        }
        
        order.setStatus(status);
        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("success", "Order status updated successfully!");
        if (redirect.equals("view")) {
            return "redirect:/admin/orders/" + id;
        }
        return "redirect:/admin/orders";
    }

    // Process payment for pending order
    @PostMapping("/{id}/process-payment")
    public String processPayment(@PathVariable("id") Long id,
                                 @RequestParam(value = "redirect", defaultValue = "list") String redirect,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Find order
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found!");
            if (redirect.equals("view")) {
                return "redirect:/admin/orders/" + id;
            }
            return "redirect:/admin/orders";
        }

        Order order = orderOpt.get();

        // Check if order is pending
        if (!"pending".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Only pending orders can be processed!");
            if (redirect.equals("view")) {
                return "redirect:/admin/orders/" + id;
            }
            return "redirect:/admin/orders";
        }

        // Process payment (deduct stock, change status to processing)
        boolean success = orderService.processPayment(id);
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Payment processed successfully! Order status changed to processing.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to process payment. Please check stock availability.");
        }

        if (redirect.equals("view")) {
            return "redirect:/admin/orders/" + id;
        }
        return "redirect:/admin/orders";
    }

    // View orders by user
    @GetMapping("/user/{userId}")
    public String viewUserOrders(@PathVariable("userId") Long userId,
                                 Model model,
                                 HttpSession session,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Authentication authentication,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "6") int size,
                                 RedirectAttributes redirectAttributes) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info for header
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

        // Get user
        Optional<User> targetUserOpt = userRepository.findById(userId);
        if (targetUserOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/admin/orders";
        }

        User targetUser = targetUserOpt.get();
        model.addAttribute("targetUser", targetUser);

        // Create pageable with sorting
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderId").descending());
        
        // Get orders for this user
        Page<Order> orderPage;
        String statusFilter = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        
        if (statusFilter != null) {
            orderPage = orderRepository.findByUserUserIdAndStatus(userId, statusFilter, pageable);
        } else {
            orderPage = orderRepository.findByUserUserId(userId, pageable);
        }

        // Calculate pagination info
        int startPage = Math.max(0, page - 1);
        int endPage = Math.min(orderPage.getTotalPages() - 1, page + 1);
        boolean showFirstPage = page > 2;
        boolean showLastPage = page < orderPage.getTotalPages() - 2 && orderPage.getTotalPages() > 1;
        boolean showFirstEllipsis = page > 3;
        boolean showLastEllipsis = page < orderPage.getTotalPages() - 3;
        
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("totalItems", orderPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("selectedStatus", status != null ? status : "");
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showFirstPage", showFirstPage);
        model.addAttribute("showLastPage", showLastPage);
        model.addAttribute("showFirstEllipsis", showFirstEllipsis);
        model.addAttribute("showLastEllipsis", showLastEllipsis);
        return "admin/user-orders";
    }
}
