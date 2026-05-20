# Backend Testing Guide

This document describes the Football-501 Spring Boot test suite, explains how to run each tier, and — most importantly — guides you through what to update when you change production code.

---

## Table of Contents

1. [Test Tiers Overview](#test-tiers-overview)
2. [Running Tests](#running-tests)
3. [When You Change X, Update Y](#when-you-change-x-update-y)
4. [H2 Compatibility Rules](#h2-compatibility-rules)
5. [Testcontainers Tier](#testcontainers-tier)
6. [Mockito Patterns](#mockito-patterns)
7. [Test Data Setup](#test-data-setup)

---

## Test Tiers Overview

| Tier | Tooling | Speed | What It Tests | When to Run |
|------|---------|-------|---------------|-------------|
| **3 — Unit** | JUnit 5 + Mockito | ~2s | Single class in isolation | Every change |
| **1 — Integration (H2)** | SpringBootTest + H2 | ~15s | Full HTTP stack, in-memory DB | Every change |
| **2 — Container (PostgreSQL)** | Testcontainers + PostgreSQL | ~45s | PostgreSQL-specific features (pg_trgm fuzzy matching) | When changing answer lookup or matching logic |

### Tier 3 — Unit tests

Located in `src/test/java/com/football501/`:

| Class | Tests |
|-------|-------|
| `engine/DartsValidatorTest` | Invalid/valid score boundary cases |
| `engine/ScoringServiceTest` | Bust rules, checkout range, sequential deduction |
| `engine/AnswerEvaluatorTest` | Answer evaluation orchestration with mocked repos |
| `controller/PracticeGameControllerTest` | HTTP layer only (`@WebMvcTest`), all services mocked |
| `service/QuestionServiceTest` | Question selection, filtering, category lookup |
| `service/MatchServiceTest` | Match lifecycle, game completion, win conditions |
| `service/GameServiceTest` | Game creation, score processing |

These use `@ExtendWith(MockitoExtension.class)`. No Spring context, no database, no Docker.

### Tier 1 — Integration tests (H2)

Located in `src/test/java/com/football501/controller/`:

| Class | Tests | What it covers |
|-------|-------|----------------|
| `PracticeGameIntegrationTest` | 8 | Start game, submit answers, duplicate detection, score deduction, cumulative scoring |
| `AdminAnswerIntegrationTest` | 7 | Admin CRUD, bulk create with duplicate-skipping, reported created/skipped counts |

Also in `src/test/java/com/football501/`:

| Class | Tests | What it covers |
|-------|-------|----------------|
| `engine/AnswerEvaluatorIntegrationTest` | 2 | `@DataJpaTest` — evaluator + repository layer, H2. One test `@Disabled` for fuzzy path. |
| `repository/AnswerRepositoryTest` | 4 | Repository queries on H2. Two tests `@Disabled` for pg_trgm queries. |

These all extend `BaseTest` (except `AnswerRepositoryTest` and `AnswerEvaluatorIntegrationTest` which use `@DataJpaTest`) and share the `test` Spring profile which wires H2 in PostgreSQL compatibility mode.

### Tier 2 — Container tests (PostgreSQL)

Located in `src/test/java/com/football501/engine/`:

| Class | Tests | What it covers |
|-------|-------|----------------|
| `FuzzyMatchingContainerTest` | 3 | Exact match, typo fuzzy match via `pg_trgm similarity()`, unknown name returns INVALID |

Requires Docker. Skips gracefully without it via `@Testcontainers(disabledWithoutDocker = true)`.

---

## Running Tests

### Prerequisites

The project targets Java 25. Homebrew installs Java 25 by default, so no `JAVA_HOME` override is needed. Lombok 1.18.42+ supports Java 25.

```bash
# Verify Java 25 is active
java -version
```

If you are on an older JDK, install Java 25:
```bash
brew install --cask temurin@25
```

### Commands

```bash
# All tests — unit, H2 integration, and container tests (if Docker is running)
mvn clean test

# Unit tests only (fast, no DB)
mvn clean test -Dtest="DartsValidatorTest,ScoringServiceTest,AnswerEvaluatorTest,PracticeGameControllerTest,QuestionServiceTest,MatchServiceTest,GameServiceTest"

# A single test class
mvn clean test -Dtest=PracticeGameIntegrationTest

# Tier 2 container tests only (requires Docker running)
mvn clean test -Dtest=FuzzyMatchingContainerTest
```

### Why `mvn clean test`, not `mvn test`

Stale `.class` files compiled against a different JDK can cause silent failures. Always use `clean` when switching JDK versions or changing compiler flags.

---

## When You Change X, Update Y

This is the most important section. Find the change you're making below.

---

### Adding a new REST endpoint

**What to update:**

1. **Unit test** — Add a test in the corresponding `*ControllerTest` using `@WebMvcTest`. Mock all service dependencies with `@MockBean`. Cover the happy path and at least one error path (e.g., not found → 404).

2. **Integration test** — Add a test in the corresponding `*IntegrationTest` that exercises the full HTTP → service → repository → H2 stack. Seed the necessary data in `@BeforeEach` or within the test itself.

**Example pattern from `PracticeGameIntegrationTest`:**

```java
@Test
@DisplayName("GET /api/practice/games/{id} returns current game state")
void getGameState_returnsCurrentState() throws Exception {
    UUID gameId = startGame(); // helper that calls POST /api/practice/start

    mockMvc.perform(get("/api/practice/games/{id}", gameId)
            .param("playerId", playerId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameId").value(gameId.toString()))
        .andExpect(jsonPath("$.currentScore").value(501))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
}
```

The key discipline: the controller unit test (`@WebMvcTest`) focuses on HTTP serialisation, status codes, and that the right service method is called. The integration test focuses on that the correct data flows through the entire stack end-to-end.

---

### Changing a service method signature

Service signatures appear in two places: the controller test's `@MockBean` stubs and the integration tests (indirectly through the full stack).

**What to update:**

1. **Controller unit tests** — Find every `when(someService.methodName(...))` stub and update the argument matchers to match the new signature. If you added a parameter, add a matcher for it. If you removed a parameter, remove it from the stub.

2. **Other service unit tests** that call this service as a collaborator (e.g., `MatchServiceTest` stubs `QuestionService`). Search for the method name across `src/test`.

3. **Integration tests** — These call through the full stack, so they will fail at compile time if the controller no longer compiles. But they may also implicitly test the signature through the seeded data shape.

**Example:** `QuestionService.selectRandomQuestion` has three overloads. `MatchServiceTest` stubs the three-argument form with exact matchers:

```java
when(questionService.selectRandomQuestion(eq(categoryId), eq(2), eq(10)))
    .thenReturn(Optional.of(question));
```

If you change the method to take a fourth parameter (e.g., `excludeIds`), every stub using this call must be updated. Mockito strict stubbing will surface the mismatch at runtime as a `PotentialStubbingProblem`.

---

### Adding a new field to a domain model with `@Builder.Default`

`@Builder.Default` fields have non-null runtime values even when you do not set them in a builder call. This means Mockito argument matchers must use the actual runtime value, not `null` or `any()`.

**The concrete example from this codebase:**

`Match.difficulty` is declared as:

```java
@Column(name = "difficulty")
@Builder.Default
private Integer difficulty = 2;
```

`MatchService.startNextGame` reads `match.getDifficulty()` and passes it to `selectRandomQuestion`. Tests build a `Match` without setting `difficulty`, so at runtime `getDifficulty()` returns `2`. The stub therefore uses `eq(2)`:

```java
when(questionService.selectRandomQuestion(eq(categoryId), eq(2), eq(10)))
    .thenReturn(Optional.of(question));
```

Using `eq(null)` here would cause the stub to never match, and the test would fail with `Optional.empty()` being returned where a question was expected.

**When you add a new `@Builder.Default` field:**

1. Identify which service methods read the field from the model.
2. Find all unit tests that build the model without setting that field.
3. Update stubs to use `eq(<default value>)` rather than `isNull()` or `any()`.
4. Add a comment in the test noting the default, so the next developer knows why the matcher is not `any()`.

---

### Adding a query that uses PostgreSQL-specific functions

H2 in PostgreSQL compatibility mode handles most standard SQL and many PostgreSQL extensions, but it does **not** support `pg_trgm` functions (`similarity()`, `word_similarity()`, `%` operator).

**What to do:**

1. `@Disabled` the test in `AnswerRepositoryTest` (or whichever `@DataJpaTest` or H2-backed test would run the query) with a comment pointing to the container test:

   ```java
   @Test
   @Disabled("Requires pg_trgm on PostgreSQL — covered by FuzzyMatchingContainerTest")
   @DisplayName("Fuzzy match finds answer with typo")
   void shouldFindBestMatchByFuzzyName() { ... }
   ```

2. Add the real test to `FuzzyMatchingContainerTest` (see [Testcontainers Tier](#testcontainers-tier) below).

3. Do **not** attempt to add `pg_trgm` to H2 through a custom function bridge. The maintenance cost is not worth it when the container test gives you a real PostgreSQL environment.

---

### Changing answer scoring or validation logic

Scoring and validation changes touch multiple tiers:

| Change type | Tests to update |
|-------------|-----------------|
| Change bust boundaries (e.g., new invalid darts score) | `DartsValidatorTest`, `ScoringServiceTest` |
| Change checkout range (currently -10 to 0) | `ScoringServiceTest` — the `@ValueSource` on the checkout parameterised test |
| Change how `AnswerEvaluator` handles bust/valid/checkout | `AnswerEvaluatorTest` (unit), `AnswerEvaluatorIntegrationTest` (H2), `PracticeGameIntegrationTest` (HTTP) |
| Change score deduction formula | `ScoringServiceTest` + `PracticeGameIntegrationTest.multipleValidAnswers_scoreDeductsCumulatively` |
| Change darts validation flags on answer creation | `AdminAnswerIntegrationTest.createAnswer_invalidDartsScore_isFlaggedCorrectly` — the test creates an answer with score 179 and asserts `isValidDarts = false` |

**Parameterised tests in `DartsValidatorTest` and `ScoringServiceTest` use `@ValueSource` with hardcoded score lists.** When invalid scores change, update both:

```java
// DartsValidatorTest
@ValueSource(ints = {163, 166, 169, 172, 173, 175, 176, 178, 179, 181, 200, 0, -1, -10})
void shouldReturnFalseForInvalidScores(int score) { ... }

// ScoringServiceTest
@ValueSource(ints = {163, 166, 169, 172, 173, 175, 176, 178, 179, 181, 200, 501, 0, -1, -10})
void shouldBustOnInvalidDartsScore(int invalidScore) { ... }
```

Both lists must stay in sync with the production constants in `DartsValidator`.

---

### Adding a new answer evaluation result type

`GameMove.MoveResult` currently has: `VALID`, `INVALID`, `BUST`, `CHECKOUT`.

If you add a new result type:

1. Add a unit test in `AnswerEvaluatorTest` covering the new path.
2. Add a case in `ScoringServiceTest` if the new type depends on score calculation.
3. Add an HTTP-level test in `PracticeGameIntegrationTest` that seeds data triggering the new result and asserts the correct JSON field values.
4. Update `PracticeGameControllerTest` to cover how the controller serialises the new result type.

---

## H2 Compatibility Rules

The test profile configures H2 in PostgreSQL compatibility mode:

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    driver-class-name: org.h2.Driver
```

`DATABASE_TO_LOWER=TRUE` means H2 treats all identifiers as lowercase, matching PostgreSQL's default behaviour. `DEFAULT_NULL_ORDERING=HIGH` matches PostgreSQL's null sort order.

### What works on H2 in PostgreSQL mode

- `jsonb` column type declarations (H2 accepts them but stores as a generic type)
- UUID primary key generation
- `LIKE`, `ILIKE`
- Standard aggregate functions
- `CREATE EXTENSION IF NOT EXISTS` — silently ignored by H2 (does not fail)

### What does NOT work on H2

- `similarity()`, `word_similarity()`, `<->` distance operator (all require `pg_trgm`)
- `ts_vector`, `ts_query`, full-text search operators
- PostgreSQL-specific JSON path operators (e.g., `->`, `->>` in WHERE clauses in some contexts)
- `pg_catalog` system tables

### Rule of thumb

If your repository method calls a native query that uses a PostgreSQL function, the test for that method must be:

1. `@Disabled` in any H2-backed test class with a comment pointing to the container test.
2. Covered by a real test in `FuzzyMatchingContainerTest`.

If you are unsure whether a function works under H2, write a simple `@DataJpaTest` with a `@Sql` script that calls the function. If H2 throws `JdbcSQLSyntaxErrorException`, move the test to the container tier.

### Flyway

Flyway is disabled in the test profile (`spring.flyway.enabled: false`). Schema creation is handled by `spring.jpa.hibernate.ddl-auto: create-drop`, which creates the schema from your JPA entities on startup and drops it on shutdown. This means:

- You do not need migration scripts to run tests.
- Columns in your JPA entities are the source of truth for the H2 schema.
- If you add a non-nullable column to an entity without a `@Builder.Default` or `@PrePersist` default, tests that build and save that entity will fail with a constraint violation.

---

## Testcontainers Tier

### How the container is configured

`FuzzyMatchingContainerTest` spins up a real `postgres:17` Docker container for its Spring context. It does not extend `BaseTest`; it carries its own annotations:

```java
// Spring Boot 4 import (moved to webmvc module)
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class FuzzyMatchingContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withInitScript("db/init-test-trgm.sql");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
            () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }
}
```

`@DynamicPropertySource` runs before the Spring context starts and overrides whatever the `test` profile set for the datasource. This is the correct pattern: `application-test.yml` sets H2 defaults, and the container test replaces them at runtime with the real PostgreSQL URL from the running container.

### The init script

```sql
-- src/test/resources/db/init-test-trgm.sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

This runs once when the container starts, before any tests. It enables the trigram extension that the `findBestMatchByFuzzyName` repository method depends on. The script path is relative to the classpath root, which resolves to `src/test/resources/`.

### Adding a new container test

If you add a new repository method that uses a PostgreSQL-specific function:

1. Add the test method to `FuzzyMatchingContainerTest` (or create a new container test class if the concern is unrelated to answer matching).

2. Seed data in `@BeforeEach`. The container test clears and re-seeds identically to the H2 integration tests — deleting in dependency order and saving fresh entities.

3. Use the same `@DynamicPropertySource` pattern if you create a new container test class. Copy it exactly; do not omit the `dialect` or `ddl-auto` overrides, because without them the H2 dialect from `application-test.yml` would be used against a PostgreSQL datasource.

4. Add `@Testcontainers(disabledWithoutDocker = true)` so CI environments without Docker can still run the other tiers.

**If you need a different init script** (e.g., to add a custom PostgreSQL function or seed reference data), add the SQL file to `src/test/resources/db/` and change the `withInitScript` path in the container declaration:

```java
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
    .withInitScript("db/your-init-script.sql");
```

The init script runs once per container start, so keep it minimal — data seeding belongs in `@BeforeEach`.

---

## Mockito Patterns

### Strict stubbing

All unit tests use `@ExtendWith(MockitoExtension.class)`, which enables strict stubbing mode. This means:

- Every stub you declare must be exercised by at least one test in that test class. An unused stub causes `UnnecessaryStubbingException` at the end of the test run.
- When you call a mocked method without a matching stub, Mockito returns the default (`null`, `0`, `false`, `Optional.empty()`) rather than throwing — so missing stubs produce silent wrong behaviour, not failures.

**When you add a new service call in a controller or service method:**

1. The integration tests will exercise the real implementation, so they require no stub changes.
2. The unit tests mock the collaborator. Add a `when(...).thenReturn(...)` stub for the new call in the test(s) that exercise that code path.
3. Remove any stubs that the changed code path no longer calls. Strict stubbing will fail the test if you leave orphaned stubs.

**Diagnosing `PotentialStubbingProblem`:**

This error means a stub was declared but never matched at runtime. The most common cause in this codebase is a mismatched argument value due to a `@Builder.Default` field. Read the full error message — Mockito prints both the declared stub and the actual invocation, showing you exactly which argument differed.

### `@MockitoBean` (Spring Boot 4+)

Spring Boot 4 removed `@MockBean` in favour of `@MockitoBean` from Spring Framework's own test support. Use:

```java
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@MockitoBean
private GameService gameService;
```

`@MockitoBean` behaves identically to the old `@MockBean` for all practical purposes in `@WebMvcTest` slices.

### Stubbing with `@Builder.Default` fields

When a domain model has `@Builder.Default` fields and you build it without setting those fields, the getters return the default values at runtime. Stubs must use `eq(<default value>)`.

`Match.difficulty` defaults to `2`. In `MatchServiceTest`:

```java
// Correct: difficulty defaults to 2 via @Builder.Default
when(questionService.selectRandomQuestion(eq(categoryId), eq(2), eq(10)))
    .thenReturn(Optional.of(question));
```

Using `isNull()` instead of `eq(2)` would mean the stub never fires, `selectRandomQuestion` returns `Optional.empty()`, and the test fails inside `startNextGame` with "No question available" — a confusing failure that hides the real cause.

The same applies to `Match.type` (defaults to `CASUAL`), `Match.format` (defaults to `BEST_OF_3`), and `Match.status` (defaults to `IN_PROGRESS`). If a service method branches on these values, your stubs and assertions must match the actual defaults.

### ArgumentCaptor pattern

When you need to assert on an object passed to a void method (or a method whose return value you do not care about), use `ArgumentCaptor`:

```java
// From MatchServiceTest
ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
verify(matchRepository).save(matchCaptor.capture());
Match savedMatch = matchCaptor.getValue();

assertThat(savedMatch.getStatus()).isEqualTo(Match.MatchStatus.COMPLETED);
assertThat(savedMatch.getWinnerId()).isEqualTo(player1Id);
```

This is the correct way to test that a service correctly mutates state before persisting it. Do not use `any(Match.class)` in a `verify` if you care about the content of what was saved.

---

## Test Data Setup

### Why `@BeforeEach` deletes everything

H2 uses a single in-memory database shared across all tests in a Spring context. If one test creates data and the next test does not clean up, the second test inherits stale rows and may fail on count assertions or uniqueness constraints.

Both integration test classes delete in dependency order at the start of each test:

```java
// PracticeGameIntegrationTest
@BeforeEach
void setUp() {
    gameMoveRepository.deleteAll();   // child of game
    gameRepository.deleteAll();       // child of match
    matchRepository.deleteAll();
    answerRepository.deleteAll();     // child of question
    questionRepository.deleteAll();
    categoryRepository.deleteAll();
    // ... seed fresh data
}
```

Delete order matters. Deleting a parent before its children will violate a foreign key constraint. Always delete children first.

### Minimum answer count threshold

`QuestionService.selectRandomQuestion` requires a minimum number of answers before a question is eligible for a game. The default minimum is `10` (`DEFAULT_MIN_ANSWERS`). Both `PracticeGameIntegrationTest` and `FuzzyMatchingContainerTest` seed **12 answers** to clear this threshold:

```java
// PracticeGameIntegrationTest — seeds 12 answers (indices 0-11, scores 10-21)
for (int i = 0; i < 12; i++) {
    Answer a = answerRepository.save(Answer.builder()
        .questionId(question.getId())
        .displayText("Player " + i)
        .answerKey("player " + i)
        .score(10 + i)
        .isValidDarts(true)
        .isBust(false)
        .build());
    if (i == 0) knownAnswer = a;
}
```

If you change `DEFAULT_MIN_ANSWERS`, update the seed count in both integration test `@BeforeEach` methods to stay above the new threshold. If you add a new integration test class that calls `startGame()`, seed at least `DEFAULT_MIN_ANSWERS + 2` answers.

### `TestEntityManager` vs repository saves

`AnswerRepositoryTest` uses `@DataJpaTest` with `TestEntityManager`:

```java
@Autowired
private TestEntityManager entityManager;

// In @BeforeEach:
entityManager.persist(category);
entityManager.persist(question);
entityManager.flush();
```

`TestEntityManager` operates within the test transaction that `@DataJpaTest` manages. After the test, that transaction is rolled back, so there is no need for explicit `deleteAll()` calls.

The full `@SpringBootTest` integration tests (`PracticeGameIntegrationTest`, `AdminAnswerIntegrationTest`) do **not** use `@DataJpaTest` and do not roll back between tests. They use repository saves and explicit `deleteAll()` in `@BeforeEach`. Do not switch them to `TestEntityManager` — the full HTTP stack does not run inside a `@DataJpaTest` transaction.

**Rule:** Use `TestEntityManager` only in `@DataJpaTest` classes. Use `@Autowired` repository saves in full `@SpringBootTest` classes with explicit cleanup in `@BeforeEach`.

### `@DataJpaTest` and `@AutoConfigureTestDatabase`

`AnswerRepositoryTest` and `AnswerEvaluatorIntegrationTest` use:

```java
// Spring Boot 4 — packages moved to modular jars
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager; // if needed

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
```

`Replace.NONE` tells `@DataJpaTest` not to swap out the datasource. Without it, `@DataJpaTest` would configure its own embedded database and ignore `application-test.yml` entirely, meaning the `MODE=PostgreSQL` options would not apply and `jsonb` column declarations could fail silently.

Always include `Replace.NONE` when writing `@DataJpaTest` tests in this project.
