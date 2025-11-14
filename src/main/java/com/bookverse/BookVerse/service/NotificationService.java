package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Order;
import com.bookverse.BookVerse.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final String NOTIFICATIONS_KEY = "adminNotifications";

    /**
     * Thêm notification khi có order mới
     */
    public void addNewOrderNotification(Order order, HttpSession session) {
        List<Notification> notifications = getNotifications(session);
        
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
        
        notifications.add(0, notification); // Add to beginning
        
        // Keep only last 50 notifications
        if (notifications.size() > 50) {
            notifications = notifications.subList(0, 50);
        }
        
        session.setAttribute(NOTIFICATIONS_KEY, notifications);
    }

    /**
     * Lấy tất cả notifications
     */
    @SuppressWarnings("unchecked")
    public List<Notification> getNotifications(HttpSession session) {
        Object notificationsObj = session.getAttribute(NOTIFICATIONS_KEY);
        if (notificationsObj == null) {
            return new ArrayList<>();
        }
        return (List<Notification>) notificationsObj;
    }

    /**
     * Lấy unread notifications count
     */
    public long getUnreadCount(HttpSession session) {
        return getNotifications(session).stream()
                .filter(n -> !n.isRead())
                .count();
    }

    /**
     * Đánh dấu notification là đã đọc
     */
    public void markAsRead(Long notificationId, HttpSession session) {
        List<Notification> notifications = getNotifications(session);
        notifications.stream()
                .filter(n -> n.getId().equals(notificationId))
                .findFirst()
                .ifPresent(n -> n.setRead(true));
        session.setAttribute(NOTIFICATIONS_KEY, notifications);
    }

    /**
     * Đánh dấu tất cả notifications là đã đọc
     */
    public void markAllAsRead(HttpSession session) {
        List<Notification> notifications = getNotifications(session);
        notifications.forEach(n -> n.setRead(true));
        session.setAttribute(NOTIFICATIONS_KEY, notifications);
    }

    /**
     * Xóa notification
     */
    public void deleteNotification(Long notificationId, HttpSession session) {
        List<Notification> notifications = getNotifications(session);
        notifications.removeIf(n -> n.getId().equals(notificationId));
        session.setAttribute(NOTIFICATIONS_KEY, notifications);
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


