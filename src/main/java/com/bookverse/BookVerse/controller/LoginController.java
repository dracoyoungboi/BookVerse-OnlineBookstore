package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Role;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.RoleRepository;
import com.bookverse.BookVerse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping("/login")
    public String loginForm(Model model, HttpSession session) {
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
    public String showRegisterForm(Model model) {
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

            if (user.getPassword() == null || user.getPassword().length() < 4) {
                model.addAttribute("error", "Password must be at least 4 characters!");
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
}
