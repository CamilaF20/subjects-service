package edu.uptc.subjects.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.uptc.subjects.exception.InvalidQueryParameterException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class PageRequestFactoryTest {

    private final PageRequestFactory factory = new PageRequestFactory();

    private static final Map<String, String> SUBJECT_FIELDS = Map.of(
            "id", "id",
            "name", "name",
            "credits", "credits",
            "program", "program");

    @Test
    void shouldApplyDefaults_whenParamsAreNull() {
        PageRequest request = factory.create(null, null, null, null, SUBJECT_FIELDS);

        assertEquals(0, request.getPageNumber());
        assertEquals(10, request.getPageSize());
        assertEquals("id", request.getSort().getOrderFor("id").getProperty());
        assertEquals(Sort.Direction.ASC, request.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void shouldApplyDefaultSortBy_whenBlank() {
        PageRequest request = factory.create(0, 10, " ", "asc", SUBJECT_FIELDS);

        assertEquals("id", request.getSort().getOrderFor("id").getProperty());
    }

    @Test
    void shouldBuildRequest_whenParamsAreValid() {
        PageRequest request = factory.create(2, 25, "name", "desc", SUBJECT_FIELDS);

        assertEquals(2, request.getPageNumber());
        assertEquals(25, request.getPageSize());
        assertEquals("name", request.getSort().getOrderFor("name").getProperty());
        assertEquals(Sort.Direction.DESC, request.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void shouldIgnoreCase_whenSortDirectionIsGiven() {
        PageRequest request = factory.create(0, 10, "id", "DESC", SUBJECT_FIELDS);

        assertEquals(Sort.Direction.DESC, request.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void shouldMapApiFieldToEntityProperty() {
        Map<String, String> courseFields = Map.of(
                "id", "id",
                "subjectId", "subject.id",
                "schedule", "schedule");

        PageRequest request = factory.create(0, 10, "subjectId", "asc", courseFields);

        assertEquals("subject.id", request.getSort().getOrderFor("subject.id").getProperty());
    }

    @Test
    void shouldThrow_whenPageNumberIsNegative() {
        InvalidQueryParameterException ex = assertThrows(InvalidQueryParameterException.class,
                () -> factory.create(-1, 10, "id", "asc", SUBJECT_FIELDS));

        assertTrue(ex.getMessage().contains("pageNumber"));
    }

    @Test
    void shouldThrow_whenPageSizeIsOutOfRange() {
        InvalidQueryParameterException ex = assertThrows(InvalidQueryParameterException.class,
                () -> factory.create(0, 0, "id", "asc", SUBJECT_FIELDS));

        assertTrue(ex.getMessage().contains("pageSize"));
    }

    @Test
    void shouldThrow_whenPageSizeExceedsMaximum() {
        InvalidQueryParameterException ex = assertThrows(InvalidQueryParameterException.class,
                () -> factory.create(0, 101, "id", "asc", SUBJECT_FIELDS));

        assertTrue(ex.getMessage().contains("pageSize"));
    }

    @Test
    void shouldThrow_whenSortByIsNotAllowed() {
        InvalidQueryParameterException ex = assertThrows(InvalidQueryParameterException.class,
                () -> factory.create(0, 10, "invalid", "asc", SUBJECT_FIELDS));

        assertTrue(ex.getMessage().contains("sortBy"));
        assertTrue(ex.getMessage().contains("invalid"));
    }

    @Test
    void shouldThrow_whenSortDirectionIsInvalid() {
        InvalidQueryParameterException ex = assertThrows(InvalidQueryParameterException.class,
                () -> factory.create(0, 10, "id", "sideways", SUBJECT_FIELDS));

        assertTrue(ex.getMessage().contains("sortDirection"));
    }

    @Test
    void shouldReportAllErrors_whenMultipleParamsAreInvalid() {
        InvalidQueryParameterException ex = assertThrows(InvalidQueryParameterException.class,
                () -> factory.create(-1, 0, "junk", "up", SUBJECT_FIELDS));

        assertTrue(ex.getMessage().contains("pageNumber"));
        assertTrue(ex.getMessage().contains("pageSize"));
        assertTrue(ex.getMessage().contains("sortBy"));
        assertTrue(ex.getMessage().contains("sortDirection"));
    }
}