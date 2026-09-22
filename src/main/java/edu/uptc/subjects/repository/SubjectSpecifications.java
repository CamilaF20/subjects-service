package edu.uptc.subjects.repository;

import edu.uptc.subjects.entity.Subject;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class SubjectSpecifications {

    private SubjectSpecifications() {
    }

    public static Specification<Subject> withFilters(String name, String program, Integer credits) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (program != null && !program.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("program")), "%" + program.toLowerCase() + "%"));
            }
            if (credits != null) {
                predicates.add(cb.equal(root.get("credits"), credits));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}