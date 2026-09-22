package edu.uptc.subjects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ErrorResponse(
        @Schema(example = "2026-09-14T15:30:00") LocalDateTime timestamp,
        @Schema(example = "404") int status,
        @Schema(example = "Not Found") String error,
        @Schema(example = "Course with id 12 was not found") String message,
        @Schema(example = "/api/courses/12") String path) {
}