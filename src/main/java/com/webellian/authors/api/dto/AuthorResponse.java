package com.webellian.authors.api.dto;

import com.webellian.authors.domain.Author;

public record AuthorResponse(String id, String name) {

    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getId(), author.getName());
    }
}
