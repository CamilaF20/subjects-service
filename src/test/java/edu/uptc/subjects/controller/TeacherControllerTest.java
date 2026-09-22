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
import edu.uptc.subjects.dto.PageResponse;
import edu.uptc.subjects.dto.TeacherRequest;
import edu.uptc.subjects.dto.TeacherResponse;
import edu.uptc.subjects.exception.ConflictException;
import edu.uptc.subjects.exception.ResourceNotFoundException;
import edu.uptc.subjects.service.TeacherService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TeacherController.class)
@Import(PageRequestFactory.class)
class TeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeacherService teacherService;

    @Test
    void shouldReturnPageResponse_whenListing() throws Exception {
        PageResponse<TeacherResponse> page = new PageResponse<>(
                List.of(new TeacherResponse(3L, "Andres", "Vargas", "andres@example.com")),
                0, 10, 1, 1);

        when(teacherService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.content[0].firstName").value("Andres"))
                .andExpect(jsonPath("$.content[0].lastName").value("Vargas"))
                .andExpect(jsonPath("$.content[0].email").value("andres@example.com"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldReturn400_whenSortByIsNotAllowed() throws Exception {
        mockMvc.perform(get("/api/teachers").param("sortBy", "middleName"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("sortBy")));
    }

    @Test
    void shouldReturnCreatedWithLocation_whenCreating() throws Exception {
        TeacherResponse response = new TeacherResponse(3L, "Andres", "Vargas", "andres@example.com");

        when(teacherService.create(any(TeacherRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Andres", "lastName": "Vargas", "email": "andres@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/teachers/3")))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.firstName").value("Andres"))
                .andExpect(jsonPath("$.lastName").value("Vargas"))
                .andExpect(jsonPath("$.email").value("andres@example.com"));
    }

    @Test
    void shouldReturn400_whenEmailIsMalformed() throws Exception {
        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Andres", "lastName": "Vargas", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    void shouldReturn409_whenEmailIsAlreadyTaken() throws Exception {
        when(teacherService.create(any(TeacherRequest.class)))
                .thenThrow(new ConflictException("A teacher with email 'andres@example.com' already exists"));

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Andres", "lastName": "Vargas", "email": "andres@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A teacher with email 'andres@example.com' already exists"));
    }

    @Test
    void shouldReturnTeacher_whenFindingById() throws Exception {
        when(teacherService.findById(3L))
                .thenReturn(new TeacherResponse(3L, "Andres", "Vargas", "andres@example.com"));

        mockMvc.perform(get("/api/teachers/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.email").value("andres@example.com"));
    }

    @Test
    void shouldReturn404_whenFindingByIdThatDoesNotExist() throws Exception {
        when(teacherService.findById(99L)).thenThrow(new ResourceNotFoundException("Teacher with id 99 was not found"));

        mockMvc.perform(get("/api/teachers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Teacher with id 99 was not found"));
    }

    @Test
    void shouldReturnUpdatedTeacher_whenUpdating() throws Exception {
        when(teacherService.update(eq(3L), any(TeacherRequest.class)))
                .thenReturn(new TeacherResponse(3L, "Andres", "Garcia", "andres@example.com"));

        mockMvc.perform(put("/api/teachers/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Andres", "lastName": "Garcia", "email": "andres@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Garcia"));
    }

    @Test
    void shouldReturnNoContent_whenDeleting() throws Exception {
        org.mockito.Mockito.doNothing().when(teacherService).delete(3L);

        mockMvc.perform(delete("/api/teachers/3"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn409_whenDeletingTeacherWithCourses() throws Exception {
        org.mockito.Mockito.doThrow(
                        new ConflictException("Teacher with id 3 has associated courses and cannot be deleted"))
                .when(teacherService).delete(3L);

        mockMvc.perform(delete("/api/teachers/3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}