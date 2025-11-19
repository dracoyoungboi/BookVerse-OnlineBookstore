package com.bookverse.BookVerse.config;

import com.bookverse.BookVerse.entity.Role;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.RoleRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.security.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    public CustomOAuth2AuthenticationSuccessHandler() {
        super();
        System.out.println("[DEBUG] CustomOAuth2AuthenticationSuccessHandler created");
    }

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       Authentication authentication) throws IOException, ServletException {
        
        HttpSession session = request.getSession();
        System.out.println("[DEBUG] CustomOAuth2AuthenticationSuccessHandler.onAuthenticationSuccess() called");
        System.out.println("[DEBUG] Principal type: " + authentication.getPrincipal().getClass().getName());
        
        // Get OAuth2User from authentication
        if (authentication.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
            User user = customOAuth2User.getUser();
            
            System.out.println("[DEBUG] CustomOAuth2User found, user: " + (user != null ? user.getUsername() : "null"));
            
            if (user != null) {
                // Reload user from DB to ensure we have the latest data (especially for newly created users)
                String email = user.getEmail();
                if (email != null) {
                    java.util.List<User> users = userRepository.findAllByEmailWithRole(email);
                    if (!users.isEmpty()) {
                        user = users.get(0);
                        System.out.println("[DEBUG] Reloaded user from DB - userId: " + user.getUserId() + 
                                         ", username: " + user.getUsername() + 
                                         ", fullName: " + user.getFullName());
                    }
                }
                
                // Force initialize role if lazy
                if (user.getRole() != null) {
                    user.getRole().getName(); // Force fetch
                }
                
                // Ensure username and fullName are not null
                String username = user.getUsername();
                if (username == null || username.trim().isEmpty()) {
                    username = user.getEmail() != null ? user.getEmail() : "user";
                    System.out.println("[WARN] Username is null, using email: " + username);
                }
                
                String fullName = user.getFullName();
                if (fullName == null || fullName.trim().isEmpty()) {
                    if (username.contains("@")) {
                        fullName = username.split("@")[0];
                    } else {
                        fullName = username;
                    }
                    System.out.println("[WARN] FullName is null, using: " + fullName);
                }
                
                // Store user info in session (always use non-null values)
                session.setAttribute("currentUser", user);
                session.setAttribute("username", username);
                session.setAttribute("fullName", fullName);
                session.setAttribute("email", user.getEmail() != null ? user.getEmail() : "");
                if (user.getRole() != null) {
                    session.setAttribute("role", user.getRole().getName());
                }
                // Set session timeout to 30 minutes
                session.setMaxInactiveInterval(30 * 60);
                
                System.out.println("[DEBUG] Saved to session - username: " + username + 
                                 ", fullName: " + fullName + 
                                 ", email: " + (user.getEmail() != null ? user.getEmail() : "null") +
                                 ", role: " + (user.getRole() != null ? user.getRole().getName() : "null"));
            } else {
                System.out.println("[WARN] CustomOAuth2User has null user object!");
            }
        } else if (authentication.getPrincipal() instanceof OidcUser) {
            // Handle OIDC User (Google uses OIDC)
            System.out.println("[DEBUG] OidcUser found (Google OIDC)");
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String email = oidcUser.getEmail();
            System.out.println("[DEBUG] OidcUser email: " + email);
            
            if (email != null) {
                // Check if user exists in database
                java.util.List<User> users = userRepository.findAllByEmailWithRole(email);
                User user;
                
                if (users.isEmpty()) {
                    // Create new user
                    System.out.println("[DEBUG] Creating new user for OIDC email: " + email);
                    user = new User();
                    user.setEmail(email);
                    user.setUsername(email);
                    
                    // Get name from OIDC claims
                    String name = oidcUser.getFullName();
                    if (name == null || name.trim().isEmpty()) {
                        String givenName = oidcUser.getGivenName();
                        String familyName = oidcUser.getFamilyName();
                        if (givenName != null || familyName != null) {
                            name = ((givenName != null ? givenName : "") + " " + (familyName != null ? familyName : "")).trim();
                        }
                        if (name == null || name.trim().isEmpty()) {
                            name = email.split("@")[0];
                        }
                    }
                    user.setFullName(name);
                    
                    user.setCreatedAt(java.time.LocalDateTime.now());
                    
                    // Get USER role
                    Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
                        Role r = new Role();
                        r.setName("USER");
                        return roleRepository.save(r);
                    });
                    user.setRole(userRole);
                    
                    // Save user
                    user = userRepository.save(user);
                    System.out.println("[DEBUG] New OIDC user saved - userId: " + user.getUserId() + 
                                     ", username: " + user.getUsername() + 
                                     ", fullName: " + user.getFullName());
                    
                    // Reload from DB
                    java.util.List<User> reloadedUsers = userRepository.findAllByEmailWithRole(email);
                    if (!reloadedUsers.isEmpty()) {
                        user = reloadedUsers.get(0);
                    }
                } else {
                    // User exists
                    user = users.get(0);
                    if (users.size() > 1) {
                        System.out.println("[WARN] Found " + users.size() + " users with email: " + email + ". Using user_id: " + user.getUserId());
                    }
                    System.out.println("[DEBUG] OIDC user exists - userId: " + user.getUserId() + 
                                     ", username: " + user.getUsername() + 
                                     ", fullName: " + user.getFullName());
                }
                
                // Force initialize role if lazy
                if (user.getRole() != null) {
                    user.getRole().getName(); // Force fetch
                }
                
                // Ensure username and fullName are not null
                String username = user.getUsername();
                if (username == null || username.trim().isEmpty()) {
                    username = email;
                }
                
                String fullName = user.getFullName();
                if (fullName == null || fullName.trim().isEmpty()) {
                    fullName = username.contains("@") ? username.split("@")[0] : username;
                }
                
                // Save to session
                session.setAttribute("currentUser", user);
                session.setAttribute("username", username);
                session.setAttribute("fullName", fullName);
                session.setAttribute("email", email);
                if (user.getRole() != null) {
                    session.setAttribute("role", user.getRole().getName());
                }
                session.setMaxInactiveInterval(30 * 60);
                
                System.out.println("[DEBUG] Saved OIDC user to session - username: " + username + 
                                 ", fullName: " + fullName);
            }
        } else if (authentication.getPrincipal() instanceof OAuth2User) {
            // Fallback for regular OAuth2User
            System.out.println("[DEBUG] Regular OAuth2User found (fallback)");
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String email = oauth2User.getAttribute("email");
            System.out.println("[DEBUG] OAuth2User email: " + email);
            
            if (email != null) {
                // Handle duplicate emails - get the most recent user
                java.util.List<User> users = userRepository.findAllByEmailWithRole(email);
                if (!users.isEmpty()) {
                    User user = users.get(0);
                    if (users.size() > 1) {
                        System.out.println("[WARN] Found " + users.size() + " users with email: " + email + ". Using user_id: " + user.getUserId());
                    }
                    // Force initialize role if lazy
                    if (user.getRole() != null) {
                        user.getRole().getName(); // Force fetch
                    }
                    
                    String username = user.getUsername() != null ? user.getUsername() : email;
                    String fullName = user.getFullName() != null ? user.getFullName() : username.contains("@") ? username.split("@")[0] : username;
                    
                    session.setAttribute("currentUser", user);
                    session.setAttribute("username", username);
                    session.setAttribute("fullName", fullName);
                    session.setAttribute("email", user.getEmail());
                    if (user.getRole() != null) {
                        session.setAttribute("role", user.getRole().getName());
                    }
                    session.setMaxInactiveInterval(30 * 60);
                    System.out.println("[DEBUG] Saved to session (fallback) - username: " + username);
                } else {
                    System.out.println("[WARN] No user found with email: " + email);
                }
            }
        } else {
            System.out.println("[WARN] Unknown principal type: " + authentication.getPrincipal().getClass().getName());
        }
        
        // Verify session attributes before redirect
        String sessionUsername = (String) session.getAttribute("username");
        System.out.println("[DEBUG] Session username before redirect: " + sessionUsername);
        
        // Redirect based on role
        String role = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .findFirst()
                .orElse("");
        
        System.out.println("[DEBUG] User role: " + role + ", redirecting to: " + (role.contains("ADMIN") ? "/demo/admin" : "/"));
        
        // Check if user is admin
        boolean isAdmin = role.contains("ADMIN");
        
        // Also check from database if not found in authorities
        User currentUser = (User) session.getAttribute("currentUser");
        if (!isAdmin && currentUser != null && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            if (roleName != null && roleName.trim().toUpperCase().equals("ADMIN")) {
                isAdmin = true;
            }
        }
        
        if (isAdmin) {
            response.sendRedirect("/demo/admin");
        } else {
            response.sendRedirect("/");
        }
    }
}

