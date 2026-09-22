package edu.uptc.subjects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.uptc.subjects.dto.CourseRequest;
import edu.uptc.subjects.dto.CourseResponse;
import edu.uptc.subjects.dto.PageResponse;
import edu.uptc.subjects.entity.Course;
import edu.uptc.subjects.entity.Subject;
import edu.uptc.subjects.entity.Teacher;
import edu.uptc.subjects.exception.ResourceNotFoundException;
import edu.uptc.subjects.mapper.CourseMapper;
import edu.uptc.subjects.repository.CourseRepository;
import edu.uptc.subjects.repository.SubjectRepository;
import edu.uptc.subjects.repository.TeacherRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository repository;

    @Mock
    private CourseMapper mapper;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private CourseService service;

    private static final Subject SUBJECT = new Subject("Distributed Systems", 3, "Systems Engineering");
    private static final Teacher TEACHER = new Teacher("Andres", "Vargas", "andres@example.com");

    @Test
    void shouldCreateAndReturnResponse_whenSubjectAndTeacherExist() {
        Course entity = new Course(SUBJECT, TEACHER, "Monday 8:00", "2026-2", 30);
        CourseRequest request = new CourseRequest(4L, 3L, "Monday 8:00", "2026-2", 30);
        CourseResponse response = new CourseResponse(7L, 4L, 3L, "Monday 8:00", "2026-2", 30);

        when(subjectRepository.findById(4L)).thenReturn(Optional.of(SUBJECT));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(TEACHER));
        when(mapper.toEntity(request, SUBJECT, TEACHER)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        CourseResponse result = service.create(request);

        assertEquals(response, result);
        verify(repository).save(entity);
    }

    @Test
    void shouldThrow_whenCreateReferencesMissingSubject() {
        CourseRequest request = new CourseRequest(99L, 3L, "Monday 8:00", "2026-2", 30);

        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.create(request));

        assertEquals("Subject with id 99 was not found", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrow_whenCreateReferencesMissingTeacher() {
        CourseRequest request = new CourseRequest(4L, 99L, "Monday 8:00", "2026-2", 30);

        when(subjectRepository.findById(4L)).thenReturn(Optional.of(SUBJECT));
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.create(request));

        assertEquals("Teacher with id 99 was not found", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldReturnResponse_whenFindingById() {
        Course entity = new Course(SUBJECT, TEACHER, "Monday 8:00", "2026-2", 30);

        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(new CourseResponse(7L, 4L, 3L, "Monday 8:00", "2026-2", 30));

        CourseResponse result = service.findById(7L);

        assertEquals(7L, result.id());
        assertEquals(4L, result.subjectId());
        assertEquals(3L, result.teacherId());
    }

    @Test
    void shouldThrow_whenFindingByIdThatDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));

        assertEquals("Course with id 99 was not found", ex.getMessage());
    }

    @Test
    void shouldReturnPageResponse_whenFindingAll() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Course c1 = new Course(SUBJECT, TEACHER, "Monday 8:00", "2026-2", 30);
        Course c2 = new Course(SUBJECT, TEACHER, "Tuesday 10:00", "2026-1", 25);
        Page<Course> page = new PageImpl<>(List.of(c1, c2), pageRequest, 2);

        when(repository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(page);
        when(mapper.toResponse(c1)).thenReturn(new CourseResponse(1L, 4L, 3L, "Monday 8:00", "2026-2", 30));
        when(mapper.toResponse(c2)).thenReturn(new CourseResponse(2L, 4L, 3L, "Tuesday 10:00", "2026-1", 25));

        PageResponse<CourseResponse> result = service.findAll(pageRequest, null, null, null);

        assertEquals(2, result.content().size());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void shouldUpdateFields_whenCourseExists() {
        Course entity = new Course(SUBJECT, TEACHER, "Old", "2026-1", 20);
        CourseRequest request = new CourseRequest(4L, 3L, "New", "2026-2", 35);

        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        when(subjectRepository.findById(4L)).thenReturn(Optional.of(SUBJECT));
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(TEACHER));
        when(mapper.toResponse(entity)).thenReturn(new CourseResponse(7L, 4L, 3L, "New", "2026-2", 35));

        CourseResponse result = service.update(7L, request);

        assertEquals("New", entity.getSchedule());
        assertEquals("2026-2", entity.getPeriod());
        assertEquals(35, entity.getCapacity());
        assertEquals(new CourseResponse(7L, 4L, 3L, "New", "2026-2", 35), result);
    }

    @Test
    void shouldThrow_whenUpdatingCourseThatDoesNotExist() {
        CourseRequest request = new CourseRequest(4L, 3L, "New", "2026-2", 35);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(99L, request));
    }

    @Test
    void shouldDelete_whenCourseExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Course(SUBJECT, TEACHER, "S", "2026-1", 0)));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void shouldThrow_whenDeletingCourseThatDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }
}