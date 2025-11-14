package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.Category;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.BookRepository;
import com.bookverse.BookVerse.repository.CategoryRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.service.FileUploadService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin/books")
public class AdminBookController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileUploadService fileUploadService;

    // List all books
    @GetMapping
    public String listBooks(Model model,
                           HttpSession session,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Authentication authentication,
                           @RequestParam(required = false) String search,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "6") int size,
                           @RequestParam(defaultValue = "bookId") String sortBy,
                           @RequestParam(defaultValue = "desc") String sortDir) {
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
        
        // Get books with pagination
        Page<Book> bookPage;
        if (search != null && !search.trim().isEmpty()) {
            bookPage = bookRepository.searchBooksWithCategory(search.trim(), pageable);
        } else {
            bookPage = bookRepository.findAllWithCategoryPaged(pageable);
        }

        // Calculate pagination info
        int startPage = Math.max(0, page - 1);
        int endPage = Math.min(bookPage.getTotalPages() - 1, page + 1);
        boolean showFirstPage = page > 2;
        boolean showLastPage = page < bookPage.getTotalPages() - 2 && bookPage.getTotalPages() > 1;
        boolean showFirstEllipsis = page > 3;
        boolean showLastEllipsis = page < bookPage.getTotalPages() - 3;
        
        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("totalItems", bookPage.getTotalElements());
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
        return "admin/books-list";
    }

    // Show add book form
    @GetMapping("/add")
    public String showAddBookForm(Model model,
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

        model.addAttribute("book", new Book());
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        return "admin/book-add";
    }

    // Process add book
    @PostMapping("/add")
    public String addBook(@ModelAttribute("book") Book book,
                         @RequestParam("categoryId") Long categoryId,
                         @RequestParam(value = "discountStart", required = false) String discountStartStr,
                         @RequestParam(value = "discountEnd", required = false) String discountEndStr,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         @RequestParam(value = "imageUrl", required = false) String imageUrl,
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
            // Validate title
            if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Title cannot be empty!");
                return "redirect:/admin/books/add";
            }

            // Validate author
            if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Author cannot be empty!");
                return "redirect:/admin/books/add";
            }

            // Validate price
            if (book.getPrice() == null || book.getPrice() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Price must be greater than 0!");
                return "redirect:/admin/books/add";
            }

            // Validate stock
            if (book.getStock() < 0) {
                redirectAttributes.addFlashAttribute("error", "Stock cannot be negative!");
                return "redirect:/admin/books/add";
            }

            // Handle image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                // Validate image file
                if (!fileUploadService.isImageFile(imageFile)) {
                    redirectAttributes.addFlashAttribute("error", "Please upload a valid image file!");
                    return "redirect:/admin/books/add";
                }
                
                // Upload image and get URL
                String uploadedImageUrl = fileUploadService.uploadFile(imageFile);
                if (uploadedImageUrl != null) {
                    book.setImageUrl(uploadedImageUrl);
                } else {
                    redirectAttributes.addFlashAttribute("error", "Failed to upload image!");
                    return "redirect:/admin/books/add";
                }
            } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // Use provided URL if no file is uploaded
                book.setImageUrl(imageUrl.trim());
            }

            // Set category
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            if (categoryOpt.isPresent()) {
                book.setCategory(categoryOpt.get());
            } else {
                redirectAttributes.addFlashAttribute("error", "Category not found!");
                return "redirect:/admin/books/add";
            }

            // Set default values
            if (book.getDiscountPercent() == null) {
                book.setDiscountPercent(0.0);
            }
            if (book.getStock() == 0) {
                book.setStock(0);
            }
            book.setCreatedAt(LocalDateTime.now());
            
            // Parse datetime strings to LocalDateTime
            if (discountStartStr != null && !discountStartStr.trim().isEmpty()) {
                try {
                    book.setDiscountStart(LocalDateTime.parse(discountStartStr));
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Invalid discount start date format!");
                    return "redirect:/admin/books/add";
                }
            }
            if (discountEndStr != null && !discountEndStr.trim().isEmpty()) {
                try {
                    book.setDiscountEnd(LocalDateTime.parse(discountEndStr));
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Invalid discount end date format!");
                    return "redirect:/admin/books/add";
                }
            }

            // Save book
            bookRepository.save(book);

            redirectAttributes.addFlashAttribute("success", "Book added successfully!");
            return "redirect:/admin/books";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding book: " + e.getMessage());
            return "redirect:/admin/books/add";
        }
    }

    // Show edit book form
    @GetMapping("/edit/{id}")
    public String showEditBookForm(@PathVariable("id") Long id,
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

        Optional<Book> bookOpt = bookRepository.findByIdWithCategoryForAdmin(id);
        if (bookOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Book not found!");
            return "redirect:/admin/books";
        }

        Book book = bookOpt.get();
        model.addAttribute("book", book);
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        return "admin/book-edit";
    }

    // Process edit book
    @PostMapping("/edit/{id}")
    public String updateBook(@PathVariable("id") Long id,
                            @ModelAttribute("book") Book book,
                            @RequestParam("categoryId") Long categoryId,
                            @RequestParam(value = "discountStart", required = false) String discountStartStr,
                            @RequestParam(value = "discountEnd", required = false) String discountEndStr,
                            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                            @RequestParam(value = "imageUrl", required = false) String imageUrl,
                            @RequestParam(value = "deleteOldImage", required = false) String deleteOldImage,
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
            Optional<Book> bookOpt = bookRepository.findByIdWithCategoryForAdmin(id);
            if (bookOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Book not found!");
                return "redirect:/admin/books";
            }

            Book existingBook = bookOpt.get();

            // Validate title
            if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Title cannot be empty!");
                return "redirect:/admin/books/edit/" + id;
            }

            // Validate author
            if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Author cannot be empty!");
                return "redirect:/admin/books/edit/" + id;
            }

            // Validate price
            if (book.getPrice() == null || book.getPrice() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Price must be greater than 0!");
                return "redirect:/admin/books/edit/" + id;
            }

            // Validate stock
            if (book.getStock() < 0) {
                redirectAttributes.addFlashAttribute("error", "Stock cannot be negative!");
                return "redirect:/admin/books/edit/" + id;
            }

            // Handle image upload
            String oldImageUrl = existingBook.getImageUrl();
            if (imageFile != null && !imageFile.isEmpty()) {
                // Validate image file
                if (!fileUploadService.isImageFile(imageFile)) {
                    redirectAttributes.addFlashAttribute("error", "Please upload a valid image file!");
                    return "redirect:/admin/books/edit/" + id;
                }
                
                // Upload new image and get URL
                String uploadedImageUrl = fileUploadService.uploadFile(imageFile);
                if (uploadedImageUrl != null) {
                    // Delete old image if it exists and is in our upload directory
                    if (oldImageUrl != null && oldImageUrl.startsWith("/user/img/product/")) {
                        fileUploadService.deleteFile(oldImageUrl);
                    }
                    existingBook.setImageUrl(uploadedImageUrl);
                } else {
                    redirectAttributes.addFlashAttribute("error", "Failed to upload image!");
                    return "redirect:/admin/books/edit/" + id;
                }
            } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // Use provided URL if no file is uploaded
                // Delete old image if it exists and is in our upload directory, and URL has changed
                if (oldImageUrl != null && oldImageUrl.startsWith("/user/img/product/") && !oldImageUrl.equals(imageUrl.trim())) {
                    fileUploadService.deleteFile(oldImageUrl);
                }
                existingBook.setImageUrl(imageUrl.trim());
            } else if ("true".equals(deleteOldImage)) {
                // Delete old image if user wants to remove it
                if (oldImageUrl != null && oldImageUrl.startsWith("/user/img/product/")) {
                    fileUploadService.deleteFile(oldImageUrl);
                }
                existingBook.setImageUrl(null);
            }
            // If no image file, no image URL, and no delete flag, keep the existing image

            // Update book fields
            existingBook.setTitle(book.getTitle());
            existingBook.setAuthor(book.getAuthor());
            existingBook.setPrice(book.getPrice());
            existingBook.setStock(book.getStock());
            existingBook.setDescription(book.getDescription());
            existingBook.setDiscountPercent(book.getDiscountPercent() != null ? book.getDiscountPercent() : 0.0);
            
            // Parse datetime strings to LocalDateTime
            if (discountStartStr != null && !discountStartStr.trim().isEmpty()) {
                try {
                    existingBook.setDiscountStart(LocalDateTime.parse(discountStartStr));
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Invalid discount start date format!");
                    return "redirect:/admin/books/edit/" + id;
                }
            } else {
                existingBook.setDiscountStart(null);
            }
            if (discountEndStr != null && !discountEndStr.trim().isEmpty()) {
                try {
                    existingBook.setDiscountEnd(LocalDateTime.parse(discountEndStr));
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Invalid discount end date format!");
                    return "redirect:/admin/books/edit/" + id;
                }
            } else {
                existingBook.setDiscountEnd(null);
            }

            // Set category
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            if (categoryOpt.isPresent()) {
                existingBook.setCategory(categoryOpt.get());
            } else {
                redirectAttributes.addFlashAttribute("error", "Category not found!");
                return "redirect:/admin/books/edit/" + id;
            }

            // Save updated book
            bookRepository.save(existingBook);

            redirectAttributes.addFlashAttribute("success", "Book updated successfully!");
            return "redirect:/admin/books";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating book: " + e.getMessage());
            return "redirect:/admin/books/edit/" + id;
        }
    }

    // View book details
    @GetMapping("/view/{id}")
    public String viewBook(@PathVariable("id") Long id,
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

        Optional<Book> bookOpt = bookRepository.findByIdWithCategoryForAdmin(id);
        if (bookOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Book not found!");
            return "redirect:/admin/books";
        }

        Book book = bookOpt.get();
        
        // Determine status for display
        boolean isActive = book.getDeleted() == null || !book.getDeleted();
        String status = isActive ? "Active" : "Inactive";
        
        model.addAttribute("book", book);
        model.addAttribute("isActive", isActive);
        model.addAttribute("status", status);

        return "admin/book-view";
    }

    // Toggle book status (active/inactive) - set deleted = true for inactive, false for active
    @PostMapping("/toggle-status/{id}")
    public String toggleBookStatus(@PathVariable("id") Long id,
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
            // Find book by ID (including inactive books)
            Optional<Book> bookOpt = bookRepository.findByIdWithCategoryForAdmin(id);
            if (bookOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Book not found!");
                if ("view".equals(redirect)) {
                    return "redirect:/admin/books/view/" + id;
                }
                return "redirect:/admin/books";
            }

            Book book = bookOpt.get();
            // Category is already loaded via JOIN in query

            // Toggle status: if deleted (inactive), set to active; if active, set to inactive
            boolean currentStatus = book.getDeleted() != null ? book.getDeleted() : false;
            book.setDeleted(!currentStatus);
            bookRepository.save(book);

            String statusMessage = !currentStatus ? "Book set to inactive successfully!" : "Book set to active successfully!";
            redirectAttributes.addFlashAttribute("success", statusMessage);

            if ("view".equals(redirect)) {
                return "redirect:/admin/books/view/" + id;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating book status: " + e.getMessage());
            if ("view".equals(redirect)) {
                return "redirect:/admin/books/view/" + id;
            }
        }

        return "redirect:/admin/books";
    }
}

