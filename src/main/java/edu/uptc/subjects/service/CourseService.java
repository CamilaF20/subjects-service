package edu.uptc.subjects.service;

import edu.uptc.subjects.dto.CourseRequest;
import edu.uptc.subjects.dto.CourseResponse;
import edu.uptc.subjects.dto.PageResponse;
import edu.uptc.subjects.entity.Course;
import edu.uptc.subjects.entity.Subject;
import edu.uptc.subjects.entity.Teacher;
import edu.uptc.subjects.exception.ResourceNotFoundException;
import edu.uptc.subjects.mapper.CourseMapper;
import edu.uptc.subjects.repository.CourseRepository;
import edu.uptc.subjects.repository.CourseSpecifications;
import edu.uptc.subjects.repository.SubjectRepository;
import edu.uptc.subjects.repository.TeacherRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository repository;
    private final CourseMapper mapper;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public CourseService(
            CourseRepository repository,
            CourseMapper mapper,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> findAll(PageRequest pageRequest, Long subjectId, Long teacherId, String period) {
        Specification<Course> spec = CourseSpecifications.withFilters(subjectId, teacherId, period);
        Page<Course> page = repository.findAll(spec, pageRequest);
        List<CourseResponse> content = page.getContent().stream().map(mapper::toResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(Long id) {
        return mapper.toResponse(findByIdOrThrow(id));
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        Subject subject = findSubjectOrThrow(request.subjectId());
        Teacher teacher = findTeacherOrThrow(request.teacherId());
        Course course = mapper.toEntity(request, subject, teacher);
        return mapper.toResponse(repository.save(course));
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = findByIdOrThrow(id);
        Subject subject = findSubjectOrThrow(request.subjectId());
        Teacher teacher = findTeacherOrThrow(request.teacherId());
        course.setSubject(subject);
        course.setTeacher(teacher);
        course.setSchedule(request.schedule());
        course.setPeriod(request.period());
        course.setCapacity(request.capacity());
        return mapper.toResponse(course);
    }

    @Transactional
    public void delete(Long id) {
        findByIdOrThrow(id);
        repository.deleteById(id);
    }

    private Course findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course with id " + id + " was not found"));
    }

    private Subject findSubjectOrThrow(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject with id " + subjectId + " was not found"));
    }

    private Teacher findTeacherOrThrow(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher with id " + teacherId + " was not found"));
    }
}