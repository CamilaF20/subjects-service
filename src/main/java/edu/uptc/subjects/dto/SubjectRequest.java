package edu.uptc.subjects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
        @Schema(example = "Distributed Systems")
        @NotBlank @Size(max = 150) String name,
        @Schema(example = "3", minimum = "1", maximum = "10")
        @NotNull @Min(1) @Max(10) Integer credits,
        @Schema(example = "Systems Engineering")
        @NotBlank @Size(max = 150) String program) {
}