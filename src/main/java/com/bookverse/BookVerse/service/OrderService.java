package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.Order;
import com.bookverse.BookVerse.entity.OrderItem;
import com.bookverse.BookVerse.repository.BookRepository;
import com.bookverse.BookVerse.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BookRepository bookRepository;

    /**
     * Process payment: Change order status from pending to processing and deduct stock
     * @param orderId Order ID
     * @return true if successful, false otherwise
     */
    @Transactional
    public boolean processPayment(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();
        
        // Only process pending orders
        if (!"pending".equals(order.getStatus())) {
            return false;
        }

        // Check stock availability and deduct stock
        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            return false;
        }

        // First, check if all items have enough stock
        for (OrderItem item : orderItems) {
            if (item.getBook() == null || item.getBook().getBookId() == null) {
                return false;
            }
            
            // Reload book to ensure we have the latest stock
            Optional<Book> bookOpt = bookRepository.findById(item.getBook().getBookId());
            if (bookOpt.isEmpty()) {
                return false; // Book not found
            }
            
            Book book = bookOpt.get();
            int requestedQuantity = item.getQuantity();
            int availableStock = book.getStock();
            
            if (availableStock < requestedQuantity) {
                return false; // Not enough stock
            }
        }

        // Deduct stock for all items
        for (OrderItem item : orderItems) {
            if (item.getBook() == null || item.getBook().getBookId() == null) {
                continue; // Skip if book is null
            }
            
            // Reload book to ensure we have the latest stock
            Optional<Book> bookOpt = bookRepository.findById(item.getBook().getBookId());
            if (bookOpt.isEmpty()) {
                continue; // Skip if book not found
            }
            
            Book book = bookOpt.get();
            int requestedQuantity = item.getQuantity();
            int currentStock = book.getStock();
            book.setStock(currentStock - requestedQuantity);
            bookRepository.save(book);
        }

        // Update order status to processing
        order.setStatus("processing");
        orderRepository.save(order);

        return true;
    }

    /**
     * Get order with user and items
     */
    public Optional<Order> getOrderWithDetails(Long orderId) {
        return orderRepository.findByIdWithUserAndItems(orderId);
    }
}

