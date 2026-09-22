package edu.uptc.subjects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.uptc.subjects.dto.PageResponse;
import edu.uptc.subjects.dto.SubjectRequest;
import edu.uptc.subjects.dto.SubjectResponse;
import edu.uptc.subjects.entity.Subject;
import edu.uptc.subjects.exception.ConflictException;
import edu.uptc.subjects.exception.ResourceNotFoundException;
import edu.uptc.subjects.mapper.SubjectMapper;
import edu.uptc.subjects.repository.CourseRepository;
import edu.uptc.subjects.repository.SubjectRepository;
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
class SubjectServiceTest {

    @Mock
    private SubjectRepository repository;

    @Mock
    private SubjectMapper mapper;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private SubjectService service;

    @Test
    void shouldCreateAndReturnResponse_whenRequestIsValid() {
        Subject entity = new Subject("Distributed Systems", 3, "Systems Engineering");
        SubjectRequest request = new SubjectRequest("Distributed Systems", 3, "Systems Engineering");
        SubjectResponse response = new SubjectResponse(1L, "Distributed Systems", 3, "Systems Engineering");

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        SubjectResponse result = service.create(request);

        assertEquals(response, result);
        verify(repository).save(entity);
    }

    @Test
    void shouldReturnResponse_whenFindingById() {
        Subject entity = new Subject("Distributed Systems", 3, "Systems Engineering");

        when(repository.findById(4L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(new SubjectResponse(4L, "Distributed Systems", 3, "Systems Engineering"));

        SubjectResponse result = service.findById(4L);

        assertEquals(4L, result.id());
        assertEquals("Distributed Systems", result.name());
    }

    @Test
    void shouldThrow_whenFindingByIdThatDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));

        assertEquals("Subject with id 99 was not found", ex.getMessage());
    }

    @Test
    void shouldReturnPageResponse_whenFindingAll() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Subject s1 = new Subject("A", 3, "P");
        Subject s2 = new Subject("B", 4, "P");
        Page<Subject> page = new PageImpl<>(List.of(s1, s2), pageRequest, 2);

        when(repository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(page);
        when(mapper.toResponse(s1)).thenReturn(new SubjectResponse(1L, "A", 3, "P"));
        when(mapper.toResponse(s2)).thenReturn(new SubjectResponse(2L, "B", 4, "P"));

        PageResponse<SubjectResponse> result = service.findAll(pageRequest, "na", "pro", 3);

        assertEquals(2, result.content().size());
        assertEquals(0, result.pageNumber());
        assertEquals(10, result.pageSize());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void shouldUpdateFields_whenSubjectExists() {
        Subject entity = new Subject("Old", 3, "Old Program");
        SubjectRequest request = new SubjectRequest("New", 5, "New Program");

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(new SubjectResponse(1L, "New", 5, "New Program"));

        SubjectResponse result = service.update(1L, request);

        assertEquals("New", entity.getName());
        assertEquals(5, entity.getCredits());
        assertEquals("New Program", entity.getProgram());
        assertEquals(new SubjectResponse(1L, "New", 5, "New Program"), result);
    }

    @Test
    void shouldThrow_whenUpdatingSubjectThatDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(99L, new SubjectRequest("X", 3, "Y")));
    }

    @Test
    void shouldDelete_whenSubjectExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Subject("A", 3, "P")));
        when(courseRepository.existsBySubjectId(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void shouldThrowConflict_whenDeletingSubjectWithCourses() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Subject("A", 3, "P")));
        when(courseRepository.existsBySubjectId(1L)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.delete(1L));

        assertEquals("Subject with id 1 has associated courses and cannot be deleted", ex.getMessage());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void shouldThrow_whenDeletingSubjectThatDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }
}