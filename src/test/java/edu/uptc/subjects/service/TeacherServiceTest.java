package edu.uptc.subjects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.uptc.subjects.dto.PageResponse;
import edu.uptc.subjects.dto.TeacherRequest;
import edu.uptc.subjects.dto.TeacherResponse;
import edu.uptc.subjects.entity.Teacher;
import edu.uptc.subjects.exception.ConflictException;
import edu.uptc.subjects.exception.ResourceNotFoundException;
import edu.uptc.subjects.mapper.TeacherMapper;
import edu.uptc.subjects.repository.CourseRepository;
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

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository repository;

    @Mock
    private TeacherMapper mapper;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private TeacherService service;

    @Test
    void shouldCreateAndReturnResponse_whenEmailIsAvailable() {
        Teacher entity = new Teacher("Andres", "Vargas", "andres@example.com");
        TeacherRequest request = new TeacherRequest("Andres", "Vargas", "andres@example.com");
        TeacherResponse response = new TeacherResponse(3L, "Andres", "Vargas", "andres@example.com");

        when(repository.existsByEmail("andres@example.com")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        TeacherResponse result = service.create(request);

        assertEquals(response, result);
        verify(repository).save(entity);
    }

    @Test
    void shouldThrowConflict_whenEmailIsAlreadyTaken() {
        TeacherRequest request = new TeacherRequest("Andres", "Vargas", "andres@example.com");

        when(repository.existsByEmail("andres@example.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(request));

        assertEquals("A teacher with email 'andres@example.com' already exists", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateFields_whenEmailBelongsToSameTeacher() {
        Teacher entity = new Teacher("Old", "Name", "same@example.com");
        TeacherRequest request = new TeacherRequest("Andres", "Vargas", "same@example.com");

        when(repository.findById(3L)).thenReturn(Optional.of(entity));
        when(repository.existsByEmailAndIdNot("same@example.com", 3L)).thenReturn(false);
        when(mapper.toResponse(entity)).thenReturn(new TeacherResponse(3L, "Andres", "Vargas", "same@example.com"));

        TeacherResponse result = service.update(3L, request);

        assertEquals("Andres", entity.getFirstName());
        assertEquals("Vargas", entity.getLastName());
        assertEquals("same@example.com", entity.getEmail());
        assertEquals(new TeacherResponse(3L, "Andres", "Vargas", "same@example.com"), result);
    }

    @Test
    void shouldThrowConflict_whenUpdateTargetsAnotherTeachersEmail() {
        Teacher entity = new Teacher("A", "B", "a@example.com");
        TeacherRequest request = new TeacherRequest("A", "B", "other@example.com");

        when(repository.findById(3L)).thenReturn(Optional.of(entity));
        when(repository.existsByEmailAndIdNot("other@example.com", 3L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(3L, request));
    }

    @Test
    void shouldReturnResponse_whenFindingById() {
        Teacher entity = new Teacher("Andres", "Vargas", "andres@example.com");

        when(repository.findById(3L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(new TeacherResponse(3L, "Andres", "Vargas", "andres@example.com"));

        TeacherResponse result = service.findById(3L);

        assertEquals(3L, result.id());
        assertEquals("andres@example.com", result.email());
    }

    @Test
    void shouldThrow_whenFindingByIdThatDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));

        assertEquals("Teacher with id 99 was not found", ex.getMessage());
    }

    @Test
    void shouldReturnPageResponse_whenFindingAll() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Teacher t1 = new Teacher("A", "B", "a@example.com");
        Teacher t2 = new Teacher("C", "D", "c@example.com");
        Page<Teacher> page = new PageImpl<>(List.of(t1, t2), pageRequest, 2);

        when(repository.findAll(pageRequest)).thenReturn(page);
        when(mapper.toResponse(t1)).thenReturn(new TeacherResponse(1L, "A", "B", "a@example.com"));
        when(mapper.toResponse(t2)).thenReturn(new TeacherResponse(2L, "C", "D", "c@example.com"));

        PageResponse<TeacherResponse> result = service.findAll(pageRequest);

        assertEquals(2, result.content().size());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void shouldDelete_whenTeacherExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Teacher("A", "B", "a@example.com")));
        when(courseRepository.existsByTeacherId(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void shouldThrowConflict_whenDeletingTeacherWithCourses() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Teacher("A", "B", "a@example.com")));
        when(courseRepository.existsByTeacherId(1L)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.delete(1L));

        assertEquals("Teacher with id 1 has associated courses and cannot be deleted", ex.getMessage());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void shouldThrow_whenDeletingTeacherThatDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }
}