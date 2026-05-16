# Common Patterns Library

Ready-to-use regex and glob patterns for skill triggers. Copy and customize for your skills.

---

## Intent Patterns (Regex)

### Feature/Endpoint Creation
```regex
(add|create|implement|build).*?(feature|endpoint|route|service|controller)
```

### Component Creation
```regex
(create|add|make|build).*?(component|UI|page|modal|dialog|form)
```

### Database Work
```regex
(add|create|modify|update).*?(user|table|column|field|schema|migration)
(database|jpa|hibernate).*?(change|update|query)
```

### Error Handling
```regex
(fix|handle|catch|debug).*?(error|exception|bug)
(add|implement).*?(try|catch|error.*?handling)
```

### Explanation Requests
```regex
(how does|how do|explain|what is|describe|tell me about).*?
```

### Workflow Operations
```regex
(create|add|modify|update).*?(workflow|step|branch|condition)
(debug|troubleshoot|fix).*?workflow
```

### Testing
```regex
(write|create|add).*?(test|spec|unit.*?test)
```

---

## File Path Patterns (Glob)

### SvelteKit Frontend
```glob
frontend/src/**/*.svelte     # All Svelte components
frontend/src/**/*.ts         # All TypeScript files
src/**/*.svelte              # Svelte files at root level
src/lib/**/*.ts              # Library TypeScript files
```

### Spring Boot Backend
```glob
backend/src/**/*.java        # All Java files
backend/src/main/**/*.java   # Main source Java files
backend/src/test/**/*.java   # Test Java files
```

### Python Scraper
```glob
scraper/**/*.py              # All Python files
```

### Database
```glob
**/schema.sql               # SQL schema files
**/migrations/**/*.sql      # Migration files
backend/src/main/resources/**  # Spring Boot resources
```

### Test Exclusions
```glob
**/*Test.java               # JUnit test files
**/*.test.ts                # TypeScript tests
**/*.spec.ts                # Spec files
**/*.test.svelte            # Svelte component tests
```

---

## Content Patterns (Regex)

### Spring Boot / JPA
```regex
@RestController              # REST controllers
@Service                     # Spring services
@Repository                  # Spring repositories
@SpringBootApplication       # Main application class
@Entity                      # JPA entities
@MessageMapping              # WebSocket handlers
import.*springframework      # Spring imports
```

### SvelteKit Components
```regex
<script lang="ts">           # TypeScript Svelte script
export let                   # Svelte component props
import.*\$lib                # $lib imports
writable\(|readable\(|derived\(  # Svelte store creation
{#await                      # Svelte await blocks
```

### Error Handling
```regex
try\s*\{                    # Try blocks
catch\s*\(                  # Catch blocks
throw new                    # Throw statements
```

---

**Usage Example:**

```json
{
  "sveltekit-dev-guidelines": {
    "promptTriggers": {
      "intentPatterns": [
        "(create|add|build).*?(component|UI|page)"
      ]
    },
    "fileTriggers": {
      "pathPatterns": [
        "frontend/src/**/*.svelte",
        "src/**/*.svelte"
      ],
      "contentPatterns": [
        "<script lang=\"ts\">",
        "export let"
      ]
    }
  }
}
```

---

**Related Files:**
- [SKILL.md](../SKILL.md) - Main skill guide
- [TRIGGER_TYPES.md](TRIGGER_TYPES.md) - Detailed trigger documentation
- [SKILL_RULES_REFERENCE.md](SKILL_RULES_REFERENCE.md) - Complete schema
