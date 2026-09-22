package edu.uptc.subjects.config;

import edu.uptc.subjects.entity.Course;
import edu.uptc.subjects.entity.Subject;
import edu.uptc.subjects.entity.Teacher;
import edu.uptc.subjects.repository.CourseRepository;
import edu.uptc.subjects.repository.SubjectRepository;
import edu.uptc.subjects.repository.TeacherRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final boolean seedEnabled;

    public DataSeeder(
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            CourseRepository courseRepository,
            @Value("${app.seed.enabled:false}") boolean seedEnabled) {
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.courseRepository = courseRepository;
        this.seedEnabled = seedEnabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        if (subjectRepository.count() > 0 || teacherRepository.count() > 0 || courseRepository.count() > 0) {
            log.info("Seed skipped: database is not empty.");
            return;
        }

        List<Subject> subjects = subjects();
        subjectRepository.saveAll(subjects);

        List<Teacher> teachers = teachers();
        teacherRepository.saveAll(teachers);

        List<Course> courses = courses(subjects, teachers);
        courseRepository.saveAll(courses);

        log.info("Seed completed: {} subjects, {} teachers, {} courses.",
                subjects.size(), teachers.size(), courses.size());
    }

    private List<Subject> subjects() {
        // The 4th subject must keep id 4 (anchor §11).
        return List.of(
                new Subject("Calculus I", 4, "Systems Engineering"),
                new Subject("Linear Algebra", 4, "Systems Engineering"),
                new Subject("Discrete Mathematics", 3, "Systems Engineering"),
                new Subject("Distributed Systems", 3, "Systems Engineering"),
                new Subject("Databases", 4, "Systems Engineering"),
                new Subject("Operating Systems", 4, "Systems Engineering"),
                new Subject("Computer Networks", 4, "Systems Engineering"),
                new Subject("Software Engineering I", 4, "Software Engineering"),
                new Subject("Web Programming", 2, "Software Engineering"),
                new Subject("Mobile Development", 3, "Software Engineering"),
                new Subject("Data Structures", 3, "Software Engineering"),
                new Subject("Artificial Intelligence", 5, "Software Engineering"),
                new Subject("Statistics", 4, "Industrial Engineering"),
                new Subject("Operations Research", 4, "Industrial Engineering"),
                new Subject("Simulation", 4, "Industrial Engineering"));
    }

    private List<Teacher> teachers() {
        // The 3rd teacher must keep id 3 (anchor §11).
        return List.of(
                new Teacher("Carlos", "Quintero", "carlos@example.com"),
                new Teacher("Maria", "Torres", "maria@example.com"),
                new Teacher("Andres", "Vargas", "andres@example.com"),
                new Teacher("Laura", "Rincon", "laura@example.com"),
                new Teacher("Jorge", "Beltran", "jorge@example.com"),
                new Teacher("Diana", "Hernandez", "diana@example.com"),
                new Teacher("Luis", "Pineda", "luis@example.com"),
                new Teacher("Sofia", "Ramirez", "sofia@example.com"),
                new Teacher("Pedro", "Castro", "pedro@example.com"),
                new Teacher("Camila", "Gomez", "camila@example.com"));
    }

    private List<Course> courses(List<Subject> subjects, List<Teacher> teachers) {
        // The 7th course must keep id 7 (anchor §11): subject 4, teacher 3, 2026-2, capacity 30.
        return List.of(
                course(subjects.get(0), teachers.get(1), "Monday-Thursday 18:00-20:00", "2026-1", 25),
                course(subjects.get(1), teachers.get(0), "Tuesday-Friday 16:00-18:00", "2026-1", 30),
                course(subjects.get(2), teachers.get(9), "Monday-Wednesday 10:00-12:00", "2026-1", 25),
                course(subjects.get(4), teachers.get(3), "Monday-Thursday 14:00-16:00", "2026-1", 35),
                course(subjects.get(5), teachers.get(4), "Tuesday-Friday 08:00-10:00", "2026-1", 30),
                course(subjects.get(6), teachers.get(5), "Monday-Wednesday 14:00-16:00", "2026-1", 40),
                course(subjects.get(3), teachers.get(2), "Monday-Wednesday 8:00-10:00", "2026-2", 30),
                course(subjects.get(7), teachers.get(6), "Tuesday-Friday 10:00-12:00", "2026-1", 30),
                course(subjects.get(8), teachers.get(7), "Monday-Thursday 08:00-10:00", "2026-1", 25),
                course(subjects.get(9), teachers.get(8), "Wednesday-Friday 16:00-18:00", "2026-1", 30),
                course(subjects.get(10), teachers.get(1), "Monday-Thursday 10:00-12:00", "2026-2", 30),
                course(subjects.get(11), teachers.get(0), "Tuesday-Friday 14:00-16:00", "2026-2", 25),
                course(subjects.get(12), teachers.get(3), "Monday-Thursday 16:00-18:00", "2026-2", 30),
                course(subjects.get(13), teachers.get(4), "Tuesday-Friday 08:00-10:00", "2026-2", 20),
                course(subjects.get(14), teachers.get(5), "Monday-Wednesday 08:00-10:00", "2026-2", 30),
                course(subjects.get(0), teachers.get(6), "Monday-Thursday 14:00-16:00", "2026-2", 25),
                course(subjects.get(3), teachers.get(8), "Wednesday-Friday 10:00-12:00", "2026-1", 35),
                course(subjects.get(6), teachers.get(9), "Monday-Thursday 08:00-10:00", "2026-2", 30),
                course(subjects.get(2), teachers.get(2), "Tuesday-Friday 12:00-14:00", "2026-2", 40),
                course(subjects.get(1), teachers.get(7), "Monday-Thursday 08:00-10:00", "2026-2", 30));
    }

    private Course course(Subject subject, Teacher teacher, String schedule, String period, int capacity) {
        return new Course(subject, teacher, schedule, period, capacity);
    }
}