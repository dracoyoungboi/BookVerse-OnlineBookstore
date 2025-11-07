package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.Category;
import com.bookverse.BookVerse.repository.BookRepository;
import com.bookverse.BookVerse.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {
    
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    
    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
    }
    
    public Page<Book> getAllBooks(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findAll(pageable);
    }
    
    public Page<Book> getBooksByCategory(Long categoryId, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByCategoryCategoryId(categoryId, pageable);
    }
    
    public Page<Book> searchBooks(String keyword, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }
    
    // Lấy sách theo khoảng giá
    public Page<Book> getBooksByPriceRange(Double minPrice, Double maxPrice, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByPriceBetween(minPrice, maxPrice, pageable);
    }
    
    // Lấy sách theo ID
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
    
    // Lấy sách theo ID với reviews
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Optional<Book> getBookByIdWithDetails(Long id) {
        // Fetch book với reviews
        Optional<Book> bookOpt = bookRepository.findByIdWithReviews(id);
        if (bookOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Book book = bookOpt.get();
        
        // Đảm bảo reviews đã được load
        if (book.getReviews() != null) {
            book.getReviews().size(); // Trigger lazy loading nếu cần
        }
        
        return Optional.of(book);
    }
    
    // Lấy tất cả categories
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    // Lấy sách còn hàng
    public Page<Book> getAvailableBooks(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByStockGreaterThan(0, pageable);
    }
    
    // Lấy sách đang giảm giá
    public Page<Book> getOnSaleBooks(int page, int size, String sortBy, String sortDir) {
        // map 'discount' to 'discountPercent' for sorting
        if ("discount".equalsIgnoreCase(sortBy)) {
            sortBy = "discountPercent";
            sortDir = "desc"; // mặc định giảm giá nhiều trước
        }
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByDiscountPercentGreaterThan(0.0, pageable);
    }
    // Đếm số lượng sách theo category
    public long countBooksByCategory(Long categoryId) {
        return bookRepository.findByCategoryCategoryId(categoryId).size();
    }
    
    // Lấy sách ngẫu nhiên (limit số lượng)
    public List<Book> getRandomBooks(int limit) {
        List<Book> allBooks = bookRepository.findAll();
        Collections.shuffle(allBooks);
        return allBooks.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }
    
    // Lấy sách liên quan (cùng category, loại trừ sách hiện tại)
    public List<Book> getRelatedBooks(Long bookId, Long categoryId, int limit) {
        List<Book> relatedBooks = bookRepository.findRelatedBooks(categoryId, bookId);
        Collections.shuffle(relatedBooks);
        return relatedBooks.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }
    
    // Lấy best seller books (random hoặc mới nhất)
    public List<Book> getBestSellerBooks(int limit) {
        return getRandomBooks(limit);
    }
    
    // Lấy new arrival books (sách mới nhất)
    public List<Book> getNewArrivalBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return bookRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    // Lấy on sale books (không phân trang, chỉ lấy list)
    public List<Book> getOnSaleBooksList(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("discountPercent").descending());
        Page<Book> page = bookRepository.findByDiscountPercentGreaterThan(0.0, pageable);
        return page.getContent();
    }
    
    // Lấy featured books (random)
    public List<Book> getFeaturedBooks(int limit) {
        return getRandomBooks(limit);
    }
    
    // Lấy sách theo category (không phân trang, chỉ lấy list)
    public List<Book> getBooksByCategoryList(Long categoryId, int limit) {
        List<Book> books = bookRepository.findByCategoryCategoryId(categoryId);
        Collections.shuffle(books);
        return books.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }
}

