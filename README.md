# subjects-service

> Subjects module for Laboratory 3 (Distributed Systems — UPTC). Manages the academic offering: **Subjects**, **Teachers** and **Courses**. It runs as one of four independent processes behind an API Gateway.

```
Client → API Gateway :8080 ─┬─► students-service     :8081 → students_db
                            ├─► subjects-service     :8082 → subjects_db   ◄── THIS MODULE
                            └─► enrollments-service  :8083 → enrollments_db
```

Inside Docker, other services reach this one at `http://subjects-service:8082` (never `localhost`). This module never calls the Gateway or any other module.

---

## 1. What this module does

- **Subject** — the catalog entry (what a subject *is*). Example: `Distributed Systems`.
- **Teacher** — a person who teaches.
- **Course** — the concrete offering of a Subject: who teaches it, when (`schedule`), in which `period`, and with what `capacity`.

```
Subject  ◄── N:1 ──  Course  ── N:1 ──►  Teacher
```

Full CRUD for the three resources, with pagination, sorting and filters, plus a uniform JSON error format.

## 2. Stack

| Concern | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 (standalone `subjects_db`) |
| Build | Maven 3.9 |
| API docs | springdoc-openapi 2.8.x (`springdoc-openapi-starter-webmvc-ui`) |
| Validation | Jakarta Validation (`spring-boot-starter-validation`) |
| Health | Spring Boot Actuator (`spring-boot-starter-actuator`) |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc |
| Test DB | H2 in PostgreSQL mode (test scope only) |
| Container | Docker (multi-stage build, non-root user) |

## 3. Domain model

All ids are `Long` and generated. Database columns are `snake_case`; the API exposes numeric `subjectId` and `teacherId`, never nested objects.

| Table/entity | Fields |
|---|---|
| `subjects` | `id`, `name` (required, ≤ 150), `credits` (1–10), `program` (required, ≤ 150) |
| `teachers` | `id`, `firstName` (≤ 80), `lastName` (≤ 80), `email` (valid, ≤ 150, **unique**) |
| `courses` | `id`, `subject` (N:1 → Subject), `teacher` (N:1 → Teacher), `schedule` (≤ 100), `period` (`YYYY-N`, e.g. `2026-2`), `capacity` (> 0) |

Every Course must reference an existing Subject and an existing Teacher. A Subject or Teacher that still has Courses cannot be deleted (HTTP 409). Deleting a Course is always allowed by design.

## 4. How to run

### (a) Locally with Maven

Prerequisites: JDK 21+ and Maven. A PostgreSQL database must be reachable at `localhost:5433` with a database `subjects_db` (e.g. the compose `subjects-db` container, which maps `5433:5432`). The credentials come from environment variables (set them before running):

```bash
export DB_HOST=localhost
export DB_PORT=5433
export DB_NAME=subjects_db
export DB_USERNAME=subjects_user
export DB_PASSWORD=change_me
export SEED_ENABLED=true

mvn -q spring-boot:run
```

Verified boot log: `Started SubjectsServiceApplication ... Tomcat started on port 8082`. The service becomes reachable at `http://localhost:8082`.

Note: `mvn spring-boot:run` does **not** read a `.env` file; export the variables in your shell first.

### (b) With Docker Compose (recommended)

```bash
docker compose up --build
```

That builds the multi-stage Dockerfile and starts both services. `subjects-db` runs PostgreSQL 16 and `subjects-service` runs the application as the non-root user `appuser`.

Verified output:

```
subjects-service  Built
Container subjects-service-subjects-service-1  Created
subjects-service-1 | Started SubjectsServiceApplication in 4.342 seconds (process running for 4.749)
subjects-service-1 | Seed completed: 15 subjects, 10 teachers, 20 courses.
```

Compose status (verified with `docker compose ps`):

```
NAME                                  IMAGE                               COMMAND                  SERVICE            STATUS                    PORTS
subjects-service-subjects-db-1        postgres:16                         "docker-entrypoint.s…"   subjects-db        Up 12 seconds (healthy)   0.0.0.0:5433->5432/tcp, [::]:5433->5432/tcp
subjects-service-subjects-service-1   subjects-service-subjects-service   "java -jar app.jar"      subjects-service   Up 7 seconds              0.0.0.0:8082->8082/tcp, [::]:8082->8082/tcp
```

Health check (verified):

```bash
curl http://localhost:8082/actuator/health
# {"status":"UP"}   → HTTP 200
```

## 5. Environment variables

| Variable | Purpose | Local default | In Docker |
|---|---|---|---|
| `DB_HOST` | DB host | `localhost` | `subjects-db` (set by compose) |
| `DB_PORT` | DB port | `5433` (host-mapped) | `5432` (container port, **not** 5433) |
| `DB_NAME` | Database name | `subjects_db` | `subjects_db` |
| `DB_USERNAME` | DB user | *(no default, required)* | from `.env` |
| `DB_PASSWORD` | DB password | *(no default, required)* | from `.env` |
| `SEED_ENABLED` | Load sample data on empty DB | `true` | `true` |

