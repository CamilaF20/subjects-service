package edu.uptc.subjects.config;

import edu.uptc.subjects.exception.InvalidQueryParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageRequestFactory {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "asc";

    public PageRequest create(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortDirection,
            Map<String, String> allowedSortFields) {

        int number = pageNumber != null ? pageNumber : 0;
        int size = pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;
        String field = sortBy != null && !sortBy.isBlank() ? sortBy : DEFAULT_SORT_BY;
        String direction = sortDirection != null && !sortDirection.isBlank() ? sortDirection : DEFAULT_SORT_DIRECTION;

        List<String> errors = validate(number, size, field, direction, allowedSortFields);
        if (!errors.isEmpty()) {
            throw new InvalidQueryParameterException(String.join("; ", errors));
        }

        Sort sort = Sort.by(Sort.Direction.fromString(direction.toUpperCase()), allowedSortFields.get(field));
        return PageRequest.of(number, size, sort);
    }

    private List<String> validate(
            int number,
            int size,
            String field,
            String direction,
            Map<String, String> allowedSortFields) {

        List<String> errors = new ArrayList<>();
        if (number < 0) {
            errors.add("pageNumber must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            errors.add("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!allowedSortFields.containsKey(field)) {
            errors.add("sortBy '" + field + "' is not allowed. Allowed values: " + allowedSortFields.keySet());
        }
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            errors.add("sortDirection must be 'asc' or 'desc'");
        }
        return errors;
    }
}