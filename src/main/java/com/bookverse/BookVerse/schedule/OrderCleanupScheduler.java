package com.bookverse.BookVerse.schedule;

import com.bookverse.BookVerse.entity.Order;
import com.bookverse.BookVerse.repository.OrderRepository;
import com.bookverse.BookVerse.repository.OrderItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderCleanupScheduler.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    // Run every day at 2:00 AM to clean up old pending orders
    // Delete pending orders older than 7 days
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2:00 AM
    @Transactional
    public void cleanupOldPendingOrders() {
        try {
            logger.info("Starting cleanup of old pending orders...");
            
            // Calculate cutoff date (7 days ago)
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            
            // Find pending orders older than 7 days
            List<Order> oldPendingOrders = orderRepository.findPendingOrdersOlderThan(cutoffDate);
            
            if (oldPendingOrders != null && !oldPendingOrders.isEmpty()) {
                int deletedCount = 0;
                for (Order order : oldPendingOrders) {
                    try {
                        // Delete order items first to avoid TransientObjectException
                        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                            orderItemRepository.deleteAll(order.getOrderItems());
                            orderItemRepository.flush();
                        }
                        
                        // Delete order after order items are deleted
                        orderRepository.delete(order);
                        orderRepository.flush();
                        
                        deletedCount++;
                        logger.info("Deleted old pending order #{} (created at: {})", 
                                   order.getOrderId(), order.getCreatedAt());
                    } catch (Exception e) {
                        logger.error("Failed to delete order #{}: {}", order.getOrderId(), e.getMessage(), e);
                    }
                }
                logger.info("Cleanup completed. Deleted {} old pending order(s).", deletedCount);
            } else {
                logger.info("No old pending orders found to clean up.");
            }
        } catch (Exception e) {
            logger.error("Error during order cleanup: {}", e.getMessage(), e);
        }
    }
}

