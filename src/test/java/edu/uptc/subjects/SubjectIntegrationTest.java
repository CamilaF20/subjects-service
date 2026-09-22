package edu.uptc.subjects;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.uptc.subjects.entity.Course;
import edu.uptc.subjects.entity.Subject;
import edu.uptc.subjects.entity.Teacher;
import edu.uptc.subjects.repository.CourseRepository;
import edu.uptc.subjects.repository.SubjectRepository;
import edu.uptc.subjects.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubjectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void cleanDatabase() {
        courseRepository.deleteAll();
        subjectRepository.deleteAll();
        teacherRepository.deleteAll();
    }

    @Test
    void shouldCreate_whenBodyIsValid() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Distributed Systems", "credits": 3, "program": "Systems Engineering"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/subjects/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Distributed Systems"))
                .andExpect(jsonPath("$.credits").value(3))
                .andExpect(jsonPath("$.program").value("Systems Engineering"));
    }

    @Test
    void shouldReturnExactContractFields_whenGettingById() throws Exception {
        Subject saved = subjectRepository.save(new Subject("Distributed Systems", 3, "Systems Engineering"));

        String expected = """
                {"id": %d, "name": "Distributed Systems", "credits": 3, "program": "Systems Engineering"}
                """.formatted(saved.getId());

        mockMvc.perform(get("/api/subjects/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, true));
    }

    @Test
    void shouldReturn404_whenSubjectDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/subjects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Subject with id 999 was not found"))
                .andExpect(jsonPath("$.path").value("/api/subjects/999"));
    }

    @Test
    void shouldReturn400_whenBodyIsInvalid() throws Exception {
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
    void shouldReturn400_whenCreditsExceedTen() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "X", "credits": 11, "program": "P"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("credits")));
    }

    @Test
    void shouldUpdate_whenSubjectExists() throws Exception {
        Subject saved = subjectRepository.save(new Subject("Old", 2, "Old Program"));

        mockMvc.perform(put("/api/subjects/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "New", "credits": 5, "program": "New Program"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.credits").value(5))
                .andExpect(jsonPath("$.program").value("New Program"));
    }

    @Test
    void shouldDelete_thenReturn404OnSubsequentGet() throws Exception {
        Subject saved = subjectRepository.save(new Subject("To Delete", 3, "P"));

        mockMvc.perform(delete("/api/subjects/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/subjects/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409_whenDeletingSubjectWithCourses() throws Exception {
        Subject subject = subjectRepository.save(new Subject("Protected", 3, "P"));
        Teacher teacher = teacherRepository.save(new Teacher("Andres", "Vargas", "delete-subject@example.com"));
        courseRepository.save(new Course(subject, teacher, "Monday 8:00", "2026-2", 30));

        mockMvc.perform(delete("/api/subjects/" + subject.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Subject with id " + subject.getId() + " has associated courses and cannot be deleted"));
    }

    @Test
    void shouldPaginate() throws Exception {
        subjectRepository.save(new Subject("A", 3, "Systems Engineering"));
        subjectRepository.save(new Subject("B", 4, "Systems Engineering"));
        subjectRepository.save(new Subject("C", 5, "Software Engineering"));

        mockMvc.perform(get("/api/subjects").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldSortDescending() throws Exception {
        subjectRepository.save(new Subject("Alpha", 3, "P"));
        subjectRepository.save(new Subject("Bravo", 3, "P"));
        subjectRepository.save(new Subject("Charlie", 3, "P"));

        mockMvc.perform(get("/api/subjects")
                        .param("sortBy", "name")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Charlie"))
                .andExpect(jsonPath("$.content[1].name").value("Bravo"))
                .andExpect(jsonPath("$.content[2].name").value("Alpha"));
    }

    @Test
    void shouldFilterByNameContainingCaseInsensitive() throws Exception {
        subjectRepository.save(new Subject("Distributed Systems", 3, "Systems Engineering"));
        subjectRepository.save(new Subject("Databases", 4, "Systems Engineering"));

        mockMvc.perform(get("/api/subjects").param("name", "DIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Distributed Systems"));
    }

    @Test
    void shouldFilterByProgramContainingCaseInsensitive() throws Exception {
        subjectRepository.save(new Subject("S1", 3, "Systems Engineering"));
        subjectRepository.save(new Subject("S2", 3, "Software Engineering"));
        subjectRepository.save(new Subject("S3", 2, "Industrial Engineering"));

        mockMvc.perform(get("/api/subjects").param("program", "engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldFilterByCreditsExact() throws Exception {
        subjectRepository.save(new Subject("A", 3, "P"));
        subjectRepository.save(new Subject("B", 5, "P"));
        subjectRepository.save(new Subject("C", 3, "Q"));

        mockMvc.perform(get("/api/subjects").param("credits", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldCombineFiltersWithAnd() throws Exception {
        subjectRepository.save(new Subject("Distributed Systems", 3, "Systems Engineering"));
        subjectRepository.save(new Subject("Distributed Databases", 4, "Systems Engineering"));
        subjectRepository.save(new Subject("Databases", 3, "Systems Engineering"));

        mockMvc.perform(get("/api/subjects")
                        .param("name", "distributed")
                        .param("program", "systems")
                        .param("credits", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Distributed Databases"));
    }

    @Test
    void shouldReturn400_whenSortByIsNotAllowed() throws Exception {
        mockMvc.perform(get("/api/subjects").param("sortBy", "somethingElse"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("sortBy")));
    }
}