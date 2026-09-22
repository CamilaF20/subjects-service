package edu.uptc.subjects.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherService {

    private final TeacherRepository repository;
    private final TeacherMapper mapper;
    private final CourseRepository courseRepository;

    public TeacherService(TeacherRepository repository, TeacherMapper mapper, CourseRepository courseRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> findAll(PageRequest pageRequest) {
        Page<Teacher> page = repository.findAll(pageRequest);
        List<TeacherResponse> content = page.getContent().stream().map(mapper::toResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(Long id) {
        return mapper.toResponse(findByIdOrThrow(id));
    }

    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        assertEmailIsAvailable(request.email(), null);
        Teacher teacher = mapper.toEntity(request);
        return mapper.toResponse(repository.save(teacher));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        Teacher teacher = findByIdOrThrow(id);
        assertEmailIsAvailable(request.email(), id);
        teacher.setFirstName(request.firstName());
        teacher.setLastName(request.lastName());
        teacher.setEmail(request.email());
        return mapper.toResponse(teacher);
    }

    @Transactional
    public void delete(Long id) {
        findByIdOrThrow(id);
        if (courseRepository.existsByTeacherId(id)) {
            throw new ConflictException("Teacher with id " + id + " has associated courses and cannot be deleted");
        }
        repository.deleteById(id);
    }

    private Teacher findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher with id " + id + " was not found"));
    }

    private void assertEmailIsAvailable(String email, Long idToExclude) {
        boolean taken = idToExclude == null
                ? repository.existsByEmail(email)
                : repository.existsByEmailAndIdNot(email, idToExclude);
        if (taken) {
            throw new ConflictException("A teacher with email '" + email + "' already exists");
        }
    }
}