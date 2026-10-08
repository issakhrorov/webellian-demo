package com.webellian.authors.api.dto;

import com.webellian.authors.domain.AuthorWork;

public record AuthorWorkResponse(String id, String title) {

    public static AuthorWorkResponse from(AuthorWork work) {
        return new AuthorWorkResponse(work.getWorkId(), work.getTitle());
    }
}
