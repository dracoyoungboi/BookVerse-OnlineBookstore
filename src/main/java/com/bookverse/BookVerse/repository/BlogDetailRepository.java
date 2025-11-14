package com.bookverse.BookVerse.repository;

import com.bookverse.BookVerse.entity.BlogDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogDetailRepository extends JpaRepository<BlogDetail, Long> {
}
