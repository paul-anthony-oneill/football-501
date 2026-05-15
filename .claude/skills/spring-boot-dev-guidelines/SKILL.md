---
name: spring-boot-dev-guidelines
description: Backend development guidelines for Spring Boot 3.x / Java 17 / PostgreSQL. Covers REST controllers, service layer, repository pattern, JPA/Hibernate, game engine logic, WebSocket (STOMP), input validation with Bean Validation, Spring Security, and testing with JUnit 5 + TestContainers. For the Football-501 backend.
---

# Spring Boot Development Guidelines

## Purpose

Establish consistent patterns for the Football-501 Spring Boot backend — REST APIs, WebSocket handlers, game engine, Spring Security, JPA repositories, and testing.

## When to Use This Skill

- Creating or modifying REST endpoints
- Building controllers, services, repositories
- Implementing game engine logic
- WebSocket / STOMP message handlers
- Spring Security configuration
- Input validation with Bean Validation
- Database access with Spring Data JPA / Hibernate
- Testing with JUnit 5 + TestContainers

---

## Quick Start

### New Feature Checklist

- [ ] **Controller**: `@RestController`, extend no base class — use `ResponseEntity`
- [ ] **Service**: `@Service`, business logic only
- [ ] **Repository**: `@Repository` extending `JpaRepository<T, ID>`
- [ ] **Validation**: Bean Validation (`@Valid`, `@NotNull`, `@Size`)
- [ ] **Exception handling**: `@ExceptionHandler` or global `@ControllerAdvice`
- [ ] **Tests**: JUnit 5 unit tests + TestContainers integration test

### New Module Checklist

- [ ] Package structure (see below)
- [ ] Controller → Service → Repository layers
- [ ] DTOs for request/response (not entity classes)
- [ ] Global exception handler
- [ ] Tests

---

## Architecture

### Layered Architecture

```
HTTP Request
    ↓
@RestController (routing + request/response mapping)
    ↓
@Service (business logic)
    ↓
@Repository (data access via JPA)
    ↓
PostgreSQL
```

**Key Principle:** Each layer has ONE responsibility.

---

## Package Structure

```
com.football501/
  controller/           # REST controllers & WebSocket handlers
    GameController.java
    MatchController.java
    UserController.java
  service/              # Business logic
    GameService.java
    GameEngineService.java
    MatchmakingService.java
  repository/           # Spring Data JPA repositories
    GameRepository.java
    AnswerRepository.java
    UserRepository.java
  model/                # JPA entities
    Game.java
    Match.java
    User.java
    Answer.java
  dto/                  # Request/Response DTOs (NOT entities)
    request/
      SubmitAnswerRequest.java
    response/
      GameStateResponse.java
      AnswerResultResponse.java
  websocket/            # STOMP message handlers
    GameWebSocketHandler.java
  scheduler/            # @Scheduled tasks
    DailyChallengeScheduler.java
    StatsRefreshScheduler.java
  security/             # Spring Security config
    SecurityConfig.java
    JwtAuthFilter.java
  exception/            # Custom exceptions + handlers
    GlobalExceptionHandler.java
    GameNotFoundException.java
  config/               # Configuration classes
    WebSocketConfig.java
    CacheConfig.java
```

---

## Core Principles (7 Key Rules)

### 1. Controllers Only Handle HTTP — No Business Logic

```java
// NEVER: Business logic in controller
@PostMapping("/games/{gameId}/answers")
public ResponseEntity<?> submitAnswer(@PathVariable String gameId, @RequestBody AnswerRequest req) {
    // 50 lines of game logic
}

// ALWAYS: Delegate to service
@PostMapping("/games/{gameId}/answers")
public ResponseEntity<AnswerResultResponse> submitAnswer(
        @PathVariable String gameId,
        @Valid @RequestBody SubmitAnswerRequest request,
        @AuthenticationPrincipal UserPrincipal user) {
    AnswerResultResponse result = gameService.processAnswer(gameId, request, user.getId());
    return ResponseEntity.ok(result);
}
```

### 2. Use DTOs — Never Return Entities Directly

```java
// NEVER: Return JPA entity
public ResponseEntity<Game> getGame(String gameId) { ... }

// ALWAYS: Return DTO
public ResponseEntity<GameStateResponse> getGame(String gameId) { ... }
```

### 3. Spring Data JPA for Repositories

```java
@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    // Fuzzy search using pg_trgm — via native query
    @Query(value = "SELECT * FROM answers WHERE question_id = :qId " +
                   "AND similarity(player_name, :name) > 0.3 " +
                   "ORDER BY similarity(player_name, :name) DESC LIMIT 1",
           nativeQuery = true)
    Optional<Answer> findByFuzzyName(@Param("qId") Long questionId,
                                     @Param("name") String playerName);
}
```

