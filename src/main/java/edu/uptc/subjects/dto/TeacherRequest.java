package edu.uptc.subjects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherRequest(
        @Schema(example = "Andres")
        @NotBlank @Size(max = 80) String firstName,
        @Schema(example = "Vargas")
        @NotBlank @Size(max = 80) String lastName,
        @Schema(example = "andres@example.com")
        @NotBlank @Email @Size(max = 150) String email) {
}