Copy `.env.example` to `.env` and fill in real values. `.env` is gitignored; never commit it.

## 6. Endpoints

Base URL (locally): `http://localhost:8082`

| Resource | Method | Path | Success |
|---|---|---|---|
| Subject | GET (list) | `/api/subjects` | 200 |
| Subject | GET | `/api/subjects/{id}` | 200 |
| Subject | POST | `/api/subjects` | 201 + `Location` header |
| Subject | PUT | `/api/subjects/{id}` | 200 |
| Subject | DELETE | `/api/subjects/{id}` | 204 |
| Teacher | GET (list) | `/api/teachers` | 200 |
| Teacher | GET | `/api/teachers/{id}` | 200 |
| Teacher | POST | `/api/teachers` | 201 + `Location` header |
| Teacher | PUT | `/api/teachers/{id}` | 200 |
| Teacher | DELETE | `/api/teachers/{id}` | 204 |
| Course | GET (list) | `/api/courses` | 200 |
| Course | GET | `/api/courses/{id}` | 200 |
| Course | POST | `/api/courses` | 201 + `Location` header |
| Course | PUT | `/api/courses/{id}` | 200 |
| Course | DELETE | `/api/courses/{id}` | 204 |

`PUT` replaces the whole resource (same body as `POST`).

### Example requests (verified with `curl`)

```bash
# Get a subject by id
curl http://localhost:8082/api/subjects/4
# {"id":4,"name":"Distributed Systems","credits":3,"program":"Systems Engineering"}

# Get a teacher by id
curl http://localhost:8082/api/teachers/3
# {"id":3,"firstName":"Andres","lastName":"Vargas","email":"andres@example.com"}

# Get a course by id
curl http://localhost:8082/api/courses/7
# {"id":7,"subjectId":4,"teacherId":3,"schedule":"Monday-Wednesday 8:00-10:00","period":"2026-2","capacity":30}

# Create a subject (201 + Location header)
curl -i -X POST http://localhost:8082/api/subjects \
  -H 'Content-Type: application/json' \
  -d '{"name":"Biology","credits":4,"program":"Biology Engineering"}'
# HTTP/1.1 201
# Location: http://localhost:8082/api/subjects/16
# {"id":16,"name":"Biology","credits":4,"program":"Biology Engineering"}

# Update a subject (PUT replaces the whole resource)
curl -X PUT http://localhost:8082/api/subjects/16 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Biology II","credits":5,"program":"Biology Engineering"}'
# {"id":16,"name":"Biology II","credits":5,"program":"Biology Engineering"}

# Delete a subject
curl -X DELETE http://localhost:8082/api/subjects/16
# HTTP 204
```

## 7. Pagination, sorting and filters

All three `GET` lists share the same pagination/sorting parameters (defaults in parens):

| Param | Default | Rules |
|---|---|---|
| `pageNumber` | `0` | `>= 0` |
| `pageSize` | `10` | `1..100` |
| `sortBy` | `id` | whitelisted values (below) |
| `sortDirection` | `asc` | `asc` or `desc` (case-insensitive) |

Filters are optional, combined with **AND**; blank values are ignored:

| Endpoint | Filter | Match |
|---|---|---|
| `/api/subjects` | `name` | contains, case-insensitive |
| `/api/subjects` | `program` | contains, case-insensitive |
| `/api/subjects` | `credits` | exact |
| `/api/courses` | `subjectId` | exact |
| `/api/courses` | `teacherId` | exact |
| `/api/courses` | `period` | exact |
| `/api/teachers` | *(none — pagination and sorting only)* | |

Allowed `sortBy` values (anything else → 400):

- Subjects: `id`, `name`, `credits`, `program`
- Teachers: `id`, `firstName`, `lastName`, `email`
- Courses: `id`, `subjectId`, `teacherId`, `schedule`, `period`, `capacity` (`subjectId` sorts by `subject.id`, `teacherId` by `teacher.id`)

The response envelope is uniform:

```json
{ "content": [], "pageNumber": 0, "pageSize": 10, "totalElements": 100, "totalPages": 10 }
```

### Examples (verified with `curl`)

