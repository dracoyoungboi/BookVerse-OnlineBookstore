package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.InventoryTransaction;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.BookRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.service.ExcelService;
import com.bookverse.BookVerse.service.InventoryService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/inventory")
public class AdminInventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.bookverse.BookVerse.repository.CategoryRepository categoryRepository;

    @Autowired
    private ExcelService excelService;

    /**
     * Danh sách lịch sử kho
     */
    @GetMapping
    public String inventoryHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model,
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails,
            Authentication authentication) {
        
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info
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

        // Parse dates
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
            } catch (Exception e) {
                // Ignore
            }
        }
        
        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                endDateTime = LocalDateTime.parse(endDate + "T23:59:59");
            } catch (Exception e) {
                // Ignore
            }
        }

        // Get inventory history
        Page<InventoryTransaction> transactions = inventoryService.getInventoryHistory(
                bookId, transactionType, startDateTime, endDateTime, page, size);

        // Get all books for filter dropdown
        List<Book> books = bookRepository.findAll();

        // Calculate pagination info
        int startPage = Math.max(0, page - 1);
        int endPage = Math.min(transactions.getTotalPages() - 1, page + 1);
        boolean showFirstPage = page > 2;
        boolean showLastPage = page < transactions.getTotalPages() - 2 && transactions.getTotalPages() > 1;
        boolean showFirstEllipsis = page > 3;
        boolean showLastEllipsis = page < transactions.getTotalPages() - 3;

        model.addAttribute("transactions", transactions.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        model.addAttribute("totalItems", transactions.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("books", books);
        model.addAttribute("selectedBookId", bookId);
        model.addAttribute("selectedTransactionType", transactionType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showFirstPage", showFirstPage);
        model.addAttribute("showLastPage", showLastPage);
        model.addAttribute("showFirstEllipsis", showFirstEllipsis);
        model.addAttribute("showLastEllipsis", showLastEllipsis);

        return "admin/inventory-list";
    }

    /**
     * Form nhập kho
     */
    @GetMapping("/import")
    public String showImportForm(@RequestParam(required = false) Long bookId,
                                 Model model,
                                 HttpSession session,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Authentication authentication) {
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }

        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }

        // Get all active books
        List<Book> books = bookRepository.findAll().stream()
                .filter(book -> book.getDeleted() == null || !book.getDeleted())
                .toList();

        model.addAttribute("books", books);

        // If bookId is provided, pre-select the book and set current stock
        if (bookId != null) {
            Optional<Book> selectedBookOpt = bookRepository.findById(bookId);
            if (selectedBookOpt.isPresent()) {
                Book selectedBook = selectedBookOpt.get();
                model.addAttribute("selectedBookId", bookId);
                model.addAttribute("selectedBook", selectedBook);
                model.addAttribute("currentStock", selectedBook.getStock());
            }
        }

        return "admin/inventory-import";
    }

    /**
     * Xử lý nhập kho
     */
    @PostMapping("/import")
    public String processImport(
            @RequestParam("bookId") Long bookId,
            @RequestParam("quantity") int quantity,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "note", required = false) String note,
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Get current user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/admin/inventory/import";
        }

        // Validate
        if (quantity <= 0) {
            redirectAttributes.addFlashAttribute("error", "Quantity must be greater than 0!");
            return "redirect:/admin/inventory/import";
        }

        // Process import
        boolean success = inventoryService.importStock(bookId, quantity, reason, note, currentUser);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Stock imported successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to import stock. Please check if book exists and is active.");
        }

        return "redirect:/admin/inventory";
    }

    /**
     * Form điều chỉnh kho
     */
    @GetMapping("/adjust")
    public String showAdjustForm(@RequestParam(required = false) Long bookId,
                                Model model,
                                HttpSession session,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Authentication authentication) {
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }

        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }

        // Get all active books
        List<Book> books = bookRepository.findAll().stream()
                .filter(book -> book.getDeleted() == null || !book.getDeleted())
                .toList();

        model.addAttribute("books", books);
        
        // If bookId is provided, pre-select the book and set current stock
        if (bookId != null) {
            Optional<Book> selectedBookOpt = bookRepository.findById(bookId);
            if (selectedBookOpt.isPresent()) {
                Book selectedBook = selectedBookOpt.get();
                model.addAttribute("selectedBookId", bookId);
                model.addAttribute("selectedBook", selectedBook);
                model.addAttribute("currentStock", selectedBook.getStock());
            }
        }

        return "admin/inventory-adjust";
    }

    /**
     * Xử lý điều chỉnh kho
     */
    @PostMapping("/adjust")
    public String processAdjust(
            @RequestParam("bookId") Long bookId,
            @RequestParam("newStock") int newStock,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "note", required = false) String note,
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Get current user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/admin/inventory/adjust";
        }

        // Validate
        if (newStock < 0) {
            redirectAttributes.addFlashAttribute("error", "Stock cannot be negative!");
            return "redirect:/admin/inventory/adjust";
        }

        // Process adjust
        boolean success = inventoryService.adjustStock(bookId, newStock, reason, note, currentUser);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Stock adjusted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to adjust stock. Please check if book exists and is active.");
        }

        return "redirect:/admin/inventory";
    }

    /**
     * Trang theo dõi tồn kho
     */
    @GetMapping("/stock")
    public String stockOverview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String stockFilter, // "all", "in_stock", "low_stock", "out_of_stock"
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "bookId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model,
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails,
            Authentication authentication) {
        
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }

        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }

        // Get books with pagination and filters
        Pageable pageable;
        if ("stock".equals(sortBy)) {
            Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                Sort.by("stock").ascending() : Sort.by("stock").descending();
            pageable = PageRequest.of(page, size, sort);
        } else {
            Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            pageable = PageRequest.of(page, size, sort);
        }

        Page<Book> booksPage;
        
        // Apply filters
        if (search != null && !search.trim().isEmpty()) {
            booksPage = bookRepository.searchBooksWithCategory(search.trim(), pageable);
        } else if (categoryId != null) {
            booksPage = bookRepository.findByCategoryCategoryId(categoryId, pageable);
        } else {
            booksPage = bookRepository.findAllWithCategoryPaged(pageable);
        }

        // Apply stock filter (filter after pagination for display, but note pagination may be affected)
        List<Book> filteredBooks = booksPage.getContent();
        long filteredTotal = booksPage.getTotalElements();
        
        if (stockFilter != null && !stockFilter.isEmpty() && !"all".equals(stockFilter)) {
            filteredBooks = filteredBooks.stream()
                    .filter(book -> {
                        switch (stockFilter) {
                            case "out_of_stock":
                                return book.getStock() == 0;
                            case "low_stock":
                                return book.getStock() > 0 && book.getStock() < 10;
                            case "in_stock":
                                return book.getStock() > 0;
                            default:
                                return true;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            // Note: When filtering, totalItems may not be accurate for pagination
            // For better accuracy, we'd need to filter at repository level
        }

        // Get all categories for filter
        List<com.bookverse.BookVerse.entity.Category> categories = categoryRepository.findAll();

        // Calculate statistics
        long totalBooks = bookRepository.count();
        long inStockBooks = bookRepository.findAll().stream()
                .filter(book -> book.getStock() > 0)
                .count();
        long lowStockBooks = bookRepository.findAll().stream()
                .filter(book -> book.getStock() > 0 && book.getStock() < 10)
                .count();
        long outOfStockBooks = bookRepository.findAll().stream()
                .filter(book -> book.getStock() == 0)
                .count();

        // Calculate pagination info
        int startPage = Math.max(0, page - 1);
        int endPage = Math.min(booksPage.getTotalPages() - 1, page + 1);
        boolean showFirstPage = page > 2;
        boolean showLastPage = page < booksPage.getTotalPages() - 2 && booksPage.getTotalPages() > 1;
        boolean showFirstEllipsis = page > 3;
        boolean showLastEllipsis = page < booksPage.getTotalPages() - 3;

        model.addAttribute("books", filteredBooks);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", booksPage.getTotalPages());
        model.addAttribute("totalItems", filteredTotal);
        model.addAttribute("pageSize", size);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedStockFilter", stockFilter != null ? stockFilter : "all");
        model.addAttribute("search", search);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showFirstPage", showFirstPage);
        model.addAttribute("showLastPage", showLastPage);
        model.addAttribute("showFirstEllipsis", showFirstEllipsis);
        model.addAttribute("showLastEllipsis", showLastEllipsis);
        
        // Statistics
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("inStockBooks", inStockBooks);
        model.addAttribute("lowStockBooks", lowStockBooks);
        model.addAttribute("outOfStockBooks", outOfStockBooks);

        return "admin/inventory-stock";
    }

    /**
     * Form xuất kho
     */
    @GetMapping("/export")
    public String showExportForm(Model model,
                                HttpSession session,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Authentication authentication) {
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }

        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }

        // Get all active books
        List<Book> books = bookRepository.findAll().stream()
                .filter(book -> book.getDeleted() == null || !book.getDeleted())
                .toList();

        model.addAttribute("books", books);

        return "admin/inventory-export";
    }

    /**
     * Xử lý xuất kho
     */
    @PostMapping("/export")
    public String processExport(
            @RequestParam("bookId") Long bookId,
            @RequestParam("quantity") int quantity,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "note", required = false) String note,
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Get current user
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/admin/inventory/export";
        }

        // Validate
        if (quantity <= 0) {
            redirectAttributes.addFlashAttribute("error", "Quantity must be greater than 0!");
            return "redirect:/admin/inventory/export";
        }

        // Process export
        boolean success = inventoryService.exportStock(bookId, quantity, reason, note, currentUser);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Stock exported successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to export stock. Please check if book exists, is active, and has enough stock.");
        }

        return "redirect:/admin/inventory";
    }

    /**
     * Trang Low Stock & Out of Stock - hiển thị sách có stock thấp và hết hàng
     */
    @GetMapping("/alerts")
    public String stockAlerts(
            Model model,
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails,
            Authentication authentication) {

        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            return "redirect:/demo/user";
        }

        // Set current user info
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null && userDetails != null) {
            Optional<User> userOpt = userRepository.findByUsernameWithRole(userDetails.getUsername());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                session.setAttribute("currentUser", currentUser);
            }
        }

        if (currentUser != null) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("fullName", currentUser.getFullName());
        }

        // Get alert books
        List<Book> outOfStockBooks = inventoryService.getOutOfStockBooks();
        List<Book> lowStockBooks = inventoryService.getLowStockBooks();

        // Combine all alert books
        List<Book> alertBooks = new java.util.ArrayList<>();
        alertBooks.addAll(outOfStockBooks);
        alertBooks.addAll(lowStockBooks);

        // Statistics
        long totalOutOfStock = outOfStockBooks.size();
        long totalLowStock = lowStockBooks.size();

        model.addAttribute("alertBooks", alertBooks);
        model.addAttribute("totalOutOfStock", totalOutOfStock);
        model.addAttribute("totalLowStock", totalLowStock);

        return "admin/inventory-alerts";
    }

    /**
     * Export Inventory History to Excel
     */
    @GetMapping("/export-excel")
    public void exportInventoryHistoryToExcel(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response,
            Authentication authentication) throws Exception {
        
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Parse dates
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
            } catch (Exception e) {
                // Ignore
            }
        }
        
        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                endDateTime = LocalDateTime.parse(endDate + "T23:59:59");
            } catch (Exception e) {
                // Ignore
            }
        }

        // Get all transactions (no pagination)
        List<InventoryTransaction> transactions = inventoryService.getAllInventoryTransactionsForExport(
                bookId, transactionType, startDateTime, endDateTime);

        // Generate Excel file
        byte[] excelBytes = excelService.exportInventoryTransactions(transactions);

        // Set response headers
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=inventory_history.xlsx");
        response.setContentLength(excelBytes.length);

        // Write to response
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }

    /**
     * Export Stock Overview to Excel
     */
    @GetMapping("/stock/export-excel")
    public void exportStockOverviewToExcel(
            @RequestParam(required = false) String stockFilter,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            HttpServletResponse response,
            Authentication authentication) throws Exception {
        
        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Get all books
        List<Book> allBooks = bookRepository.findAll();
        
        // Apply filters
        List<Book> filteredBooks = allBooks.stream()
                .filter(book -> book.getDeleted() == null || !book.getDeleted())
                .filter(book -> {
                    if (search != null && !search.trim().isEmpty()) {
                        String searchLower = search.toLowerCase();
                        return (book.getTitle() != null && book.getTitle().toLowerCase().contains(searchLower)) ||
                               (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(searchLower));
                    }
                    return true;
                })
                .filter(book -> {
                    if (categoryId != null) {
                        return book.getCategory() != null && 
                               book.getCategory().getCategoryId().equals(categoryId);
                    }
                    return true;
                })
                .filter(book -> {
                    if (stockFilter != null && !stockFilter.isEmpty() && !"all".equals(stockFilter)) {
                        switch (stockFilter) {
                            case "out_of_stock":
                                return book.getStock() == 0;
                            case "low_stock":
                                return book.getStock() > 0 && book.getStock() < 10;
                            case "in_stock":
                                return book.getStock() > 0;
                            default:
                                return true;
                        }
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        // Generate Excel file
        byte[] excelBytes = excelService.exportStockOverview(filteredBooks);

        // Set response headers
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=stock_overview.xlsx");
        response.setContentLength(excelBytes.length);

        // Write to response
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }

}

