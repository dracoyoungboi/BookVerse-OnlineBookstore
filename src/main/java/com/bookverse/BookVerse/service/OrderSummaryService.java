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
    public List<UserOrderSummary> getOrderSummariesByUser(String search, String status) {
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
        
        // Sort by total amount descending
        summaries.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
        
        return summaries;
    }
}

