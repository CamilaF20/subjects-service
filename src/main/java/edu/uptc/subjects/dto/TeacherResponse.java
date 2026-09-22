package edu.uptc.subjects.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeacherResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "Andres") String firstName,
        @Schema(example = "Vargas") String lastName,
        @Schema(example = "andres@example.com") String email) {
}