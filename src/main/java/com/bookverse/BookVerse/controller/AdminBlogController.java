package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Blog;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.BlogRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin/blogs")
public class AdminBlogController {

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.bookverse.BookVerse.repository.BlogDetailRepository blogDetailRepository;

    @GetMapping("/add")
    public String showAddBlogForm(Model model,
                                  HttpSession session,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().contains("ADMIN"));
        if (!isAdmin) {
            return "redirect:/demo/user";
        }

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

        model.addAttribute("blog", new Blog());
        return "admin/blog-add";
    }

    @PostMapping("/add")
    public String addBlog(@ModelAttribute("blog") Blog blog,
                          @RequestParam(value = "content", required = false) String content,
                          RedirectAttributes redirectAttributes,
                          Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().contains("ADMIN"));
        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        try {
            if (blog.getTitle() == null || blog.getTitle().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Title cannot be empty!");
                return "redirect:/admin/blogs/add";
            }
            if (blog.getAuthor() == null || blog.getAuthor().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Author cannot be empty!");
                return "redirect:/admin/blogs/add";
            }
            // Thumbnail is optional. CSS/content section may be left blank by admin later.
            blog.setCreatedAt(LocalDateTime.now());
            blog = blogRepository.save(blog);

            // Optionally save a content block; allow blank per request
            if (content != null && !content.trim().isEmpty()) {
                com.bookverse.BookVerse.entity.BlogDetail detail = new com.bookverse.BookVerse.entity.BlogDetail();
                detail.setBlog(blog);
                detail.setContent(content.trim());
                detail.setCreatedAt(LocalDateTime.now());
                blogDetailRepository.save(detail);
            }
            redirectAttributes.addFlashAttribute("success", "Blog added successfully!");
            return "redirect:/admin/blogs/add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding blog: " + e.getMessage());
            return "redirect:/admin/blogs/add";
        }
    }

    // List blogs with pagination and search (like Books List)
    @GetMapping
    public String listBlogs(Model model,
                            HttpSession session,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Authentication authentication,
                            @RequestParam(required = false) String search,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().contains("ADMIN"));
        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            userOpt.ifPresent(user -> {
                session.setAttribute("currentUser", user);
                session.setAttribute("username", user.getUsername());
                session.setAttribute("fullName", user.getFullName());
            });
            currentUser = (User) session.getAttribute("currentUser");
        }
        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("blogId").descending());
        Page<Blog> blogPage = (search != null && !search.trim().isEmpty())
                ? blogRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(search.trim(), search.trim(), pageable)
                : blogRepository.findAll(pageable);

        int startPage = Math.max(0, page - 1);
        int endPage = Math.min(blogPage.getTotalPages() - 1, page + 1);
        boolean showFirstPage = page > 2;
        boolean showLastPage = page < blogPage.getTotalPages() - 2 && blogPage.getTotalPages() > 1;
        boolean showFirstEllipsis = page > 3;
        boolean showLastEllipsis = page < blogPage.getTotalPages() - 3;

        model.addAttribute("blogs", blogPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", blogPage.getTotalPages());
        model.addAttribute("totalItems", blogPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showFirstPage", showFirstPage);
        model.addAttribute("showLastPage", showLastPage);
        model.addAttribute("showFirstEllipsis", showFirstEllipsis);
        model.addAttribute("showLastEllipsis", showLastEllipsis);
        return "admin/blogs-list";
    }

    // View blog details
    @GetMapping("/view/{id}")
    public String viewBlog(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
                           Model model,
                           HttpSession session,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().contains("ADMIN"));
        if (!isAdmin) {
            return "redirect:/demo/user";
        }

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

        Optional<Blog> blogOpt = blogRepository.findById(id);
        if (blogOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Blog not found!");
            return "redirect:/admin/blogs";
        }
        model.addAttribute("blog", blogOpt.get());
        return "admin/blog-view";
    }

    // Show edit form
    @GetMapping("/edit/{id}")
    public String showEditBlog(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
                               Model model,
                               HttpSession session,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().contains("ADMIN"));
        if (!isAdmin) {
            return "redirect:/demo/user";
        }

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

        Optional<Blog> blogOpt = blogRepository.findById(id);
        if (blogOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Blog not found!");
            return "redirect:/admin/blogs";
        }
        model.addAttribute("blog", blogOpt.get());
        return "admin/blog-edit";
    }

    // Process edit
    @PostMapping("/edit/{id}")
    public String updateBlog(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
                             @ModelAttribute("blog") Blog blog,
                             @RequestParam(value = "content", required = false) String content,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().contains("ADMIN"));
        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        try {
            Optional<Blog> blogOpt = blogRepository.findById(id);
            if (blogOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Blog not found!");
                return "redirect:/admin/blogs";
            }
            Blog existing = blogOpt.get();
            if (blog.getTitle() == null || blog.getTitle().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Title cannot be empty!");
                return "redirect:/admin/blogs/edit/" + id;
            }
            if (blog.getAuthor() == null || blog.getAuthor().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Author cannot be empty!");
                return "redirect:/admin/blogs/edit/" + id;
            }
            existing.setTitle(blog.getTitle());
            existing.setAuthor(blog.getAuthor());
            existing.setThumbnail(blog.getThumbnail());
            blogRepository.save(existing);

            if (content != null) {
                // Update or create a single detail as convenience (first detail)
                com.bookverse.BookVerse.entity.BlogDetail detail;
                if (existing.getBlogDetails() != null && !existing.getBlogDetails().isEmpty()) {
                    detail = existing.getBlogDetails().get(0);
                    detail.setContent(content.trim());
                    if (detail.getCreatedAt() == null) detail.setCreatedAt(LocalDateTime.now());
                } else if (!content.trim().isEmpty()) {
                    detail = new com.bookverse.BookVerse.entity.BlogDetail();
                    detail.setBlog(existing);
                    detail.setContent(content.trim());
                    detail.setCreatedAt(LocalDateTime.now());
                } else {
                    detail = null;
                }
                if (detail != null) {
                    blogDetailRepository.save(detail);
                }
            }

            redirectAttributes.addFlashAttribute("success", "Blog updated successfully!");
            return "redirect:/admin/blogs";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating blog: " + e.getMessage());
            return "redirect:/admin/blogs/edit/" + id;
        }
    }

    // Delete blog (hard delete)
    @PostMapping("/delete/{id}")
    public String deleteBlog(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().contains("ADMIN"));
        if (!isAdmin) {
            return "redirect:/demo/user";
        }
        try {
            if (!blogRepository.existsById(id)) {
                redirectAttributes.addFlashAttribute("error", "Blog not found!");
                return "redirect:/admin/blogs";
            }
            blogRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Blog deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting blog: " + e.getMessage());
        }
        return "redirect:/admin/blogs";
    }
}
