# Football 501 Backend

Spring Boot backend service for Football 501 game.

## Tech Stack

- **Java**: 17+
- **Framework**: Spring Boot 3.2.1
- **Database**: PostgreSQL 15+ (production), H2 (unit tests)
- **Build Tool**: Maven
- **Testing**: JUnit 5, TestContainers, AssertJ

## Project Structure

```
backend/
├── src/
│   ├── main/java/com/football501/
│   │   ├── engine/           # Core game logic (TDD starting point)
│   │   ├── model/            # Domain models
│   │   ├── repository/       # Data access layer
│   │   ├── service/          # Business logic
│   │   ├── api/              # REST controllers
│   │   └── websocket/        # WebSocket handlers
│   └── test/java/com/football501/
│       ├── engine/           # Engine unit tests
│       ├── service/          # Service unit tests
│       └── integration/      # Integration tests
└── pom.xml
```

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 15+ (for integration tests, use TestContainers)

### Build

```bash
mvn clean install
```

### Run Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=DartsValidatorTest

# Skip tests
mvn clean install -DskipTests
```

### Run Application

```bash
mvn spring-boot:run
```

Application will start on `http://localhost:8080`

## Testing Strategy

### Unit Tests
- **Location**: `src/test/java/com/football501/engine/`
- **Framework**: JUnit 5
- **Scope**: Pure logic tests (no Spring context)
- **Example**: `DartsValidatorTest`, `GameEngineTest`

### Integration Tests
- **Location**: `src/test/java/com/football501/integration/`
- **Framework**: Spring Boot Test + TestContainers
- **Scope**: Database operations, API endpoints
- **Example**: `TransfermarktImportTest`, `GameApiTest`

### Test Naming Convention
- Unit tests: `*Test.java` (e.g., `DartsValidatorTest.java`)
- Integration tests: `*IntegrationTest.java`

## TDD Workflow

1. **Write failing test** in appropriate test class
2. **Run test** to confirm it fails: `mvn test -Dtest=ClassName`
3. **Implement minimal code** to pass the test
4. **Run test again** to confirm it passes
5. **Refactor** if needed
6. **Repeat**

## Environment Variables

```bash
# Database
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=football501
export DB_USER=football501
export DB_PASSWORD=your_password

# Data import
export DATA_PATH=../data/transfermarkt
```

## Database Setup (Local Development)

```bash
# Using Docker
docker run -d \
  --name football501-postgres \
  -e POSTGRES_DB=football501 \
  -e POSTGRES_USER=football501 \
  -e POSTGRES_PASSWORD=dev_password \
  -p 5432:5432 \
  postgres:15
```

## Next Steps (TDD)

1. ✅ Project structure created
2. ⏳ Write first test: `DartsValidatorTest`
3. ⏳ Implement `DartsValidator`
4. ⏳ Test data import from Transfermarkt CSV
5. ⏳ Build game engine with scoring logic
