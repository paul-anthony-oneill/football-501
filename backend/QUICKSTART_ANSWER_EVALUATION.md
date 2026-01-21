# Quick Start: Answer Evaluation Framework (Java/Spring Boot)

Get started with the Java answer evaluation framework in 5 minutes.

## What This Does

Server-side answer validation for Football 501 gameplay:
- ✅ Fuzzy name matching (handles typos)
- ✅ Darts score validation
- ✅ Win/bust condition checking
- ✅ Sub-100ms response time
- ✅ Spring Boot + PostgreSQL

## Prerequisites

1. **Java 17+** installed
2. **Maven 3.8+** installed
3. **PostgreSQL 15+** running
4. **Python scraper** has populated data

## 1. Build the Project

```bash
cd backend
mvn clean install
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
```

## 2. Run Unit Tests

```bash
mvn test -Dtest=AnswerEvaluatorTest
```

Should see all tests passing:
```
✓ Exact match returns correct answer
✓ Invalid darts score 179 results in bust
✓ Exact checkout at 0 triggers win
...
```

## 3. Run Integration Tests

**Note**: Requires PostgreSQL with test database.

```bash
# Setup test database
createdb football501_test
psql -d football501_test -c "CREATE EXTENSION pg_trgm"

# Run tests
mvn test -Dtest=AnswerEvaluatorIntegrationTest
```

## 4. Basic Usage

### In a Spring Boot Service/Controller

```java
@RestController
@RequestMapping("/api/game")
public class GameController {

    @Autowired
    private AnswerEvaluator answerEvaluator;

    @PostMapping("/{gameId}/submit-answer")
    public ResponseEntity<AnswerResult> submitAnswer(
        @PathVariable UUID gameId,
        @RequestBody SubmitAnswerRequest request
    ) {
        // Get game state
        Game game = gameService.getGame(gameId);

        // Validate answer
        AnswerResult result = answerEvaluator.evaluateAnswer(
            game.getQuestionId(),
            request.getPlayerInput(),
            game.getCurrentScore(),
            game.getUsedPlayerIds()
        );

        // Handle result
        if (result.isValid()) {
            if (result.isWin()) {
                return ResponseEntity.ok(result); // Player won!
            } else if (result.isBust()) {
                return ResponseEntity.ok(result); // Bust, no score change
            } else {
                // Update game state
                game.setScore(result.getNewTotal());
                game.addUsedPlayer(result.getPlayerId());
                return ResponseEntity.ok(result);
            }
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
```

## 5. Key Rules

### Valid Darts Scores
- **Valid**: 1-180 (excluding invalid scores)
- **Invalid**: 163, 166, 169, 172, 173, 175, 176, 178, 179
- **Bust**: > 180 or < -10

### Win Condition
Final score between **-10 and 0** (inclusive)

### Examples

| Current Score | Answer Score | Result | New Score |
|--------------|--------------|--------|-----------|
| 501 | 35 | Continue | 466 |
| 10 | 10 | **WIN** | 0 |
| 5 | 10 | **WIN** | -5 |
| 20 | 179 | **BUST** | 20 (unchanged) |

## 6. Response Structure

### AnswerResult

```java
{
  "valid": true,                      // Answer found
  "playerName": "Erling Haaland",     // Matched player
  "playerId": "uuid",                 // For tracking
  "score": 35,                        // Score deducted
  "validDartsScore": true,            // Achievable in darts
  "bust": false,                      // Turn wasted?
  "newTotal": 466,                    // New score
  "win": false,                       // Player won?
  "reason": null,                     // Error/win message
  "similarity": null                  // Fuzzy match score
}
```

### Win Example

```java
{
  "valid": true,
  "playerName": "Ederson",
  "score": 10,
  "newTotal": 0,
  "win": true,
  "reason": "Win!"
}
```

### Bust Example

```java
{
  "valid": true,
  "playerName": "Jack Grealish",
  "score": 179,
  "validDartsScore": false,
  "bust": true,
  "newTotal": 501,  // Unchanged
  "reason": "Invalid darts score"
}
```

## 7. Common Issues

### "Player not found"
- Player not in database for this question
- Player already used (check `usedPlayerIds`)
- Fuzzy match similarity too low

### "Function similarity does not exist"
- PostgreSQL `pg_trgm` extension not enabled
- Run: `CREATE EXTENSION pg_trgm;`

### Tests failing
- Database not running or not accessible
- Missing test data
- Schema not migrated (run Flyway migrations)

## 8. Project Structure

```
backend/src/main/java/com/football501/
├── model/
│   ├── Player.java                    // JPA entity
│   ├── Question.java                  // JPA entity
│   └── QuestionValidAnswer.java       // JPA entity
├── repository/
│   ├── QuestionValidAnswerRepository.java
│   └── QuestionRepository.java
├── engine/
│   ├── AnswerEvaluator.java          // Main service ⭐
│   ├── AnswerResult.java             // Response DTO
│   ├── ScoringService.java           // Score calculation
│   └── DartsValidator.java           // Validation utility
└── service/
    └── (Your game service here)

backend/src/test/java/com/football501/engine/
├── AnswerEvaluatorTest.java          // Unit tests
└── AnswerEvaluatorIntegrationTest.java // Integration tests
```

## 9. Next Steps

1. ✅ Run tests to verify setup
2. ⏭️ Create REST API endpoints
3. ⏭️ Integrate with WebSocket handler
4. ⏭️ Add turn timer logic
5. ⏭️ Implement game state management

## 10. Configuration

### application.properties

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/football501
spring.datasource.username=football501
spring.datasource.password=your_password

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

### Fuzzy Matching Threshold

Adjust in `AnswerEvaluator.java`:

```java
// Current: 0.5 (balanced)
private static final double SIMILARITY_THRESHOLD = 0.5;

// More lenient: 0.3 (accepts more typos)
// More strict: 0.7 (requires closer match)
```

## Need Help?

- **Full Documentation**: `ANSWER_EVALUATION_FRAMEWORK.md`
- **API Reference**: JavaDoc in source files
- **Test Examples**: `AnswerEvaluatorTest.java`

---

**Pro Tip**: Start by running the tests to ensure everything is set up correctly before integrating with your game logic.
