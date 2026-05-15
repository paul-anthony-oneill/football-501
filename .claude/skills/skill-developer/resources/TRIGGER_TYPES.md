# Trigger Types - Complete Guide

Complete reference for configuring skill triggers in Claude Code's skill auto-activation system.

## Table of Contents

- [Keyword Triggers (Explicit)](#keyword-triggers-explicit)
- [Intent Pattern Triggers (Implicit)](#intent-pattern-triggers-implicit)
- [File Path Triggers](#file-path-triggers)
- [Content Pattern Triggers](#content-pattern-triggers)
- [Best Practices Summary](#best-practices-summary)

---

## Keyword Triggers (Explicit)

### How It Works

Case-insensitive substring matching in user's prompt.

### Use For

Topic-based activation where user explicitly mentions the subject.

### Configuration

```json
"promptTriggers": {
  "keywords": ["svelte", "component", "store", "tailwind"]
}
```

### Example

- User prompt: "how does the **svelte** store work?"
- Matches: "svelte" keyword
- Activates: `sveltekit-dev-guidelines`

### Best Practices

- Use specific, unambiguous terms
- Include common variations ("svelte", "sveltekit", "svelte component")
- Avoid overly generic words ("system", "work", "create")
- Test with real prompts

---

## Intent Pattern Triggers (Implicit)

### How It Works

Regex pattern matching to detect user's intent even when they don't mention the topic explicitly.

### Use For

Action-based activation where user describes what they want to do rather than the specific topic.

### Configuration

```json
"promptTriggers": {
  "intentPatterns": [
    "(create|add|implement).*?(endpoint|controller|service)",
    "(how does|explain).*?(game.*?engine|scoring)"
  ]
}
```

### Examples

**Backend Work:**
- User prompt: "add user authentication service"
- Matches: `(add).*?(service)`
- Activates: `spring-boot-dev-guidelines`

**Component Creation:**
- User prompt: "create a score display widget"
- Matches: `(create).*?(component)` (if component in pattern)
- Activates: `sveltekit-dev-guidelines`

### Best Practices

- Capture common action verbs: `(create|add|modify|build|implement)`
- Include domain-specific nouns: `(feature|endpoint|component|service)`
- Use non-greedy matching: `.*?` instead of `.*`
- Test patterns thoroughly with regex tester (https://regex101.com/)
- Don't make patterns too broad (causes false positives)
- Don't make patterns too specific (causes false negatives)

### Common Pattern Examples

```regex
# Backend endpoint work
(add|create|implement).*?(endpoint|controller|service|repository)

# Frontend component work
(create|add|make|build).*?(component|UI|page|modal|store)

# Explanations
(how does|explain|what is|describe).*?

# Error Handling
(fix|handle|catch|debug).*?(error|exception|bug)

# Game Logic
(implement|add|build).*?(game.*?logic|scoring|bust|checkout)
```

---

## File Path Triggers

### How It Works

Glob pattern matching against the file path being edited.

### Use For

Domain/area-specific activation based on file location in the project.

### Configuration

```json
"fileTriggers": {
  "pathPatterns": [
    "frontend/src/**/*.svelte",
    "backend/src/**/*.java"
  ],
  "pathExclusions": [
    "**/*Test.java",
    "**/*.test.ts"
  ]
}
```

### Glob Pattern Syntax

- `**` = Any number of directories (including zero)
- `*` = Any characters within a directory name
- Examples:
  - `frontend/src/**/*.svelte` = All .svelte files in frontend/src and subdirs
  - `backend/src/**/*.java` = All .java files in backend/src subdirs
  - `**/*.sql` = Any SQL file anywhere in project

### Best Practices

- Be specific to avoid false positives
- Use exclusions for test files: `**/*Test.java`, `**/*.test.ts`
- Consider subdirectory structure
- Test patterns with actual file paths
- Use narrower patterns when possible

### Common Path Patterns for Football-501

```glob
# SvelteKit Frontend
frontend/src/**/*.svelte
frontend/src/**/*.ts
src/**/*.svelte

# Spring Boot Backend
backend/src/main/**/*.java
backend/src/main/resources/**

# Python Scraper
scraper/**/*.py

# Test Exclusions
**/*Test.java
**/*.test.ts
**/*.test.svelte
```

---

## Content Pattern Triggers

### How It Works

Regex pattern matching against the file's actual content (what's inside the file).

### Use For

Technology-specific activation based on what the code imports or uses.

### Configuration

```json
"fileTriggers": {
  "contentPatterns": [
    "@RestController",
    "@Service",
    "@Repository",
    "@MessageMapping"
  ]
}
```

### Examples

**Spring Boot Detection:**
- File contains: `@RestController`
- Matches: `@RestController`
- Activates: `spring-boot-dev-guidelines`

**SvelteKit Detection:**
- File contains: `<script lang="ts">`
- Matches: `<script lang="ts">`
- Activates: `sveltekit-dev-guidelines`

### Best Practices

- Match imports: `import.*springframework` (case-insensitive with [Pp] or flags)
- Escape special regex chars: `\\.findBy\\(` not `.findBy(`
- Patterns use case-insensitive flag
- Test against real file content
- Make patterns specific enough to avoid false matches

### Common Content Patterns for Football-501

```regex
# Spring Boot
@RestController
@Service
@Repository
@SpringBootApplication
@MessageMapping
import.*springframework

# SvelteKit
<script lang="ts">
export let
import.*\$lib
writable\(|readable\(|derived\(

# Python Scraper
import ScraperFC
from ScraperFC
import requests
```

---

## Best Practices Summary

### DO:
- Use specific, unambiguous keywords
- Test all patterns with real examples
- Include common variations
- Use non-greedy regex: `.*?`
- Escape special characters in content patterns
- Add exclusions for test files
- Make file path patterns narrow and specific

### DON'T:
- Use overly generic keywords ("system", "work")
- Make intent patterns too broad (false positives)
- Make patterns too specific (false negatives)
- Forget to test with regex tester (https://regex101.com/)
- Use greedy regex: `.*` instead of `.*?`
- Match too broadly in file paths

### Testing Your Triggers

**Test keyword/intent triggers:**
```bash
echo '{"session_id":"test","prompt":"your test prompt"}' | \
  npx tsx .claude/hooks/skill-activation-prompt.ts
```

---

**Related Files:**
- [SKILL.md](../SKILL.md) - Main skill guide
- [SKILL_RULES_REFERENCE.md](SKILL_RULES_REFERENCE.md) - Complete skill-rules.json schema
- [PATTERNS_LIBRARY.md](PATTERNS_LIBRARY.md) - Ready-to-use pattern library
