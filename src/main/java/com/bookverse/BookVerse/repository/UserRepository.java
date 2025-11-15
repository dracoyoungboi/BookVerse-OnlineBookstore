package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    // Load user with role using JOIN FETCH to avoid lazy loading issues
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsernameWithRole(@Param("username") String username);
    
    // Handle duplicate emails by selecting the most recent user (highest user_id)
    // Returns List to handle cases where multiple users have the same email
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.email = :email ORDER BY u.userId DESC")
    java.util.List<User> findAllByEmailWithRole(@Param("email") String email);
    
    // Find all users that are not deleted (for admin panel)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE (u.deleted = false OR u.deleted IS NULL) ORDER BY u.userId DESC")
    java.util.List<User> findAllActiveUsersWithRole();
    
    // Find all users with role (including both active and inactive users)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role ORDER BY u.userId DESC")
    java.util.List<User> findAllUsersWithRole();
    
    // Find user by ID that is not deleted
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.userId = :userId AND (u.deleted = false OR u.deleted IS NULL)")
    Optional<User> findActiveUserByIdWithRole(@Param("userId") Long userId);
    
    // Find user by ID with role (including inactive users)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.userId = :userId")
    Optional<User> findByIdWithRole(@Param("userId") Long userId);
    
    // Find all users with pagination (for admin panel) - using EntityGraph or default fetch
    // Note: Sorting is handled by Pageable, not hardcoded in query
    @Query(value = "SELECT u FROM User u LEFT JOIN u.role",
           countQuery = "SELECT COUNT(u) FROM User u")
    Page<User> findAllUsersWithRolePaged(Pageable pageable);
    
    // Search users by username, email, or fullName with pagination
    // Note: Sorting is handled by Pageable, not hardcoded in query
    @Query(value = "SELECT DISTINCT u FROM User u LEFT JOIN u.role WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))",
           countQuery = "SELECT COUNT(DISTINCT u) FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsersWithRole(@Param("search") String search, Pageable pageable);
}
