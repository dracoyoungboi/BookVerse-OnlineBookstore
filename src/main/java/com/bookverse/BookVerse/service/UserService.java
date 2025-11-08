package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Role;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.RoleRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // 🔍 Tìm user theo username
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean checkLogin(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String hashed = user.getPassword();
            if (hashed.startsWith("$2a$")) {
                return passwordEncoder.matches(password, hashed);
            } else {
                return password.equals(hashed); // fallback nếu chưa mã hóa
            }
        }
        return false;
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public void saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }
    public void registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }
    
    // Update user role by username and role name
    public boolean updateUserRole(String username, String roleName) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Optional<Role> roleOpt = roleRepository.findByName(roleName.toUpperCase());
            if (roleOpt.isPresent()) {
                user.setRole(roleOpt.get());
                userRepository.save(user);
                System.out.println("[DEBUG] Updated user " + username + " role to: " + roleName);
                return true;
            } else {
                System.out.println("[ERROR] Role not found: " + roleName);
                return false;
            }
        } else {
            System.out.println("[ERROR] User not found: " + username);
            return false;
        }
    }
}


