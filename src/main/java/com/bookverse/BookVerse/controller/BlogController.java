package com.bookverse.BookVerse.controller;

import com.bookverse.BookVerse.entity.Blog;
import com.bookverse.BookVerse.repository.BlogRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
public class BlogController {

    private final BlogRepository blogRepository;
    private final com.bookverse.BookVerse.service.BookService bookService;

    public BlogController(BlogRepository blogRepository, com.bookverse.BookVerse.service.BookService bookService) {
        this.blogRepository = blogRepository;
        this.bookService = bookService;
    }

    @GetMapping("/blog")
    public String blogPage(Model model) {
        // Eagerly load details to prevent LazyInitialization issues in template
        List<Blog> blogs = blogRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("blogs", blogs);
        // supply categories for header navigation
        List<com.bookverse.BookVerse.entity.Category> categories = bookService.getAllCategories();
        model.addAttribute("categories", categories);
        return "user/blog";
    }

    @GetMapping("/blog/{id}")
    public String blogDetail(@PathVariable("id") Long id, Model model) {
        // Eagerly load details for the selected blog
        Optional<Blog> blogOpt = blogRepository.findWithDetailsByBlogId(id);
        if (blogOpt.isEmpty()) {
            return "redirect:/blog";
        }
        Blog blog = blogOpt.get();
        model.addAttribute("blog", blog);
        // supply categories for header navigation
        List<com.bookverse.BookVerse.entity.Category> categories = bookService.getAllCategories();
        model.addAttribute("categories", categories);
        return "user/blog-detail";
    }
}
