package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Role;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Try to find by username first with role loaded
        Optional<User> userOpt = userRepository.findByUsernameWithRole(usernameOrEmail);
        
        // If not found, try to find by email with role loaded
        // Handle duplicate emails by getting the most recent user
        if (userOpt.isEmpty()) {
            java.util.List<User> users = userRepository.findAllByEmailWithRole(usernameOrEmail);
            if (!users.isEmpty()) {
                userOpt = Optional.of(users.get(0));
                if (users.size() > 1) {
                    System.out.println("[WARN] Found " + users.size() + " users with email: " + usernameOrEmail + ". Using user_id: " + users.get(0).getUserId());
                }
            }
        }
        
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + usernameOrEmail);
        }

        User user = userOpt.get();
        
        // Force initialize role if lazy loading
        if (user.getRole() != null) {
            user.getRole().getName(); // Force fetch
        }
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(getAuthorities(user))
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null && user.getRole().getName() != null) {
            // Spring Security expects role names to start with "ROLE_"
            // But we store "ADMIN" or "USER" in DB, so we add "ROLE_" prefix
            String roleName = user.getRole().getName().trim().toUpperCase();
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            authorities.add(new SimpleGrantedAuthority(roleName));
            System.out.println("[DEBUG] Created authority: " + roleName + " for user: " + user.getUsername());
        } else {
            System.out.println("[DEBUG] User has no role - username: " + user.getUsername());
        }
        return authorities;
    }
}

