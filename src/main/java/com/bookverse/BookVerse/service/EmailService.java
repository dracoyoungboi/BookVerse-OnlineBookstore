package com.bookverse.BookVerse.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.contact.email:contact@bookverse.com}")
    private String contactEmail;
    
    public void sendContactEmail(String name, String email, String subject, String message) {
        try {
            // Email gửi đến admin
            SimpleMailMessage adminMessage = new SimpleMailMessage();
            adminMessage.setFrom(fromEmail);
            adminMessage.setTo(contactEmail);
            adminMessage.setSubject("New Contact Form Submission: " + subject);
            adminMessage.setText(buildAdminEmailContent(name, email, subject, message));
            
            mailSender.send(adminMessage);
            
            // Email xác nhận gửi đến người dùng
            SimpleMailMessage userMessage = new SimpleMailMessage();
            userMessage.setFrom(fromEmail);
            userMessage.setTo(email);
            userMessage.setSubject("Thank you for contacting BookVerse");
            userMessage.setText(buildUserConfirmationEmailContent(name, subject));
            
            mailSender.send(userMessage);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
    
    private String buildAdminEmailContent(String name, String email, String subject, String message) {
        return String.format(
            "New contact form submission received:\n\n" +
            "Name: %s\n" +
            "Email: %s\n" +
            "Subject: %s\n\n" +
            "Message:\n%s\n\n" +
            "---\n" +
            "This is an automated message from BookVerse Contact Form.",
            name, email, subject, message
        );
    }
    
    private String buildUserConfirmationEmailContent(String name, String subject) {
        return String.format(
            "Dear %s,\n\n" +
            "Thank you for contacting BookVerse! We have received your message regarding:\n" +
            "\"%s\"\n\n" +
            "Our team will review your inquiry and get back to you as soon as possible, usually within 24-48 hours.\n\n" +
            "If you have any urgent questions, please feel free to call us at (800) 123-4567.\n\n" +
            "Best regards,\n" +
            "The BookVerse Team\n\n" +
            "---\n" +
            "This is an automated confirmation email. Please do not reply to this message.",
            name, subject
        );
    }
}

