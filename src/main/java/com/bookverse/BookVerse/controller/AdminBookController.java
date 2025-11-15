package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.Category;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.BookRepository;
import com.bookverse.BookVerse.repository.CategoryRepository;
import com.bookverse.BookVerse.repository.OrderItemRepository;
import com.bookverse.BookVerse.repository.OrderRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.service.FileUploadService;
import com.bookverse.BookVerse.entity.OrderItem;
import com.bookverse.BookVerse.entity.Order;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import jakarta.transaction.Transactional;

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

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    // List all books
    @GetMapping
    public String listBooks(Model model,
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
            sortBy = "bookId";
        }
        
        // Force ascending if sortDir is not specified or is desc (default behavior)
        String queryString = request.getQueryString();
        boolean hasSortDirInUrl = queryString != null && queryString.contains("sortDir=");
        
        if (!hasSortDirInUrl) {
            // If sortDir is not in URL, default to asc and redirect
            sortDir = "asc";
            StringBuilder redirectUrl = new StringBuilder("/admin/books?");
            redirectUrl.append("page=").append(page);
            redirectUrl.append("&size=").append(size);
            redirectUrl.append("&sortBy=").append(sortBy);
            redirectUrl.append("&sortDir=asc");
            if (search != null && !search.trim().isEmpty()) {
                redirectUrl.append("&search=").append(java.net.URLEncoder.encode(search.trim(), java.nio.charset.StandardCharsets.UTF_8));
            }
            return "redirect:" + redirectUrl.toString();
        } else {
            // If sortDir is in URL, use it
            if (sortDir == null || sortDir.trim().isEmpty()) {
                sortDir = "asc";
            }
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
        
        // Format discount dates for datetime-local input
        if (book.getDiscountStart() != null) {
            String discountStartFormatted = book.getDiscountStart().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            model.addAttribute("discountStartFormatted", discountStartFormatted);
        } else {
            model.addAttribute("discountStartFormatted", "");
        }
        if (book.getDiscountEnd() != null) {
            String discountEndFormatted = book.getDiscountEnd().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            model.addAttribute("discountEndFormatted", discountEndFormatted);
        } else {
            model.addAttribute("discountEndFormatted", "");
        }
        
        return "admin/book-edit";
    }

    // Process edit book
    @PostMapping("/edit/{id}")
    @Transactional
    public String updateBook(@PathVariable("id") Long id,
                            @ModelAttribute("book") Book book,
                            @RequestParam("categoryId") Long categoryId,
                            @RequestParam(value = "discountStart", required = false) String discountStartStr,
                            @RequestParam(value = "discountEnd", required = false) String discountEndStr,
                            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                            @RequestParam(value = "imageUrl", required = false) String imageUrl,
                            @RequestParam(value = "deleteOldImage", required = false) String deleteOldImage,
                            @RequestParam(value = "deleted", required = false) String deletedParam,
                            RedirectAttributes redirectAttributes,
                            Authentication authentication,
                            jakarta.servlet.http.HttpServletRequest request) {
        // Debug: Method entry
        System.out.println("==========================================");
        System.out.println("DEBUG: updateBook method called for book ID: " + id);
        System.out.println("DEBUG: All request parameters:");
        request.getParameterMap().forEach((key, values) -> {
            System.out.println("  " + key + " = " + String.join(", ", values));
        });
        
        // Check if user is authenticated and has ADMIN role
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("DEBUG: User not authenticated, redirecting to login");
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            System.out.println("DEBUG: User is not admin, redirecting to user page");
            return "redirect:/demo/user";
        }

        try {
            Optional<Book> bookOpt = bookRepository.findByIdWithCategoryForAdmin(id);
            if (bookOpt.isEmpty()) {
                System.out.println("DEBUG: Book not found with ID: " + id);
                redirectAttributes.addFlashAttribute("error", "Book not found!");
                return "redirect:/admin/books";
            }

            Book existingBook = bookOpt.get();
            System.out.println("DEBUG: Book found: " + existingBook.getTitle() + ", current deleted status: " + existingBook.getDeleted());
            
            // Check deleted status from form
            Boolean newDeletedStatus = false;
            if (deletedParam != null && !deletedParam.trim().isEmpty()) {
                newDeletedStatus = "true".equalsIgnoreCase(deletedParam.trim());
            }
            
            // Debug log
            System.out.println("DEBUG: deletedParam from request = " + deletedParam);
            System.out.println("DEBUG: newDeletedStatus (parsed) = " + newDeletedStatus);
            System.out.println("DEBUG: existingBook.deleted = " + existingBook.getDeleted());
            
            // If trying to set book to inactive (deleted = true)
            // Also check if book is currently active and we're trying to set it to inactive
            boolean isChangingToInactive = newDeletedStatus && (existingBook.getDeleted() == null || !existingBook.getDeleted());
            
            System.out.println("DEBUG: isChangingToInactive = " + isChangingToInactive);
            System.out.println("DEBUG: newDeletedStatus = " + newDeletedStatus);
            System.out.println("DEBUG: existingBook.getDeleted() = " + existingBook.getDeleted());
            
            // IMPORTANT: Validate orders BEFORE setting book to inactive
            // If we're trying to set book to inactive, we MUST check orders first
            if (isChangingToInactive) {
                System.out.println("==========================================");
                System.out.println("DEBUG: Starting order validation checks...");
                System.out.println("DEBUG: Book ID to check: " + id);
                
                // Optimized: Check processing orders first (fail fast - lightweight query)
                System.out.println("DEBUG: Checking for processing orders...");
                boolean hasProcessing = orderItemRepository.hasProcessingOrders(id);
                System.out.println("DEBUG: hasProcessingOrders result = " + hasProcessing);
                
                if (hasProcessing) {
                    // Get order IDs for error message
                    List<Object[]> orderData = orderItemRepository.findOrderIdsAndStatusByBookId(id);
                    Set<Long> processingOrderIds = new HashSet<>();
                    for (Object[] data : orderData) {
                        if (data[1] != null && "processing".equalsIgnoreCase(data[1].toString().trim())) {
                            processingOrderIds.add((Long) data[0]);
                        }
                    }
                    redirectAttributes.addFlashAttribute("error", 
                        "Cannot set book to inactive! This book is in " + processingOrderIds.size() + 
                        " processing order(s): " + processingOrderIds.toString() + 
                        ". Please wait until these orders are shipped or cancelled.");
                    System.out.println("DEBUG: Blocked - processing orders found");
                    return "redirect:/admin/books/edit/" + id;
                }
                
                // Optimized: Check shipped orders (fail fast - lightweight query)
                System.out.println("DEBUG: Checking for shipped orders...");
                boolean hasShipped = orderItemRepository.hasShippedOrders(id);
                System.out.println("DEBUG: hasShippedOrders result = " + hasShipped);
                
                if (hasShipped) {
                    // Get order IDs for error message
                    List<Object[]> orderData = orderItemRepository.findOrderIdsAndStatusByBookId(id);
                    Set<Long> shippedOrderIds = new HashSet<>();
                    for (Object[] data : orderData) {
                        if (data[1] != null && "shipped".equalsIgnoreCase(data[1].toString().trim())) {
                            shippedOrderIds.add((Long) data[0]);
                        }
                    }
                    redirectAttributes.addFlashAttribute("error", 
                        "Cannot set book to inactive! This book is in " + shippedOrderIds.size() + 
                        " shipped order(s): " + shippedOrderIds.toString() + 
                        ". Shipped orders cannot be modified.");
                    System.out.println("DEBUG: Blocked - shipped orders found");
                    return "redirect:/admin/books/edit/" + id;
                }
                
                // Check for other status orders (non-pending, non-processing, non-shipped)
                System.out.println("DEBUG: Getting all order IDs and statuses...");
                List<Object[]> allOrderData = orderItemRepository.findOrderIdsAndStatusByBookId(id);
                System.out.println("DEBUG: Found " + (allOrderData != null ? allOrderData.size() : 0) + " orders");
                
                Set<Long> otherOrderIds = new HashSet<>();
                Set<Long> pendingOrderIds = new HashSet<>();
                
                if (allOrderData != null) {
                    for (Object[] data : allOrderData) {
                        Long orderId = (Long) data[0];
                        String status = data[1] != null ? data[1].toString().trim().toLowerCase() : null;
                        
                        if (status == null || (!"pending".equals(status) && !"processing".equals(status) && !"shipped".equals(status))) {
                            otherOrderIds.add(orderId);
                        } else if ("pending".equals(status)) {
                            pendingOrderIds.add(orderId);
                        }
                    }
                }
                
                // Check: Other status orders → BLOCK
                if (!otherOrderIds.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Cannot set book to inactive! This book is in " + otherOrderIds.size() + 
                        " order(s) with other status: " + otherOrderIds.toString());
                    System.out.println("DEBUG: Blocked - other status orders found");
                    return "redirect:/admin/books/edit/" + id;
                }
                
                // If only has pending orders → DELETE those orders
                if (!pendingOrderIds.isEmpty()) {
                    System.out.println("DEBUG: Found " + pendingOrderIds.size() + " pending orders to delete");
                    // Optimized: Get only pending orders with full details for deletion
                    List<OrderItem> pendingOrderItems = orderItemRepository.findPendingOrderItemsByBookId(id);
                    
                    // Collect unique order IDs from pending order items
                    Set<Long> ordersToDelete = new HashSet<>();
                    for (OrderItem item : pendingOrderItems) {
                        if (item.getOrder() != null) {
                            ordersToDelete.add(item.getOrder().getOrderId());
                        }
                    }
                    
                    int deletedCount = 0;
                    for (Long orderId : ordersToDelete) {
                        try {
                            // Find order with items
                            Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(orderId);
                            if (orderOpt.isPresent()) {
                                Order order = orderOpt.get();
                                // Delete order items first to avoid TransientObjectException
                                if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                                    orderItemRepository.deleteAll(order.getOrderItems());
                                    orderItemRepository.flush();
                                }
                                // Delete order after order items are deleted
                                orderRepository.delete(order);
                                orderRepository.flush();
                                deletedCount++;
                                System.out.println("DEBUG: Deleted pending order #" + orderId);
                            } else {
                                // If order not found, try to delete by ID anyway
                                orderRepository.deleteById(orderId);
                                deletedCount++;
                                System.out.println("DEBUG: Deleted pending order #" + orderId + " (by ID)");
                            }
                        } catch (Exception e) {
                            // Log error but continue with other orders
                            System.err.println("DEBUG: Failed to delete pending order #" + orderId + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    
                    if (deletedCount > 0) {
                        redirectAttributes.addFlashAttribute("success", 
                            "Book set to inactive. Deleted " + deletedCount + " pending order(s).");
                        System.out.println("DEBUG: Successfully deleted " + deletedCount + " pending orders");
                    }
                } else {
                    System.out.println("DEBUG: No pending orders found - book can be set to inactive");
                }
                
                System.out.println("DEBUG: Order validation checks completed successfully");
            } else {
                System.out.println("DEBUG: NOT changing to inactive - skipping order validation");
            }
            System.out.println("==========================================");

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

            // Update deleted status (already validated above)
            existingBook.setDeleted(newDeletedStatus);

            // Save updated book
            System.out.println("DEBUG: Saving book with deleted status: " + newDeletedStatus);
            bookRepository.save(existingBook);
            System.out.println("DEBUG: Book saved successfully");

            // Only show success message if not already set (e.g., from deleting pending orders)
            if (!redirectAttributes.getFlashAttributes().containsKey("success")) {
                redirectAttributes.addFlashAttribute("success", "Book updated successfully!");
            }
            return "redirect:/admin/books";
        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("DEBUG: EXCEPTION in updateBook method!");
            System.err.println("DEBUG: Exception type: " + e.getClass().getName());
            System.err.println("DEBUG: Exception message: " + e.getMessage());
            System.err.println("DEBUG: Exception stack trace:");
            e.printStackTrace();
            System.err.println("==========================================");
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
    @Transactional
    public String toggleBookStatus(@PathVariable("id") Long id,
                                   @RequestParam(value = "redirect", defaultValue = "list") String redirect,
                                   RedirectAttributes redirectAttributes,
                                   Authentication authentication) {
        // Debug: Method entry
        System.out.println("==========================================");
        System.out.println("DEBUG: toggleBookStatus method called for book ID: " + id);
        System.out.println("DEBUG: Redirect parameter: " + redirect);
        
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
            System.out.println("DEBUG: Book found: " + book.getTitle() + ", current deleted status: " + book.getDeleted());
            
            // Toggle status: if deleted (inactive), set to active; if active, set to inactive
            boolean currentStatus = book.getDeleted() != null ? book.getDeleted() : false;
            boolean newStatus = !currentStatus;
            
            System.out.println("DEBUG: currentStatus = " + currentStatus);
            System.out.println("DEBUG: newStatus = " + newStatus);
            
            // IMPORTANT: Validate orders BEFORE setting book to inactive
            // If we're trying to set book to inactive (from active), we MUST check orders first
            boolean isChangingToInactive = newStatus && !currentStatus;
            
            if (isChangingToInactive) {
                System.out.println("DEBUG: Setting book from active to inactive - starting order validation checks...");
                System.out.println("DEBUG: Book ID to check: " + id);
                
                // Optimized: Check processing orders first (fail fast - lightweight query)
                System.out.println("DEBUG: Checking for processing orders...");
                boolean hasProcessing = orderItemRepository.hasProcessingOrders(id);
                System.out.println("DEBUG: hasProcessingOrders result = " + hasProcessing);
                
                if (hasProcessing) {
                    // Get order IDs for error message
                    List<Object[]> orderData = orderItemRepository.findOrderIdsAndStatusByBookId(id);
                    Set<Long> processingOrderIds = new HashSet<>();
                    for (Object[] data : orderData) {
                        if (data[1] != null && "processing".equalsIgnoreCase(data[1].toString().trim())) {
                            processingOrderIds.add((Long) data[0]);
                        }
                    }
                    redirectAttributes.addFlashAttribute("error", 
                        "Cannot set book to inactive! This book is in " + processingOrderIds.size() + 
                        " processing order(s): " + processingOrderIds.toString() + 
                        ". Please wait until these orders are shipped or cancelled.");
                    System.out.println("DEBUG: Blocked - processing orders found");
                    // Redirect based on redirect parameter
                    if ("view".equals(redirect)) {
                        return "redirect:/admin/books/view/" + id;
                    }
                    // Redirect with query parameters to avoid another redirect that would lose flash attributes
                    return "redirect:/admin/books?page=0&size=6&sortBy=bookId&sortDir=asc";
                }
                
                // Optimized: Check shipped orders (fail fast - lightweight query)
                System.out.println("DEBUG: Checking for shipped orders...");
                boolean hasShipped = orderItemRepository.hasShippedOrders(id);
                System.out.println("DEBUG: hasShippedOrders result = " + hasShipped);
                
                if (hasShipped) {
                    // Get order IDs for error message
                    List<Object[]> orderData = orderItemRepository.findOrderIdsAndStatusByBookId(id);
                    Set<Long> shippedOrderIds = new HashSet<>();
                    for (Object[] data : orderData) {
                        if (data[1] != null && "shipped".equalsIgnoreCase(data[1].toString().trim())) {
                            shippedOrderIds.add((Long) data[0]);
                        }
                    }
                    redirectAttributes.addFlashAttribute("error", 
                        "Cannot set book to inactive! This book is in " + shippedOrderIds.size() + 
                        " shipped order(s): " + shippedOrderIds.toString() + 
                        ". Shipped orders cannot be modified.");
                    System.out.println("DEBUG: Blocked - shipped orders found");
                    // Redirect based on redirect parameter
                    if ("view".equals(redirect)) {
                        return "redirect:/admin/books/view/" + id;
                    }
                    // Redirect with query parameters to avoid another redirect that would lose flash attributes
                    return "redirect:/admin/books?page=0&size=6&sortBy=bookId&sortDir=asc";
                }
                
                // Check for other status orders (non-pending, non-processing, non-shipped)
                System.out.println("DEBUG: Getting all order IDs and statuses...");
                List<Object[]> allOrderData = orderItemRepository.findOrderIdsAndStatusByBookId(id);
                System.out.println("DEBUG: Found " + (allOrderData != null ? allOrderData.size() : 0) + " orders");
                
                Set<Long> otherOrderIds = new HashSet<>();
                Set<Long> pendingOrderIds = new HashSet<>();
                
                if (allOrderData != null) {
                    for (Object[] data : allOrderData) {
                        Long orderId = (Long) data[0];
                        String status = data[1] != null ? data[1].toString().trim().toLowerCase() : null;
                        
                        if (status == null || (!"pending".equals(status) && !"processing".equals(status) && !"shipped".equals(status))) {
                            otherOrderIds.add(orderId);
                        } else if ("pending".equals(status)) {
                            pendingOrderIds.add(orderId);
                        }
                    }
                }
                
                // Check: Other status orders → BLOCK
                if (!otherOrderIds.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Cannot set book to inactive! This book is in " + otherOrderIds.size() + 
                        " order(s) with other status: " + otherOrderIds.toString());
                    System.out.println("DEBUG: Blocked - other status orders found");
                    // Redirect based on redirect parameter
                    if ("view".equals(redirect)) {
                        return "redirect:/admin/books/view/" + id;
                    }
                    // Redirect with query parameters to avoid another redirect that would lose flash attributes
                    return "redirect:/admin/books?page=0&size=6&sortBy=bookId&sortDir=asc";
                }
                
                // If only has pending orders → DELETE those orders
                if (!pendingOrderIds.isEmpty()) {
                    System.out.println("DEBUG: Found " + pendingOrderIds.size() + " pending orders to delete");
                    // Optimized: Get only pending orders with full details for deletion
                    List<OrderItem> pendingOrderItems = orderItemRepository.findPendingOrderItemsByBookId(id);
                    
                    // Collect unique order IDs from pending order items
                    Set<Long> ordersToDelete = new HashSet<>();
                    for (OrderItem item : pendingOrderItems) {
                        if (item.getOrder() != null) {
                            ordersToDelete.add(item.getOrder().getOrderId());
                        }
                    }
                    
                    int deletedCount = 0;
                    for (Long orderId : ordersToDelete) {
                        try {
                            // Find order with items
                            Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(orderId);
                            if (orderOpt.isPresent()) {
                                Order order = orderOpt.get();
                                // Delete order items first to avoid TransientObjectException
                                if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                                    orderItemRepository.deleteAll(order.getOrderItems());
                                    orderItemRepository.flush();
                                }
                                // Delete order after order items are deleted
                                orderRepository.delete(order);
                                orderRepository.flush();
                                deletedCount++;
                                System.out.println("DEBUG: Deleted pending order #" + orderId);
                            } else {
                                // If order not found, try to delete by ID anyway
                                orderRepository.deleteById(orderId);
                                deletedCount++;
                                System.out.println("DEBUG: Deleted pending order #" + orderId + " (by ID)");
                            }
                        } catch (Exception e) {
                            // Log error but continue with other orders
                            System.err.println("DEBUG: Failed to delete pending order #" + orderId + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    
                    if (deletedCount > 0) {
                        redirectAttributes.addFlashAttribute("success", 
                            "Book set to inactive. Deleted " + deletedCount + " pending order(s).");
                        System.out.println("DEBUG: Successfully deleted " + deletedCount + " pending orders");
                    }
                } else {
                    System.out.println("DEBUG: No pending orders found - book can be set to inactive");
                }
                
                System.out.println("DEBUG: Order validation checks completed successfully");
            } else {
                System.out.println("DEBUG: NOT changing to inactive (changing to active) - skipping order validation");
            }
            
            // Now set the new status after validation
            book.setDeleted(newStatus);
            System.out.println("DEBUG: Saving book with deleted status: " + newStatus);
            bookRepository.save(book);
            System.out.println("DEBUG: Book saved successfully");

            // Only show success message if not already set (e.g., from deleting pending orders)
            if (!redirectAttributes.getFlashAttributes().containsKey("success")) {
                String statusMessage = newStatus ? "Book set to inactive successfully!" : "Book set to active successfully!";
                redirectAttributes.addFlashAttribute("success", statusMessage);
            }
            
            System.out.println("==========================================");

            if ("view".equals(redirect)) {
                return "redirect:/admin/books/view/" + id;
            }
            // Redirect with query parameters to avoid another redirect that would lose flash attributes
            return "redirect:/admin/books?page=0&size=6&sortBy=bookId&sortDir=asc";
        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("DEBUG: EXCEPTION in toggleBookStatus method!");
            System.err.println("DEBUG: Exception type: " + e.getClass().getName());
            System.err.println("DEBUG: Exception message: " + e.getMessage());
            System.err.println("DEBUG: Exception stack trace:");
            e.printStackTrace();
            System.err.println("==========================================");
            redirectAttributes.addFlashAttribute("error", "Error updating book status: " + e.getMessage());
            if ("view".equals(redirect)) {
                return "redirect:/admin/books/view/" + id;
            }
            // Redirect with query parameters to avoid another redirect that would lose flash attributes
            return "redirect:/admin/books?page=0&size=6&sortBy=bookId&sortDir=asc";
        }
    }
}

