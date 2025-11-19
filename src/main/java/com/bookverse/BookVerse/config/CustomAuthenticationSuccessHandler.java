package com.bookverse.BookVerse.config;

import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       Authentication authentication) throws IOException, ServletException {
        
        HttpSession session = request.getSession();
        
        // Get username from authentication
        String username = null;
        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            username = userDetails.getUsername();
        } else if (authentication.getPrincipal() instanceof String) {
            username = (String) authentication.getPrincipal();
        }
        
        // Load user from database and save to session
        if (username != null) {
            // Find user by username with role loaded
            Optional<User> userOpt = userRepository.findByUsernameWithRole(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Force initialize role if lazy
                if (user.getRole() != null) {
                    user.getRole().getName(); // Force fetch
                }
                
                // Store user info in session (don't store password)
                session.setAttribute("currentUser", user);
                session.setAttribute("username", user.getUsername());
                session.setAttribute("fullName", user.getFullName());
                session.setAttribute("email", user.getEmail());
                if (user.getRole() != null) {
                    String roleName = user.getRole().getName();
                    session.setAttribute("role", roleName);
                    System.out.println("[DEBUG] User role from DB: " + roleName);
                } else {
                    System.out.println("[DEBUG] User role is NULL for username: " + username);
                }
                // Set session timeout to 30 minutes
                session.setMaxInactiveInterval(30 * 60);
            }
        }
        
        // Redirect based on role - check all authorities from authentication
        boolean isAdmin = false;
        System.out.println("[DEBUG] Checking authorities for user: " + username);
        System.out.println("[DEBUG] Number of authorities: " + authentication.getAuthorities().size());
        
        for (org.springframework.security.core.GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            System.out.println("[DEBUG] Authority (raw): " + auth);
            String authUpper = auth.toUpperCase();
            System.out.println("[DEBUG] Authority (uppercase): " + authUpper);
            
            // Check if authority is ROLE_ADMIN (Spring Security format)
            // Or contains ADMIN in the name
            if (authUpper.equals("ROLE_ADMIN") || authUpper.contains("ADMIN")) {
                isAdmin = true;
                System.out.println("[DEBUG] Found ADMIN authority!");
                break;
            }
        }
        
        // Also check role from database directly as fallback
        if (!isAdmin && username != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(username);
            if (userOpt.isPresent() && userOpt.get().getRole() != null) {
                String roleName = userOpt.get().getRole().getName();
                System.out.println("[DEBUG] Fallback: Checking role from DB: " + roleName);
                if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                    isAdmin = true;
                    System.out.println("[DEBUG] Fallback: User is ADMIN based on DB role");
                }
            }
        }
        
        if (isAdmin) {
            System.out.println("[DEBUG] =====> User is ADMIN - Redirecting to /demo/admin");
            response.sendRedirect("/demo/admin");
        } else {
            System.out.println("[DEBUG] =====> User is NOT ADMIN - Redirecting to /");
            response.sendRedirect("/");
        }
    }
}

