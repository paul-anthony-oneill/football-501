# Current Implementation - Explained Simply

## What We've Built So Far

Football 501 is like a game of darts, but instead of throwing darts, you name football players. This document explains how everything works right now in simple terms.

---

## The Big Picture: How a Game Works

Imagine you're playing the game. Here's what happens step by step:

```
1. You click "Start Game" on the website
   ↓
2. The website asks the server: "Can I start a game?"
   ↓
3. The server creates a new game and gives you a question
   ↓
4. You type a player's name (like "Haaland")
   ↓
5. The website sends it to the server
   ↓
6. The server checks if it's correct and calculates your score
   ↓
7. You get feedback and your new score
   ↓
8. Repeat until you win (reach 0) or bust!
```

---

## The Parts of the System

### 🎨 Frontend (SvelteKit) - What You See

**Location:** `frontend/src/routes/+page.svelte`

This is the web page you interact with. Think of it like the game board.

**What it does:**
- Shows your current score (starts at 501)
- Displays the question
- Has a text box where you type player names
- Shows if your answer was correct or wrong
- Keeps track of your recent moves

**How it talks to the backend:**
```typescript
// Starting a game
POST http://localhost:8080/api/practice/start
→ Backend creates game and returns question

// Submitting an answer
POST http://localhost:8080/api/practice/games/{gameId}/submit
→ Backend checks answer and returns result
```

**Example flow:**
1. User types "Messi" and presses Enter
2. Frontend sends: `{ answer: "Messi" }` to backend
3. Backend responds: `{ result: "VALID", score: 501→471 }`
4. Frontend updates display: Score goes from 501 to 471

---

### 🏢 Backend (Spring Boot) - The Game Master

The backend is like the game referee. It knows all the rules and makes sure no one cheats.

---

#### 📡 **Controller Layer** - The Reception Desk

**Location:** `backend/src/main/java/com/football501/controller/PracticeGameController.java`

Think of this as the reception desk at a hotel. It receives requests and directs them to the right place.

**What it does:**
1. **Start Game** (`POST /api/practice/start`)
   - Creates a new match
   - Picks a random question
   - Returns game state

2. **Submit Answer** (`POST /api/practice/games/{gameId}/submit`)
   - Takes your answer
   - Asks GameService to check it
   - Returns if you were right or wrong

3. **Get Game State** (`GET /api/practice/games/{gameId}`)
   - Returns current score, question, turn count

**How it's tested:**
```java
// Test in: PracticeGameControllerTest.java
@Test
void shouldStartPracticeGame() {
    // Given: A request to start a game
    StartPracticeRequest request = new StartPracticeRequest(playerId, "football");

    // When: We call the start endpoint
    ResponseEntity<GameStateResponse> response = controller.startPracticeGame(request);

    // Then: We get back a game with score 501
    assertThat(response.getBody().getCurrentScore()).isEqualTo(501);
}
```

---

#### 🎮 **Service Layer** - The Game Logic

##### **GameService** - The Turn Manager

**Location:** `backend/src/main/java/com/football501/service/GameService.java`

This is like the person keeping track of whose turn it is and updating the score.

