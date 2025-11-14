package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Role;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.RoleRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    // List all users (both active and inactive)
    @GetMapping
    public String listUsers(Model model,
                           HttpSession session,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Authentication authentication,
                           @RequestParam(required = false) String search,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "6") int size,
                           @RequestParam(defaultValue = "userId") String sortBy,
                           @RequestParam(defaultValue = "asc") String sortDir) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info for header
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
                session.setAttribute("username", currentUser.getUsername());
                session.setAttribute("fullName", currentUser.getFullName());
            }
        }

        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }

        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                   Sort.by(sortBy).ascending() : 
                   Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get users with pagination
        Page<User> userPage;
        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.searchUsersWithRole(search.trim(), pageable);
        } else {
            userPage = userRepository.findAllUsersWithRolePaged(pageable);
        }

        // Calculate pagination info
        int startPage = Math.max(0, page - 1);
        int endPage = Math.min(userPage.getTotalPages() - 1, page + 1);
        boolean showFirstPage = page > 2;
        boolean showLastPage = page < userPage.getTotalPages() - 2 && userPage.getTotalPages() > 1;
        boolean showFirstEllipsis = page > 3;
        boolean showLastEllipsis = page < userPage.getTotalPages() - 3;
        
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showFirstPage", showFirstPage);
        model.addAttribute("showLastPage", showLastPage);
        model.addAttribute("showFirstEllipsis", showFirstEllipsis);
        model.addAttribute("showLastEllipsis", showLastEllipsis);
        return "admin/users-list";
    }

    // Show add user form
    @GetMapping("/add")
    public String showAddUserForm(Model model,
                                  HttpSession session,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Authentication authentication) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info for header
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
                session.setAttribute("username", currentUser.getUsername());
                session.setAttribute("fullName", currentUser.getFullName());
            }
        }

        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }

        model.addAttribute("user", new User());
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("roles", roles);
        return "admin/user-add";
    }

    // Process add user
    @PostMapping("/add")
    public String addUser(@ModelAttribute("user") User user,
                         @RequestParam("roleId") Long roleId,
                         RedirectAttributes redirectAttributes,
                         Authentication authentication) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        try {
            // Validate username
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Username cannot be empty!");
                return "redirect:/admin/users/add";
            }

            // Check if username already exists (including deleted users)
            Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
            if (existingUser.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/admin/users/add";
            }

            // Validate email
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email cannot be empty!");
                return "redirect:/admin/users/add";
            }

            if (!user.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                redirectAttributes.addFlashAttribute("error", "Invalid email format!");
                return "redirect:/admin/users/add";
            }

            // Check if email already exists (only active users)
            List<User> usersWithEmail = userRepository.findAllByEmailWithRole(user.getEmail());
            if (!usersWithEmail.isEmpty() && usersWithEmail.stream().anyMatch(u -> !u.getDeleted())) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/admin/users/add";
            }

            // Validate password
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Password cannot be empty!");
                return "redirect:/admin/users/add";
            }

            if (user.getPassword().length() < 4) {
                redirectAttributes.addFlashAttribute("error", "Password must be at least 4 characters!");
                return "redirect:/admin/users/add";
            }

            // Set role
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isPresent()) {
                user.setRole(roleOpt.get());
            } else {
                // Default to USER role if role not found
                Role defaultRole = roleRepository.findByName("USER").orElseGet(() -> {
                    Role r = new Role();
                    r.setName("USER");
                    return roleRepository.save(r);
                });
                user.setRole(defaultRole);
            }

            // Set created date
            user.setCreatedAt(LocalDateTime.now());
            user.setDeleted(false);

            // Encode password and save
            userService.saveUser(user);

            redirectAttributes.addFlashAttribute("success", "User added successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding user: " + e.getMessage());
            return "redirect:/admin/users/add";
        }
    }

    // Show edit user form
    @GetMapping("/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Long id,
                                   Model model,
                                   HttpSession session,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info for header
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
                session.setAttribute("username", currentUser.getUsername());
                session.setAttribute("fullName", currentUser.getFullName());
            }
        }

        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }

        // Allow editing both active and inactive users
        Optional<User> userOpt = userRepository.findByIdWithRole(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/admin/users";
        }

        User user = userOpt.get();
        // Don't send password to view
        user.setPassword("");

        model.addAttribute("user", user);
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("roles", roles);
        return "admin/user-edit";
    }

    // Process edit user
    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable("id") Long id,
                            @ModelAttribute("user") User user,
                            @RequestParam("roleId") Long roleId,
                            @RequestParam(value = "password", required = false) String newPassword,
                            RedirectAttributes redirectAttributes,
                            Authentication authentication) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        try {
            // Allow updating both active and inactive users
            Optional<User> userOpt = userRepository.findByIdWithRole(id);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                return "redirect:/admin/users";
            }

            User existingUser = userOpt.get();

            // Validate username
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Username cannot be empty!");
                return "redirect:/admin/users/edit/" + id;
            }

            // Check if username already exists (excluding current user)
            Optional<User> userWithUsername = userRepository.findByUsername(user.getUsername());
            if (userWithUsername.isPresent() && !userWithUsername.get().getUserId().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/admin/users/edit/" + id;
            }

            // Validate email
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email cannot be empty!");
                return "redirect:/admin/users/edit/" + id;
            }

            if (!user.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                redirectAttributes.addFlashAttribute("error", "Invalid email format!");
                return "redirect:/admin/users/edit/" + id;
            }

            // Check if email already exists (excluding current user)
            List<User> usersWithEmail = userRepository.findAllByEmailWithRole(user.getEmail());
            if (!usersWithEmail.isEmpty() && usersWithEmail.stream()
                    .anyMatch(u -> !u.getUserId().equals(id) && !u.getDeleted())) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/admin/users/edit/" + id;
            }

            // Update user fields
            existingUser.setUsername(user.getUsername());
            existingUser.setEmail(user.getEmail());
            existingUser.setFullName(user.getFullName());
            existingUser.setPhone(user.getPhone());
            existingUser.setAddress(user.getAddress());

            // Update password if provided and not empty
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (newPassword.length() < 4) {
                    redirectAttributes.addFlashAttribute("error", "Password must be at least 4 characters!");
                    return "redirect:/admin/users/edit/" + id;
                }
                existingUser.setPassword(userService.encodePassword(newPassword.trim()));
            }
            // If password is not provided, keep the existing password (don't update it)

            // Update role
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isPresent()) {
                existingUser.setRole(roleOpt.get());
            }

            userRepository.save(existingUser);

            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
            return "redirect:/admin/users/edit/" + id;
        }
    }

    // View user details
    @GetMapping("/view/{id}")
    public String viewUser(@PathVariable("id") Long id,
                          Model model,
                          HttpSession session,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info for header
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
                session.setAttribute("username", currentUser.getUsername());
                session.setAttribute("fullName", currentUser.getFullName());
            }
        }

        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }

        // Find user by ID (including inactive users for view page)
        Optional<User> userOpt = userRepository.findByIdWithRole(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/admin/users";
        }

        User user = userOpt.get();
        // Role is already loaded via JOIN FETCH in query
        
        // Determine status (active = not deleted, inactive = deleted)
        boolean isActive = user.getDeleted() == null || !user.getDeleted();
        model.addAttribute("user", user);
        model.addAttribute("isActive", isActive);
        model.addAttribute("status", isActive ? "Active" : "Inactive");

        // Calculate account active duration
        String accountActiveDuration = "Not available";
        if (user.getCreatedAt() != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime createdAt = user.getCreatedAt();

            java.time.Period period = java.time.Period.between(createdAt.toLocalDate(), now.toLocalDate());
            int years = period.getYears();
            int months = period.getMonths();
            int days = period.getDays();

            java.time.Duration duration = java.time.Duration.between(createdAt, now);
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;

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

            if (durationStr.length() == 0) {
                accountActiveDuration = "Just created";
            } else {
                accountActiveDuration = durationStr.toString();
            }
        }

        model.addAttribute("accountActiveDuration", accountActiveDuration);

        return "admin/user-view";
    }

    // Toggle user status (active/inactive) - set deleted = true for inactive, false for active
    @PostMapping("/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable("id") Long id,
                                   @RequestParam(value = "redirect", defaultValue = "list") String redirect,
                                   RedirectAttributes redirectAttributes,
                                   Authentication authentication) {
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        try {
            // Find user by ID (including inactive users)
            Optional<User> userOpt = userRepository.findByIdWithRole(id);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                if ("view".equals(redirect)) {
                    return "redirect:/admin/users/view/" + id;
                }
                return "redirect:/admin/users";
            }

            User user = userOpt.get();
            // Role is already loaded via JOIN FETCH in query
            
            // Toggle status: if deleted (inactive), set to active; if active, set to inactive
            boolean currentStatus = user.getDeleted() != null ? user.getDeleted() : false;
            user.setDeleted(!currentStatus);
            userRepository.save(user);

            String statusMessage = !currentStatus ? "User set to inactive successfully!" : "User set to active successfully!";
            redirectAttributes.addFlashAttribute("success", statusMessage);
            
            if ("view".equals(redirect)) {
                return "redirect:/admin/users/view/" + id;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user status: " + e.getMessage());
            if ("view".equals(redirect)) {
                return "redirect:/admin/users/view/" + id;
            }
        }

        return "redirect:/admin/users";
    }
}

