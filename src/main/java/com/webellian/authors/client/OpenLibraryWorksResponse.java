package com.webellian.authors.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryWorksResponse(List<OpenLibraryWork> entries) {
}
