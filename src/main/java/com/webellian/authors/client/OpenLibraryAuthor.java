package com.webellian.authors.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryAuthor(String key, String name) {
}
