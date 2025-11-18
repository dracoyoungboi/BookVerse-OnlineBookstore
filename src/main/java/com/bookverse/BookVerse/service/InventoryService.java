package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.InventoryTransaction;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.BookRepository;
import com.bookverse.BookVerse.repository.InventoryTransactionRepository;
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
public class InventoryService {

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private BookRepository bookRepository;

    /**
     * Nhập kho (tăng stock)
     */
    @Transactional
    public boolean importStock(Long bookId, int quantity, String reason, String note, User createdBy) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            return false;
        }

        Book book = bookOpt.get();
        
        // Kiểm tra sách có bị xóa không
        if (book.getDeleted() != null && book.getDeleted()) {
            return false;
        }

        // Kiểm tra số lượng
        if (quantity <= 0) {
            return false;
        }

        int stockBefore = book.getStock();
        int stockAfter = stockBefore + quantity;

        // Cập nhật stock
        book.setStock(stockAfter);
        bookRepository.save(book);

        // Lưu lịch sử
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setBook(book);
        transaction.setTransactionType("IMPORT");
        transaction.setQuantity(quantity);
        transaction.setStockBefore(stockBefore);
        transaction.setStockAfter(stockAfter);
        transaction.setReason(reason);
        transaction.setNote(note);
        transaction.setCreatedBy(createdBy);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setReferenceType("MANUAL");

        inventoryTransactionRepository.save(transaction);

        return true;
    }

    /**
     * Xuất kho (giảm stock)
     */
    @Transactional
    public boolean exportStock(Long bookId, int quantity, String reason, String note, User createdBy) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            return false;
        }

        Book book = bookOpt.get();
        
        // Kiểm tra sách có bị xóa không
        if (book.getDeleted() != null && book.getDeleted()) {
            return false;
        }

        // Kiểm tra số lượng
        if (quantity <= 0) {
            return false;
        }

        int stockBefore = book.getStock();
        
        // Kiểm tra stock có đủ không
        if (stockBefore < quantity) {
            return false;
        }

        int stockAfter = stockBefore - quantity;

        // Cập nhật stock
        book.setStock(stockAfter);
        bookRepository.save(book);

        // Lưu lịch sử
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setBook(book);
        transaction.setTransactionType("EXPORT");
        transaction.setQuantity(-quantity); // Lưu số âm để dễ tính toán
        transaction.setStockBefore(stockBefore);
        transaction.setStockAfter(stockAfter);
        transaction.setReason(reason);
        transaction.setNote(note);
        transaction.setCreatedBy(createdBy);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setReferenceType("MANUAL");

        inventoryTransactionRepository.save(transaction);

        return true;
    }

    /**
     * Điều chỉnh kho (set stock về giá trị mới)
     */
    @Transactional
    public boolean adjustStock(Long bookId, int newStock, String reason, String note, User createdBy) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            return false;
        }

        Book book = bookOpt.get();
        
        // Kiểm tra sách có bị xóa không
        if (book.getDeleted() != null && book.getDeleted()) {
            return false;
        }

        // Kiểm tra số lượng
        if (newStock < 0) {
            return false;
        }

        int stockBefore = book.getStock();
        int quantity = newStock - stockBefore; // Chênh lệch

        // Nếu không có thay đổi, không cần lưu
        if (quantity == 0) {
            return true;
        }

        // Cập nhật stock
        book.setStock(newStock);
        bookRepository.save(book);

        // Lưu lịch sử
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setBook(book);
        transaction.setTransactionType("ADJUSTMENT");
        transaction.setQuantity(quantity); // Có thể dương hoặc âm
        transaction.setStockBefore(stockBefore);
        transaction.setStockAfter(newStock);
        transaction.setReason(reason);
        transaction.setNote(note);
        transaction.setCreatedBy(createdBy);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setReferenceType("MANUAL");

        inventoryTransactionRepository.save(transaction);

        return true;
    }

    /**
     * Lưu lịch sử khi xuất kho từ order (tự động)
     */
    @Transactional
    public void recordOrderExport(Book book, int quantity, Long orderId, User createdBy) {
        int stockBefore = book.getStock();
        int stockAfter = stockBefore - quantity;

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setBook(book);
        transaction.setTransactionType("EXPORT");
        transaction.setQuantity(-quantity);
        transaction.setStockBefore(stockBefore);
        transaction.setStockAfter(stockAfter);
        transaction.setReason("Order processed");
        transaction.setNote("Stock deducted for order #" + orderId);
        transaction.setCreatedBy(createdBy);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setReferenceType("ORDER");
        transaction.setReferenceId(orderId);

        inventoryTransactionRepository.save(transaction);
    }

    /**
     * Lấy lịch sử với phân trang
     */
    public Page<InventoryTransaction> getInventoryHistory(
            Long bookId,
            String transactionType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size) {
        
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (bookId != null && transactionType != null && !transactionType.isEmpty()) {
            return inventoryTransactionRepository.findByBookBookIdAndTransactionTypeOrderByCreatedAtDesc(
                    bookId, transactionType, pageable);
        } else if (bookId != null) {
            return inventoryTransactionRepository.findByBookBookIdOrderByCreatedAtDesc(bookId, pageable);
        } else if (transactionType != null && !transactionType.isEmpty()) {
            return inventoryTransactionRepository.findByTransactionTypeOrderByCreatedAtDesc(transactionType, pageable);
        } else if (startDate != null && endDate != null) {
            return inventoryTransactionRepository.findByDateRange(startDate, endDate, pageable);
        } else {
            return inventoryTransactionRepository.findAllWithDetails(pageable);
        }
    }

    /**
     * Lấy transaction theo ID
     */
    public Optional<InventoryTransaction> getTransactionById(Long transactionId) {
        return inventoryTransactionRepository.findById(transactionId);
    }

    /**
     * Lấy danh sách sách hết hàng (stock = 0)
     */
    public List<Book> getOutOfStockBooks() {
        return bookRepository.findAll().stream()
                .filter(book -> (book.getDeleted() == null || !book.getDeleted()) && book.getStock() == 0)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Lấy danh sách sách có stock thấp (0 < stock < threshold)
     * @param threshold Ngưỡng stock thấp (mặc định 10)
     */
    public List<Book> getLowStockBooks(int threshold) {
        return bookRepository.findAll().stream()
                .filter(book -> (book.getDeleted() == null || !book.getDeleted()) 
                        && book.getStock() > 0 
                        && book.getStock() < threshold)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Lấy danh sách sách có stock thấp (mặc định threshold = 10)
     */
    public List<Book> getLowStockBooks() {
        return getLowStockBooks(10);
    }

    /**
     * Lấy tổng số sách hết hàng
     */
    public long getOutOfStockCount() {
        return bookRepository.findAll().stream()
                .filter(book -> (book.getDeleted() == null || !book.getDeleted()) && book.getStock() == 0)
                .count();
    }

    /**
     * Lấy tổng số sách có stock thấp
     */
    public long getLowStockCount(int threshold) {
        return bookRepository.findAll().stream()
                .filter(book -> (book.getDeleted() == null || !book.getDeleted()) 
                        && book.getStock() > 0 
                        && book.getStock() < threshold)
                .count();
    }

    /**
     * Lấy tổng số sách có stock thấp (mặc định threshold = 10)
     */
    public long getLowStockCount() {
        return getLowStockCount(10);
    }
}

