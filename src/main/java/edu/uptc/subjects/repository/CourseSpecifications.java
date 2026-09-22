package edu.uptc.subjects.repository;

import edu.uptc.subjects.entity.Course;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CourseSpecifications {

    private CourseSpecifications() {
    }

    public static Specification<Course> withFilters(Long subjectId, Long teacherId, String period) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (subjectId != null) {
                predicates.add(cb.equal(root.get("subject").get("id"), subjectId));
            }
            if (teacherId != null) {
                predicates.add(cb.equal(root.get("teacher").get("id"), teacherId));
            }
            if (period != null && !period.isBlank()) {
                predicates.add(cb.equal(root.get("period"), period));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}