package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Get notifications count (for AJAX)
     */
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificationCount(HttpSession session) {
        long unreadCount = notificationService.getUnreadCount(session);
        Map<String, Object> response = new HashMap<>();
        response.put("count", unreadCount);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all notifications (for AJAX)
     */
    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotifications(HttpSession session) {
        var notifications = notificationService.getNotifications(session);
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        response.put("unreadCount", notificationService.getUnreadCount(session));
        return ResponseEntity.ok(response);
    }

    /**
     * Mark notification as read
     */
    @PostMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id, HttpSession session) {
        notificationService.markAsRead(id, session);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("unreadCount", notificationService.getUnreadCount(session));
        return ResponseEntity.ok(response);
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/read-all")
    public String markAllAsRead(HttpSession session) {
        notificationService.markAllAsRead(session);
        return "redirect:/admin/orders";
    }
}

