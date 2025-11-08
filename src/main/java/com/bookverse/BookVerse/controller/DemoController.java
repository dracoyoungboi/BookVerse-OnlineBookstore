package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("demo")
public class DemoController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user")
    public String user(Model model, HttpSession session, 
                      HttpServletRequest request,
                      @AuthenticationPrincipal Object principal,
                      Authentication authentication) {
        System.out.println("[DEBUG] DemoController.user() called");
        
        // Check for logout message from session or URL parameter
        String logoutMessage = (String) session.getAttribute("logoutMessage");
        if (logoutMessage != null) {
            model.addAttribute("logoutMessage", logoutMessage);
            session.removeAttribute("logoutMessage"); // Remove after displaying
        } else if (request.getParameter("logout") != null) {
            // Also check URL parameter as fallback
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        
        // Try to get user from session first (most reliable after OAuth2 login)
        User currentUser = (User) session.getAttribute("currentUser");
        String sessionUsername = (String) session.getAttribute("username");
        
        System.out.println("[DEBUG] User from session: " + (currentUser != null ? currentUser.getUsername() : "null"));
        System.out.println("[DEBUG] Username from session: " + sessionUsername);
        System.out.println("[DEBUG] Principal type: " + (principal != null ? principal.getClass().getName() : "null"));
        
        // If not in session, try to get from authentication principal
        if (currentUser == null && authentication != null && authentication.isAuthenticated()) {
            String usernameOrEmail = null;
            
            // Handle different principal types
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                org.springframework.security.core.userdetails.UserDetails userDetails = 
                    (org.springframework.security.core.userdetails.UserDetails) principal;
                usernameOrEmail = userDetails.getUsername();
                System.out.println("[DEBUG] Found UserDetails principal, username: " + usernameOrEmail);
            } else if (principal instanceof com.bookverse.BookVerse.security.CustomOAuth2User) {
                com.bookverse.BookVerse.security.CustomOAuth2User customOAuth2User = 
                    (com.bookverse.BookVerse.security.CustomOAuth2User) principal;
                User user = customOAuth2User.getUser();
                if (user != null) {
                    currentUser = user;
                    usernameOrEmail = user.getEmail();
                    System.out.println("[DEBUG] Found CustomOAuth2User principal, email: " + usernameOrEmail);
                }
            } else if (principal instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) principal;
                usernameOrEmail = oidcUser.getEmail();
                System.out.println("[DEBUG] Found OidcUser principal, email: " + usernameOrEmail);
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                    (org.springframework.security.oauth2.core.user.OAuth2User) principal;
                usernameOrEmail = oauth2User.getAttribute("email");
                System.out.println("[DEBUG] Found OAuth2User principal, email: " + usernameOrEmail);
            }
            
            // If we have username/email but no user object, load from database
            if (currentUser == null && usernameOrEmail != null) {
                // Try by username first
                Optional<User> userOpt = userRepository.findByUsernameWithRole(usernameOrEmail);
                
                // If not found, try by email
                if (userOpt.isEmpty()) {
                    java.util.List<User> users = userRepository.findAllByEmailWithRole(usernameOrEmail);
                    if (!users.isEmpty()) {
                        userOpt = Optional.of(users.get(0));
                    }
                }
                
                if (userOpt.isPresent()) {
                    currentUser = userOpt.get();
                    // Force initialize role if lazy
                    if (currentUser.getRole() != null) {
                        currentUser.getRole().getName(); // Force fetch
                    }
                    System.out.println("[DEBUG] Loaded user from DB: " + currentUser.getUsername());
                }
            }
            
            // Save to session for future requests
            if (currentUser != null) {
                session.setAttribute("currentUser", currentUser);
                session.setAttribute("username", currentUser.getUsername());
                session.setAttribute("fullName", currentUser.getFullName());
                session.setAttribute("email", currentUser.getEmail());
                if (currentUser.getRole() != null) {
                    session.setAttribute("role", currentUser.getRole().getName());
                }
                session.setMaxInactiveInterval(30 * 60);
                System.out.println("[DEBUG] Saved user to session: " + currentUser.getUsername());
            }
        }
        
        // Always get username from session if available (more reliable)
        String displayUsername = sessionUsername != null ? sessionUsername : 
                                 (currentUser != null && currentUser.getUsername() != null ? currentUser.getUsername() : null);
        
        // Get fullName from session or user object
        String displayFullName = (String) session.getAttribute("fullName");
        if (displayFullName == null && currentUser != null) {
            displayFullName = currentUser.getFullName();
        }
        
        // Ensure username is not null (fallback to email)
        if (displayUsername == null && currentUser != null && currentUser.getEmail() != null) {
            displayUsername = currentUser.getEmail().split("@")[0];
            System.out.println("[WARN] Username is null, using email prefix: " + displayUsername);
        }
        
        // Ensure fullName is not null (fallback to username or email)
        if (displayFullName == null || displayFullName.trim().isEmpty()) {
            if (displayUsername != null) {
                displayFullName = displayUsername.contains("@") ? displayUsername.split("@")[0] : displayUsername;
            } else if (currentUser != null && currentUser.getEmail() != null) {
                displayFullName = currentUser.getEmail().split("@")[0];
            } else {
                displayFullName = "User";
            }
            System.out.println("[WARN] FullName is null, using: " + displayFullName);
        }
        
        // Add user info to model for template
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("username", displayUsername);
            model.addAttribute("fullName", displayFullName);
            System.out.println("[DEBUG] Added to model - username: " + displayUsername + ", fullName: " + displayFullName);
        } else if (displayUsername != null) {
            // Even if we don't have full user object, add username to model
            model.addAttribute("username", displayUsername);
            model.addAttribute("fullName", displayFullName != null ? displayFullName : displayUsername);
            System.out.println("[DEBUG] Added username to model: " + displayUsername + ", fullName: " + displayFullName);
        } else {
            System.out.println("[WARN] No user found in session or authentication!");
        }
        
        return "user/index-7";
    }
    
    @GetMapping("/admin")
    public String admin(Model model, HttpSession session,
                       @AuthenticationPrincipal UserDetails userDetails) {
        // Try to get user from session first
        User currentUser = (User) session.getAttribute("currentUser");
        
        // If not in session, try to get from authentication
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                // Force initialize role if lazy
                if (currentUser.getRole() != null) {
                    currentUser.getRole().getName(); // Force fetch
                }
                session.setAttribute("currentUser", currentUser);
                session.setAttribute("username", currentUser.getUsername());
                session.setAttribute("fullName", currentUser.getFullName());
                if (currentUser.getRole() != null) {
                    session.setAttribute("role", currentUser.getRole().getName());
                }
            }
        }
        
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }
        
        return "admin/invoice-list";
    }
}


