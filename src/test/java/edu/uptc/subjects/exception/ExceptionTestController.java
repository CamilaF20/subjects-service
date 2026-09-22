package edu.uptc.subjects.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExceptionTestController {

    @GetMapping("/test/not-found")
    void notFound() {
        throw new ResourceNotFoundException("Course with id 12 was not found");
    }

    @GetMapping("/test/conflict")
    void conflict() {
        throw new ConflictException("Teacher still has courses");
    }

    @GetMapping("/test/invalid-query")
    void invalidQuery() {
        throw new InvalidQueryParameterException("pageSize must be between 1 and 100");
    }

    @PostMapping("/test/validate")
    void validate(@Valid @RequestBody TestBody body) {
    }

    @GetMapping("/test/unexpected")
    void unexpected() {
        throw new IllegalStateException("something broke");
    }

    public record TestBody(@NotBlank String name) {
    }
}