package edu.uptc.subjects.controller;

import edu.uptc.subjects.config.PageRequestFactory;
import edu.uptc.subjects.dto.ErrorResponse;
import edu.uptc.subjects.dto.PageResponse;
import edu.uptc.subjects.dto.SubjectRequest;
import edu.uptc.subjects.dto.SubjectResponse;
import edu.uptc.subjects.service.SubjectService;
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
@RequestMapping("/api/subjects")
@Tag(name = "Subjects", description = "CRUD, pagination, sorting and filters for academic subjects")
public class SubjectController {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "id",
            "name", "name",
            "credits", "credits",
            "program", "program");

    private final SubjectService subjectService;
    private final PageRequestFactory pageRequestFactory;

    public SubjectController(SubjectService subjectService, PageRequestFactory pageRequestFactory) {
        this.subjectService = subjectService;
        this.pageRequestFactory = pageRequestFactory;
    }

    @GetMapping
    @Operation(summary = "List subjects",
            description = "Returns a paginated, sorted list of subjects with optional filters by name, program and credits.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of subjects"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination, sorting or filter parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<SubjectResponse>> findAll(
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) Integer credits) {

        PageRequest pageRequest = pageRequestFactory.create(
                pageNumber, pageSize, sortBy, sortDirection, ALLOWED_SORT_FIELDS);
        return ResponseEntity.ok(subjectService.findAll(pageRequest, name, program, credits));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a subject by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subject found",
                    content = @Content(schema = @Schema(implementation = SubjectResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subject not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubjectResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a subject")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subject created",
                    content = @Content(schema = @Schema(implementation = SubjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        SubjectResponse response = subjectService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a subject")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subject updated",
                    content = @Content(schema = @Schema(implementation = SubjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subject not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubjectResponse> update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a subject")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Subject deleted"),
            @ApiResponse(responseCode = "404", description = "Subject not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Subject has associated courses and cannot be deleted",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}