**What it does:**
1. **Process Player Move** - The main action
   ```java
   GameMove processPlayerMove(UUID gameId, UUID playerId, String answer)
   ```

   Steps:
   - Check it's really your turn
   - Get list of already-used answers (can't repeat!)
   - Ask AnswerEvaluator: "Is this answer valid?"
   - Update your score if valid
   - Create a record of this move
   - Check if you won

2. **Handle Timeout** - When time runs out
   - Tracks consecutive timeouts (3 = you lose!)
   - Reduces timer (45s → 30s → 15s)

3. **Create Game** - Sets up a new game
   - Both players start at 501 points
   - Timer starts at 45 seconds

**Special Rules It Enforces:**
- **Invalid answers** = No score change, you can try again immediately
- **Bust** = No score change, turn wasted, next player's turn
- **Valid** = Score deducted, next player's turn
- **Checkout** = You win! (-10 to 0 range)

**How it's tested:**
```java
// Test in: GameServiceTest.java
@Test
void shouldProcessValidMove() {
    // Given: A game at score 501
    Game game = createTestGame(501);

    // When: Player submits valid answer worth 35 points
    GameMove move = gameService.processPlayerMove(gameId, playerId, "Messi");

    // Then: Score should be 466 (501 - 35)
    assertThat(move.getScoreAfter()).isEqualTo(466);
    assertThat(move.getResult()).isEqualTo(MoveResult.VALID);
}
```

---

##### **MatchService** - The Match Organizer

**Location:** `backend/src/main/java/com/football501/service/MatchService.java`

Think of this as tournament management. It handles the overall match (which contains individual games).

**What it does:**
- Creates matches (best-of-1, best-of-3, etc.)
- Starts the next game in a match
- Determines match winner

---

##### **QuestionService** - The Question Master

**Location:** `backend/src/main/java/com/football501/service/QuestionService.java`

This picks which question you'll get asked.

**What it does:**
- Get random question from database
- Get questions by category (Premier League, La Liga, etc.)
- Manage question categories

---

#### 🧠 **Engine Layer** - The Smart Rules

This is where the "intelligence" lives - the parts that know the rules of the game.

##### **AnswerEvaluator** - The Answer Checker

**Location:** `backend/src/main/java/com/football501/engine/AnswerEvaluator.java`

This is like the teacher grading your test. It checks if your answer is correct.

**What it does:**
1. **Normalize your input**
   ```
   "MESSI" → "messi"
   "  Haaland  " → "haaland"
   ```

2. **Try exact match first** (fast!)
   - "messi" exactly matches database entry? ✓

3. **Try fuzzy match if exact fails** (handles typos!)
   ```
   "messsi" → matches "messi" (one typo)
   "Aguero" → matches "Agüero" (missing accent)
   ```
   Uses similarity threshold of 0.5 (50% similar)

4. **Check if already used**
   - "Can't use the same player twice!"

5. **Ask ScoringService: "What happens to the score?"**

6. **Return result**
   ```java
   AnswerResult {
     valid: true,
     displayText: "Lionel Messi",
     score: 35,
     newTotal: 466,
     isWin: false,
     isBust: false
   }
   ```

**How it's tested:**
```java
// Test in: AnswerEvaluatorTest.java (unit test with mocks)
@Test
void shouldMatchWithFuzzySearch() {
    // Given: Database has "Messi"

    // When: User types "Messi" with typo
    AnswerResult result = evaluator.evaluateAnswer(questionId, "messsi", 501, []);

    // Then: Should still match!
    assertThat(result.isValid()).isTrue();
    assertThat(result.getDisplayText()).isEqualTo("Lionel Messi");
}

// Test in: AnswerEvaluatorIntegrationTest.java (full database test)
@Test
void testFullGameFlow() {
    // This test uses REAL database with REAL player data
    // 1. Submit exact match
    // 2. Submit fuzzy match
    // 3. Submit invalid darts score → bust
    // 4. Try duplicate → rejected
}
```

---

##### **ScoringService** - The Calculator

**Location:** `backend/src/main/java/com/football501/engine/ScoringService.java`

This does the math. It calculates: "If your score is X and you score Y points, what's your new score?"

**The Rules:**
```
Starting score: 501
Goal: Get to 0 (or between -10 and 0)

Valid moves:
  501 - 35 = 466  ✓ (Good!)
  466 - 28 = 438  ✓ (Good!)
  438 - 30 = 408  ✓ (Good!)
  ...
  15 - 20 = -5    ✓ (YOU WIN! -10 to 0 is checkout range)

Bust moves (score doesn't change):
  501 - 179 = 322  ✗ (179 is invalid darts score)
  501 - 200 = 301  ✗ (200 is too high - max is 180)
  15 - 30 = -15    ✗ (Below -10 = bust)
  -5 - 10 = -15    ✗ (Already won, can't score again!)
```

**How it's tested:**
```java
// Test in: ScoringServiceTest.java
@Test
void shouldCheckoutInRange() {
    // Score of -10 to 0 = WIN!
    ScoreResult result = scoringService.calculateScore(50, 50);
    assertThat(result.getNewScore()).isEqualTo(0);
    assertThat(result.isCheckout()).isTrue();
}

@Test
void shouldBustWhenTooLow() {
    // Score below -10 = BUST!
    ScoreResult result = scoringService.calculateScore(15, 30);
    assertThat(result.isBust()).isTrue();
    assertThat(result.getNewScore()).isEqualTo(15); // No change
}
```

---

##### **DartsValidator** - The Darts Rules Expert

**Location:** `backend/src/main/java/com/football501/engine/DartsValidator.java`

This knows which scores are possible in real darts.

**The Problem:**
In real darts (501 game), you throw 3 darts. Most scores from 1-180 are possible, but some aren't!

**Impossible scores** (with 3 darts):
```
163, 166, 169, 172, 173, 175, 176, 178, 179
```

These scores result in BUST in our game!

**How it works:**
```java
public static boolean isValidDartsScore(int score) {
    if (score < 1 || score > 180) return false;
    if (INVALID_SCORES.contains(score)) return false;
    return true;
}
```

**How it's tested:**
```java
// Test in: DartsValidatorTest.java
@Test
void shouldRejectInvalidScores() {
    assertThat(DartsValidator.isValidDartsScore(179)).isFalse();
    assertThat(DartsValidator.isValidDartsScore(163)).isFalse();
}

@Test
void shouldAcceptValidScores() {
    assertThat(DartsValidator.isValidDartsScore(180)).isTrue();
    assertThat(DartsValidator.isValidDartsScore(1)).isTrue();
}
```

---

#### 💾 **Database Layer** - The Memory

##### **Models (Entities)** - The Data Structures

These are like the forms you fill out. Each one represents something stored in the database.

**Game** (`backend/src/main/java/com/football501/model/Game.java`)
```java
class Game {
    UUID id;                    // Unique game ID
    UUID matchId;               // Which match is this game part of?
    Integer gameNumber;         // Game 1, 2, or 3 (in best-of-3)
    UUID questionId;            // Which question for this game?
    GameStatus status;          // WAITING, IN_PROGRESS, COMPLETED

    // Scores
    Integer player1Score;       // Starts at 501
    Integer player2Score;       // Starts at 501

    // Turn tracking
    UUID currentTurnPlayerId;   // Whose turn is it?
    Integer turnCount;          // How many turns so far?
    Integer turnTimerSeconds;   // 45, 30, or 15 seconds

    // Timeout tracking
    Integer player1ConsecutiveTimeouts;
    Integer player2ConsecutiveTimeouts;

    // Winner
    UUID winnerId;              // Who won? (null if still playing)
}
```

**Answer** (`backend/src/main/java/com/football501/model/Answer.java`)
```java
class Answer {
    UUID id;                    // Unique answer ID
    UUID questionId;            // Which question is this for?
    String answerKey;           // "lionel messi" (normalized for matching)
    String displayText;         // "Lionel Messi" (shown to user)
    Integer score;              // 35 (the player's stat value)
    Boolean isValidDarts;       // true (is 35 a valid darts score?)
    Boolean isBust;             // false (would this cause a bust?)
}
```

**Question**, **Match**, **GameMove** - Similar structures for other game data

##### **Repositories** - The Database Queries

These are like librarians who know how to find things in the database.

**AnswerRepository** (`backend/src/main/java/com/football501/repository/AnswerRepository.java`)

**Important queries:**
1. **Exact match** (fast!)
   ```java
   Optional<Answer> findByQuestionIdAndAnswerKey(UUID questionId, String answerKey);
   // Example: Find "messi" for question ABC
   ```

2. **Fuzzy match** (handles typos!)
   ```java
   Optional<Answer> findBestMatchByFuzzyName(
       UUID questionId,
       String input,
       List<UUID> usedIds,
       double threshold
   );
   // Example: Find best match for "messsi" (typo)
   // Uses PostgreSQL trigram similarity (pg_trgm extension)
   ```

3. **Top scorers**
   ```java
   List<Answer> findTopAnswers(UUID questionId, boolean excludeInvalidDarts);
   // Example: Get highest-scoring valid answers
   ```

**How it's tested:**
```java
// Test in: AnswerRepositoryTest.java
@Test
void shouldFindExactMatch() {
    // Given: Database has answer "messi" with score 35
    Answer saved = answerRepository.save(createAnswer("messi", 35));

    // When: We search for exact match
    Optional<Answer> found = answerRepository.findByQuestionIdAndAnswerKey(
        questionId, "messi"
    );

    // Then: Should find it!
    assertThat(found).isPresent();
    assertThat(found.get().getScore()).isEqualTo(35);
}

@Test
void shouldFindFuzzyMatch() {
    // Given: Database has "agüero"
    answerRepository.save(createAnswer("agüero", 28));

    // When: User types "aguero" (no accent)
    Optional<Answer> found = answerRepository.findBestMatchByFuzzyName(
        questionId, "aguero", null, 0.5
    );

    // Then: Should still match!
    assertThat(found).isPresent();
    assertThat(found.get().getDisplayText()).contains("agüero");
}
```

---

## The Complete Flow (With Code)

Let's trace a single answer submission through the entire system:

### Step 1: User Types "Messi" and Hits Enter

**Frontend (`+page.svelte`):**
```typescript
async function submitAnswer() {
    const response = await fetch(
        `${API_BASE}/games/${gameId}/submit?playerId=${playerId}`,
        {
            method: 'POST',
            body: JSON.stringify({ answer: "Messi" })
        }
    );
    const data = await response.json();
    // Update UI with result
}
```

### Step 2: Request Arrives at Controller

**Controller (`PracticeGameController.java`):**
```java
@PostMapping("/games/{gameId}/submit")
public ResponseEntity<SubmitAnswerResponse> submitAnswer(
    @PathVariable UUID gameId,
    @RequestParam UUID playerId,
    @RequestBody SubmitAnswerRequest request  // { answer: "Messi" }
) {
    // Delegate to GameService
    GameMove move = gameService.processPlayerMove(gameId, playerId, "Messi");

    // Build response
    return ResponseEntity.ok(buildResponse(move));
}
```

### Step 3: GameService Processes the Move

**GameService (`GameService.java`):**
```java
@Transactional
public GameMove processPlayerMove(UUID gameId, UUID playerId, String answer) {
    // 1. Load game from database
    Game game = gameRepository.findById(gameId);
    // Current score: 501

    // 2. Check it's player's turn
    validatePlayerTurn(game, playerId);

    // 3. Get already-used answers
    List<UUID> usedAnswers = gameMoveRepository.findUsedAnswerIdsByGameId(gameId);
    // usedAnswers: []

    // 4. Ask AnswerEvaluator to check the answer
    AnswerResult result = answerEvaluator.evaluateAnswer(
        game.getQuestionId(),
        "Messi",
        501,  // current score
        usedAnswers
    );

    // 5. Create move record
    GameMove move = GameMove.builder()
        .submittedAnswer("Messi")
        .matchedDisplayText(result.getDisplayText())  // "Lionel Messi"
        .scoreBefore(501)
        .scoreAfter(result.getNewTotal())  // 466
        .result(MoveResult.VALID)
        .build();

    // 6. Save move to database
    gameMoveRepository.save(move);

    // 7. Update game state
    game.setPlayer1Score(466);  // 501 - 35
    game.setTurnCount(game.getTurnCount() + 1);
    gameRepository.save(game);

    return move;
}
```

### Step 4: AnswerEvaluator Checks the Answer

**AnswerEvaluator (`AnswerEvaluator.java`):**
```java
public AnswerResult evaluateAnswer(
    UUID questionId,
    String userInput,  // "Messi"
    int currentScore,  // 501
    List<UUID> usedAnswerIds
) {
    // 1. Normalize input
    String normalized = normalizeInput("Messi");  // "messi"

    // 2. Try exact match first
    Optional<Answer> match = answerRepository.findByQuestionIdAndAnswerKey(
        questionId,
        "messi"
    );
    // Found! Answer { displayText: "Lionel Messi", score: 35 }

    // 3. Ask ScoringService what happens to the score
    ScoreResult scoreResult = scoringService.calculateScore(501, 35);
    // scoreResult: { newScore: 466, isBust: false, isCheckout: false }

    // 4. Build result
    return AnswerResult.valid(
        "Lionel Messi",  // displayText
        answerId,
        35,              // score
        true,            // isValidDarts
        false,           // isBust
        466,             // newTotal
        false,           // isCheckout
        null,            // reason
        null             // similarity
    );
}
```

### Step 5: ScoringService Calculates New Score

**ScoringService (`ScoringService.java`):**
```java
public ScoreResult calculateScore(int currentScore, int answerScore) {
    // currentScore: 501
    // answerScore: 35

    // 1. Check if answer score is valid in darts
    if (!DartsValidator.isValidDartsScore(35)) {
        return ScoreResult.bust(501);  // Would return here if invalid
    }

    // 2. Calculate new score
    int newScore = 501 - 35;  // = 466

    // 3. Check if below checkout minimum (-10)
    if (newScore < -10) {
        return ScoreResult.bust(501);  // Would return here if too low
    }

    // 4. Check if in checkout range (-10 to 0)
    if (newScore >= -10 && newScore <= 0) {
        return ScoreResult.checkout(newScore);  // Would return here if win
    }

    // 5. Valid score!
    return ScoreResult.validScore(466);
}
```

### Step 6: Response Returns to Frontend

**Controller Response:**
```json
{
    "result": "VALID",
    "matchedAnswer": "Lionel Messi",
    "scoreValue": 35,
    "scoreBefore": 501,
    "scoreAfter": 466,
    "reason": null,
    "isWin": false,
    "gameState": {
        "gameId": "...",
        "questionText": "Appearances for Barcelona in La Liga 2019/20",
        "currentScore": 466,
        "turnCount": 1,
        "status": "IN_PROGRESS"
    }
}
```

### Step 7: Frontend Updates UI

**Frontend:**
```typescript
// Received response
const data = await response.json();

// Update score display
score = 466;  // Down from 501

// Show feedback
feedback = "✓ Lionel Messi: 35 points! Score: 466";
feedbackType = "success";

// Add to move history
moves = [
    { answer: "Messi", result: "VALID", scoreBefore: 501, scoreAfter: 466 },
    ...moves
];
```

---

## Testing Strategy

We have **three levels** of tests:

### 1. Unit Tests (Isolated Components)

**Example:** `ScoringServiceTest.java`
- Tests ONE class in isolation
- No database, no other services
- Fast! Runs in milliseconds
- Focuses on business logic

```java
@Test
void shouldCheckoutInRange() {
    // GIVEN: ScoringService (no dependencies)
    ScoringService service = new ScoringService();

    // WHEN: Calculate score that reaches 0
    ScoreResult result = service.calculateScore(50, 50);

    // THEN: Should be checkout
    assertThat(result.isCheckout()).isTrue();
}
```

### 2. Integration Tests (Components + Database)

**Example:** `AnswerEvaluatorIntegrationTest.java`
- Tests components working together
- Uses REAL database (PostgreSQL)
- Tests database queries work correctly
- Tests full game flow with real data

```java
@DataJpaTest  // Sets up real database
@Test
void testRealGameFlow() {
    // Uses REAL database with REAL player data

    // Turn 1: Submit exact player name
    AnswerResult turn1 = evaluator.evaluateAnswer(
        questionId,
        "Erling Haaland",  // Real player from database
        501,
        []
    );

    assertThat(turn1.isValid()).isTrue();
    assertThat(turn1.getDisplayText()).isEqualTo("Erling Haaland");
    // Real database query executed here!
}
```

### 3. Controller Tests (API Endpoints)

**Example:** `PracticeGameControllerTest.java`
- Tests HTTP endpoints work
- Tests request/response format
- Tests error handling

```java
@Test
void shouldStartPracticeGame() {
    // WHEN: POST to /api/practice/start
    ResponseEntity<GameStateResponse> response = controller.startPracticeGame(request);

    // THEN: Should return 200 OK with game state
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getCurrentScore()).isEqualTo(501);
}
```

---

## What's Currently Working

✅ **Single-player practice mode**
- Start a game
- Submit answers
- Get instant feedback
- Fuzzy matching for typos
- Darts scoring rules enforced
- Win detection

✅ **Database integration**
- Store questions and answers
- Prevent duplicate answers
- Track game moves
- Persistent storage

✅ **Web interface**
- Clean, responsive UI
- Real-time feedback
- Move history
- Score display

---

## What's NOT Yet Implemented

❌ **Multiplayer**
- No real-time 2-player games yet
- WebSocket not implemented
- Turn-based gameplay partially ready (code exists but not used)

❌ **Authentication**
- No user accounts
- No OAuth login
- Guest mode only

❌ **Matchmaking**
- No ranked games
- No MMR system
- No leagues/tiers

❌ **Question Population**
- No API-Football integration yet
- Using manually-created test data
- No automatic question generation

❌ **Daily Challenges**
- No daily challenge mode
- No leaderboards

---

## Key Files to Know

### Backend
```
PracticeGameController.java   - HTTP endpoints
GameService.java              - Core game logic
AnswerEvaluator.java          - Answer validation
ScoringService.java           - Score calculation
DartsValidator.java           - Darts rules
AnswerRepository.java         - Database queries
Game.java                     - Game data model
Answer.java                   - Answer data model
```

### Frontend
```
+page.svelte                  - Main game UI
```

### Tests
```
AnswerEvaluatorIntegrationTest.java  - Full flow test with database
ScoringServiceTest.java              - Scoring logic tests
DartsValidatorTest.java              - Darts validation tests
PracticeGameControllerTest.java      - API endpoint tests
```

---

## How to Add New Features

### Example: Adding a New Validation Rule

Let's say you want to add: "Players can't score more than 180 in one turn"

1. **Add test first** (TDD approach):
   ```java
   // In ScoringServiceTest.java
   @Test
   void shouldBustWhenScoreAbove180() {
       ScoreResult result = scoringService.calculateScore(501, 181);
       assertThat(result.isBust()).isTrue();
   }
   ```

2. **Update ScoringService:**
   ```java
   public ScoreResult calculateScore(int currentScore, int answerScore) {
       if (answerScore > 180) {
           return ScoreResult.bust(currentScore);
       }
       // ... rest of logic
   }
   ```

3. **Test passes!** ✅

### Example: Adding a New Endpoint

Let's say you want: "GET /api/practice/games/{gameId}/history"

1. **Add to Controller:**
   ```java
   @GetMapping("/games/{gameId}/history")
   public ResponseEntity<List<GameMove>> getGameHistory(@PathVariable UUID gameId) {
       List<GameMove> moves = gameService.getGameMoves(gameId);
       return ResponseEntity.ok(moves);
   }
   ```

2. **Add to GameService:**
   ```java
   public List<GameMove> getGameMoves(UUID gameId) {
       return gameMoveRepository.findByGameIdOrderByMoveNumberAsc(gameId);
   }
   ```

3. **Test it:**
   ```java
   @Test
   void shouldReturnGameHistory() {
       ResponseEntity<List<GameMove>> response = controller.getGameHistory(gameId);
       assertThat(response.getBody()).hasSize(3);
   }
   ```

---

## Common Pitfalls and Solutions

### Problem: "Why does fuzzy matching sometimes not work?"

**Cause:** Similarity threshold too high (0.8 means 80% similar required)

**Solution:** Adjust threshold in AnswerEvaluator:
```java
private static final double SIMILARITY_THRESHOLD = 0.5; // Lower = more lenient
```

### Problem: "Why do some valid answers show as bust?"

**Cause:** Score is in the invalid darts scores list

**Solution:** Check DartsValidator - scores like 179 are invalid in real darts!

### Problem: "Frontend shows wrong score after answer"

**Cause:** Frontend using local calculation instead of server response

**Solution:** Always trust the server response:
```typescript
// ❌ Wrong: Calculate locally
score = score - 35;

// ✅ Correct: Use server response
score = data.gameState.currentScore;
```

---

## Summary

The current implementation provides a **working single-player game** with:
- Full answer validation (exact + fuzzy matching)
- Proper darts scoring rules
- Database persistence
- Clean web interface
- Comprehensive test coverage

The foundation is solid and ready for multiplayer, authentication, and advanced features!
