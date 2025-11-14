package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Page<Blog> findAll(Pageable pageable);
    Page<Blog> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author, Pageable pageable);

    @EntityGraph(attributePaths = {"blogDetails"})
    List<Blog> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"blogDetails"})
    Optional<Blog> findWithDetailsByBlogId(Long blogId);
}
