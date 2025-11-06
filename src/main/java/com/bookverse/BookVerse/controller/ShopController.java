package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/shop")
public class ShopController {
    
    private final BookService bookService;
    
    public ShopController(BookService bookService) {
        this.bookService = bookService;
    }
    
    @GetMapping
    public String shopPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false, defaultValue = "grid") String view,
            @RequestParam(required = false, defaultValue = "false") boolean saleOnly,
            Model model) {
        
        Page<Book> books;
        
        // Lọc theo category
        if (categoryId != null) {
            books = bookService.getBooksByCategory(categoryId, page, size, sortBy, sortDir);
        }
        // Tìm kiếm
        else if (search != null && !search.trim().isEmpty()) {
            books = bookService.searchBooks(search, page, size, sortBy, sortDir);
        }
        // Lọc theo giá
        else if (minPrice != null && maxPrice != null) {
            books = bookService.getBooksByPriceRange(minPrice, maxPrice, page, size, sortBy, sortDir);
        }
        // Chỉ đang giảm giá
        else if (saleOnly) {
            books = bookService.getOnSaleBooks(page, size, sortBy, sortDir);
        }
        // Lấy tất cả
        else {
            books = bookService.getAllBooks(page, size, sortBy, sortDir);
        }
        
        // Lấy categories và tính số lượng sách
        var categories = bookService.getAllCategories();
        
        // Lấy sách ngẫu nhiên cho sidebar (6 sách)
        var randomBooks = bookService.getRandomBooks(6);
        
        // Thêm dữ liệu vào model
        model.addAttribute("books", books);
        model.addAttribute("categories", categories);
        model.addAttribute("randomBooks", randomBooks);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", books.getTotalPages());
        model.addAttribute("totalItems", books.getTotalElements());
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("viewType", view);
        model.addAttribute("saleOnly", saleOnly);
        
        return "user/shop";
    }
}

