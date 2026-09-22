package edu.uptc.subjects.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.uptc.subjects.config.PageRequestFactory;
import edu.uptc.subjects.dto.CourseRequest;
import edu.uptc.subjects.dto.CourseResponse;
import edu.uptc.subjects.dto.PageResponse;
import edu.uptc.subjects.exception.ResourceNotFoundException;
import edu.uptc.subjects.service.CourseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CourseController.class)
@Import(PageRequestFactory.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Test
    void shouldReturnPageResponse_whenListing() throws Exception {
        PageResponse<CourseResponse> page = new PageResponse<>(
                List.of(new CourseResponse(7L, 4L, 3L, "Monday-Wednesday 8:00-10:00", "2026-2", 30)),
                0, 10, 1, 1);

        when(courseService.findAll(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].subjectId").value(4))
                .andExpect(jsonPath("$.content[0].teacherId").value(3))
                .andExpect(jsonPath("$.content[0].schedule").value("Monday-Wednesday 8:00-10:00"))
                .andExpect(jsonPath("$.content[0].period").value("2026-2"))
                .andExpect(jsonPath("$.content[0].capacity").value(30))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldPassFiltersAndNestedSort_thenMapSortBy() throws Exception {
        PageResponse<CourseResponse> page = new PageResponse<>(List.of(), 0, 10, 0, 0);

        when(courseService.findAll(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/courses")
                        .param("sortBy", "subjectId")
                        .param("sortDirection", "desc")
                        .param("subjectId", "4")
                        .param("teacherId", "3")
                        .param("period", "2026-2"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(courseService)
                .findAll(any(), eq(4L), eq(3L), eq("2026-2"));
    }

    @Test
    void shouldReturn400_whenSortByIsNotAllowed() throws Exception {
        mockMvc.perform(get("/api/courses").param("sortBy", "subject"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("sortBy")));
    }

    @Test
    void shouldReturn400_whenPageSizeIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/courses").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("pageSize")));
    }

    @Test
    void shouldReturnCreatedWithLocation_whenCreating() throws Exception {
        CourseResponse response = new CourseResponse(7L, 4L, 3L, "Monday-Wednesday 8:00-10:00", "2026-2", 30);

        when(courseService.create(any(CourseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": 4, "teacherId": 3, "schedule": "Monday-Wednesday 8:00-10:00", "period": "2026-2", "capacity": 30}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/courses/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.subjectId").value(4))
                .andExpect(jsonPath("$.teacherId").value(3))
                .andExpect(jsonPath("$.capacity").value(30));
    }

    @Test
    void shouldReturn400_whenPeriodDoesNotMatchPattern() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": 4, "teacherId": 3, "schedule": "Monday 8:00", "period": "2026-10", "capacity": 30}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("period")));
    }

    @Test
    void shouldReturn400_whenCapacityIsNotGreaterThanZero() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": 4, "teacherId": 3, "schedule": "Monday 8:00", "period": "2026-2", "capacity": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("capacity")));
    }

    @Test
    void shouldReturn404_whenCreatingReferencesMissingSubject() throws Exception {
        when(courseService.create(any(CourseRequest.class)))
                .thenThrow(new ResourceNotFoundException("Subject with id 99 was not found"));

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": 99, "teacherId": 3, "schedule": "Monday 8:00", "period": "2026-2", "capacity": 30}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Subject with id 99 was not found"));
    }

    @Test
    void shouldReturnCourse_whenFindingById() throws Exception {
        when(courseService.findById(7L))
                .thenReturn(new CourseResponse(7L, 4L, 3L, "Monday-Wednesday 8:00-10:00", "2026-2", 30));

        mockMvc.perform(get("/api/courses/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.subjectId").value(4))
                .andExpect(jsonPath("$.teacherId").value(3));
    }

    @Test
    void shouldReturn404_whenFindingByIdThatDoesNotExist() throws Exception {
        when(courseService.findById(99L)).thenThrow(new ResourceNotFoundException("Course with id 99 was not found"));

        mockMvc.perform(get("/api/courses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Course with id 99 was not found"))
                .andExpect(jsonPath("$.path").value("/api/courses/99"));
    }

    @Test
    void shouldReturnUpdatedCourse_whenUpdating() throws Exception {
        when(courseService.update(eq(7L), any(CourseRequest.class)))
                .thenReturn(new CourseResponse(7L, 5L, 6L, "Friday 14:00-16:00", "2026-1", 40));

        mockMvc.perform(put("/api/courses/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": 5, "teacherId": 6, "schedule": "Friday 14:00-16:00", "period": "2026-1", "capacity": 40}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value(5))
                .andExpect(jsonPath("$.teacherId").value(6))
                .andExpect(jsonPath("$.capacity").value(40));
    }

    @Test
    void shouldReturnNoContent_whenDeleting() throws Exception {
        org.mockito.Mockito.doNothing().when(courseService).delete(7L);

        mockMvc.perform(delete("/api/courses/7"))
                .andExpect(status().isNoContent());
    }
}