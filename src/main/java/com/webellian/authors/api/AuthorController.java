package com.webellian.authors.api;

import com.webellian.authors.api.dto.AuthorResponse;
import com.webellian.authors.api.dto.AuthorWorkResponse;
import com.webellian.authors.service.AuthorService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping(path = "/api/authors", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    List<AuthorResponse> searchAuthors(@RequestParam @NotBlank String name) {
        return authorService.searchAuthors(name);
    }

    @GetMapping("/{authorId}/works")
    List<AuthorWorkResponse> searchAuthorWorks(@PathVariable @NotBlank String authorId) {
        return authorService.searchWorks(authorId);
    }
}
