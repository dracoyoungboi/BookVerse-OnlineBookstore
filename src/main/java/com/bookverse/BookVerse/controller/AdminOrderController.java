package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.dto.UserOrderSummary;
import com.bookverse.BookVerse.entity.Order;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.OrderRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.service.OrderSummaryService;
import com.bookverse.BookVerse.service.QRCodeService;
import com.bookverse.BookVerse.service.OrderService;
import com.bookverse.BookVerse.service.EmailService;
import com.bookverse.BookVerse.repository.OrderItemRepository;
import com.bookverse.BookVerse.entity.OrderItem;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
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

    @Autowired
    private EmailService emailService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    /**
     * Lists aggregated order info per customer with search/status filters so admins can drill into users.
     * Also guards access so only ADMIN roles can reach the list page. 
     */
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

        // Ensure default sort direction is ascending
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "userId";
        }
        if (sortDir == null || sortDir.trim().isEmpty()) {
            sortDir = "asc";
        }
        
        // Get order summaries grouped by user - this powers the manage orders landing table
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

    /**
     * Shows the detail view for a single order, including items and management actions.
     */
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

        // Find order with user and items so the template can render a full breakdown
        Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found!");
            return "redirect:/admin/orders";
        }

        Order order = orderOpt.get();
        
        model.addAttribute("order", order);
        return "admin/order-view";
    }

    /**
     * Handles the status transition form within the admin UI (processing -> shipped).
     * Redirect query param lets the same handler keep list + detail pages in sync. 
     */
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

        String normalizedStatus = status != null ? status.trim().toLowerCase() : "";

        // Validate status - only allow expected statuses to prevent arbitrary values from UI tampering
        if (!isAllowedStatus(normalizedStatus)) {
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
        
        String currentStatus = order.getStatus() != null ? order.getStatus().toLowerCase() : "pending";

        // Only allow forward/linear transitions (pending -> processing handled elsewhere, processing -> shipped here)
        if (!isValidStatusTransition(currentStatus, normalizedStatus)) {
            redirectAttributes.addFlashAttribute("error", "Invalid status transition!");
            if (redirect.equals("view")) {
                return "redirect:/admin/orders/" + id;
            }
            return "redirect:/admin/orders";
        }
        
        order.setStatus(normalizedStatus);
        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("success", "Order status updated successfully!");
        if (redirect.equals("view")) {
            return "redirect:/admin/orders/" + id;
        }
        return "redirect:/admin/orders";
    }

    /**
     * Processes a pending order (deduct stock, mark processing) when admins click "Process Payment".
     */
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

        // Process payment (deduct stock, change status to processing) via service, surface message to UI
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

    /**
     * Renders a paginated per-user order list so admins can inspect one customer's history.
     */
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

        // Create pageable with sorting (ASC by orderId)
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderId").ascending());
        
        // Get orders for this user filtered by status if provided
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

    /**
     * Deletes an order (pending/processing) and optionally notifies customers depending on status.
     * Includes extra validation so admins cannot wipe fulfilled orders accidentally.
     */
    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteOrder(@PathVariable("id") Long id,
                             @RequestParam(value = "cancellationReason", required = false) String cancellationReason,
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

        try {
            // Find order with user so we can email them about cancellations
            Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(id);
            if (orderOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Order not found!");
                return "redirect:/admin/orders";
            }

            Order order = orderOpt.get();
            String orderStatus = order.getStatus();

            // Validate: Only allow deletion of pending or processing orders
            if (orderStatus == null || (!orderStatus.equalsIgnoreCase("pending") && !orderStatus.equalsIgnoreCase("processing"))) {
                redirectAttributes.addFlashAttribute("error", "Only pending or processing orders can be deleted!");
                return "redirect:/admin/orders/" + id;
            }

            // If processing order, cancellation reason is required
            if (orderStatus.equalsIgnoreCase("processing")) {
                if (cancellationReason == null || cancellationReason.trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Cancellation reason is required for processing orders!");
                    return "redirect:/admin/orders/" + id;
                }

                // Send cancellation email to user
                if (order.getUser() != null && order.getUser().getEmail() != null) {
                    try {
                        String userName = order.getUser().getFullName() != null ? order.getUser().getFullName() : order.getUser().getUsername();
                        emailService.sendOrderCancellationEmail(
                            order.getUser().getEmail(),
                            userName,
                            order.getOrderId(),
                            cancellationReason.trim()
                        );
                    } catch (Exception e) {
                        // Log error but continue with deletion
                        System.err.println("Failed to send cancellation email: " + e.getMessage());
                    }
                }
            }
            
            // If pending order, send email with payment issue reason
            if (orderStatus.equalsIgnoreCase("pending")) {
                // Send email to user about payment issue
                if (order.getUser() != null && order.getUser().getEmail() != null) {
                    try {
                        String userName = order.getUser().getFullName() != null ? order.getUser().getFullName() : order.getUser().getUsername();
                        String paymentIssueReason = "Chưa thanh toán hoặc lỗi thanh toán. Quý khách hàng nên check lại.";
                        emailService.sendOrderCancellationEmail(
                            order.getUser().getEmail(),
                            userName,
                            order.getOrderId(),
                            paymentIssueReason
                        );
                    } catch (Exception e) {
                        // Log error but continue with deletion
                        System.err.println("Failed to send payment issue email: " + e.getMessage());
                    }
                }
            }

            // Delete order items first to avoid TransientObjectException when removing the parent
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                orderItemRepository.deleteAll(order.getOrderItems());
                orderItemRepository.flush(); // Flush to ensure items are deleted before order
            }

            // Delete order after order items are deleted
            orderRepository.delete(order);
            orderRepository.flush(); // Flush to ensure order is deleted

            if (orderStatus.equalsIgnoreCase("processing")) {
                redirectAttributes.addFlashAttribute("success", "Order deleted successfully! Cancellation email sent to customer.");
            } else if (orderStatus.equalsIgnoreCase("pending")) {
                redirectAttributes.addFlashAttribute("success", "Order deleted successfully! Payment issue email sent to customer.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Order deleted successfully!");
            }

            return "redirect:/admin/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting order: " + e.getMessage());
            return "redirect:/admin/orders/" + id;
        }
    }

    private boolean isAllowedStatus(String status) {
        return "pending".equals(status) || "processing".equals(status) || "shipped".equals(status);
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus.equals(newStatus)) {
            return true;
        }
        return switch (currentStatus) {
            case "pending" -> "processing".equals(newStatus);
            case "processing" -> "shipped".equals(newStatus);
            default -> false;
        };
    }
}
