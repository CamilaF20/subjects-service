package edu.uptc.subjects.repository;

import edu.uptc.subjects.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    boolean existsBySubjectId(Long subjectId);

    boolean existsByTeacherId(Long teacherId);
}