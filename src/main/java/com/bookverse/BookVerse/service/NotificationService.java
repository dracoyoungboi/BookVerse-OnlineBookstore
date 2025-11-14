package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Order;
import com.bookverse.BookVerse.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    // Application-level storage for notifications (shared across all sessions)
    private static final CopyOnWriteArrayList<Notification> applicationNotifications = new CopyOnWriteArrayList<>();
    // Session-level storage for read status (per admin user)
    private static final ConcurrentHashMap<String, List<Long>> readNotificationsBySession = new ConcurrentHashMap<>();

    /**
     * Thêm notification khi có order mới (lưu vào application-level storage)
     */
    public void addNewOrderNotification(Order order, HttpSession session) {
        Notification notification = new Notification();
        notification.setId(System.currentTimeMillis());
        notification.setType("order");
        notification.setTitle("New Order");
        notification.setMessage("User " + (order.getUser() != null ? order.getUser().getFullName() : "Unknown") + 
                               " has placed a new order #" + order.getOrderId());
        notification.setOrderId(order.getOrderId());
        notification.setUserId(order.getUser() != null ? order.getUser().getUserId() : null);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        
        // Add to application-level storage (beginning of list)
        applicationNotifications.add(0, notification);
        
        // Keep only last 100 notifications
        if (applicationNotifications.size() > 100) {
            List<Notification> toRemove = new ArrayList<>();
            for (int i = 100; i < applicationNotifications.size(); i++) {
                toRemove.add(applicationNotifications.get(i));
            }
            applicationNotifications.removeAll(toRemove);
        }
    }

    /**
     * Thêm notification khi user gửi payment request (lưu vào application-level storage)
     */
    public void addPaymentRequestNotification(Order order, String message, HttpSession session) {
        Notification notification = new Notification();
        notification.setId(System.currentTimeMillis());
        notification.setType("payment");
        notification.setTitle("Payment Request");
        String userInfo = order.getUser() != null ? order.getUser().getFullName() : "Unknown";
        String notificationMessage = "User " + userInfo + " has sent a payment request for order #" + order.getOrderId();
        if (message != null && !message.trim().isEmpty()) {
            notificationMessage += ": " + message.trim();
        }
        notification.setMessage(notificationMessage);
        notification.setOrderId(order.getOrderId());
        notification.setUserId(order.getUser() != null ? order.getUser().getUserId() : null);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        
        // Add to application-level storage (beginning of list)
        applicationNotifications.add(0, notification);
        
        // Keep only last 100 notifications
        if (applicationNotifications.size() > 100) {
            List<Notification> toRemove = new ArrayList<>();
            for (int i = 100; i < applicationNotifications.size(); i++) {
                toRemove.add(applicationNotifications.get(i));
            }
            applicationNotifications.removeAll(toRemove);
        }
    }

    /**
     * Lấy tất cả notifications (from application-level storage)
     */
    public List<Notification> getNotifications(HttpSession session) {
        String sessionId = session.getId();
        List<Long> readIds = readNotificationsBySession.getOrDefault(sessionId, new ArrayList<>());
        
        // Return all notifications with read status based on session
        return applicationNotifications.stream()
                .map(n -> {
                    Notification copy = new Notification();
                    copy.setId(n.getId());
                    copy.setType(n.getType());
                    copy.setTitle(n.getTitle());
                    copy.setMessage(n.getMessage());
                    copy.setOrderId(n.getOrderId());
                    copy.setUserId(n.getUserId());
                    copy.setCreatedAt(n.getCreatedAt());
                    copy.setRead(readIds.contains(n.getId()));
                    return copy;
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy unread notifications count
     */
    public long getUnreadCount(HttpSession session) {
        String sessionId = session.getId();
        List<Long> readIds = readNotificationsBySession.getOrDefault(sessionId, new ArrayList<>());
        
        return applicationNotifications.stream()
                .filter(n -> !readIds.contains(n.getId()))
                .count();
    }

    /**
     * Đánh dấu notification là đã đọc (for this session)
     */
    public void markAsRead(Long notificationId, HttpSession session) {
        String sessionId = session.getId();
        List<Long> readIds = readNotificationsBySession.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        if (!readIds.contains(notificationId)) {
            readIds.add(notificationId);
        }
    }

    /**
     * Đánh dấu tất cả notifications là đã đọc (for this session)
     */
    public void markAllAsRead(HttpSession session) {
        String sessionId = session.getId();
        List<Long> readIds = readNotificationsBySession.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        // Mark all notifications as read for this session
        applicationNotifications.forEach(n -> {
            if (!readIds.contains(n.getId())) {
                readIds.add(n.getId());
            }
        });
    }

    /**
     * Xóa notification (from application-level storage - only for cleanup)
     */
    public void deleteNotification(Long notificationId, HttpSession session) {
        applicationNotifications.removeIf(n -> n.getId().equals(notificationId));
        // Also remove from all session read lists
        readNotificationsBySession.values().forEach(readIds -> readIds.remove(notificationId));
    }

    /**
     * Inner class cho Notification
     */
    public static class Notification {
        private Long id;
        private String type;
        private String title;
        private String message;
        private Long orderId;
        private Long userId;
        private LocalDateTime createdAt;
        private boolean read;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public boolean isRead() {
            return read;
        }

        public void setRead(boolean read) {
            this.read = read;
        }
    }
}


