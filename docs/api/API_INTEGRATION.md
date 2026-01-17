# Football 501 - API Integration Specification

**Version**: 1.0
**Last Updated**: 2026-01-17
**Status**: Draft
**API Provider**: API-Football

---

## Table of Contents

1. [Overview](#overview)
2. [API Provider Details](#api-provider-details)
3. [Authentication](#authentication)
4. [Endpoints Used](#endpoints-used)
5. [Data Models](#data-models)
6. [Caching Strategy](#caching-strategy)
7. [Error Handling](#error-handling)
8. [Rate Limiting](#rate-limiting)
9. [Question Population Workflow](#question-population-workflow)
10. [Data Update Strategy](#data-update-strategy)

---

## Overview

Football 501 integrates with **API-Football** to obtain player statistics for answer validation. The integration follows an **aggressive caching strategy** to minimize API calls and ensure match validation never depends on real-time API availability.

### Integration Architecture

```
┌──────────────────┐
│  API-Football    │  (External Service)
│  api-football.com│
└────────┬─────────┘
         │ HTTPS (Batch Calls)
         │
┌────────▼─────────────────┐
│  Integration Service     │  (Spring Boot)
│  - Question Populator    │
│  - Stats Updater         │
│  - API Client            │
└────────┬─────────────────┘
         │
┌────────▼─────────┐
│  PostgreSQL      │
│  - questions     │
│  - answers       │  (Cached Data)
└──────────────────┘
```

**Key Principle**: All match validation uses cached database, **never live API calls**.

---

## API Provider Details

### Provider

- **Name**: API-Football
- **Website**: https://www.api-football.com
- **Documentation**: https://www.api-football.com/documentation-v3
- **Alternative Access**: RapidAPI (https://rapidapi.com/api-sports/api/api-football)

### API Version

- **Version**: v3
- **Base URL**: `https://v3.football.api-sports.io/`
- **Alternative (RapidAPI)**: `https://api-football-v1.p.rapidapi.com/v3/`

### Subscription Tier

| Tier | Cost | Requests/Day | Use Case |
|------|------|--------------|----------|
| **Free** | $0/month | 100 | **MVP** - Initial question population |
| **Pro** | $19/month | 7,500 | Post-MVP - Faster question additions |
| **Ultra** | $29/month | 75,000 | Scale - Frequent updates |
| **Mega** | $39/month | 150,000 | High-traffic - Daily updates |

**Initial Plan**: Start with **Free tier** for MVP (sufficient for 20-30 question population over 2-3 weeks).

---

## Authentication

### API Key

API-Football uses API key authentication via request headers.

#### Headers Required

```http
X-RapidAPI-Key: {YOUR_API_KEY}
X-RapidAPI-Host: v3.football.api-sports.io
```

**OR** (if using RapidAPI):

```http
X-RapidAPI-Key: {YOUR_RAPIDAPI_KEY}
X-RapidAPI-Host: api-football-v1.p.rapidapi.com
```

### Storing API Key

- **Environment Variable**: `FOOTBALL_API_KEY`
- **Spring Configuration**: `application.yml`

```yaml
football-api:
  base-url: https://v3.football.api-sports.io
  api-key: ${FOOTBALL_API_KEY}
  timeout: 10000 # ms
```

**Security**:
- ❌ Never commit API key to version control
- ✅ Use environment variables or secret management
- ✅ Rotate keys periodically

---

## Endpoints Used

### 1. Players Statistics Endpoint

**Purpose**: Get player statistics for a specific team, league, and season

**Endpoint**: `GET /players`

**URL**: `https://v3.football.api-sports.io/players`

#### Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `team` | int | Yes | Team ID | `50` (Manchester City) |
| `league` | int | Yes | League ID | `39` (Premier League) |
| `season` | int | Yes | Season year | `2023` |
| `page` | int | No | Pagination | `1`, `2`, `3`... |

#### Example Request

```http
GET /players?team=50&league=39&season=2023 HTTP/1.1
Host: v3.football.api-sports.io
X-RapidAPI-Key: YOUR_API_KEY
X-RapidAPI-Host: v3.football.api-sports.io
```

#### Example Response

```json
{
  "get": "players",
  "parameters": {
    "team": "50",
    "league": "39",
    "season": "2023"
  },
  "errors": [],
  "results": 30,
  "paging": {
    "current": 1,
    "total": 1
  },
  "response": [
    {
      "player": {
        "id": 154,
        "name": "Kevin De Bruyne",
        "firstname": "Kevin",
        "lastname": "De Bruyne",
        "age": 32,
        "birth": {
          "date": "1991-06-28",
          "place": "Drongen",
          "country": "Belgium"
        },
        "nationality": "Belgium",
        "height": "181 cm",
        "weight": "70 kg",
        "photo": "https://media.api-sports.io/football/players/154.png"
      },
      "statistics": [
        {
          "team": {
            "id": 50,
            "name": "Manchester City",
            "logo": "https://media.api-sports.io/football/teams/50.png"
          },
          "league": {
            "id": 39,
            "name": "Premier League",
            "country": "England",
            "logo": "https://media.api-sports.io/football/leagues/39.png",
            "flag": "https://media.api-sports.io/flags/gb.svg",
            "season": 2023
          },
          "games": {
            "appearences": 32,
            "lineups": 29,
            "minutes": 2567,
            "number": null,
            "position": "Midfielder",
            "rating": "7.583333",
            "captain": false
          },
          "substitutes": {
            "in": 3,
            "out": 12,
            "bench": 3
          },
          "shots": {
            "total": 47,
            "on": 22
          },
          "goals": {
            "total": 6,
            "conceded": 0,
            "assists": 18,
            "saves": null
          },
          "passes": {
            "total": 2455,
            "key": 91,
            "accuracy": 86
          },
          "tackles": {
            "total": 38,
            "blocks": 4,
            "interceptions": 18
          },
          "duels": {
            "total": 187,
            "won": 92
          },
          "dribbles": {
            "attempts": 61,
            "success": 36,
            "past": null
          },
          "fouls": {
            "drawn": 38,
            "committed": 23
          },
          "cards": {
            "yellow": 2,
            "yellowred": 0,
            "red": 0
          },
          "penalty": {
            "won": null,
            "commited": null,
            "scored": 0,
            "missed": 0,
            "saved": null
          }
        }
      ]
    }
  ]
}
```

#### Relevant Fields for Football 501

| Field Path | Description | Use Case |
|------------|-------------|----------|
| `response[].player.id` | Player API ID | Unique identifier |
| `response[].player.name` | Player full name | Display name |
| `response[].statistics[].games.appearences` | Appearances | Score for "Appearances" questions |
| `response[].statistics[].goals.total` | Goals scored | Score for "Goals" or "Apps + Goals" |
| `response[].statistics[].goals.assists` | Assists | Future question type |
| `response[].player.nationality` | Nationality | For nationality-filtered questions |

---

### 2. Teams Endpoint (Reference Data)

**Purpose**: Get team IDs for question setup

**Endpoint**: `GET /teams`

**URL**: `https://v3.football.api-sports.io/teams`

#### Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `league` | int | Yes | League ID | `39` |
| `season` | int | Yes | Season | `2023` |

#### Example Request

```http
GET /teams?league=39&season=2023 HTTP/1.1
Host: v3.football.api-sports.io
X-RapidAPI-Key: YOUR_API_KEY
```

#### Example Response

```json
{
  "response": [
    {
      "team": {
        "id": 50,
        "name": "Manchester City",
        "code": "MCI",
        "country": "England",
        "founded": 1880,
        "national": false,
        "logo": "https://media.api-sports.io/football/teams/50.png"
      },
      "venue": {
        "id": 555,
        "name": "Etihad Stadium",
        "address": "Ashton New Road",
        "city": "Manchester",
        "capacity": 55097,
        "surface": "grass",
        "image": "https://media.api-sports.io/football/venues/555.png"
      }
    }
  ]
}
```

---

### 3. Leagues Endpoint (Reference Data)

**Purpose**: Get league IDs for question setup

**Endpoint**: `GET /leagues`

**URL**: `https://v3.football.api-sports.io/leagues`

#### Common League IDs

| League | ID | Country |
|--------|----|---------|
| Premier League | `39` | England |
| La Liga | `140` | Spain |
| Serie A | `135` | Italy |
| Bundesliga | `78` | Germany |
| Ligue 1 | `61` | France |
| Champions League | `2` | Europe |
| World Cup | `1` | International |

---

## Data Models

### Internal Question Model

```java
@Entity
@Table(name = "questions")
public class Question {
    @Id
    private UUID id;

    private String questionType;        // 'team_league_appearances'
    private String questionText;        // "Appearances for Manchester City in Premier League"
    private String questionTemplate;    // "Appearances for {team} in {league}"

    private String teamName;            // "Manchester City"
    private Integer teamId;             // 50 (API-Football ID)

    private String leagueName;          // "Premier League"
    private Integer leagueId;           // 39 (API-Football ID)

    private String season;              // "2023-2024"

    private String countryFilter;       // For nationality questions
    private String positionFilter;      // 'goalkeeper', etc.
    private String statType;            // 'appearances', 'goals', 'apps_goals'

    private Boolean isActive;
    private String difficultyTier;      // 🔄 IN PROGRESS

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
```

### Internal Answer Model

```java
@Entity
@Table(name = "answers")
public class Answer {
    @Id
    private UUID id;

    @ManyToOne
    private Question question;

    private String playerName;          // "Kevin De Bruyne"
    private Integer playerApiId;        // 154 (API-Football ID)

    private Integer score;              // 32 (appearances)
    private Boolean isValidDartsScore;  // true
    private Boolean isBust;             // false

    private Integer usageCount;         // Track popularity
    private Timestamp lastUsed;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
```

### API Response Mapping

**From API to Database**:

```java
// Pseudo-code mapping
APIResponse response = footballApiClient.getPlayers(teamId, leagueId, season);

for (PlayerData player : response.getResponse()) {
    Answer answer = new Answer();
    answer.setPlayerName(player.getPlayer().getName());
    answer.setPlayerApiId(player.getPlayer().getId());

    // For "Appearances" question
    int appearances = player.getStatistics().get(0).getGames().getAppearences();
    answer.setScore(appearances);

    // Validate darts score
    answer.setIsValidDartsScore(DartsValidator.isValid(appearances));
    answer.setIsBust(appearances > 180 || !answer.getIsValidDartsScore());

    answerRepository.save(answer);
}
```

---

## Caching Strategy

### Principle

**All match validation uses cached data. API is only called for population and updates.**

### Cache Workflow

```
┌─────────────────────┐
│  Admin Creates      │
│  New Question       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Batch API Calls    │  (Over hours/days to respect rate limits)
│  - Get team players │
│  - Get statistics   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Populate Answers   │
│  Table              │
│  - Player names     │
│  - Scores           │
│  - Validation flags │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Question Active    │  ✅ Ready for use in matches
└─────────────────────┘
```

### Cache Invalidation

#### Weekly Refresh (Automated)

```
Every Sunday at 3:00 AM UTC:
1. Query questions with current season
2. For each question:
   - Fetch latest player stats from API
   - Update existing answers
   - Add new players (transfers, debuts)
   - Mark retired/transferred players
3. Log API calls used
```

#### Manual Refresh (Admin Panel)

Admin can trigger immediate refresh for a specific question.

---

## Error Handling

### API Errors

| Error | HTTP Code | Handling |
|-------|-----------|----------|
| Invalid API Key | 401 | Log error, alert admin, halt population |
| Rate Limit Exceeded | 429 | Pause, retry after rate limit resets |
| Team Not Found | 404 | Log error, mark question as invalid |
| Server Error | 500 | Retry with exponential backoff (3 attempts) |
| Timeout | - | Retry once, then fail |

### Handling During Population

```java
try {
    APIResponse response = apiClient.getPlayers(teamId, leagueId, season);
    processResponse(response);
} catch (RateLimitException e) {
    // Pause population, schedule retry
    logger.warn("Rate limit hit. Pausing population.");
    scheduleRetry(questionId, delayMinutes: 60);
} catch (APIException e) {
    // Log and alert
    logger.error("API error during population", e);
    alertAdmin("Question population failed for question: " + questionId);
}
```

### Handling During Matches

**Matches never call API**, so API errors cannot affect gameplay.

If answer validation fails due to missing cached data:
- Log error
- Return "invalid answer" to player
- Alert admin to populate question

---

## Rate Limiting

### Free Tier Limits

- **100 requests per day**
- **Resets**: Daily at 00:00 UTC
- **Per-minute limit**: Not explicitly documented (assume ~10-20/min)

### Request Budget Strategy

#### MVP Population Plan (Free Tier)

Assuming **20 questions**, each requiring **30-50 API calls** (team squad + pagination):

```
Total API calls needed: 20 questions × 40 calls = 800 calls
Daily limit: 100 calls/day
Time to populate: 800 / 100 = 8 days
```

**Strategy**:
- Populate **2-3 questions per day**
- Run batch job once daily (3:00 AM UTC)
- Monitor usage via API response headers

#### Scaling Plan (Pro Tier - $19/month)

With **7,500 requests/day**:
- Can populate **100+ questions per day**
- Weekly refresh of all questions feasible
- Real-time question additions possible

### Rate Limit Monitoring

Track API usage in database:

```sql
CREATE TABLE api_usage_log (
    id UUID PRIMARY KEY,
    endpoint VARCHAR(255),
    requests_made INT,
    date DATE,
    tier VARCHAR(50), -- 'free', 'pro', etc.
    created_at TIMESTAMP
);
```

Daily job checks usage and alerts if approaching limit.

---

## Question Population Workflow

### Manual Question Creation (MVP)

Admin manually creates question via admin panel or database insert.

```sql
INSERT INTO questions (id, question_type, question_text, team_name, team_id, league_name, league_id, season, stat_type, is_active)
VALUES (
    gen_random_uuid(),
    'team_league_appearances',
    'Appearances for Manchester City in Premier League',
    'Manchester City',
    50,
    'Premier League',
    39,
    '2023-2024',
    'appearances',
    false  -- Not active until populated
);
```

### Automatic Population Job

Backend scheduled job processes unpopulated questions:

```java
@Scheduled(cron = "0 0 3 * * *")  // Daily at 3 AM UTC
public void populateQuestions() {
    List<Question> unpopulated = questionRepository.findByIsActive(false);

    for (Question question : unpopulated) {
        if (apiUsageLimitReached()) {
            logger.info("Daily API limit reached. Pausing.");
            break;
        }

        populateQuestion(question);
    }
}

private void populateQuestion(Question question) {
    int page = 1;
    boolean hasMorePages = true;

    while (hasMorePages) {
        APIResponse response = apiClient.getPlayers(
            question.getTeamId(),
            question.getLeagueId(),
            extractYear(question.getSeason()),
            page
        );

        processPlayers(response, question);

        hasMorePages = response.getPaging().getCurrent() < response.getPaging().getTotal();
        page++;
    }

    question.setIsActive(true);
    questionRepository.save(question);
}

private void processPlayers(APIResponse response, Question question) {
    for (PlayerData playerData : response.getResponse()) {
        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setPlayerName(playerData.getPlayer().getName());
        answer.setPlayerApiId(playerData.getPlayer().getId());

        int score = calculateScore(playerData, question.getStatType());
        answer.setScore(score);
        answer.setIsValidDartsScore(DartsValidator.isValid(score));
        answer.setIsBust(score > 180 || !answer.getIsValidDartsScore());

        answerRepository.save(answer);
    }
}
```

---

## Data Update Strategy

### Current Season Updates

**Frequency**: Weekly (every Sunday)

**Process**:
1. Identify questions with current season (e.g., "2023-2024")
2. Fetch latest stats from API
3. Update existing player records
4. Add new players (transfers, debuts)
5. Keep historical data (don't delete old answers)

### Historical Data

**Frequency**: Rarely (only on errors or data corrections)

**Process**:
- Historical seasons (e.g., "2022-2023") are mostly static
- Only update if data error is reported
- Manual refresh via admin panel

---

## Testing Strategy

### Integration Tests

```java
@SpringBootTest
@TestPropertySource(properties = {
    "football-api.base-url=https://mock-api.test",
    "football-api.api-key=test-key"
})
public class FootballApiClientTest {

    @Autowired
    private FootballApiClient apiClient;

    @Test
    public void testGetPlayers() {
        // Mock API response
        mockServer.expect(requestTo("https://mock-api.test/players?team=50&league=39&season=2023"))
            .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        APIResponse response = apiClient.getPlayers(50, 39, 2023);

        assertNotNull(response);
        assertEquals(30, response.getResults());
    }

    @Test
    public void testRateLimitHandling() {
        // Mock 429 response
        mockServer.expect(requestTo("https://mock-api.test/players?team=50&league=39&season=2023"))
            .andRespond(withStatus(429));

        assertThrows(RateLimitException.class, () -> {
            apiClient.getPlayers(50, 39, 2023);
        });
    }
}
```

---

## Security Considerations

### API Key Protection

- ✅ Store in environment variables
- ✅ Never log API key
- ✅ Rotate periodically
- ✅ Use different keys for dev/staging/prod

### Data Integrity

- ✅ Validate API responses before saving
- ✅ Sanitize player names (SQL injection prevention)
- ✅ Check for duplicate answers before insert
- ✅ Log all API interactions for audit

---

## Future Enhancements

### Post-MVP Improvements

1. **API Response Caching** (Redis)
   - Cache API responses for 24 hours
   - Reduce duplicate calls

2. **Automated Question Generation**
   - Analyze popular teams/leagues
   - Auto-create questions from trending data

3. **Multi-API Support**
   - Fallback to alternative API if API-Football is down
   - Compare data accuracy across providers

4. **Real-Time Stats** (Premium Feature)
   - Live-updated stats during ongoing matches
   - Premium questions with in-season data

---

## Appendix

### Useful API-Football Resources

- [API Documentation](https://www.api-football.com/documentation-v3)
- [Coverage List](https://www.api-football.com/coverage)
- [Pricing Plans](https://www.api-football.com/pricing)
- [Rate Limit Guide](https://www.api-football.com/news/post/how-ratelimit-works)

### Common League & Team IDs

#### Premier League Teams (League ID: 39)

| Team | ID |
|------|----|
| Manchester City | 50 |
| Liverpool | 40 |
| Chelsea | 49 |
| Arsenal | 42 |
| Manchester United | 33 |
| Tottenham | 47 |

#### Other Major Leagues

| League | ID | Country |
|--------|----|---------|
| La Liga | 140 | Spain |
| Serie A | 135 | Italy |
| Bundesliga | 78 | Germany |
| Ligue 1 | 61 | France |
| Eredivisie | 88 | Netherlands |
| Liga Portugal | 94 | Portugal |

---

**Document Status**: Draft
**Last Updated**: 2026-01-17
**Next Review**: After MVP backend implementation begins
