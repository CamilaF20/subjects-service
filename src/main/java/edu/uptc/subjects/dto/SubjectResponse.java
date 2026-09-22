package edu.uptc.subjects.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SubjectResponse(
        @Schema(example = "4") Long id,
        @Schema(example = "Distributed Systems") String name,
        @Schema(example = "3") Integer credits,
        @Schema(example = "Systems Engineering") String program) {
}