package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class AccountController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/my-account")
    public String myAccount(Model model, 
                           HttpSession session,
                           @AuthenticationPrincipal Object principal,
                           Authentication authentication) {
        
        // Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        // Try to get user from session first
        User currentUser = (User) session.getAttribute("currentUser");
        String sessionUsername = (String) session.getAttribute("username");
        
        // If not in session, try to get from authentication principal
        if (currentUser == null) {
            String usernameOrEmail = null;
            
            // Handle different principal types
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                usernameOrEmail = userDetails.getUsername();
            } else if (principal instanceof com.bookverse.BookVerse.security.CustomOAuth2User) {
                com.bookverse.BookVerse.security.CustomOAuth2User customOAuth2User = 
                    (com.bookverse.BookVerse.security.CustomOAuth2User) principal;
                User user = customOAuth2User.getUser();
                if (user != null) {
                    currentUser = user;
                    usernameOrEmail = user.getEmail();
                }
            } else if (principal instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) principal;
                usernameOrEmail = oidcUser.getEmail();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                    (org.springframework.security.oauth2.core.user.OAuth2User) principal;
                usernameOrEmail = oauth2User.getAttribute("email");
            }
            
            // If we have username/email but no user object, load from database
            if (currentUser == null && usernameOrEmail != null) {
                // Try by username first
                Optional<User> userOpt = userRepository.findByUsernameWithRole(usernameOrEmail);
                
                // If not found, try by email
                if (userOpt.isEmpty()) {
                    List<User> users = userRepository.findAllByEmailWithRole(usernameOrEmail);
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
            }
        }
        
        // If still no user found, redirect to login
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Check if user is admin - block admin from accessing user pages
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String auth = authority.getAuthority().toUpperCase();
                    return auth.equals("ROLE_ADMIN") || auth.contains("ADMIN");
                });
        
        // Also check role from session/database as fallback
        if (!isAdmin && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                isAdmin = true;
            }
        }
        
        if (isAdmin) {
            return "redirect:/demo/admin";
        }
        
        // Prepare display values with fallbacks
        String displayUsername = currentUser.getUsername();
        if (displayUsername == null || displayUsername.trim().isEmpty()) {
            displayUsername = currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "User";
        }
        
        String displayFullName = currentUser.getFullName();
        if (displayFullName == null || displayFullName.trim().isEmpty()) {
            displayFullName = displayUsername;
        }
        
        String displayEmail = currentUser.getEmail();
        if (displayEmail == null || displayEmail.trim().isEmpty()) {
            displayEmail = "Not provided";
        }
        
        String displayPhone = currentUser.getPhone();
        if (displayPhone == null || displayPhone.trim().isEmpty()) {
            displayPhone = "Not provided";
        }
        
        String displayAddress = currentUser.getAddress();
        if (displayAddress == null || displayAddress.trim().isEmpty()) {
            displayAddress = "Not provided";
        }
        
        String displayRole = "User";
        if (currentUser.getRole() != null && currentUser.getRole().getName() != null) {
            displayRole = currentUser.getRole().getName();
        }
        
        // Calculate account active duration (time since account creation)
        String accountActiveDuration = "Not available";
        if (currentUser.getCreatedAt() != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime createdAt = currentUser.getCreatedAt();
            
            // Calculate years, months, days using Period for date-based calculation
            java.time.Period period = java.time.Period.between(createdAt.toLocalDate(), now.toLocalDate());
            int years = period.getYears();
            int months = period.getMonths();
            int days = period.getDays();
            
            // Calculate hours and minutes for same-day accounts
            java.time.Duration duration = java.time.Duration.between(createdAt, now);
            long totalHours = duration.toHours();
            long hours = totalHours % 24;
            long minutes = duration.toMinutes() % 60;
            
            // Build human-readable duration string
            StringBuilder durationStr = new StringBuilder();
            boolean hasPrevious = false;
            
            if (years > 0) {
                durationStr.append(years).append(" year").append(years > 1 ? "s" : "");
                hasPrevious = true;
            }
            
            if (months > 0) {
                if (hasPrevious) durationStr.append(", ");
                durationStr.append(months).append(" month").append(months > 1 ? "s" : "");
                hasPrevious = true;
            }
            
            if (days > 0) {
                if (hasPrevious) durationStr.append(", ");
                durationStr.append(days).append(" day").append(days > 1 ? "s" : "");
                hasPrevious = true;
            }
            
            // Only show hours and minutes if account is less than 1 day old, or if we don't have years/months/days
            if (!hasPrevious || (years == 0 && months == 0 && days == 0)) {
                if (hours > 0) {
                    if (hasPrevious) durationStr.append(", ");
                    durationStr.append(hours).append(" hour").append(hours > 1 ? "s" : "");
                    hasPrevious = true;
                }
                
                if (minutes > 0 || !hasPrevious) {
                    if (hasPrevious) durationStr.append(", ");
                    durationStr.append(minutes).append(" minute").append(minutes != 1 ? "s" : "");
                }
            }
            
            // Fallback if duration is 0
            if (durationStr.length() == 0) {
                accountActiveDuration = "Just created";
            } else {
                accountActiveDuration = durationStr.toString();
            }
        }
        
        // Add user info to model
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("username", displayUsername);
        model.addAttribute("fullName", displayFullName);
        model.addAttribute("email", displayEmail);
        model.addAttribute("phone", displayPhone);
        model.addAttribute("address", displayAddress);
        model.addAttribute("role", displayRole);
        model.addAttribute("accountActiveDuration", accountActiveDuration);
        model.addAttribute("userId", currentUser.getUserId());
        
        return "user/my-account";
    }
}

