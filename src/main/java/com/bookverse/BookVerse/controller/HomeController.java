package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Category;
import com.bookverse.BookVerse.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {
    
    private final BookService bookService;
    
    public HomeController(BookService bookService) {
        this.bookService = bookService;
    }
    
    @GetMapping("/")
    public String homePage(Model model) {
        // Lấy best seller books (6 sách)
        var bestSellerBooks = bookService.getBestSellerBooks(6);
        
        // Lấy deal books (on sale books, 3 sách)
        var dealBooks = bookService.getOnSaleBooksList(3);
        
        // Lấy new arrival books (6 sách)
        var newArrivalBooks = bookService.getNewArrivalBooks(6);
        
        // Lấy on sale books (6 sách)
        var onSaleBooks = bookService.getOnSaleBooksList(6);
        
        // Lấy featured books (6 sách)
        var featuredBooks = bookService.getFeaturedBooks(6);
        
        // Lấy on sale books cho sidebar (6 sách)
        var bestsellerSidebarBooks = bookService.getOnSaleBooksList(6);
        
        // Lấy categories
        List<Category> categories = bookService.getAllCategories();
        
        // Lấy books by category (4 categories đầu tiên, mỗi category 6 sách) - cho phần trên
        var booksByCategory = new java.util.HashMap<Category, java.util.List<com.bookverse.BookVerse.entity.Book>>();
        for (int i = 0; i < Math.min(4, categories.size()); i++) {
            Category category = categories.get(i);
            var books = bookService.getBooksByCategoryList(category.getCategoryId(), 6);
            booksByCategory.put(category, books);
        }
        
        // Lấy books by category (3 categories tiếp theo, mỗi category 6 sách) - cho phần dưới
        var booksByCategory2 = new java.util.HashMap<Category, java.util.List<com.bookverse.BookVerse.entity.Book>>();
        for (int i = 4; i < Math.min(7, categories.size()); i++) {
            Category category = categories.get(i);
            var books = bookService.getBooksByCategoryList(category.getCategoryId(), 6);
            booksByCategory2.put(category, books);
        }
        
        // Thêm dữ liệu vào model
        model.addAttribute("bestSellerBooks", bestSellerBooks);
        model.addAttribute("dealBooks", dealBooks);
        model.addAttribute("newArrivalBooks", newArrivalBooks);
        model.addAttribute("onSaleBooks", onSaleBooks);
        model.addAttribute("featuredBooks", featuredBooks);
        model.addAttribute("bestsellerSidebarBooks", bestsellerSidebarBooks);
        model.addAttribute("booksByCategory", booksByCategory);
        model.addAttribute("booksByCategory2", booksByCategory2);
        model.addAttribute("categories", categories);
        
        return "user/index-7";
    }
}

