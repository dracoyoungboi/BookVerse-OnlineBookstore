package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.dto.UserOrderSummary;
import com.bookverse.BookVerse.entity.Order;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.OrderRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderSummaryService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get order summaries grouped by user
     */
    public List<UserOrderSummary> getOrderSummariesByUser(String search, String status, String sortBy, String sortDir) {
        List<Long> userIds;
        
        if (search != null && !search.trim().isEmpty() && status != null && !status.trim().isEmpty()) {
            userIds = orderRepository.findDistinctUserIdsBySearchAndStatus(search.trim(), status);
        } else if (search != null && !search.trim().isEmpty()) {
            userIds = orderRepository.findDistinctUserIdsBySearchAndStatus(search.trim(), null);
        } else if (status != null && !status.trim().isEmpty()) {
            userIds = orderRepository.findDistinctUserIdsByStatus(status);
        } else {
            userIds = orderRepository.findDistinctUserIds();
        }

        List<UserOrderSummary> summaries = new ArrayList<>();
        
        for (Long userId : userIds) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                continue;
            }
            
            User user = userOpt.get();
            
            // Check if user matches search criteria
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                boolean matches = (user.getUsername() != null && user.getUsername().toLowerCase().contains(searchLower)) ||
                                 (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower)) ||
                                 (user.getFullName() != null && user.getFullName().toLowerCase().contains(searchLower));
                if (!matches) {
                    continue; // Skip this user if doesn't match search
                }
            }
            
            // Get orders for this user
            List<Order> orders;
            if (status != null && !status.trim().isEmpty()) {
                orders = orderRepository.findByUserUserId(userId).stream()
                    .filter(order -> status.equals(order.getStatus()))
                    .toList();
            } else {
                orders = orderRepository.findByUserUserId(userId);
            }
            
            if (orders.isEmpty()) {
                continue;
            }
            
            // Calculate total amount and order count
            long orderCount = orders.size();
            double totalAmount = orders.stream()
                .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
                .sum();
            
            summaries.add(new UserOrderSummary(user, orderCount, totalAmount));
        }
        
        // Sort summaries (default: userId ASC) - force ascending if not specified
        String finalSortBy = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "userId";
        String finalSortDir = (sortDir != null && !sortDir.trim().isEmpty()) ? sortDir.trim() : "asc";
        boolean ascending = "asc".equalsIgnoreCase(finalSortDir);
        
        switch (finalSortBy) {
            case "userId":
                summaries.sort((a, b) -> {
                    int compare = Long.compare(
                        a.getUser().getUserId() != null ? a.getUser().getUserId() : 0,
                        b.getUser().getUserId() != null ? b.getUser().getUserId() : 0
                    );
                    return ascending ? compare : -compare;
                });
                break;
            case "orderCount":
                summaries.sort((a, b) -> {
                    int compare = Long.compare(
                        a.getOrderCount() != null ? a.getOrderCount() : 0,
                        b.getOrderCount() != null ? b.getOrderCount() : 0
                    );
                    return ascending ? compare : -compare;
                });
                break;
            case "totalAmount":
                summaries.sort((a, b) -> {
                    int compare = Double.compare(
                        a.getTotalAmount() != null ? a.getTotalAmount() : 0.0,
                        b.getTotalAmount() != null ? b.getTotalAmount() : 0.0
                    );
                    return ascending ? compare : -compare;
                });
                break;
            default:
                // Default sort by userId ascending
                summaries.sort((a, b) -> {
                    int compare = Long.compare(
                        a.getUser().getUserId() != null ? a.getUser().getUserId() : 0,
                        b.getUser().getUserId() != null ? b.getUser().getUserId() : 0
                    );
                    return compare;
                });
                break;
        }
        
        return summaries;
    }
}

