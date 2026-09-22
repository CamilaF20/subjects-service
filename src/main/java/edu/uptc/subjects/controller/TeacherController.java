package edu.uptc.subjects.controller;

import edu.uptc.subjects.config.PageRequestFactory;
import edu.uptc.subjects.dto.ErrorResponse;
import edu.uptc.subjects.dto.PageResponse;
import edu.uptc.subjects.dto.TeacherRequest;
import edu.uptc.subjects.dto.TeacherResponse;
import edu.uptc.subjects.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/teachers")
@Tag(name = "Teachers", description = "CRUD with pagination and sorting for teachers; emails are unique")
public class TeacherController {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "id",
            "firstName", "firstName",
            "lastName", "lastName",
            "email", "email");

    private final TeacherService teacherService;
    private final PageRequestFactory pageRequestFactory;

    public TeacherController(TeacherService teacherService, PageRequestFactory pageRequestFactory) {
        this.teacherService = teacherService;
        this.pageRequestFactory = pageRequestFactory;
    }

    @GetMapping
    @Operation(summary = "List teachers",
            description = "Returns a paginated, sorted list of teachers. Filtering is not supported for teachers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of teachers",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<TeacherResponse>> findAll(
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = pageRequestFactory.create(
                pageNumber, pageSize, sortBy, sortDirection, ALLOWED_SORT_FIELDS);
        return ResponseEntity.ok(teacherService.findAll(pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a teacher by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Teacher found",
                    content = @Content(schema = @Schema(implementation = TeacherResponse.class))),
            @ApiResponse(responseCode = "404", description = "Teacher not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TeacherResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a teacher")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Teacher created",
                    content = @Content(schema = @Schema(implementation = TeacherResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A teacher with this email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherRequest request) {
        TeacherResponse response = teacherService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a teacher")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Teacher updated",
                    content = @Content(schema = @Schema(implementation = TeacherResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Teacher not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A teacher with this email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TeacherResponse> update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.ok(teacherService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a teacher")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Teacher deleted"),
            @ApiResponse(responseCode = "404", description = "Teacher not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Teacher has associated courses and cannot be deleted",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}