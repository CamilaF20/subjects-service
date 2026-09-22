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
class CourseIntegrationTest {

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

    private Subject saveSubject(String name) {
        return subjectRepository.save(new Subject(name, 3, "Systems Engineering"));
    }

    private Teacher saveTeacher(String email) {
        return teacherRepository.save(new Teacher("Andres", "Vargas", email));
    }

    private Course saveCourse(Subject subject, Teacher teacher, String period, int capacity) {
        return courseRepository.save(new Course(subject, teacher, "Monday-Wednesday 8:00-10:00", period, capacity));
    }

    @Test
    void shouldCreate_whenBodyIsValid() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Teacher teacher = saveTeacher("course-create@example.com");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": %d, "teacherId": %d, "schedule": "Monday-Wednesday 8:00-10:00", "period": "2026-2", "capacity": 30}
                                """.formatted(subject.getId(), teacher.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/courses/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.subjectId").value(subject.getId()))
                .andExpect(jsonPath("$.teacherId").value(teacher.getId()))
                .andExpect(jsonPath("$.schedule").value("Monday-Wednesday 8:00-10:00"))
                .andExpect(jsonPath("$.period").value("2026-2"))
                .andExpect(jsonPath("$.capacity").value(30));
    }

    @Test
    void shouldReturnExactContractFields_whenGettingById() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Teacher teacher = saveTeacher("course-contract@example.com");
        Course saved = saveCourse(subject, teacher, "2026-2", 30);

        String expected = """
                {"id": %d, "subjectId": %d, "teacherId": %d, "schedule": "Monday-Wednesday 8:00-10:00", "period": "2026-2", "capacity": 30}
                """.formatted(saved.getId(), subject.getId(), teacher.getId());

        mockMvc.perform(get("/api/courses/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, true));
    }

    @Test
    void shouldReturn404_whenCourseDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Course with id 999 was not found"))
                .andExpect(jsonPath("$.path").value("/api/courses/999"));
    }

    @Test
    void shouldReturn404_whenSubjectDoesNotExistOnCreate() throws Exception {
        Teacher teacher = saveTeacher("course-missing-subject@example.com");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": 999, "teacherId": %d, "schedule": "Monday 8:00", "period": "2026-2", "capacity": 30}
                                """.formatted(teacher.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Subject with id 999 was not found"));
    }

