package com.webellian.authors.repository;

import com.webellian.authors.domain.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, String> {

    List<Author> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
