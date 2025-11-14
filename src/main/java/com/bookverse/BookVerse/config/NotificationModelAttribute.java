package com.bookverse.BookVerse.config;

import com.bookverse.BookVerse.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ControllerAdvice để tự động thêm notification info vào model cho admin pages
 */
@ControllerAdvice
public class NotificationModelAttribute {
    
    @Autowired
    private NotificationService notificationService;
    
    @ModelAttribute
    public void addNotificationAttributes(Model model, HttpSession session) {
        if (session != null) {
            var notifications = notificationService.getNotifications(session);
            long unreadCount = notificationService.getUnreadCount(session);
            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadNotificationCount", unreadCount);
        }
    }
}



