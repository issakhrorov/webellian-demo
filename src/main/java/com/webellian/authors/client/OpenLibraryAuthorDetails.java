package com.webellian.authors.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryAuthorDetails(String key, String name) {
}
