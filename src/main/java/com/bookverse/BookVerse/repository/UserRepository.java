package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.User;
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
}
