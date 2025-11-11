package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.entity.Wishlist;
import com.bookverse.BookVerse.repository.BookRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import com.bookverse.BookVerse.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WishlistService {
    
    @Autowired
    private WishlistRepository wishlistRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    /**
     * Thêm sách vào wishlist
     */
    @Transactional
    public boolean addToWishlist(Long userId, Long bookId) {
        try {
            // Kiểm tra xem đã có trong wishlist chưa
            if (wishlistRepository.existsByUserUserIdAndBookBookId(userId, bookId)) {
                return false; // Đã tồn tại
            }
            
            // Lấy user và book
            Optional<User> userOpt = userRepository.findById(userId);
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            
            if (userOpt.isEmpty() || bookOpt.isEmpty()) {
                return false;
            }
            
            // Tạo wishlist item mới
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(userOpt.get());
            wishlist.setBook(bookOpt.get());
            wishlist.setAddedAt(LocalDateTime.now());
            
            wishlistRepository.save(wishlist);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Xóa sách khỏi wishlist
     */
    @Transactional
    public boolean removeFromWishlist(Long userId, Long bookId) {
        try {
            wishlistRepository.deleteByUserUserIdAndBookBookId(userId, bookId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Lấy tất cả wishlist items của user (không phân trang)
     */
    @Transactional(readOnly = true)
    public List<Wishlist> getUserWishlist(Long userId) {
        return wishlistRepository.findByUserUserIdWithBook(userId);
    }
    
    /**
     * Lấy wishlist items của user với phân trang
     */
    @Transactional(readOnly = true)
    public Page<Wishlist> getUserWishlistPaged(Long userId, int page, int size, String sortBy, String sortDir) {
        // Map sortBy field names to entity fields
        String sortField;
        switch (sortBy) {
            case "book.title":
                sortField = "book.title";
                break;
            case "book.price":
                sortField = "book.price";
                break;
            case "addedAt":
            default:
                sortField = "addedAt";
                break;
        }
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get paginated results (without fetching book/category to allow sorting)
        Page<Wishlist> pageResult = wishlistRepository.findByUserUserIdWithBookPaged(userId, pageable);
        
        // If page has content, fetch books and categories for the items
        if (!pageResult.getContent().isEmpty()) {
            List<Long> wishlistIds = pageResult.getContent().stream()
                .map(Wishlist::getWishlistId)
                .toList();
            
            // Fetch with book and category
            List<Wishlist> wishlistsWithBooks = wishlistRepository.findByWishlistIdInWithBook(wishlistIds);
            
            // Create a map for quick lookup
            java.util.Map<Long, Wishlist> wishlistMap = wishlistsWithBooks.stream()
                .collect(java.util.stream.Collectors.toMap(Wishlist::getWishlistId, w -> w));
            
            // Replace content with fetched items (maintaining order)
            List<Wishlist> sortedContent = pageResult.getContent().stream()
                .map(w -> wishlistMap.get(w.getWishlistId()))
                .filter(java.util.Objects::nonNull)
                .toList();
            
            // Create new page with fetched content
            return new org.springframework.data.domain.PageImpl<>(
                sortedContent, 
                pageable, 
                pageResult.getTotalElements()
            );
        }
        
        return pageResult;
    }
    
    /**
     * Kiểm tra xem sách đã có trong wishlist chưa
     */
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long bookId) {
        return wishlistRepository.existsByUserUserIdAndBookBookId(userId, bookId);
    }
    
    /**
     * Đếm số lượng wishlist items của user
     */
    @Transactional(readOnly = true)
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserUserId(userId);
    }
    
    /**
     * Toggle wishlist (thêm nếu chưa có, xóa nếu đã có)
     */
    @Transactional
    public boolean toggleWishlist(Long userId, Long bookId) {
        if (isInWishlist(userId, bookId)) {
            return removeFromWishlist(userId, bookId);
        } else {
            return addToWishlist(userId, bookId);
        }
    }
}