### 4. Validate All Input with Bean Validation

```java
public record SubmitAnswerRequest(
    @NotBlank @Size(min = 2, max = 100) String playerName
) {}

// Controller uses @Valid:
public ResponseEntity<?> submit(@Valid @RequestBody SubmitAnswerRequest req) { ... }
```

### 5. Use @ControllerAdvice for Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGameNotFound(GameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("GAME_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }
}
```

### 6. Game Engine Logic Lives in GameEngineService

```java
@Service
public class GameEngineService {

    private static final Set<Integer> INVALID_DARTS_SCORES =
        Set.of(163, 166, 169, 172, 173, 175, 176, 178, 179);

    public boolean isValidDartsScore(int score) {
        return score >= 1 && score <= 180 && !INVALID_DARTS_SCORES.contains(score);
    }

    public boolean isBust(int currentScore, int statValue) {
        int newScore = currentScore - statValue;
        return newScore > 180 || newScore < -10 || !isValidDartsScore(statValue);
    }

    public boolean isCheckout(int score) {
        return score >= -10 && score <= 0;
    }
}
```

### 7. Zero External API Calls During Gameplay

```java
// NEVER during match validation
apiFootballClient.getPlayerStats(playerId);

// ALWAYS read from pre-cached answers table
answerRepository.findByFuzzyName(questionId, playerName);
```

---

## WebSocket (STOMP)

```java
@Controller
public class GameWebSocketHandler {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game.submitAnswer")
    public void submitAnswer(@Payload SubmitAnswerMessage msg,
                              @Header("gameId") String gameId,
                              Principal principal) {
        AnswerResult result = gameService.processAnswer(gameId, msg.playerName(), principal.getName());

        // Broadcast result to all subscribers
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameId,
            new GameStateMessage("ANSWER_RESULT", result)
        );
    }
}
```

---

## Common Annotations Quick Reference

| Annotation | Use |
|---|---|
| `@RestController` | REST controller (combines @Controller + @ResponseBody) |
| `@Service` | Business logic service |
| `@Repository` | Data access layer |
| `@Transactional` | Method or class runs in DB transaction |
| `@Valid` | Trigger Bean Validation on parameter |
| `@AuthenticationPrincipal` | Inject current authenticated user |
| `@Scheduled` | Cron/fixed-rate tasks |
| `@Cacheable` | Spring cache result |
| `@MessageMapping` | STOMP WebSocket handler |

---

## Testing

### Unit Test (Service)
```java
@ExtendWith(MockitoExtension.class)
class GameEngineServiceTest {

    @InjectMocks
    private GameEngineService gameEngineService;

    @Test
    void shouldDetectBustOnInvalidDartsScore() {
        assertTrue(gameEngineService.isBust(501, 179)); // 179 is invalid darts
    }

    @Test
    void shouldAllowValidCheckout() {
        assertFalse(gameEngineService.isBust(10, 10)); // score reaches 0 — checkout!
    }
}
```

### Integration Test (TestContainers)
```java
@SpringBootTest
@Testcontainers
class AnswerRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }

    @Autowired
    private AnswerRepository answerRepository;

    @Test
    void shouldFindAnswerByFuzzyName() {
        Optional<Answer> result = answerRepository.findByFuzzyName(1L, "Aguero");
        assertTrue(result.isPresent());
    }
}
```

---

## Anti-Patterns to Avoid

- Business logic in controllers
- Returning JPA entities directly from REST endpoints
- Calling API-Football during match validation
- Skipping `@Valid` on request bodies
- Direct `System.out.println` — use SLF4J `@Slf4j` + `log.info()`
- Missing `@Transactional` on multi-step DB operations
- Hardcoding configuration — use `application.yml` + `@Value`

---

## Navigation Guide

| Need to... | Resource |
|---|---|
| Create a REST endpoint | `resources/rest-controllers.md` |
| Implement game logic | `resources/game-engine.md` |
| Database queries | `resources/jpa-repositories.md` |
| WebSocket handlers | `resources/websocket-stomp.md` |
| Input validation | `resources/validation.md` |
| Spring Security / JWT | `resources/security.md` |
| Testing patterns | `resources/testing.md` |

---

## Core Principles

1. **Controllers → Service → Repository** — strict layering, no shortcuts
2. **DTOs not Entities** — never expose JPA entities in API responses
3. **Zero API calls during gameplay** — only read from `answers` table
4. **GameEngineService owns game rules** — darts validation, bust detection, checkout
5. **Bean Validation on all inputs** — `@Valid` + records
6. **@ControllerAdvice** — centralised exception handling
7. **TestContainers** for integration tests — real PostgreSQL, not H2
