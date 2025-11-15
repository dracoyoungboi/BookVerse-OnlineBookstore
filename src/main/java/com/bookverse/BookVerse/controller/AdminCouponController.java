package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Coupon;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.CouponRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Controller
@RequestMapping("/admin/coupons")
public class AdminCouponController {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserRepository userRepository;

    // List all coupons
    @GetMapping
    public String listCoupons(Model model,
                              HttpSession session,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Authentication authentication,
                              @RequestParam(required = false) String search,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "6") int size,
                              @RequestParam(required = false) String sortBy,
                              @RequestParam(required = false) String sortDir,
                              jakarta.servlet.http.HttpServletRequest request) {
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

        // Ensure default sort direction is ascending
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "couponId";
        }
        
        // Force ascending if sortDir is not specified
        String queryString = request.getQueryString();
        boolean hasSortDirInUrl = queryString != null && queryString.contains("sortDir=");
        
        if (!hasSortDirInUrl) {
            sortDir = "asc";
            StringBuilder redirectUrl = new StringBuilder("/admin/coupons?");
            redirectUrl.append("page=").append(page);
            redirectUrl.append("&size=").append(size);
            redirectUrl.append("&sortBy=").append(sortBy);
            redirectUrl.append("&sortDir=asc");
            if (search != null && !search.trim().isEmpty()) {
                redirectUrl.append("&search=").append(java.net.URLEncoder.encode(search.trim(), java.nio.charset.StandardCharsets.UTF_8));
            }
            return "redirect:" + redirectUrl.toString();
        } else {
            if (sortDir == null || sortDir.trim().isEmpty()) {
                sortDir = "asc";
            }
        }
        
        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                   Sort.by(sortBy).ascending() : 
                   Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get coupons with pagination
        Page<Coupon> couponPage;
        if (search != null && !search.trim().isEmpty()) {
            couponPage = couponRepository.findByCodeContainingIgnoreCase(search.trim(), pageable);
        } else {
            couponPage = couponRepository.findAll(pageable);
        }

        // Calculate pagination info
        int startPage = Math.max(0, page - 1);
        int endPage = Math.min(couponPage.getTotalPages() - 1, page + 1);
        boolean showFirstPage = page > 2;
        boolean showLastPage = page < couponPage.getTotalPages() - 2 && couponPage.getTotalPages() > 1;
        boolean showFirstEllipsis = page > 3;
        boolean showLastEllipsis = page < couponPage.getTotalPages() - 3;
        
        model.addAttribute("coupons", couponPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", couponPage.getTotalPages());
        model.addAttribute("totalItems", couponPage.getTotalElements());
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
        
        return "admin/coupons-list";
    }

    // Show add coupon form
    @GetMapping("/add")
    public String showAddForm(Model model,
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

        model.addAttribute("coupon", new Coupon());
        return "admin/coupon-add";
    }

    // Process add coupon
    @PostMapping("/add")
    public String addCoupon(@ModelAttribute Coupon coupon,
                           @RequestParam(required = false) String expiryDateStr,
                           RedirectAttributes redirectAttributes,
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

        // Validate code uniqueness
        if (coupon.getCode() != null && !coupon.getCode().trim().isEmpty()) {
            Optional<Coupon> existingCoupon = couponRepository.findByCode(coupon.getCode().trim().toUpperCase());
            if (existingCoupon.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Coupon code already exists!");
                return "redirect:/admin/coupons/add";
            }
            coupon.setCode(coupon.getCode().trim().toUpperCase());
        }

        // Parse expiry date
        if (expiryDateStr != null && !expiryDateStr.trim().isEmpty()) {
            try {
                LocalDateTime expiryDate = LocalDateTime.parse(expiryDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                coupon.setExpiryDate(expiryDate);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Invalid expiry date format!");
                return "redirect:/admin/coupons/add";
            }
        }

        // Set default values
        if (coupon.getUsedCount() == null) {
            coupon.setUsedCount(0);
        }
        if (coupon.getActive() == null) {
            coupon.setActive(true);
        }
        if (coupon.getMinPurchaseAmount() == null) {
            coupon.setMinPurchaseAmount(0.0);
        }
        if (coupon.getCreatedAt() == null) {
            coupon.setCreatedAt(LocalDateTime.now());
        }

        couponRepository.save(coupon);
        redirectAttributes.addFlashAttribute("success", "Coupon added successfully!");
        return "redirect:/admin/coupons";
    }

    // Show edit coupon form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id,
                              Model model,
                              RedirectAttributes redirectAttributes,
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

        Optional<Coupon> couponOpt = couponRepository.findById(id);
        if (couponOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Coupon not found!");
            return "redirect:/admin/coupons";
        }

        Coupon coupon = couponOpt.get();
        model.addAttribute("coupon", coupon);
        
        // Format expiry date for datetime-local input
        if (coupon.getExpiryDate() != null) {
            String expiryDateFormatted = coupon.getExpiryDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            model.addAttribute("expiryDateFormatted", expiryDateFormatted);
        } else {
            model.addAttribute("expiryDateFormatted", "");
        }
        
        return "admin/coupon-edit";
    }

    // Process edit coupon
    @PostMapping("/edit/{id}")
    public String updateCoupon(@PathVariable("id") Long id,
                              @ModelAttribute Coupon coupon,
                              @RequestParam(required = false) String expiryDateStr,
                              RedirectAttributes redirectAttributes,
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

        Optional<Coupon> couponOpt = couponRepository.findById(id);
        if (couponOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Coupon not found!");
            return "redirect:/admin/coupons";
        }

        Coupon existingCoupon = couponOpt.get();

        // Validate code uniqueness (if changed)
        if (coupon.getCode() != null && !coupon.getCode().trim().isEmpty()) {
            String newCode = coupon.getCode().trim().toUpperCase();
            if (!newCode.equals(existingCoupon.getCode())) {
                Optional<Coupon> duplicateCoupon = couponRepository.findByCode(newCode);
                if (duplicateCoupon.isPresent()) {
                    redirectAttributes.addFlashAttribute("error", "Coupon code already exists!");
                    return "redirect:/admin/coupons/edit/" + id;
                }
            }
            existingCoupon.setCode(newCode);
        }

        // Update fields
        existingCoupon.setDiscountType(coupon.getDiscountType());
        existingCoupon.setDiscountValue(coupon.getDiscountValue());
        existingCoupon.setMinPurchaseAmount(coupon.getMinPurchaseAmount() != null ? coupon.getMinPurchaseAmount() : 0.0);
        existingCoupon.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        existingCoupon.setUsageLimit(coupon.getUsageLimit());
        existingCoupon.setActive(coupon.getActive() != null ? coupon.getActive() : true);
        existingCoupon.setDescription(coupon.getDescription());

        // Parse expiry date
        if (expiryDateStr != null && !expiryDateStr.trim().isEmpty()) {
            try {
                LocalDateTime expiryDate = LocalDateTime.parse(expiryDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                existingCoupon.setExpiryDate(expiryDate);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Invalid expiry date format!");
                return "redirect:/admin/coupons/edit/" + id;
            }
        } else {
            existingCoupon.setExpiryDate(null);
        }

        couponRepository.save(existingCoupon);
        redirectAttributes.addFlashAttribute("success", "Coupon updated successfully!");
        return "redirect:/admin/coupons";
    }

    // View coupon details
    @GetMapping("/view/{id}")
    public String viewCoupon(@PathVariable("id") Long id,
                            Model model,
                            RedirectAttributes redirectAttributes,
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

        Optional<Coupon> couponOpt = couponRepository.findById(id);
        if (couponOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Coupon not found!");
            return "redirect:/admin/coupons";
        }

        model.addAttribute("coupon", couponOpt.get());
        return "admin/coupon-view";
    }

    // Delete coupon
    @PostMapping("/delete/{id}")
    public String deleteCoupon(@PathVariable("id") Long id,
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

        Optional<Coupon> couponOpt = couponRepository.findById(id);
        if (couponOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Coupon not found!");
        } else {
            couponRepository.delete(couponOpt.get());
            redirectAttributes.addFlashAttribute("success", "Coupon deleted successfully!");
        }

        return "redirect:/admin/coupons";
    }
}

