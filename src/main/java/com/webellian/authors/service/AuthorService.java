package com.webellian.authors.service;

import com.webellian.authors.api.dto.AuthorResponse;
import com.webellian.authors.api.dto.AuthorWorkResponse;
import com.webellian.authors.client.OpenLibraryAuthor;
import com.webellian.authors.client.OpenLibraryClient;
import com.webellian.authors.client.OpenLibraryWork;
import com.webellian.authors.domain.Author;
import com.webellian.authors.domain.AuthorWork;
import com.webellian.authors.repository.AuthorRepository;
import com.webellian.authors.repository.AuthorWorkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorWorkRepository authorWorkRepository;
    private final OpenLibraryClient openLibraryClient;

    public AuthorService(
            AuthorRepository authorRepository,
            AuthorWorkRepository authorWorkRepository,
            OpenLibraryClient openLibraryClient
    ) {
        this.authorRepository = authorRepository;
        this.authorWorkRepository = authorWorkRepository;
        this.openLibraryClient = openLibraryClient;
    }

    @Transactional
    public List<AuthorResponse> searchAuthors(String name) {
        String searchTerm = name.trim();
        List<Author> cachedAuthors = authorRepository.findByNameContainingIgnoreCaseOrderByNameAsc(searchTerm);
        if (!cachedAuthors.isEmpty()) {
            return cachedAuthors.stream().map(AuthorResponse::from).toList();
        }

        List<Author> authors = new ArrayList<>();
        for (OpenLibraryAuthor author : openLibraryClient.searchAuthors(searchTerm)) {
            if (hasText(author.key()) && hasText(author.name())) {
                authors.add(saveAuthor(author));
            }
        }

        return authors.stream().map(AuthorResponse::from).toList();
    }

    @Transactional
    public List<AuthorWorkResponse> searchWorks(String authorId) {
        String normalizedAuthorId = normalizeOpenLibraryId(authorId);
        List<AuthorWork> cachedWorks = authorWorkRepository.findByAuthorIdOrderByTitleAsc(normalizedAuthorId);
        if (!cachedWorks.isEmpty()) {
            return cachedWorks.stream().map(AuthorWorkResponse::from).toList();
        }

        Author author = findOrCreateAuthor(normalizedAuthorId);
        List<AuthorWork> works = new ArrayList<>();
        for (OpenLibraryWork work : openLibraryClient.searchWorks(normalizedAuthorId)) {
            if (hasText(work.key()) && hasText(work.title())) {
                works.add(saveWork(author, work));
            }
        }

        return works.stream().map(AuthorWorkResponse::from).toList();
    }

    private Author saveAuthor(OpenLibraryAuthor result) {
        String authorId = normalizeOpenLibraryId(result.key());
        Author author = authorRepository.findById(authorId)
                .orElseGet(() -> new Author(authorId, result.name()));
        author.updateName(result.name());
        return authorRepository.save(author);
    }

    private Author findOrCreateAuthor(String authorId) {
        Optional<Author> cachedAuthor = authorRepository.findById(authorId);
        if (cachedAuthor.isPresent()) {
            return cachedAuthor.get();
        }

        // Works need an author row even when Open Library has no author details.
        String name = openLibraryClient.getAuthor(authorId)
                .filter(author -> hasText(author.name()))
                .map(author -> author.name())
                .orElse(authorId);
        return authorRepository.save(new Author(authorId, name));
    }

    private AuthorWork saveWork(Author author, OpenLibraryWork result) {
        String workId = normalizeOpenLibraryId(result.key());
        AuthorWork work = authorWorkRepository.findByAuthorIdAndWorkId(author.getId(), workId)
                .orElseGet(() -> new AuthorWork(workId, author, result.title()));
        work.updateTitle(result.title());
        return authorWorkRepository.save(work);
    }

    private String normalizeOpenLibraryId(String key) {
        return Objects.requireNonNull(key, "Open Library key is required")
                .replace("/authors/", "")
                .replace("/works/", "")
                .trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
