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
        List<Author> cachedAuthors = authorRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name.trim());
        if (!cachedAuthors.isEmpty()) {
            return cachedAuthors.stream().map(AuthorResponse::from).toList();
        }

        List<Author> authors = openLibraryClient.searchAuthors(name.trim()).stream()
                .filter(author -> hasText(author.key()) && hasText(author.name()))
                .map(this::upsertAuthor)
                .toList();

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
        List<AuthorWork> works = openLibraryClient.searchWorks(normalizedAuthorId).stream()
                .filter(work -> hasText(work.key()) && hasText(work.title()))
                .map(work -> upsertWork(author, work))
                .toList();

        return works.stream().map(AuthorWorkResponse::from).toList();
    }

    private Author upsertAuthor(OpenLibraryAuthor openLibraryAuthor) {
        String authorId = normalizeOpenLibraryId(openLibraryAuthor.key());
        Author author = authorRepository.findById(authorId)
                .orElseGet(() -> new Author(authorId, openLibraryAuthor.name()));
        author.updateName(openLibraryAuthor.name());
        return authorRepository.save(author);
    }

    private Author findOrCreateAuthor(String authorId) {
        Optional<Author> cachedAuthor = authorRepository.findById(authorId);
        if (cachedAuthor.isPresent()) {
            return cachedAuthor.get();
        }

        return openLibraryClient.getAuthor(authorId)
                .filter(author -> hasText(author.name()))
                .map(author -> authorRepository.save(new Author(authorId, author.name())))
                .orElseGet(() -> authorRepository.save(new Author(authorId, authorId)));
    }

    private AuthorWork upsertWork(Author author, OpenLibraryWork openLibraryWork) {
        String workId = normalizeOpenLibraryId(openLibraryWork.key());
        AuthorWork work = authorWorkRepository.findByAuthorIdAndWorkId(author.getId(), workId)
                .orElseGet(() -> new AuthorWork(workId, author, openLibraryWork.title()));
        work.updateTitle(openLibraryWork.title());
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
