package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Role;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.RoleRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.security.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    public CustomOAuth2UserService() {
        super();
        System.out.println("[DEBUG] CustomOAuth2UserService created");
    }
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println("[DEBUG] Google OAuth2 attributes: " + oAuth2User.getAttributes());
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            System.out.println("[DEBUG] No email received from Google!");
            // Return with null user - will have no authorities
            return new CustomOAuth2User(oAuth2User, null);
        }

        // Use findAllByEmailWithRole to handle duplicate emails
        // Get the most recent user (first in DESC order by userId)
        java.util.List<User> users = userRepository.findAllByEmailWithRole(email);
        User user;
        
        if (users.isEmpty()) {
            System.out.println("[DEBUG] Google email not found, creating new user: " + email);
            user = new User();
            user.setEmail(email);
            // Use email as username if no other identifier
            user.setUsername(email);
            
            // Get name from Google, fallback to email if null
            String googleName = oAuth2User.getAttribute("name");
            if (googleName == null || googleName.trim().isEmpty()) {
                // Try to get name from given_name and family_name
                String givenName = oAuth2User.getAttribute("given_name");
                String familyName = oAuth2User.getAttribute("family_name");
                if (givenName != null || familyName != null) {
                    googleName = ((givenName != null ? givenName : "") + " " + (familyName != null ? familyName : "")).trim();
                }
                // If still null, use email as fallback
                if (googleName == null || googleName.trim().isEmpty()) {
                    googleName = email.split("@")[0]; // Use part before @ as name
                }
            }
            user.setFullName(googleName);
            
            user.setCreatedAt(java.time.LocalDateTime.now());
            user.setPhone(oAuth2User.getAttribute("phone"));
            user.setAddress(oAuth2User.getAttribute("locale"));
            
            // Get USER role from database or create if not exists
            Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
                Role r = new Role();
                r.setName("USER");
                return roleRepository.save(r);
            });
            user.setRole(userRole);
            
            // Save user
            user = userRepository.save(user);
            System.out.println("[DEBUG] New user saved to DB - userId: " + user.getUserId() + 
                             ", email: " + user.getEmail() + 
                             ", username: " + user.getUsername() + 
                             ", fullName: " + user.getFullName() + 
                             ", Role: " + user.getRole().getName());
            
            // Reload user from DB with role to ensure all fields are properly loaded
            java.util.List<User> reloadedUsers = userRepository.findAllByEmailWithRole(email);
            if (!reloadedUsers.isEmpty()) {
                user = reloadedUsers.get(0);
                System.out.println("[DEBUG] Reloaded user from DB - userId: " + user.getUserId() + 
                                 ", username: " + user.getUsername() + 
                                 ", fullName: " + user.getFullName());
            }
        } else {
            // Get the first user (most recent by userId DESC)
            user = users.get(0);
            if (users.size() > 1) {
                System.out.println("[WARN] Found " + users.size() + " users with email: " + email + ". Using user_id: " + user.getUserId());
            }
            // Force initialize role if lazy
            if (user.getRole() != null) {
                user.getRole().getName(); // Force fetch
            }
            // Keep existing role - don't override ADMIN role with USER
            if (user.getRole() == null) {
                Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
                    Role r = new Role();
                    r.setName("USER");
                    return roleRepository.save(r);
                });
                user.setRole(userRole);
                userRepository.save(user);
            }
            System.out.println("[DEBUG] Google email exists in DB - userId: " + user.getUserId() + 
                             ", email: " + email + 
                             ", username: " + user.getUsername() + 
                             ", fullName: " + user.getFullName() + 
                             ", Role: " + (user.getRole() != null ? user.getRole().getName() : "null"));
        }
        
        // Ensure user has all required fields
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            user.setUsername(user.getEmail());
            userRepository.save(user);
            System.out.println("[WARN] Username was null, set to email: " + user.getEmail());
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            String name = user.getEmail().split("@")[0];
            user.setFullName(name);
            userRepository.save(user);
            System.out.println("[WARN] FullName was null, set to: " + name);
        }
        
        // Return CustomOAuth2User with proper authorities based on user role
        System.out.println("[DEBUG] Returning CustomOAuth2User with user - username: " + user.getUsername() + 
                         ", fullName: " + user.getFullName());
        return new CustomOAuth2User(oAuth2User, user);
    }
}
