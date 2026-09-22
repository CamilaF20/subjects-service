package edu.uptc.subjects.service;

import edu.uptc.subjects.dto.PageResponse;
import edu.uptc.subjects.dto.SubjectRequest;
import edu.uptc.subjects.dto.SubjectResponse;
import edu.uptc.subjects.entity.Subject;
import edu.uptc.subjects.exception.ConflictException;
import edu.uptc.subjects.exception.ResourceNotFoundException;
import edu.uptc.subjects.mapper.SubjectMapper;
import edu.uptc.subjects.repository.CourseRepository;
import edu.uptc.subjects.repository.SubjectRepository;
import edu.uptc.subjects.repository.SubjectSpecifications;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository repository;
    private final SubjectMapper mapper;
    private final CourseRepository courseRepository;

    public SubjectService(SubjectRepository repository, SubjectMapper mapper, CourseRepository courseRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> findAll(PageRequest pageRequest, String name, String program, Integer credits) {
        Specification<Subject> spec = SubjectSpecifications.withFilters(name, program, credits);
        Page<Subject> page = repository.findAll(spec, pageRequest);
        List<SubjectResponse> content = page.getContent().stream().map(mapper::toResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public SubjectResponse findById(Long id) {
        return mapper.toResponse(findByIdOrThrow(id));
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        Subject subject = mapper.toEntity(request);
        return mapper.toResponse(repository.save(subject));
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = findByIdOrThrow(id);
        subject.setName(request.name());
        subject.setCredits(request.credits());
        subject.setProgram(request.program());
        return mapper.toResponse(subject);
    }

    @Transactional
    public void delete(Long id) {
        findByIdOrThrow(id);
        if (courseRepository.existsBySubjectId(id)) {
            throw new ConflictException("Subject with id " + id + " has associated courses and cannot be deleted");
        }
        repository.deleteById(id);
    }

    private Subject findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject with id " + id + " was not found"));
    }
}