package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Role;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.RoleRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.service.EmailService;
import com.bookverse.BookVerse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;

    @GetMapping("/login")
    public String loginForm(Model model, HttpSession session, Authentication authentication) {
        // If user is already authenticated, redirect to appropriate page
        if (authentication != null && authentication.isAuthenticated()) {
            // Check if user is admin
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN") 
                            || authority.getAuthority().contains("ADMIN"));
            
            if (isAdmin) {
                return "redirect:/demo/admin";
            } else {
                return "redirect:/";
            }
        }
        
        // Get last username from session if login failed
        String lastUsername = (String) session.getAttribute("lastUsername");
        if (lastUsername != null) {
            model.addAttribute("lastUsername", lastUsername);
            session.removeAttribute("lastUsername"); // Remove after using
        }
        return "user/login";
    }

    // Remove this method - Spring Security will handle form login via /perform_login
    // Keep only GET /login to show login page



    @GetMapping("/register")
    public String showRegisterForm(Model model, Authentication authentication) {
        // If user is already authenticated, redirect to appropriate page
        if (authentication != null && authentication.isAuthenticated()) {
            // Check if user is admin
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN") 
                            || authority.getAuthority().contains("ADMIN"));
            
            if (isAdmin) {
                return "redirect:/demo/admin";
            } else {
                return "redirect:/";
            }
        }
        
        model.addAttribute("user", new User());
        return "user/register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, Model model) {
        try {
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                model.addAttribute("error", "Username cannot be empty!");
                return "user/register";
            }
            if (userService.findByUsername(user.getUsername()).isPresent()) {
                model.addAttribute("error", "Username already exists!");
                return "user/register";
            }

            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                model.addAttribute("error", "Email cannot be empty!");
                return "user/register";
            }

            if (!user.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                model.addAttribute("error", "Invalid email format!");
                return "user/register";
            }

            // Check if email already exists
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                model.addAttribute("error", "Email already exists! Please use a different email or try logging in.");
                return "user/register";
            }

            // Validate password strength
            String passwordError = validatePassword(user.getPassword());
            if (passwordError != null) {
                model.addAttribute("error", passwordError);
                return "user/register";
            }

            // Get USER role from database
            Role defaultRole = roleRepository.findByName("USER").orElseGet(() -> {
                Role r = new Role();
                r.setName("USER");
                return roleRepository.save(r);
            });
            user.setRole(defaultRole);

            userService.registerUser(user);

            model.addAttribute("success", "Registration successful! Please log in.");
            return "user/login";

        } catch (Exception e) {
            model.addAttribute("error", "Registration failed! Please try again." + e.getMessage());
            return "user/register";
        }
    }
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model, Authentication authentication) {
        // If user is already authenticated, redirect to appropriate page
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN") 
                            || authority.getAuthority().contains("ADMIN"));
            
            if (isAdmin) {
                return "redirect:/demo/admin";
            } else {
                return "redirect:/";
            }
        }
        
        return "user/forgot-password";
    }
    
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, 
                                        Model model, 
                                        HttpServletRequest request) {
        try {
            // Validate email format
            if (email == null || email.trim().isEmpty()) {
                model.addAttribute("error", "Email cannot be empty!");
                return "user/forgot-password";
            }
            
            if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                model.addAttribute("error", "Invalid email format!");
                model.addAttribute("email", email);
                return "user/forgot-password";
            }
            
            // Find user by email
            List<User> users = userRepository.findAllByEmailWithRole(email);
            
            if (users.isEmpty()) {
                // For security, don't reveal if email exists or not
                // Just show success message
                model.addAttribute("success", 
                    "If an account with that email exists, we have sent a password reset link to your email address.");
                return "user/forgot-password";
            }
            
            // Get the first user (most recent if duplicates exist)
            User user = users.get(0);
            String userName = user.getFullName() != null ? user.getFullName() : user.getUsername();
            
            // Generate reset link (for demo, we'll use a simple approach)
            // In production, you should generate a secure token and store it in database
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String resetLink = baseUrl + "/reset-password?email=" + email + "&token=" + 
                             java.util.UUID.randomUUID().toString();
            
            // Send password reset email
            emailService.sendPasswordResetEmail(email, resetLink, userName);
            
            model.addAttribute("success", 
                "If an account with that email exists, we have sent a password reset link to your email address.");
            
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred. Please try again later.");
            model.addAttribute("email", email);
        }
        
        return "user/forgot-password";
    }
    
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "email", required = false) String email,
                                        @RequestParam(value = "token", required = false) String token,
                                        Model model,
                                        Authentication authentication) {
        // If user is already authenticated, redirect to appropriate page
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN") 
                            || authority.getAuthority().contains("ADMIN"));
            
            if (isAdmin) {
                return "redirect:/demo/admin";
            } else {
                return "redirect:/";
            }
        }
        
        // Validate email and token are provided
        if (email == null || email.trim().isEmpty() || token == null || token.trim().isEmpty()) {
            model.addAttribute("error", "Invalid reset link. Please request a new password reset.");
            return "user/reset-password";
        }
        
        // Validate email format
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            model.addAttribute("error", "Invalid email format.");
            return "user/reset-password";
        }
        
        // Check if user exists
        List<User> users = userRepository.findAllByEmailWithRole(email);
        if (users.isEmpty()) {
            model.addAttribute("error", "Invalid reset link. User not found.");
            return "user/reset-password";
        }
        
        // For demo purposes, we'll accept any token format (UUID)
        // In production, you should validate token against database and check expiration
        try {
            java.util.UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Invalid reset token format.");
            return "user/reset-password";
        }
        
        model.addAttribute("email", email);
        model.addAttribute("token", token);
        return "user/reset-password";
    }
    
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("email") String email,
                                       @RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       Model model,
                                       Authentication authentication) {
        // If user is already authenticated, redirect to appropriate page
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN") 
                            || authority.getAuthority().contains("ADMIN"));
            
            if (isAdmin) {
                return "redirect:/demo/admin";
            } else {
                return "redirect:/demo/user";
            }
        }
        
        try {
            // Validate inputs
            if (email == null || email.trim().isEmpty() || token == null || token.trim().isEmpty()) {
                model.addAttribute("error", "Invalid reset link. Please request a new password reset.");
                return "user/reset-password";
            }
            
            // Validate password strength
            String passwordError = validatePassword(password);
            if (passwordError != null) {
                model.addAttribute("error", passwordError);
                model.addAttribute("email", email);
                model.addAttribute("token", token);
                return "user/reset-password";
            }
            
            if (!password.equals(confirmPassword)) {
                model.addAttribute("error", "Passwords do not match!");
                model.addAttribute("email", email);
                model.addAttribute("token", token);
                return "user/reset-password";
            }
            
            // Validate email format
            if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                model.addAttribute("error", "Invalid email format.");
                return "user/reset-password";
            }
            
            // Validate token format
            try {
                java.util.UUID.fromString(token);
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", "Invalid reset token format.");
                return "user/reset-password";
            }
            
            // Find user by email
            List<User> users = userRepository.findAllByEmailWithRole(email);
            if (users.isEmpty()) {
                model.addAttribute("error", "Invalid reset link. User not found.");
                return "user/reset-password";
            }
            
            // Get the first user (most recent if duplicates exist)
            User user = users.get(0);
            
            // Update password
            user.setPassword(userService.encodePassword(password));
            userRepository.save(user);
            
            model.addAttribute("success", "Your password has been reset successfully! You can now login with your new password.");
            // Don't include email and token in model after success to prevent form display
            
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while resetting your password. Please try again.");
            model.addAttribute("email", email);
            model.addAttribute("token", token);
        }
        
        return "user/reset-password";
    }
    
    @GetMapping("/check-email")
    @ResponseBody
    public java.util.Map<String, Boolean> checkEmail(@RequestParam("email") String email) {
        java.util.Map<String, Boolean> response = new java.util.HashMap<>();
        boolean exists = userRepository.findByEmail(email).isPresent();
        response.put("exists", exists);
        return response;
    }
    
    @GetMapping("/GoogleLogin")
    public String home(Model model,
                       @AuthenticationPrincipal OAuth2User
                               oauth2User) {
        if (oauth2User != null) {
            model.addAttribute("name",
                    oauth2User.getAttribute("name"));
        }
        return "index";
    }
    
    /**
     * Validates password strength
     * Password must contain:
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     * - Minimum 8 characters
     * 
     * @param password The password to validate
     * @return Error message if validation fails, null if valid
     */
    private String validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return "Password cannot be empty!";
        }
        
        if (password.length() < 8) {
            return "Password must be at least 8 characters long!";
        }
        
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowercase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecialChar = true;
            }
        }
        
        if (!hasUppercase) {
            return "Password must contain at least one uppercase letter!";
        }
        
        if (!hasLowercase) {
            return "Password must contain at least one lowercase letter!";
        }
        
        if (!hasDigit) {
            return "Password must contain at least one digit!";
        }
        
        if (!hasSpecialChar) {
            return "Password must contain at least one special character!";
        }
        
        return null; // Password is valid
    }
}