```bash
# Courses of period 2026-2, sorted by period desc, 3 per page
curl "http://localhost:8082/api/courses?pageNumber=0&pageSize=3&sortBy=period&sortDirection=desc&period=2026-2"
# {"content":[{"id":7,...,"period":"2026-2","capacity":30},{"id":11,...},{"id":12,...}],
#  "pageNumber":0,"pageSize":3,"totalElements":10,"totalPages":4}

# Subjects whose name contains "Discrete"
curl "http://localhost:8082/api/subjects?pageSize=5&name=Discrete"
# {"content":[{"id":3,"name":"Discrete Mathematics","credits":3,"program":"Systems Engineering"}],
#  "pageNumber":0,"pageSize":5,"totalElements":1,"totalPages":1}

# Teachers sorted by email descending, 2 per page
curl "http://localhost:8082/api/teachers?pageSize=2&sortBy=email&sortDirection=desc"
# {"content":[{"id":8,"firstName":"Sofia",...,"email":"sofia@example.com"},{"id":9,...,"email":"pedro@example.com"}],
#  "pageNumber":0,"pageSize":2,"totalElements":10,"totalPages":5}
```

Invalid pagination, sorting or filter parameters produce `400`.

## 8. Error format

Every failure is returned by a single `@RestControllerAdvice` (`GlobalExceptionHandler`) with one shape:

```json
{
  "timestamp": "2026-09-14T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Course with id 12 was not found",
  "path": "/api/courses/12"
}
```

| Situation | Status |
|---|---|
| Resource id does not exist | 404 |
| Referenced `subjectId` / `teacherId` missing on create/update | 404 |
| Bean validation fails (body) | 400 |
| Malformed JSON / wrong type / bad path or query value | 400 |
| Invalid page/sort parameter | 400 |
| Duplicate teacher email | 409 |
| Deleting a Subject/Teacher that still has Courses | 409 |
| Anything unexpected | 500 |

### Examples (verified with `curl`)

```bash
# 404
curl http://localhost:8082/api/courses/999
# {"timestamp":"2026-09-21T22:55:38","status":404,"error":"Not Found",
#  "message":"Course with id 999 was not found","path":"/api/courses/999"}   → HTTP 404

# 400 (validation)
curl -X POST http://localhost:8082/api/subjects -H 'Content-Type: application/json' \
  -d '{"name":"Botany","credits":0,"program":"Systems Engineering"}'
# {"timestamp":"...","status":400,"error":"Bad Request",
#  "message":"credits: must be greater than or equal to 1","path":"/api/subjects"}   → HTTP 400

# 409 (duplicate email)
curl -X POST http://localhost:8082/api/teachers -H 'Content-Type: application/json' \
  -d '{"firstName":"Andres","lastName":"Vargas","email":"andres@example.com"}'
# {"timestamp":"...","status":409,"error":"Conflict",
#  "message":"A teacher with email 'andres@example.com' already exists","path":"/api/teachers"}   → HTTP 409
```

## 9. Swagger and health

- Swagger UI: `http://localhost:8082/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`
- Health: `http://localhost:8082/actuator/health`

All three were verified with `curl` while running under Docker (HTTP 200).

## 10. How to run the tests

```bash
mvn -B test
```

Verified result:

```
[INFO] Tests run: 130, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Tests use the `test` profile with H2 (PostgreSQL mode) and the seeder disabled. They cover unit (service with Mockito), web (`@WebMvcTest` + MockMvc) and integration (`@SpringBootTest`) layers, including CRUD, pagination, sorting, the 3 filters per list, error handling and the §6.4 contract-shape checks.

## 11. Seed data and fixed anchors

`DataSeeder` (a `CommandLineRunner`) runs only when `app.seed.enabled=true` **and** the database is empty (idempotent). Inserts happen in a fixed order (subjects → teachers → courses) so ids are deterministic. The seeded volume is **15 subjects, 10 teachers, 20 courses** across periods `2026-1` and `2026-2`.

Verified on a fresh database:

```
subjects-service-1 | Seed completed: 15 subjects, 10 teachers, 20 courses.
```

Fixed anchors (they match the team's `/detail` contract — keep them unchanged):

| Entity | id | Values |
|---|---|---|
| Subject | 4 | `Distributed Systems`, credits 3, program `Systems Engineering` |
| Teacher | 3 | `Andres Vargas`, `andres@example.com` |
| Course | 7 | subjectId 4, teacherId 3, `Monday-Wednesday 8:00-10:00`, `2026-2`, capacity 30 |

`GET /api/courses/7` returns exactly:

```json
{"id":7,"subjectId":4,"teacherId":3,"schedule":"Monday-Wednesday 8:00-10:00","period":"2026-2","capacity":30}
```

## 12. Known limitations

- `spring.jpa.hibernate.ddl-auto: update` — acceptable for this lab, but in production schema migrations (e.g. Flyway) should be used.
- No cross-module integrity checks: deleting a Course is always allowed even if another module still references its `courseId`. This is the distributed-consistency gap the course formalizes later, and it is intentional.
- A Subject or Teacher with associated Courses cannot be deleted within this module (409), protecting integrity *inside* this database only.
- `spring-boot:run` does not read `.env`; local runs need the environment variables exported in the shell.

## 13. Author / responsible team member

Maria Camila Figueredo Molano — Distributed Systems Laboratory 3 (UPTC), Subjects module.