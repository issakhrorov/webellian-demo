package com.webellian.authors.client;

import com.webellian.authors.config.OpenLibraryProperties;
import com.webellian.authors.service.OpenLibraryException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class OpenLibraryClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final OpenLibraryProperties properties;

    public OpenLibraryClient(WebClient openLibraryWebClient, OpenLibraryProperties properties) {
        this.webClient = openLibraryWebClient;
        this.properties = properties;
    }

    public List<OpenLibraryAuthor> searchAuthors(String name) {
        OpenLibraryAuthorSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/authors.json")
                        .queryParam("q", name)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> Mono.error(
                        new OpenLibraryException("Author search returned HTTP " + clientResponse.statusCode().value())))
                .bodyToMono(OpenLibraryAuthorSearchResponse.class)
                .block(REQUEST_TIMEOUT);

        if (response == null || response.docs() == null) {
            return List.of();
        }
        return response.docs();
    }

    public Optional<OpenLibraryAuthorDetails> getAuthor(String authorId) {
        return Optional.ofNullable(webClient.get()
                .uri("/authors/{authorId}.json", authorId)
                .retrieve()
                .onStatus(status -> status.value() == 404, clientResponse -> Mono.empty())
                .onStatus(HttpStatusCode::isError, clientResponse -> Mono.error(
                        new OpenLibraryException("Author lookup returned HTTP " + clientResponse.statusCode().value())))
                .bodyToMono(OpenLibraryAuthorDetails.class)
                .block(REQUEST_TIMEOUT));
    }

    public List<OpenLibraryWork> searchWorks(String authorId) {
        OpenLibraryWorksResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/authors/{authorId}/works.json")
                        .queryParam("limit", properties.worksLimit())
                        .build(authorId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> Mono.error(
                        new OpenLibraryException("Works search returned HTTP " + clientResponse.statusCode().value())))
                .bodyToMono(OpenLibraryWorksResponse.class)
                .block(REQUEST_TIMEOUT);

        if (response == null || response.entries() == null) {
            return List.of();
        }
        return response.entries();
    }
}
