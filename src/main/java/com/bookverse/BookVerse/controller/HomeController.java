package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.dto.ContactFormDTO;
import com.bookverse.BookVerse.entity.Category;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.service.BookService;
import com.bookverse.BookVerse.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    
    private final BookService bookService;
    private final EmailService emailService;
    
    public HomeController(BookService bookService, EmailService emailService) {
        this.bookService = bookService;
        this.emailService = emailService;
    }
    
    @GetMapping("/")
    public String homePage(Model model, Authentication authentication, HttpSession session) {
        // Block admin from accessing user pages
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> {
                        String auth = authority.getAuthority().toUpperCase();
                        return auth.equals("ROLE_ADMIN") || auth.contains("ADMIN");
                    });
            
            // Also check role from session/database as fallback
            if (!isAdmin) {
                User currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null && currentUser.getRole() != null) {
                    String roleName = currentUser.getRole().getName();
                    if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                        isAdmin = true;
                    }
                }
            }
            
            if (isAdmin) {
                return "redirect:/demo/admin";
            }
        }
        // Lấy best seller books (6 sách)
        var bestSellerBooks = bookService.getBestSellerBooks(6);
        
        // Lấy deal books (on sale books, 3 sách)
        var dealBooks = bookService.getOnSaleBooksList(3);
        
        // Lấy new arrival books (6 sách)
        var newArrivalBooks = bookService.getNewArrivalBooks(6);
        
        // Lấy on sale books (6 sách)
        var onSaleBooks = bookService.getOnSaleBooksList(6);
        
        // Lấy featured books (6 sách)
        var featuredBooks = bookService.getFeaturedBooks(6);
        
        // Lấy on sale books cho sidebar (6 sách)
        var bestsellerSidebarBooks = bookService.getOnSaleBooksList(6);
        
        // Lấy categories
        List<Category> categories = bookService.getAllCategories();
        
        // Lấy books by category (4 categories đầu tiên, mỗi category 6 sách) - cho phần trên
        var booksByCategory = new java.util.HashMap<Category, java.util.List<com.bookverse.BookVerse.entity.Book>>();
        for (int i = 0; i < Math.min(4, categories.size()); i++) {
            Category category = categories.get(i);
            var books = bookService.getBooksByCategoryList(category.getCategoryId(), 6);
            booksByCategory.put(category, books);
        }
        
        // Lấy books by category (3 categories tiếp theo, mỗi category 6 sách) - cho phần dưới
        var booksByCategory2 = new java.util.HashMap<Category, java.util.List<com.bookverse.BookVerse.entity.Book>>();
        for (int i = 4; i < Math.min(7, categories.size()); i++) {
            Category category = categories.get(i);
            var books = bookService.getBooksByCategoryList(category.getCategoryId(), 6);
            booksByCategory2.put(category, books);
        }
        
        // Thêm dữ liệu vào model
        model.addAttribute("bestSellerBooks", bestSellerBooks);
        model.addAttribute("dealBooks", dealBooks);
        model.addAttribute("newArrivalBooks", newArrivalBooks);
        model.addAttribute("onSaleBooks", onSaleBooks);
        model.addAttribute("featuredBooks", featuredBooks);
        model.addAttribute("bestsellerSidebarBooks", bestsellerSidebarBooks);
        model.addAttribute("booksByCategory", booksByCategory);
        model.addAttribute("booksByCategory2", booksByCategory2);
        model.addAttribute("categories", categories);
        
        return "user/index-7";
    }
    
    @GetMapping("/about")
    public String aboutPage(Model model, Authentication authentication, HttpSession session) {
        // Block admin from accessing user pages
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> {
                        String auth = authority.getAuthority().toUpperCase();
                        return auth.equals("ROLE_ADMIN") || auth.contains("ADMIN");
                    });
            
            if (!isAdmin) {
                User currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null && currentUser.getRole() != null) {
                    String roleName = currentUser.getRole().getName();
                    if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                        isAdmin = true;
                    }
                }
            }
            
            if (isAdmin) {
                return "redirect:/demo/admin";
            }
        }
        // Lấy categories để hiển thị trong header
        List<Category> categories = bookService.getAllCategories();
        model.addAttribute("categories", categories);
        return "user/about";
    }
    
    @GetMapping("/contact")
    public String contactPage(Model model, Authentication authentication, HttpSession session) {
        // Block admin from accessing user pages
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> {
                        String auth = authority.getAuthority().toUpperCase();
                        return auth.equals("ROLE_ADMIN") || auth.contains("ADMIN");
                    });
            
            if (!isAdmin) {
                User currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null && currentUser.getRole() != null) {
                    String roleName = currentUser.getRole().getName();
                    if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                        isAdmin = true;
                    }
                }
            }
            
            if (isAdmin) {
                return "redirect:/demo/admin";
            }
        }
        // Lấy categories để hiển thị trong header
        List<Category> categories = bookService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("contactForm", new ContactFormDTO());
        return "user/contact";
    }
    
    @PostMapping("/contact")
    @ResponseBody
    public ResponseEntity<?> submitContactForm(@Valid ContactFormDTO contactForm, BindingResult bindingResult) {
        // Validate form
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            
            // Tạo error message từ validation errors
            String errorMessage = errors.values().stream()
                .collect(Collectors.joining(", "));
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorMessage);
        }
        
        try {
            // Gửi email
            emailService.sendContactEmail(
                contactForm.getName(),
                contactForm.getEmail(),
                contactForm.getSubject(),
                contactForm.getMessage()
            );
            
            return ResponseEntity.ok("Thank you for contacting us! We have received your message and will get back to you soon.");
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Sorry, there was an error sending your message. Please try again later or contact us directly at contact@bookverse.com");
        }
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        // Tạo error message từ validation errors
        String errorMessage = errors.values().stream()
            .collect(Collectors.joining(", "));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }
}

