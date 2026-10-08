package com.webellian.authors.service;

import com.webellian.authors.api.dto.AuthorResponse;
import com.webellian.authors.api.dto.AuthorWorkResponse;
import com.webellian.authors.client.OpenLibraryAuthor;
import com.webellian.authors.client.OpenLibraryAuthorDetails;
import com.webellian.authors.client.OpenLibraryClient;
import com.webellian.authors.client.OpenLibraryWork;
import com.webellian.authors.config.OpenLibraryProperties;
import com.webellian.authors.domain.Author;
import com.webellian.authors.domain.AuthorWork;
import com.webellian.authors.repository.AuthorRepository;
import com.webellian.authors.repository.AuthorWorkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthorServiceIntegrationTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorWorkRepository authorWorkRepository;

    @Autowired
    private FakeOpenLibraryClient openLibraryClient;

    @BeforeEach
    void setUp() {
        authorWorkRepository.deleteAll();
        authorRepository.deleteAll();
        openLibraryClient.reset();
    }

    @Test
    void searchesAuthorsFromOpenLibraryWhenCacheIsEmptyAndThenUsesCache() {
        openLibraryClient.authors = List.of(new OpenLibraryAuthor("OL23919A", "J. K. Rowling"));

        List<AuthorResponse> firstResponse = authorService.searchAuthors("rowling");
        List<AuthorResponse> secondResponse = authorService.searchAuthors("rowling");

        assertThat(firstResponse).containsExactly(new AuthorResponse("OL23919A", "J. K. Rowling"));
        assertThat(secondResponse).containsExactly(new AuthorResponse("OL23919A", "J. K. Rowling"));
        assertThat(authorRepository.findById("OL23919A")).isPresent();
        assertThat(openLibraryClient.authorSearches).containsExactly("rowling");
    }

    @Test
    void searchesWorksFromOpenLibraryWhenCacheIsEmptyAndThenUsesCache() {
        openLibraryClient.authorDetails = Optional.of(new OpenLibraryAuthorDetails("/authors/OL23919A", "J. K. Rowling"));
        openLibraryClient.works = List.of(new OpenLibraryWork("/works/OL82563W", "Harry Potter and the Philosopher's Stone"));

        List<AuthorWorkResponse> firstResponse = authorService.searchWorks("OL23919A");
        List<AuthorWorkResponse> secondResponse = authorService.searchWorks("OL23919A");

        assertThat(firstResponse).containsExactly(new AuthorWorkResponse("OL82563W", "Harry Potter and the Philosopher's Stone"));
        assertThat(secondResponse).containsExactly(new AuthorWorkResponse("OL82563W", "Harry Potter and the Philosopher's Stone"));
        assertThat(authorWorkRepository.findByAuthorIdOrderByTitleAsc("OL23919A")).hasSize(1);
        assertThat(openLibraryClient.authorLookups).containsExactly("OL23919A");
        assertThat(openLibraryClient.workSearches).containsExactly("OL23919A");
    }

    @Test
    void readsWorksFromCacheWhenAvailable() {
        Author author = authorRepository.save(new Author("OL23919A", "J. K. Rowling"));
        authorWorkRepository.save(new AuthorWork("OL82563W", author, "Harry Potter and the Philosopher's Stone"));

        List<AuthorWorkResponse> response = authorService.searchWorks("OL23919A");

        assertThat(response).containsExactly(new AuthorWorkResponse("OL82563W", "Harry Potter and the Philosopher's Stone"));
        assertThat(openLibraryClient.authorLookups).isEmpty();
        assertThat(openLibraryClient.workSearches).isEmpty();
    }

    @Test
    void trimsAuthorSearchAndSkipsIncompleteResults() {
        openLibraryClient.authors = List.of(
                new OpenLibraryAuthor("/authors/OL23919A", "J. K. Rowling"),
                new OpenLibraryAuthor(null, "Missing ID"),
                new OpenLibraryAuthor("OL1A", " "),
                new OpenLibraryAuthor("", "Empty ID"),
                new OpenLibraryAuthor("OL2A", null));

        List<AuthorResponse> response = authorService.searchAuthors("  rowling  ");

        assertThat(response).containsExactly(new AuthorResponse("OL23919A", "J. K. Rowling"));
        assertThat(authorRepository.count()).isEqualTo(1);
        assertThat(openLibraryClient.authorSearches).containsExactly("rowling");
    }

    @Test
    void savesWorksWithoutAuthorDetailsAndSkipsIncompleteResults() {
        openLibraryClient.works = List.of(
                new OpenLibraryWork("/works/OL82563W", "Harry Potter and the Philosopher's Stone"),
                new OpenLibraryWork(null, "Missing ID"),
                new OpenLibraryWork("OL1W", " "),
                new OpenLibraryWork("", "Empty ID"),
                new OpenLibraryWork("OL2W", null));

        List<AuthorWorkResponse> response = authorService.searchWorks(" /authors/OL23919A ");

        assertThat(response).containsExactly(
                new AuthorWorkResponse("OL82563W", "Harry Potter and the Philosopher's Stone"));
        assertThat(authorRepository.findById("OL23919A")).get()
                .extracting(Author::getName).isEqualTo("OL23919A");
        assertThat(authorWorkRepository.count()).isEqualTo(1);
        assertThat(openLibraryClient.authorLookups).containsExactly("OL23919A");
        assertThat(openLibraryClient.workSearches).containsExactly("OL23919A");
    }

    @Test
    void usesExistingAuthorWhenFetchingWorks() {
        authorRepository.save(new Author("OL23919A", "J. K. Rowling"));
        openLibraryClient.works = List.of(new OpenLibraryWork("/works/OL82563W", "Harry Potter"));

        List<AuthorWorkResponse> response = authorService.searchWorks("OL23919A");

        assertThat(response).containsExactly(new AuthorWorkResponse("OL82563W", "Harry Potter"));
        assertThat(openLibraryClient.authorLookups).isEmpty();
        assertThat(openLibraryClient.workSearches).containsExactly("OL23919A");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FakeOpenLibraryClient fakeOpenLibraryClient() {
            return new FakeOpenLibraryClient();
        }
    }

    static class FakeOpenLibraryClient extends OpenLibraryClient {

        private List<OpenLibraryAuthor> authors = List.of();
        private Optional<OpenLibraryAuthorDetails> authorDetails = Optional.empty();
        private List<OpenLibraryWork> works = List.of();
        private final List<String> authorSearches = new ArrayList<>();
        private final List<String> authorLookups = new ArrayList<>();
        private final List<String> workSearches = new ArrayList<>();

        FakeOpenLibraryClient() {
            super(WebClient.builder().baseUrl("https://openlibrary.org").build(),
                    new OpenLibraryProperties("https://openlibrary.org", 100));
        }

        @Override
        public List<OpenLibraryAuthor> searchAuthors(String name) {
            authorSearches.add(name);
            return authors;
        }

        @Override
        public Optional<OpenLibraryAuthorDetails> getAuthor(String authorId) {
            authorLookups.add(authorId);
            return authorDetails;
        }

        @Override
        public List<OpenLibraryWork> searchWorks(String authorId) {
            workSearches.add(authorId);
            return works;
        }

        void reset() {
            authors = List.of();
            authorDetails = Optional.empty();
            works = List.of();
            authorSearches.clear();
            authorLookups.clear();
            workSearches.clear();
        }
    }
}
