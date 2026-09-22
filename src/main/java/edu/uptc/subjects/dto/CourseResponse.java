package edu.uptc.subjects.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CourseResponse(
        @Schema(example = "7") Long id,
        @Schema(example = "4") Long subjectId,
        @Schema(example = "3") Long teacherId,
        @Schema(example = "Monday-Wednesday 8:00-10:00") String schedule,
        @Schema(example = "2026-2", pattern = "^\\d{4}-[1-9]$") String period,
        @Schema(example = "30") Integer capacity) {
}