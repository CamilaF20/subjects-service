package edu.uptc.subjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.uptc.subjects.config.DataSeeder;
import edu.uptc.subjects.entity.Course;
import edu.uptc.subjects.entity.Subject;
import edu.uptc.subjects.entity.Teacher;
import edu.uptc.subjects.repository.CourseRepository;
import edu.uptc.subjects.repository.SubjectRepository;
import edu.uptc.subjects.repository.TeacherRepository;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:seeddb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class DataSeederIntegrationTest {

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void shouldLoadFixedAnchorsFromSpec() {
        Subject subject = subjectRepository.findById(4L).orElse(null);
        assertNotNull(subject, "Subject anchor id 4 must exist");
        assertEquals("Distributed Systems", subject.getName());
        assertEquals(3, subject.getCredits());
        assertEquals("Systems Engineering", subject.getProgram());

        Teacher teacher = teacherRepository.findById(3L).orElse(null);
        assertNotNull(teacher, "Teacher anchor id 3 must exist");
        assertEquals("Andres Vargas", teacher.getFirstName() + " " + teacher.getLastName());
        assertEquals("andres@example.com", teacher.getEmail());

        Course course = courseRepository.findById(7L).orElse(null);
        assertNotNull(course, "Course anchor id 7 must exist");
        assertEquals(4L, course.getSubject().getId());
        assertEquals(3L, course.getTeacher().getId());
        assertEquals("Monday-Wednesday 8:00-10:00", course.getSchedule());
        assertEquals("2026-2", course.getPeriod());
        assertEquals(30, course.getCapacity());
    }

    @Test
    void shouldLoadMinimumVolumesFromSpec() {
        assertTrue(subjectRepository.count() >= 15, "at least 15 subjects required by §11");
        assertTrue(teacherRepository.count() >= 10, "at least 10 teachers required by §11");
        assertTrue(courseRepository.count() >= 20, "at least 20 courses required by §11");

        Set<String> programs = subjectRepository.findAll().stream()
                .map(Subject::getProgram)
                .collect(Collectors.toSet());
        assertTrue(programs.size() >= 3, "at least 3 different program values required by §11");

        Set<String> periods = courseRepository.findAll().stream()
                .map(Course::getPeriod)
                .collect(Collectors.toSet());
        assertTrue(periods.contains("2026-1") && periods.contains("2026-2"),
                "courses must span periods 2026-1 and 2026-2");

        Set<Long> subjectIdsInCourses = courseRepository.findAll().stream()
                .map(course -> course.getSubject().getId())
                .collect(Collectors.toSet());
        Set<Long> teacherIdsInCourses = courseRepository.findAll().stream()
                .map(course -> course.getTeacher().getId())
                .collect(Collectors.toSet());
        assertEquals(subjectRepository.count(), subjectIdsInCourses.size(),
                "every subject must appear in at least one course");
        assertEquals(teacherRepository.count(), teacherIdsInCourses.size(),
                "every teacher must appear in at least one course");
    }

    @Test
    void shouldBeIdempotent_whenRunAgain() {
        long subjects = subjectRepository.count();
        long teachers = teacherRepository.count();
        long courses = courseRepository.count();

        dataSeeder.run();

        assertEquals(subjects, subjectRepository.count(), "no duplicate subjects on a second run");
        assertEquals(teachers, teacherRepository.count(), "no duplicate teachers on a second run");
        assertEquals(courses, courseRepository.count(), "no duplicate courses on a second run");
        assertNotNull(subjectRepository.findById(4L).orElse(null), "anchor data must remain intact");
        assertNotNull(courseRepository.findById(7L).orElse(null), "anchor data must remain intact");
    }
}