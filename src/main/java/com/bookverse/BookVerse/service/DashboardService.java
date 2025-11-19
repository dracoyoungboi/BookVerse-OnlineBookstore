package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.Order;
import com.bookverse.BookVerse.entity.OrderItem;
import com.bookverse.BookVerse.entity.User;
import com.bookverse.BookVerse.repository.BookRepository;
import com.bookverse.BookVerse.repository.OrderItemRepository;
import com.bookverse.BookVerse.repository.OrderRepository;
import com.bookverse.BookVerse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    /**
     * Get total number of products (books)
     */
    public long getTotalProducts() {
        return bookRepository.findAll().stream()
                .filter(book -> book.getDeleted() == null || !book.getDeleted())
                .count();
    }

    /**
     * Get total number of customers (users with USER role)
     */
    public long getTotalCustomers() {
        return userRepository.findAll().stream()
                .filter(user -> {
                    if (user.getDeleted() != null && user.getDeleted()) {
                        return false;
                    }
                    if (user.getRole() == null) {
                        return false;
                    }
                    String roleName = user.getRole().getName();
                    return roleName != null && !roleName.toUpperCase().equals("ADMIN");
                })
                .count();
    }

    /**
     * Get total number of orders
     */
    public long getTotalOrders() {
        return orderRepository.count();
    }

    /**
     * Get total sales amount
     */
    public Double getTotalSales() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .filter(order -> order.getTotalAmount() != null)
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }

    /**
     * Get total sales for a specific period
     */
    public Double getTotalSales(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .filter(order -> {
                    if (order.getCreatedAt() == null) return false;
                    return !order.getCreatedAt().isBefore(startDate) && !order.getCreatedAt().isAfter(endDate);
                })
                .filter(order -> order.getTotalAmount() != null)
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }

    /**
     * Get recent orders (last 5) with order items loaded
     */
    public List<Order> getRecentOrders(int limit) {
        return orderRepository.findAll().stream()
                .map(order -> {
                    // Load order with items
                    return orderRepository.findByIdWithUserAndItems(order.getOrderId())
                            .orElse(order);
                })
                .sorted((o1, o2) -> {
                    if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
                    if (o1.getCreatedAt() == null) return 1;
                    if (o2.getCreatedAt() == null) return -1;
                    return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get top selling products
     */
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        List<OrderItem> allOrderItems = orderItemRepository.findAll();
        
        // Group by book and sum quantities
        Map<Long, Integer> bookQuantities = new HashMap<>();
        Map<Long, Book> bookMap = new HashMap<>();
        
        for (OrderItem item : allOrderItems) {
            if (item.getBook() != null && item.getBook().getBookId() != null) {
                Long bookId = item.getBook().getBookId();
                bookQuantities.put(bookId, bookQuantities.getOrDefault(bookId, 0) + item.getQuantity());
                bookMap.put(bookId, item.getBook());
            }
        }
        
        // Sort by quantity and get top N
        return bookQuantities.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> productData = new HashMap<>();
                    Book book = bookMap.get(entry.getKey());
                    if (book != null) {
                        productData.put("book", book);
                        productData.put("totalSold", entry.getValue());
                        
                        // Calculate total orders for this book
                        long totalOrders = allOrderItems.stream()
                                .filter(item -> item.getBook() != null && 
                                               item.getBook().getBookId().equals(entry.getKey()))
                                .map(OrderItem::getOrder)
                                .filter(order -> order != null)
                                .distinct()
                                .count();
                        productData.put("totalOrders", totalOrders);
                    }
                    return productData;
                })
                .filter(map -> map.get("book") != null)
                .collect(Collectors.toList());
    }

    /**
     * Get stock report (top 5 by stock level)
     */
    public List<Book> getStockReport() {
        return bookRepository.findAll().stream()
                .filter(book -> book.getDeleted() == null || !book.getDeleted())
                .sorted((b1, b2) -> Integer.compare(b2.getStock(), b1.getStock()))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Get statistics for period comparison
     */
    public Map<String, Object> getPeriodStatistics(String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime previousStartDate;
        
        switch (period.toLowerCase()) {
            case "today":
                startDate = now.toLocalDate().atStartOfDay();
                previousStartDate = startDate.minusDays(1);
                break;
            case "weekly":
                startDate = now.minusWeeks(1);
                previousStartDate = startDate.minusWeeks(1);
                break;
            case "monthly":
                startDate = now.minusMonths(1);
                previousStartDate = startDate.minusMonths(1);
                break;
            case "yearly":
                startDate = now.minusYears(1);
                previousStartDate = startDate.minusYears(1);
                break;
            default:
                startDate = now.minusMonths(1);
                previousStartDate = startDate.minusMonths(1);
        }
        
        LocalDateTime previousEndDate = startDate;
        
        // Current period stats
        long currentOrders = orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt() != null && 
                                !order.getCreatedAt().isBefore(startDate))
                .count();
        
        Double currentSales = getTotalSales(startDate, now);
        
        // Previous period stats
        long previousOrders = orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt() != null && 
                                !order.getCreatedAt().isBefore(previousStartDate) &&
                                order.getCreatedAt().isBefore(previousEndDate))
                .count();
        
        Double previousSales = getTotalSales(previousStartDate, previousEndDate);
        
        // Calculate changes
        long orderChange = currentOrders - previousOrders;
        double orderChangePercent = previousOrders > 0 ? 
                ((double) orderChange / previousOrders) * 100 : 0;
        
        double salesChange = (currentSales != null ? currentSales : 0) - (previousSales != null ? previousSales : 0);
        double salesChangePercent = previousSales != null && previousSales > 0 ? 
                (salesChange / previousSales) * 100 : 0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("currentOrders", currentOrders);
        stats.put("currentSales", currentSales != null ? currentSales : 0.0);
        stats.put("previousOrders", previousOrders);
        stats.put("previousSales", previousSales != null ? previousSales : 0.0);
        stats.put("orderChange", orderChange);
        stats.put("orderChangePercent", orderChangePercent);
        stats.put("salesChange", salesChange);
        stats.put("salesChangePercent", salesChangePercent);
        
        return stats;
    }

    /**
     * Get order statistics by status
     */
    public Map<String, Long> getOrderStatisticsByStatus() {
        List<Order> orders = orderRepository.findAll();
        Map<String, Long> statusCount = new HashMap<>();
        
        for (Order order : orders) {
            String status = order.getStatus() != null ? order.getStatus() : "unknown";
            statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1);
        }
        
        return statusCount;
    }

    /**
     * Get product increase/decrease for this week
     */
    public long getProductChangeThisWeek() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        return bookRepository.findAll().stream()
                .filter(book -> book.getDeleted() == null || !book.getDeleted())
                .filter(book -> book.getCreatedAt() != null && !book.getCreatedAt().isBefore(weekAgo))
                .count();
    }

    /**
     * Get customer increase/decrease for this week
     */
    public long getCustomerChangeThisWeek() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        return userRepository.findAll().stream()
                .filter(user -> {
                    if (user.getDeleted() != null && user.getDeleted()) {
                        return false;
                    }
                    if (user.getRole() == null) {
                        return false;
                    }
                    String roleName = user.getRole().getName();
                    return roleName != null && !roleName.toUpperCase().equals("ADMIN");
                })
                .filter(user -> user.getCreatedAt() != null && !user.getCreatedAt().isBefore(weekAgo))
                .count();
    }

    /**
     * Get order increase/decrease for this week
     */
    public long getOrderChangeThisWeek() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        return orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().isBefore(weekAgo))
                .count();
    }

    /**
     * Get processing orders for transactions
     */
    public List<Order> getProcessingOrders(int limit) {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() != null && order.getStatus().equalsIgnoreCase("processing"))
                .map(order -> {
                    // Load order with items
                    return orderRepository.findByIdWithUserAndItems(order.getOrderId())
                            .orElse(order);
                })
                .sorted((o1, o2) -> {
                    if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
                    if (o1.getCreatedAt() == null) return 1;
                    if (o2.getCreatedAt() == null) return -1;
                    return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get top customers by order count (only processing or shipped orders)
     */
    public List<Map<String, Object>> getTopCustomers(int limit) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> {
                    String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
                    return status.equals("processing") || status.equals("shipped");
                })
                .filter(order -> order.getUser() != null)
                .collect(Collectors.toList());
        
        // Group by user and count orders
        Map<Long, Integer> userOrderCount = new HashMap<>();
        Map<Long, User> userMap = new HashMap<>();
        
        for (Order order : orders) {
            Long userId = order.getUser().getUserId();
            userOrderCount.put(userId, userOrderCount.getOrDefault(userId, 0) + 1);
            userMap.put(userId, order.getUser());
        }
        
        // Sort by order count and get top N
        return userOrderCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> customerData = new HashMap<>();
                    User user = userMap.get(entry.getKey());
                    if (user != null) {
                        customerData.put("user", user);
                        customerData.put("orderCount", entry.getValue());
                    }
                    return customerData;
                })
                .filter(map -> map.get("user") != null)
                .collect(Collectors.toList());
    }

    /**
     * Get order status distribution for pie chart (with period filter)
     */
    public Map<String, Long> getOrderStatusDistribution(String period) {
        List<Order> orders = orderRepository.findAll();
        
        // Filter by period if specified
        if (period != null && !period.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate;
            
            switch (period.toLowerCase()) {
                case "today":
                    startDate = now.toLocalDate().atStartOfDay();
                    break;
                case "weekly":
                    startDate = now.minusWeeks(1);
                    break;
                case "monthly":
                    startDate = now.minusMonths(1);
                    break;
                case "yearly":
                    startDate = now.minusYears(1);
                    break;
                default:
                    startDate = null;
            }
            
            if (startDate != null) {
                final LocalDateTime finalStartDate = startDate;
                orders = orders.stream()
                        .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().isBefore(finalStartDate))
                        .collect(Collectors.toList());
            }
        }
        
        Map<String, Long> statusDistribution = new HashMap<>();
        
        for (Order order : orders) {
            String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "unknown";
            statusDistribution.put(status, statusDistribution.getOrDefault(status, 0L) + 1);
        }
        
        return statusDistribution;
    }

    /**
     * Get revenue by category for pie chart (with period filter)
     */
    public Map<String, Double> getRevenueByCategory(String period) {
        List<Order> orders = orderRepository.findAll();
        
        // Filter by period if specified
        if (period != null && !period.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate;
            
            switch (period.toLowerCase()) {
                case "today":
                    startDate = now.toLocalDate().atStartOfDay();
                    break;
                case "weekly":
                    startDate = now.minusWeeks(1);
                    break;
                case "monthly":
                    startDate = now.minusMonths(1);
                    break;
                case "yearly":
                    startDate = now.minusYears(1);
                    break;
                default:
                    startDate = null;
            }
            
            if (startDate != null) {
                final LocalDateTime finalStartDate = startDate;
                orders = orders.stream()
                        .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().isBefore(finalStartDate))
                        .collect(Collectors.toList());
            }
        }
        
        Map<String, Double> revenueByCategory = new HashMap<>();
        
        for (Order order : orders) {
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    if (item.getBook() != null && item.getBook().getCategory() != null) {
                        String categoryName = item.getBook().getCategory().getName();
                        if (categoryName == null) {
                            categoryName = "Uncategorized";
                        }
                        Double itemTotal = item.getPrice() != null ? item.getPrice() * item.getQuantity() : 0.0;
                        revenueByCategory.put(categoryName, 
                                revenueByCategory.getOrDefault(categoryName, 0.0) + itemTotal);
                    }
                }
            }
        }
        
        return revenueByCategory;
    }

    /**
     * Parse country from address string
     * Supports both English and Vietnamese country names
     */
    private String parseCountryFromAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        
        String addressLower = address.toLowerCase().trim();
        
        // Map Vietnamese country names to English country codes
        Map<String, String> vietnameseToEnglish = new HashMap<>();
        vietnameseToEnglish.put("việt nam", "VN");
        vietnameseToEnglish.put("vietnam", "VN");
        vietnameseToEnglish.put("viet nam", "VN");
        vietnameseToEnglish.put("vn", "VN");
        
        vietnameseToEnglish.put("mỹ", "US");
        vietnameseToEnglish.put("hoa kỳ", "US");
        vietnameseToEnglish.put("usa", "US");
        vietnameseToEnglish.put("united states", "US");
        vietnameseToEnglish.put("united states of america", "US");
        
        vietnameseToEnglish.put("anh", "GB");
        vietnameseToEnglish.put("vương quốc anh", "GB");
        vietnameseToEnglish.put("uk", "GB");
        vietnameseToEnglish.put("united kingdom", "GB");
        vietnameseToEnglish.put("britain", "GB");
        
        vietnameseToEnglish.put("trung quốc", "CN");
        vietnameseToEnglish.put("china", "CN");
        
        vietnameseToEnglish.put("nhật bản", "JP");
        vietnameseToEnglish.put("japan", "JP");
        
        vietnameseToEnglish.put("úc", "AU");
        vietnameseToEnglish.put("australia", "AU");
        
        vietnameseToEnglish.put("đức", "DE");
        vietnameseToEnglish.put("germany", "DE");
        
        vietnameseToEnglish.put("pháp", "FR");
        vietnameseToEnglish.put("france", "FR");
        
        vietnameseToEnglish.put("canada", "CA");
        vietnameseToEnglish.put("ca-na-đa", "CA");
        
        vietnameseToEnglish.put("ấn độ", "IN");
        vietnameseToEnglish.put("india", "IN");
        
        // Check Vietnamese names first
        for (Map.Entry<String, String> entry : vietnameseToEnglish.entrySet()) {
            if (addressLower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Check English country names directly
        if (addressLower.contains("vietnam") || addressLower.contains("viet nam")) {
            return "VN";
        } else if (addressLower.contains("usa") || addressLower.contains("united states") || 
                   addressLower.contains("america") || addressLower.matches(".*\\bus\\b.*")) {
            return "US";
        } else if (addressLower.contains("uk") || addressLower.contains("united kingdom") || 
                   addressLower.contains("britain") || addressLower.contains("england")) {
            return "GB";
        } else if (addressLower.contains("china") || addressLower.contains("chinese")) {
            return "CN";
        } else if (addressLower.contains("japan") || addressLower.contains("japanese")) {
            return "JP";
        } else if (addressLower.contains("australia") || addressLower.contains("australian")) {
            return "AU";
        } else if (addressLower.contains("germany") || addressLower.contains("german")) {
            return "DE";
        } else if (addressLower.contains("france") || addressLower.contains("french")) {
            return "FR";
        } else if (addressLower.contains("canada") || addressLower.contains("canadian")) {
            return "CA";
        } else if (addressLower.contains("india") || addressLower.contains("indian")) {
            return "IN";
        }
        
        // Check for country codes (2-3 letters, usually at the end or separated)
        String[] parts = address.split("[,\\s]+");
        for (String part : parts) {
            String cleanPart = part.trim().toUpperCase();
            if (cleanPart.length() == 2 || cleanPart.length() == 3) {
                if (cleanPart.equals("VN") || cleanPart.equals("US") || cleanPart.equals("GB") || 
                    cleanPart.equals("CN") || cleanPart.equals("JP") || cleanPart.equals("AU") || 
                    cleanPart.equals("DE") || cleanPart.equals("FR") || cleanPart.equals("CA") || 
                    cleanPart.equals("IN")) {
                    return cleanPart;
                }
            }
        }
        
        return null;
    }

    /**
     * Get country distribution for world map from order addresses
     */
    public Map<String, Object> getCountryDistribution(String period) {
        List<Order> orders = orderRepository.findAll();
        
        // Filter by period if specified
        if (period != null && !period.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate;
            
            switch (period.toLowerCase()) {
                case "today":
                    startDate = now.toLocalDate().atStartOfDay();
                    break;
                case "weekly":
                    startDate = now.minusWeeks(1);
                    break;
                case "monthly":
                    startDate = now.minusMonths(1);
                    break;
                case "yearly":
                    startDate = now.minusYears(1);
                    break;
                default:
                    startDate = null;
            }
            
            if (startDate != null) {
                final LocalDateTime finalStartDate = startDate;
                orders = orders.stream()
                        .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().isBefore(finalStartDate))
                        .collect(Collectors.toList());
            }
        }
        
        // Parse country from order addresses
        Map<String, Integer> countryCount = new HashMap<>();
        
        for (Order order : orders) {
            if (order.getAddress() != null) {
                String countryCode = parseCountryFromAddress(order.getAddress());
                if (countryCode != null) {
                    countryCount.put(countryCode, countryCount.getOrDefault(countryCode, 0) + 1);
                }
            }
        }
        
        // Calculate total orders for percentage calculation
        int totalOrders = countryCount.values().stream().mapToInt(Integer::intValue).sum();
        
        // Country names, coordinates, and flags
        Map<String, Map<String, Object>> countryData = new HashMap<>();
        
        // USA
        Map<String, Object> usData = new HashMap<>();
        usData.put("name", "USA");
        usData.put("lat", 36.77);
        usData.put("lng", -119.41);
        usData.put("count", countryCount.getOrDefault("US", 0));
        usData.put("code", "US");
        usData.put("flag", "🇺🇸");
        usData.put("percentage", totalOrders > 0 ? (countryCount.getOrDefault("US", 0) * 100.0 / totalOrders) : 0.0);
        countryData.put("US", usData);
        
        // Vietnam
        Map<String, Object> vnData = new HashMap<>();
        vnData.put("name", "Vietnam");
        vnData.put("lat", 14.0583);
        vnData.put("lng", 108.2772);
        vnData.put("count", countryCount.getOrDefault("VN", 0));
        vnData.put("code", "VN");
        vnData.put("flag", "🇻🇳");
        vnData.put("percentage", totalOrders > 0 ? (countryCount.getOrDefault("VN", 0) * 100.0 / totalOrders) : 0.0);
        countryData.put("VN", vnData);
        
        // UK
        Map<String, Object> gbData = new HashMap<>();
        gbData.put("name", "UK");
        gbData.put("lat", 55.37);
        gbData.put("lng", -3.41);
        gbData.put("count", countryCount.getOrDefault("GB", 0));
        gbData.put("code", "GB");
        gbData.put("flag", "🇬🇧");
        gbData.put("percentage", totalOrders > 0 ? (countryCount.getOrDefault("GB", 0) * 100.0 / totalOrders) : 0.0);
        countryData.put("GB", gbData);
        
        // China
        Map<String, Object> cnData = new HashMap<>();
        cnData.put("name", "China");
        cnData.put("lat", 35.8617);
        cnData.put("lng", 104.1954);
        cnData.put("count", countryCount.getOrDefault("CN", 0));
        cnData.put("code", "CN");
        cnData.put("flag", "🇨🇳");
        cnData.put("percentage", totalOrders > 0 ? (countryCount.getOrDefault("CN", 0) * 100.0 / totalOrders) : 0.0);
        countryData.put("CN", cnData);
        
        // Japan
        Map<String, Object> jpData = new HashMap<>();
        jpData.put("name", "Japan");
        jpData.put("lat", 36.2048);
        jpData.put("lng", 138.2529);
        jpData.put("count", countryCount.getOrDefault("JP", 0));
        jpData.put("code", "JP");
        jpData.put("flag", "🇯🇵");
        jpData.put("percentage", totalOrders > 0 ? (countryCount.getOrDefault("JP", 0) * 100.0 / totalOrders) : 0.0);
        countryData.put("JP", jpData);
        
        // Australia
        Map<String, Object> auData = new HashMap<>();
        auData.put("name", "Australia");
        auData.put("lat", -25.2744);
        auData.put("lng", 133.7751);
        auData.put("count", countryCount.getOrDefault("AU", 0));
        auData.put("code", "AU");
        auData.put("flag", "🇦🇺");
        auData.put("percentage", totalOrders > 0 ? (countryCount.getOrDefault("AU", 0) * 100.0 / totalOrders) : 0.0);
        countryData.put("AU", auData);
        
        // Germany
        Map<String, Object> deData = new HashMap<>();
        deData.put("name", "Germany");
        deData.put("lat", 51.1657);
        deData.put("lng", 10.4515);
        deData.put("count", countryCount.getOrDefault("DE", 0));
        deData.put("code", "DE");
        deData.put("flag", "🇩🇪");
        deData.put("percentage", totalOrders > 0 ? (countryCount.getOrDefault("DE", 0) * 100.0 / totalOrders) : 0.0);
        countryData.put("DE", deData);
        
        // France
        Map<String, Object> frData = new HashMap<>();
        frData.put("name", "France");
        frData.put("lat", 46.2276);
        frData.put("lng", 2.2137);
        frData.put("count", countryCount.getOrDefault("FR", 0));
        frData.put("code", "FR");
        frData.put("flag", "🇫🇷");
        frData.put("percentage", totalOrders > 0 ? (countryCount.getOrDefault("FR", 0) * 100.0 / totalOrders) : 0.0);
        countryData.put("FR", frData);
        
        // Canada
        Map<String, Object> caData = new HashMap<>();
        caData.put("name", "Canada");
        caData.put("lat", 56.1304);
        caData.put("lng", -106.3468);
        caData.put("count", countryCount.getOrDefault("CA", 0));
        caData.put("code", "CA");
        caData.put("flag", "🇨🇦");
        caData.put("percentage", totalOrders > 0 ? (countryCount.getOrDefault("CA", 0) * 100.0 / totalOrders) : 0.0);
        countryData.put("CA", caData);
        
        // India
        Map<String, Object> inData = new HashMap<>();
        inData.put("name", "India");
        inData.put("lat", 20.5937);
        inData.put("lng", 78.9629);
        inData.put("count", countryCount.getOrDefault("IN", 0));
        inData.put("code", "IN");
        inData.put("flag", "🇮🇳");
        inData.put("percentage", totalOrders > 0 ? (countryCount.getOrDefault("IN", 0) * 100.0 / totalOrders) : 0.0);
        countryData.put("IN", inData);
        
        // Sort by count descending and get top countries
        List<Map<String, Object>> sortedCountries = countryData.values().stream()
                .filter(data -> ((Integer) data.get("count")) > 0)
                .sorted((a, b) -> Integer.compare((Integer) b.get("count"), (Integer) a.get("count")))
                .limit(10)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("countryData", countryData);
        result.put("countryCount", countryCount);
        result.put("sortedCountries", sortedCountries);
        result.put("totalOrders", totalOrders);
        
        return result;
    }

    /**
     * Get monthly order statistics for the last 12 months
     */
    public Map<String, Object> getMonthlyOrderStatistics() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> monthlyOrders = new HashMap<>();
        Map<String, String> monthLabels = new HashMap<>();
        
        // Initialize all 12 months with 0
        for (int i = 11; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            
            String monthKey = monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue());
            String monthLabel = monthStart.getMonth().toString().substring(0, 1) + 
                               monthStart.getMonth().toString().substring(1).toLowerCase() + " " + 
                               monthStart.getYear();
            
            monthLabels.put(monthKey, monthLabel);
            monthlyOrders.put(monthKey, 0L);
        }
        
        // Get all orders and count by month
        List<Order> allOrders = orderRepository.findAll();
        for (Order order : allOrders) {
            if (order.getCreatedAt() != null) {
                LocalDateTime orderDate = order.getCreatedAt();
                String monthKey = orderDate.getYear() + "-" + String.format("%02d", orderDate.getMonthValue());
                
                // Check if this month is within the last 12 months
                if (monthlyOrders.containsKey(monthKey)) {
                    monthlyOrders.put(monthKey, monthlyOrders.get(monthKey) + 1);
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("monthlyOrders", monthlyOrders);
        result.put("monthLabels", monthLabels);
        
        return result;
    }

    /**
     * Get total revenue for current month
     */
    public Double getCurrentMonthRevenue() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
        
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt() != null && 
                        !order.getCreatedAt().isBefore(monthStart) && 
                        !order.getCreatedAt().isAfter(monthEnd))
                .collect(Collectors.toList());
        
        return orders.stream()
                .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
                .sum();
    }

    /**
     * Get revenue by period (today, monthly, yearly)
     */
    public Map<String, Object> getRevenueByPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        String label;
        
        switch (period != null ? period.toLowerCase() : "monthly") {
            case "today":
                startDate = now.toLocalDate().atStartOfDay();
                label = "Today's total";
                break;
            case "monthly":
                startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                label = "Current month total";
                break;
            case "yearly":
                startDate = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                label = "Current year total";
                break;
            default:
                startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                label = "Current month total";
        }
        
        LocalDateTime endDate = now;
        
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt() != null && 
                        !order.getCreatedAt().isBefore(startDate) && 
                        !order.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());
        
        Double revenue = orders.stream()
                .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
                .sum();
        
        Map<String, Object> result = new HashMap<>();
        result.put("revenue", revenue != null ? revenue : 0.0);
        result.put("label", label);
        
        return result;
    }
}

