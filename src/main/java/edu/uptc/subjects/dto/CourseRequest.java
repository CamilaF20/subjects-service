package edu.uptc.subjects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @Schema(example = "4") @NotNull Long subjectId,
        @Schema(example = "3") @NotNull Long teacherId,
        @Schema(example = "Monday-Wednesday 8:00-10:00")
        @NotBlank @Size(max = 100) String schedule,
        @Schema(example = "2026-2", pattern = "^\\d{4}-[1-9]$")
        @NotBlank @Pattern(regexp = "^\\d{4}-[1-9]$") String period,
        @Schema(example = "30", minimum = "1") @NotNull @Min(1) Integer capacity) {
}