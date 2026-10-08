package com.webellian.authors.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "open-library")
public record OpenLibraryProperties(
        @NotBlank String baseUrl,
        @Min(1) int worksLimit
) {
}