    @Test
    void shouldReturn404_whenTeacherDoesNotExistOnCreate() throws Exception {
        Subject subject = saveSubject("Distributed Systems");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": %d, "teacherId": 999, "schedule": "Monday 8:00", "period": "2026-2", "capacity": 30}
                                """.formatted(subject.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Teacher with id 999 was not found"));
    }

    @Test
    void shouldReturn400_whenPeriodDoesNotMatchPattern() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Teacher teacher = saveTeacher("course-bad-period@example.com");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": %d, "teacherId": %d, "schedule": "Monday 8:00", "period": "2026-10", "capacity": 30}
                                """.formatted(subject.getId(), teacher.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("period")));
    }

    @Test
    void shouldReturn400_whenCapacityIsNotGreaterThanZero() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Teacher teacher = saveTeacher("course-bad-capacity@example.com");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": %d, "teacherId": %d, "schedule": "Monday 8:00", "period": "2026-2", "capacity": 0}
                                """.formatted(subject.getId(), teacher.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("capacity")));
    }

    @Test
    void shouldUpdate_whenCourseExists() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Subject otherSubject = saveSubject("Databases");
        Teacher teacher = saveTeacher("course-update@example.com");
        Course saved = saveCourse(subject, teacher, "2026-2", 30);

        mockMvc.perform(put("/api/courses/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": %d, "teacherId": %d, "schedule": "Friday 14:00-16:00", "period": "2026-1", "capacity": 40}
                                """.formatted(otherSubject.getId(), teacher.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value(otherSubject.getId()))
                .andExpect(jsonPath("$.period").value("2026-1"))
                .andExpect(jsonPath("$.capacity").value(40));
    }

    @Test
    void shouldDelete_thenReturn404OnSubsequentGet() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Teacher teacher = saveTeacher("course-delete@example.com");
        Course saved = saveCourse(subject, teacher, "2026-2", 30);

        mockMvc.perform(delete("/api/courses/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFilterBySubjectIdExact() throws Exception {
        Subject s1 = saveSubject("Distributed Systems");
        Subject s2 = saveSubject("Databases");
        Teacher teacher = saveTeacher("course-f-subject@example.com");
        saveCourse(s1, teacher, "2026-2", 30);
        saveCourse(s2, teacher, "2026-2", 30);

        mockMvc.perform(get("/api/courses").param("subjectId", String.valueOf(s1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].subjectId").value(s1.getId()));
    }

    @Test
    void shouldFilterByTeacherIdExact() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Teacher t1 = saveTeacher("course-f-t1@example.com");
        Teacher t2 = saveTeacher("course-f-t2@example.com");
        saveCourse(subject, t1, "2026-2", 30);
        saveCourse(subject, t2, "2026-2", 30);

        mockMvc.perform(get("/api/courses").param("teacherId", String.valueOf(t1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].teacherId").value(t1.getId()));
    }

    @Test
    void shouldFilterByPeriodExact() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Teacher teacher = saveTeacher("course-f-period@example.com");
        saveCourse(subject, teacher, "2026-1", 30);
        saveCourse(subject, teacher, "2026-2", 30);

        mockMvc.perform(get("/api/courses").param("period", "2026-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].period").value("2026-2"));
    }

    @Test
    void shouldCombineFiltersWithAnd() throws Exception {
        Subject s1 = saveSubject("Distributed Systems");
        Subject s2 = saveSubject("Databases");
        Teacher t1 = saveTeacher("course-combo-t1@example.com");
        Teacher t2 = saveTeacher("course-combo-t2@example.com");
        saveCourse(s1, t1, "2026-2", 30);
        saveCourse(s1, t1, "2026-1", 30);
        saveCourse(s1, t2, "2026-2", 30);
        saveCourse(s2, t1, "2026-2", 30);

        mockMvc.perform(get("/api/courses")
                        .param("subjectId", String.valueOf(s1.getId()))
                        .param("teacherId", String.valueOf(t1.getId()))
                        .param("period", "2026-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldPaginate() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Teacher teacher = saveTeacher("course-pagination@example.com");
        saveCourse(subject, teacher, "2026-1", 30);
        saveCourse(subject, teacher, "2026-2", 30);
        saveCourse(subject, teacher, "2026-1", 40);

        mockMvc.perform(get("/api/courses").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldSortByPeriodDescending() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Teacher teacher = saveTeacher("course-sort-period@example.com");
        saveCourse(subject, teacher, "2026-1", 30);
        saveCourse(subject, teacher, "2026-2", 30);

        mockMvc.perform(get("/api/courses")
                        .param("sortBy", "period")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].period").value("2026-2"))
                .andExpect(jsonPath("$.content[1].period").value("2026-1"));
    }

    @Test
    void shouldSortBySubjectIdMappedToSubjectId() throws Exception {
        Subject s1 = saveSubject("Distributed Systems");
        Subject s2 = saveSubject("Databases");
        Teacher teacher = saveTeacher("course-sort-subject@example.com");
        saveCourse(s1, teacher, "2026-2", 30);
        saveCourse(s2, teacher, "2026-2", 30);

        mockMvc.perform(get("/api/courses")
                        .param("sortBy", "subjectId")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].subjectId").value(s1.getId()))
                .andExpect(jsonPath("$.content[1].subjectId").value(s2.getId()));
    }

    @Test
    void shouldSortByTeacherIdMappedToTeacherId() throws Exception {
        Subject subject = saveSubject("Distributed Systems");
        Teacher t1 = saveTeacher("course-sort-t1@example.com");
        Teacher t2 = saveTeacher("course-sort-t2@example.com");
        saveCourse(subject, t1, "2026-2", 30);
        saveCourse(subject, t2, "2026-2", 30);

        mockMvc.perform(get("/api/courses")
                        .param("sortBy", "teacherId")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].teacherId").value(t1.getId()))
                .andExpect(jsonPath("$.content[1].teacherId").value(t2.getId()));
    }

    @Test
    void shouldReturn400_whenSortByIsNotAllowed() throws Exception {
        mockMvc.perform(get("/api/courses").param("sortBy", "somethingElse"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("sortBy")));
    }
}