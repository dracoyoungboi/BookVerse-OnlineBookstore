package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Order;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.OrderRepository;
import com.bookverse.BookVerse.repository.UserRepository;
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

import java.util.Optional;

@Controller
@RequestMapping("/my-orders")
public class UserOrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderService orderService;

    // List user's orders
    @GetMapping
    public String listMyOrders(Model model,
                               HttpSession session,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Authentication authentication,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "6") int size,
                               RedirectAttributes redirectAttributes) {
        // Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Please login to view your orders!");
            return "redirect:/login";
        }

        // Get current user
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

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/login";
        }

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("fullName", currentUser.getFullName());

        // Create pageable with sorting
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderId").descending());
        
        // Get user's orders with pagination and status filter
        Page<Order> orderPage;
        String statusFilter = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        
        if (statusFilter != null) {
            orderPage = orderRepository.findByUserUserIdAndStatus(currentUser.getUserId(), statusFilter, pageable);
        } else {
            orderPage = orderRepository.findByUserUserId(currentUser.getUserId(), pageable);
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
        return "user/orders-list";
    }

    // View order details
    @GetMapping("/{id}")
    public String viewMyOrder(@PathVariable("id") Long id,
                             Model model,
                             HttpSession session,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        // Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Please login to view your order!");
            return "redirect:/login";
        }

        // Get current user
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

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/login";
        }

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("fullName", currentUser.getFullName());

        // Find order with user and items
        Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found!");
            return "redirect:/my-orders";
        }

        Order order = orderOpt.get();
        
        // Check if order belongs to current user
        if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "You don't have permission to view this order!");
            return "redirect:/my-orders";
        }

        model.addAttribute("order", order);
        return "user/order-view";
    }

    // View invoice (printable)
    @GetMapping("/{id}/invoice")
    public String viewInvoice(@PathVariable("id") Long id,
                             Model model,
                             HttpSession session,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        // Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Please login to view invoice!");
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

        // Find order with user and items
        Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found!");
            return "redirect:/my-orders";
        }

        Order order = orderOpt.get();
        
        // Check if order belongs to current user and is in processing or shipped status
        if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "You don't have permission to view this invoice!");
            return "redirect:/my-orders";
        }

        if (!order.getStatus().equals("processing") && !order.getStatus().equals("shipped")) {
            redirectAttributes.addFlashAttribute("error", "Invoice is only available for processing or shipped orders!");
            return "redirect:/my-orders/" + id;
        }

        model.addAttribute("order", order);
        return "user/invoice";
    }

    // Process payment: Change order from pending to processing
    @PostMapping("/{id}/process-payment")
    public String processPayment(@PathVariable("id") Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        // Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Please login to process payment!");
            return "redirect:/login";
        }

        // Process payment
        boolean success = orderService.processPayment(id);
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Payment processed successfully! Your order is now being processed.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to process payment. Please check if the order is pending and has sufficient stock.");
        }

        return "redirect:/my-orders/" + id;
    }
}

