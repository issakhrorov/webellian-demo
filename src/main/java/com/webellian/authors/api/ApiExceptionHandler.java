package com.webellian.authors.api;

import com.webellian.authors.service.OpenLibraryException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class})
    ProblemDetail handleValidation(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail(exception.getMessage());
        return problem;
    }

    @ExceptionHandler(OpenLibraryException.class)
    ProblemDetail handleOpenLibrary(OpenLibraryException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("Open Library request failed");
        problem.setDetail(exception.getMessage());
        return problem;
    }
}
