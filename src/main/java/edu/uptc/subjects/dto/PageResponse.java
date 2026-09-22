package edu.uptc.subjects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PageResponse<T>(
        @Schema(description = "Items on the current page") List<T> content,
        @Schema(example = "0", description = "Zero-based page number") int pageNumber,
        @Schema(example = "10") int pageSize,
        @Schema(example = "100") long totalElements,
        @Schema(example = "10") int totalPages) {
}