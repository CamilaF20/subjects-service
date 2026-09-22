package edu.uptc.subjects.controller;

import edu.uptc.subjects.config.PageRequestFactory;
import edu.uptc.subjects.dto.CourseRequest;
import edu.uptc.subjects.dto.CourseResponse;
import edu.uptc.subjects.dto.ErrorResponse;
import edu.uptc.subjects.dto.PageResponse;
import edu.uptc.subjects.service.CourseService;
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
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "CRUD with pagination, sorting and filters for course offerings (Subject + Teacher)")
public class CourseController {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "id",
            "subjectId", "subject.id",
            "teacherId", "teacher.id",
            "schedule", "schedule",
            "period", "period",
            "capacity", "capacity");

    private final CourseService courseService;
    private final PageRequestFactory pageRequestFactory;

    public CourseController(CourseService courseService, PageRequestFactory pageRequestFactory) {
        this.courseService = courseService;
        this.pageRequestFactory = pageRequestFactory;
    }

    @GetMapping
    @Operation(summary = "List courses",
            description = "Returns a paginated, sorted list of courses with optional filters by subjectId, teacherId and period. "
                    + "sortBy subjectId and teacherId sort by the referenced subject.id and teacher.id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of courses"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination, sorting or filter parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<CourseResponse>> findAll(
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String period) {

        PageRequest pageRequest = pageRequestFactory.create(
                pageNumber, pageSize, sortBy, sortDirection, ALLOWED_SORT_FIELDS);
        return ResponseEntity.ok(courseService.findAll(pageRequest, subjectId, teacherId, period));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a course by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course found",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a course",
            description = "The referenced subject and teacher must already exist, otherwise a 404 is returned.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Course created",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Referenced subject or teacher not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a course",
            description = "Replaces the whole course; the referenced subject and teacher must already exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course updated",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course, subject or teacher not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseResponse> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a course",
            description = "Deleting a course is always allowed.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Course deleted"),
            @ApiResponse(responseCode = "404", description = "Course not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}