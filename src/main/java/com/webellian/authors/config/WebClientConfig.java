package com.webellian.authors.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(OpenLibraryProperties.class)
public class WebClientConfig {

    @Bean
    WebClient openLibraryWebClient(WebClient.Builder builder, OpenLibraryProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("User-Agent", "webellian-demo-authors-api/0.0.1")
                .build();
    }
}
