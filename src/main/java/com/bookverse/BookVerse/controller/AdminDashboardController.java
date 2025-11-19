package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Order;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private com.bookverse.BookVerse.repository.UserRepository userRepository;

    @GetMapping
    public String dashboard(Model model, HttpSession session,
                          @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails userDetails,
                          @RequestParam(required = false, defaultValue = "monthly") String period,
                          @RequestParam(required = false, defaultValue = "monthly") String revenuePeriod) {
        
        // Try to get user from session first
        User currentUser = (User) session.getAttribute("currentUser");
        
        // If not in session, try to get from authentication
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                // Force initialize role if lazy
                if (currentUser.getRole() != null) {
                    currentUser.getRole().getName(); // Force fetch
                }
                session.setAttribute("currentUser", currentUser);
                session.setAttribute("username", currentUser.getUsername());
                session.setAttribute("fullName", currentUser.getFullName());
                if (currentUser.getRole() != null) {
                    session.setAttribute("role", currentUser.getRole().getName());
                }
            }
        }
        
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }
        
        // Get dashboard statistics
        long totalProducts = dashboardService.getTotalProducts();
        long totalCustomers = dashboardService.getTotalCustomers();
        long totalOrders = dashboardService.getTotalOrders();
        Double totalSales = dashboardService.getTotalSales();
        
        // Get period statistics
        Map<String, Object> periodStats = dashboardService.getPeriodStatistics(period);
        
        // Get changes for this week
        long productChange = dashboardService.getProductChangeThisWeek();
        long customerChange = dashboardService.getCustomerChangeThisWeek();
        long orderChange = dashboardService.getOrderChangeThisWeek();
        
        // Get recent orders
        List<Order> recentOrders = dashboardService.getRecentOrders(5);
        
        // Get top selling products
        List<Map<String, Object>> topSellingProducts = dashboardService.getTopSellingProducts(5);
        
        // Get stock report
        List<com.bookverse.BookVerse.entity.Book> stockReport = dashboardService.getStockReport();
        
        // Get order statistics by status
        Map<String, Long> orderStatusStats = dashboardService.getOrderStatisticsByStatus();
        
        // Get processing orders for transactions
        List<Order> processingOrders = dashboardService.getProcessingOrders(6);
        
        // Get data for pie charts (with period filter)
        Map<String, Long> orderStatusDistribution = dashboardService.getOrderStatusDistribution(period);
        Map<String, Double> revenueByCategory = dashboardService.getRevenueByCategory(period);
        
        // Get country distribution for world map
        Map<String, Object> countryDistribution = dashboardService.getCountryDistribution(period);
        
        // Get monthly order statistics for chart
        Map<String, Object> monthlyOrderStats = dashboardService.getMonthlyOrderStatistics();
        
        // Get revenue by period (default: monthly)
        Map<String, Object> revenueData = dashboardService.getRevenueByPeriod(revenuePeriod);
        
        // Add to model
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalSales", totalSales != null ? totalSales : 0.0);
        model.addAttribute("period", period);
        model.addAttribute("periodStats", periodStats);
        model.addAttribute("productChange", productChange);
        model.addAttribute("customerChange", customerChange);
        model.addAttribute("orderChange", orderChange);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("topSellingProducts", topSellingProducts);
        model.addAttribute("stockReport", stockReport);
        model.addAttribute("orderStatusStats", orderStatusStats);
        model.addAttribute("processingOrders", processingOrders);
        model.addAttribute("orderStatusDistribution", orderStatusDistribution);
        model.addAttribute("revenueByCategory", revenueByCategory);
        model.addAttribute("countryDistribution", countryDistribution);
        model.addAttribute("monthlyOrderStats", monthlyOrderStats);
        model.addAttribute("revenueData", revenueData);
        model.addAttribute("revenuePeriod", revenuePeriod);
        
        return "admin/dasboard";
    }
}

