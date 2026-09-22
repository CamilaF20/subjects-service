package edu.uptc.subjects;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void cleanDatabase() {
        courseRepository.deleteAll();
        teacherRepository.deleteAll();
        subjectRepository.deleteAll();
    }

    @Test
    void shouldCreate_whenBodyIsValid() throws Exception {
        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Andres", "lastName": "Vargas", "email": "andres@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/teachers/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Andres"))
                .andExpect(jsonPath("$.lastName").value("Vargas"))
                .andExpect(jsonPath("$.email").value("andres@example.com"));
    }

    @Test
    void shouldReturnExactContractFields_whenGettingById() throws Exception {
        Teacher saved = teacherRepository.save(new Teacher("Andres", "Vargas", "andres@example.com"));

        String expected = """
                {"id": %d, "firstName": "Andres", "lastName": "Vargas", "email": "andres@example.com"}
                """.formatted(saved.getId());

        mockMvc.perform(get("/api/teachers/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, true));
    }

    @Test
    void shouldReturn404_whenTeacherDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/teachers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Teacher with id 999 was not found"))
                .andExpect(jsonPath("$.path").value("/api/teachers/999"));
    }

    @Test
    void shouldReturn400_whenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "", "lastName": "", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("firstName: must not be blank")));
    }

    @Test
    void shouldReturn409_whenCreatingDuplicateEmail() throws Exception {
        teacherRepository.save(new Teacher("One", "Teacher", "dup@example.com"));

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Two", "lastName": "Teacher", "email": "dup@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A teacher with email 'dup@example.com' already exists"));
    }

    @Test
    void shouldAllowUpdatingTeacher_whenKeepingOwnEmail() throws Exception {
        Teacher saved = teacherRepository.save(new Teacher("Andres", "Vargas", "andres@example.com"));

        mockMvc.perform(put("/api/teachers/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Andres", "lastName": "Garcia", "email": "andres@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Garcia"));
    }

    @Test
    void shouldReturn409_whenUpdatingToAnotherTeachersEmail() throws Exception {
        teacherRepository.save(new Teacher("One", "Teacher", "one@example.com"));
        Teacher other = teacherRepository.save(new Teacher("Two", "Teacher", "two@example.com"));

        mockMvc.perform(put("/api/teachers/" + other.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Two", "lastName": "Teacher", "email": "one@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A teacher with email 'one@example.com' already exists"));
    }

    @Test
    void shouldEnforceUniqueEmailInDatabase() {
        teacherRepository.save(new Teacher("One", "Teacher", "unique@example.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> teacherRepository.save(new Teacher("Two", "Teacher", "unique@example.com")));
    }

    @Test
    void shouldDelete_thenReturn404OnSubsequentGet() throws Exception {
        Teacher saved = teacherRepository.save(new Teacher("To", "Delete", "delete@example.com"));

        mockMvc.perform(delete("/api/teachers/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teachers/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409_whenDeletingTeacherWithCourses() throws Exception {
        Subject subject = subjectRepository.save(new Subject("Subject", 3, "P"));
        Teacher teacher = teacherRepository.save(new Teacher("Protected", "Teacher", "delete-teacher@example.com"));
        courseRepository.save(new Course(subject, teacher, "Monday 8:00", "2026-2", 30));

        mockMvc.perform(delete("/api/teachers/" + teacher.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Teacher with id " + teacher.getId() + " has associated courses and cannot be deleted"));
    }

    @Test
    void shouldPaginateAndSort() throws Exception {
        teacherRepository.save(new Teacher("Anna", "A", "anna@example.com"));
        teacherRepository.save(new Teacher("Carlos", "B", "carlos@example.com"));
        teacherRepository.save(new Teacher("Beatriz", "C", "beatriz@example.com"));

        mockMvc.perform(get("/api/teachers")
                        .param("pageSize", "2")
                        .param("sortBy", "firstName")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].firstName").value("Anna"))
                .andExpect(jsonPath("$.content[1].firstName").value("Beatriz"));
    }
}