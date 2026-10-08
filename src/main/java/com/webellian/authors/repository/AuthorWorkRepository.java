package com.webellian.authors.repository;

import com.webellian.authors.domain.AuthorWork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorWorkRepository extends JpaRepository<AuthorWork, Long> {

    List<AuthorWork> findByAuthorIdOrderByTitleAsc(String authorId);

    Optional<AuthorWork> findByAuthorIdAndWorkId(String authorId, String workId);
}
