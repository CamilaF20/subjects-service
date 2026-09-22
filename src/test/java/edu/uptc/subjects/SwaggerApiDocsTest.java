package edu.uptc.subjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExposeExactlyTheFifteenContractEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = root.get("paths");

        assertEquals("Subjects Service API", root.at("/info/title").asText());

        Map<String, Set<String>> expected = Map.of(
                "/api/subjects", ops("get", "post"),
                "/api/subjects/{id}", ops("get", "put", "delete"),
                "/api/teachers", ops("get", "post"),
                "/api/teachers/{id}", ops("get", "put", "delete"),
                "/api/courses", ops("get", "post"),
                "/api/courses/{id}", ops("get", "put", "delete"));

        Set<String> pathNames = new TreeSet<>();
        paths.fieldNames().forEachRemaining(pathNames::add);

        assertEquals(expected.keySet().size(), pathNames.stream().filter(p -> !p.startsWith("/test")).count(),
                "Only the §6.1 contract paths plus test-support paths must exist");
        assertTrue(pathNames.stream().filter(p -> p.startsWith("/test")).allMatch(p -> p.matches("/test/[a-z-]+")),
                "Non-contract paths must be the test-support endpoints only");

        int totalOperations = 0;
        for (Map.Entry<String, Set<String>> entry : expected.entrySet()) {
            assertTrue(pathNames.contains(entry.getKey()), "Contract path must exist: " + entry.getKey());
            Set<String> actual = new TreeSet<>();
            paths.get(entry.getKey()).fieldNames().forEachRemaining(actual::add);
            assertEquals(entry.getValue(), actual, "HTTP methods for " + entry.getKey());
            totalOperations += actual.size();
        }

        assertEquals(15, totalOperations, "Exactly the 15 endpoints defined in §6.1");
    }

    private static Set<String> ops(String... methods) {
        return new TreeSet<>(Arrays.asList(methods));
    }
}