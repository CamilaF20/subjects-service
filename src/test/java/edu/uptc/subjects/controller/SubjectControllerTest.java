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
import edu.uptc.subjects.dto.SubjectRequest;
import edu.uptc.subjects.dto.SubjectResponse;
import edu.uptc.subjects.exception.ConflictException;
import edu.uptc.subjects.exception.ResourceNotFoundException;
import edu.uptc.subjects.service.SubjectService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SubjectController.class)
@Import(PageRequestFactory.class)
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubjectService subjectService;

    @Test
    void shouldReturnPageResponse_whenListing() throws Exception {
        PageResponse<SubjectResponse> page = new PageResponse<>(
                List.of(new SubjectResponse(1L, "Distributed Systems", 3, "Systems Engineering")),
                0, 10, 1, 1);

        when(subjectService.findAll(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Distributed Systems"))
                .andExpect(jsonPath("$.content[0].credits").value(3))
                .andExpect(jsonPath("$.content[0].program").value("Systems Engineering"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldPassPaginationAndFilterParams_toService() throws Exception {
        PageResponse<SubjectResponse> page = new PageResponse<>(List.of(), 1, 5, 0, 0);

        when(subjectService.findAll(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/subjects")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("sortBy", "name")
                        .param("sortDirection", "desc")
                        .param("name", "Distributed")
                        .param("program", "Systems")
                        .param("credits", "3"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<org.springframework.data.domain.PageRequest> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.PageRequest.class);
        org.mockito.Mockito.verify(subjectService)
                .findAll(captor.capture(), eq("Distributed"), eq("Systems"), eq(3));

        org.springframework.data.domain.PageRequest request = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1, request.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(5, request.getPageSize());
        org.junit.jupiter.api.Assertions.assertEquals("name", request.getSort().getOrderFor("name").getProperty());
        org.junit.jupiter.api.Assertions.assertEquals(
                org.springframework.data.domain.Sort.Direction.DESC,
                request.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void shouldReturn400_whenSortByIsNotAllowed() throws Exception {
        mockMvc.perform(get("/api/subjects").param("sortBy", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("sortBy")));
    }

    @Test
    void shouldReturn400_whenPageSizeIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/subjects").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("pageSize")));
    }

    @Test
    void shouldReturnCreatedWithLocation_whenCreating() throws Exception {
        SubjectResponse response = new SubjectResponse(4L, "Distributed Systems", 3, "Systems Engineering");

        when(subjectService.create(any(SubjectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Distributed Systems", "credits": 3, "program": "Systems Engineering"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/subjects/4")))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Distributed Systems"))
                .andExpect(jsonPath("$.credits").value(3))
                .andExpect(jsonPath("$.program").value("Systems Engineering"));
    }

    @Test
    void shouldReturn400_whenCreateBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "credits": 0, "program": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("name: must not be blank")));
    }

    @Test
    void shouldReturnSubject_whenFindingById() throws Exception {
        when(subjectService.findById(4L)).thenReturn(new SubjectResponse(4L, "Distributed Systems", 3, "Systems Engineering"));

        mockMvc.perform(get("/api/subjects/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Distributed Systems"));
    }

    @Test
    void shouldReturn404_whenFindingByIdThatDoesNotExist() throws Exception {
        when(subjectService.findById(99L)).thenThrow(new ResourceNotFoundException("Subject with id 99 was not found"));

        mockMvc.perform(get("/api/subjects/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Subject with id 99 was not found"))
                .andExpect(jsonPath("$.path").value("/api/subjects/99"));
    }

    @Test
    void shouldReturnUpdatedSubject_whenUpdating() throws Exception {
        when(subjectService.update(eq(4L), any(SubjectRequest.class)))
                .thenReturn(new SubjectResponse(4L, "Advanced Systems", 4, "Systems Engineering"));

        mockMvc.perform(put("/api/subjects/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Advanced Systems", "credits": 4, "program": "Systems Engineering"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Advanced Systems"))
                .andExpect(jsonPath("$.credits").value(4));
    }

    @Test
    void shouldReturnNoContent_whenDeleting() throws Exception {
        org.mockito.Mockito.doNothing().when(subjectService).delete(4L);

        mockMvc.perform(delete("/api/subjects/4"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn409_whenDeletingSubjectWithCourses() throws Exception {
        org.mockito.Mockito.doThrow(
                        new ConflictException("Subject with id 4 has associated courses and cannot be deleted"))
                .when(subjectService).delete(4L);

        mockMvc.perform(delete("/api/subjects/4"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}