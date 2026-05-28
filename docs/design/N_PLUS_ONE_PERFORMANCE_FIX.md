# N+1 Performance Fix — Bulk Question Activation

## Problem Summary

Activating 100 draft questions (promoting them to "active" and materialising their
answer rows from `player_season_stints`) took over 90 seconds. The root cause was
three nested N+1 query patterns in `QuestionMaterializerService.upsertAnswers()`,
plus a Spring Security filter-ordering bug that surfaced after restart.

## What is an N+1 Query?

An N+1 query happens when you execute one query to fetch a list, then **for each
element** of that list, you execute another query. If the list has 150 items, you
make 151 database round-trips instead of 2.

```
// N+1 pattern (BAD):
for (String key : answerKeys) {                          // 1 trip to get keys
    Answer a = repo.findByQuestionIdAndAnswerKey(qId, key);  // N trips (one per key)
}

// Batched pattern (GOOD):
Map<String, Answer> existing = repo.findByQuestionIdAndAnswerKeyIn(qId, answerKeys);  // 1 trip
```

Each round-trip costs: network latency (~0.1ms local, ~1-5ms remote), connection
checkout from the pool, query parsing, and result marshalling. With N=150 answers
per question × 100 questions, that's roughly 45,000 unnecessary queries.

---

## File-by-File Changes

### 1. `AnswerRepository.java` — Added Batch Lookup

**What changed:** Added one method.

```java
List<Answer> findByQuestionIdAndAnswerKeyIn(UUID questionId, Set<String> answerKeys);
```

**Why:** The old code called `findByQuestionIdAndAnswerKey(questionId, answerKey)`
inside a loop — one query per answer. This Spring Data JPA derived query generates:

```sql
SELECT * FROM answers
WHERE question_id = ? AND answer_key IN (?, ?, ?, ...)
```

Spring Data JPA derives the query from the method name. The `In` suffix tells it
to use SQL's `IN` clause. No `@Query` annotation is needed because the method
name follows Spring Data's naming convention.

**Pattern:** Batch fetch — collect all the keys you need, fetch them in one query,
then index the results into a `Map` for O(1) lookup. This is the single biggest
performance win in the change set.

---

### 2. `NamedEntityRepository.java` — Added Batch Entity Lookup

**What changed:** Added one method.

```java
List<NamedEntity> findByEntityTypeAndNormalizedNameIn(String entityType, Set<String> normalizedNames);
```

**Why:** The old `EntitySearchService.upsertEntity()` method did a
`findByEntityTypeAndNormalizedName()` call per answer to check if the entity
already existed before inserting. Calling this 150 times per question = 150
individual SELECTs.

The batch version fetches all existing entities for all answer names in one query,
then the service layer filters which ones actually need inserting.

**Pattern:** Same batch-fetch pattern as `AnswerRepository`. Collect keys → one
`IN` query → index into a `Set<String>` of already-existing normalized names.

---

### 3. `EntitySearchService.java` — Batch Entity Registration

**What changed:** Added a `batchUpsertEntities` method and an `EntityEntry` record.

```java
public record EntityEntry(String displayText, String entityType, String hint) {}

public int batchUpsertEntities(List<EntityEntry> entries)
```

**What it does (step by step):**

1. **Extract normalized names** — takes all display text, strips accents, lowercases
2. **One batch query** — calls `findByEntityTypeAndNormalizedNameIn()` to get all
   already-existing entities
3. **Build a `Set<String>` of existing normalized names** — `HashSet.contains()` is O(1)
4. **Deduplicate entries** — `Collectors.groupingBy()` ensures we don't try to
   insert the same name twice (two answers for the same player)
5. **Filter to only new entries** — skips any whose normalized name is already in the set
6. **`saveAll()`** — single batch insert for all genuinely-new entities

**Why `saveAll()` instead of a native `INSERT ... ON CONFLICT`:** The entity
table already has a unique constraint on `(entity_type, normalized_name)`, and an
existing `bulkUpsertFootballersFromPlayers()` method uses the native pattern. For
the materializer flow, using `saveAll()` with pre-filtered new entities is simpler
and lets Hibernate handle ID generation. Both approaches are valid; the key
insight is that **pre-filtering in the service layer eliminates the need for
individual existence checks**.

**Why the `EntityEntry` record:** It's a lightweight DTO to pass `(displayText,
entityType, hint)` triples as a typed list. Using a record (Java 16+) gives us a
constructor, accessor methods, `equals`, `hashCode`, and `toString` for free —
ideal for data-transfer objects.

---

### 4. `QuestionMaterializerService.java` — Core N+1 Fix

**What changed:** The `upsertAnswers()` method was rewritten. This is where all
three N+1 fixes converge.

**The old flow (per question, with ~150 answers):**

```
for each MaterializedAnswer:
  1. DartsValidator.isValidDartsScore(score)      // in-memory, fast
  2. answerRepository.findByQuestionIdAndAnswerKey() // DB round-trip (N+1 #1)
  3. answerRepository.save()                        // DB round-trip (N+1 #2)
  4. entitySearchService.upsertEntity()             // DB round-trip (N+1 #3)
      └─ findByEntityTypeAndNormalizedName() + save()
```

Total: ~450 DB queries per question, ~45,000 per 100 questions.

**The new flow:**

```
Phase 1 — Batch fetch (2 queries total, regardless of answer count):
  A. answerRepository.findByQuestionIdAndAnswerKeyIn(qId, allKeys)
     → Map<String, Answer> existingByKey
  B. Build EntityEntry list from all computed answers

Phase 2 — Loop through computed answers (in-memory only, no DB calls):
  for each MaterializedAnswer:
    1. DartsValidator.isValidDartsScore(score)     // in-memory
    2. Lookup in existingByKey Map                  // O(1) in-memory
    3. If found: mutate managed entity (Hibernate dirty-checks later)
       If not: build new Answer, add to newAnswers list
    4. Accumulate zone counts (high/mid/checkout)   // in-memory

Phase 3 — Batch writes:
  C. answerRepository.saveAll(newAnswers)          // 1 batch INSERT
  D. entitySearchService.batchUpsertEntities()     // 1 batch SELECT + 1 batch INSERT
  E. Hibernate auto-flushes dirty-checked managed entities as UPDATEs
```

Total: 4-5 DB queries per question, ~500 per 100 questions — a **90x reduction**.

**Key concept — Managed vs Detached entities:**

When you fetch an entity from a Spring Data repository inside a `@Transactional`
method, that entity is **managed** by Hibernate's persistence context. Hibernate
tracks its state. If you call `setScore()` on a managed entity, Hibernate
detects the change and automatically issues an `UPDATE` at flush time — you
don't need to call `save()` at all.

```java
// Managed entity — no save() needed:
Answer a = existingByKey.get(key);  // fetched from DB, managed
a.setScore(100);                    // Hibernate dirty-checks this
// At transaction commit: UPDATE answers SET score = 100 WHERE id = ?

// Detached/new entity — needs persist:
Answer a = Answer.builder()...build();  // created in Java, not managed
newAnswers.add(a);                       // will be INSERTed via saveAll()
```

The old code called `save()` on both managed and new entities. Calling `save()`
on a managed entity triggers an unnecessary `merge()` operation. The new code
**separates the two cases**: managed entities are mutated in place, new entities
are collected and batch-inserted.

**Why this matters for the duplicate-key fix:** The first version of the fix
tried to `saveAll()` a mixed list of managed and new entities. For the managed
entities, `saveAll()`'s internal `isNew()` check returned `false` (because they
had IDs), so it called `merge()`. But `merge()` returns a new managed instance
while the original stays detached — leading to state confusion and, in some
cases, duplicate INSERT attempts. Separating the two paths avoids this entirely.

**`Set` import:** The `java.util.Set` import was added for the `Set<String>
answerKeys` collection used in the batch lookup.

**`HashMap` import removed:** A vestigial import from the first attempt that
was never used. Clean compile caught this.

---

### 5. `SecurityConfig.java` — Filter Chain Positioning + Injection Fix

Two independent fixes in this file.

#### 5a. ObjectProvider Injection

**What changed:** The constructor uses `ObjectProvider<DevModeAuthFilter>`
instead of `@Autowired(required = false)`.

```java
// Before:
public SecurityConfig(@Autowired(required = false) DevModeAuthFilter f) {
    this.devModeAuthFilter = f;
}

// After:
public SecurityConfig(ObjectProvider<DevModeAuthFilter> provider) {
    this.devModeAuthFilter = provider.getIfAvailable();
}
```

**Why:** In Spring Framework 7.x (which ships with Spring Boot 4.x), nullable
constructor injection with `@Autowired(required = false)` can have ambiguous
resolution when the bean is absent (e.g., on the `prod` profile where
`DevModeAuthFilter` isn't created). `ObjectProvider.getIfAvailable()` is the
idiomatic way to express "I want this bean if it exists, null otherwise."

This didn't cause the immediate bug (the filter was present on the `dev` profile),
but it's the correct pattern and avoids future production issues.

#### 5b. Filter Chain Position

**What changed:** The `addFilterBefore` reference class.

```java
// Before:
http.addFilterBefore(devModeAuthFilter, UsernamePasswordAuthenticationFilter.class);

// After:
http.addFilterBefore(devModeAuthFilter, AuthorizationFilter.class);
```

**Why:** `UsernamePasswordAuthenticationFilter` is only added to the Spring
Security filter chain when form-login is configured. With `SessionCreationPolicy.STATELESS`
and no form-login, that filter is absent. `addFilterBefore` with a missing
reference class places the filter at an unpredictable position in the chain.

`AuthorizationFilter` is always present in every Spring Security filter chain
(it's the filter that enforces URL-level access rules like `.requestMatchers("/api/admin/**").hasRole("ADMIN")`).
Placing `DevModeAuthFilter` immediately before it guarantees the dev principal is
in the `SecurityContext` before any authorization decision is made.

**Filter chain order (visible in TRACE logs):**
```
 3. SecurityContextHolderFilter      — loads SecurityContext from session (none in stateless)
 8. SecurityContextHolderAwareRequestFilter
 9. AnonymousAuthenticationFilter    — sets anonymous token ← THIS WAS THE BUG
12. DevModeAuthFilter                — our filter
13. AuthorizationFilter              — enforces .hasRole() rules
```

---

### 6. `DevModeAuthFilter.java` — Anonymous Token Override

**What changed:** The guard condition and added import.

```java
// Before:
if (SecurityContextHolder.getContext().getAuthentication() == null) {
    // inject dev principal
}

// After:
import org.springframework.security.authentication.AnonymousAuthenticationToken;

var auth = SecurityContextHolder.getContext().getAuthentication();
if (auth == null || auth instanceof AnonymousAuthenticationToken) {
    // inject dev principal
}
```

**Why (the bug):** With stateless session management, Spring Security adds an
`AnonymousAuthenticationFilter` to the chain. This filter checks if the
`SecurityContext` has no authentication, and if so, sets an
`AnonymousAuthenticationToken` (principal="anonymousUser", role=ROLE_ANONYMOUS).

The old code checked `getAuthentication() == null`. But by the time
`DevModeAuthFilter` runs (position 12), `AnonymousAuthenticationFilter` (position 9)
has already set the anonymous token. The null check passes over it, and
`DevModeAuthFilter` becomes a no-op. The request proceeds to
`AuthorizationFilter` with only `ROLE_ANONYMOUS`, which fails the
`.hasRole("ADMIN")` check — 403 Forbidden.

The fix also handles the null case defensively (for filter chain configurations
where `AnonymousAuthenticationFilter` isn't present).

**Why check `instanceof AnonymousAuthenticationToken` instead of checking the
role:** The anonymous token is a specific Spring Security class. Checking the
class directly is more precise than checking for `ROLE_ANONYMOUS` (which could
theoretically be granted to a real user). It's also more readable — "if there's
no auth, or only the anonymous placeholder, inject the dev principal."

**The `var` keyword:** Java 10+ local variable type inference. The type of
`auth` is inferred as `Authentication` from the return type of
`getAuthentication()`. This is safe because the type is obvious from the
right-hand side.

---

## Patterns to Know for Your Interview

### 1. Batch Fetching (the "IN clause" pattern)

When you have N IDs/keys and need to fetch their corresponding entities, never
loop. Collect the keys, use an `IN` query, and index by key.

```java
Set<String> keys = items.stream().map(Item::key).collect(Collectors.toSet());
Map<String, Entity> byKey = repo.findByKeyIn(keys).stream()
    .collect(Collectors.toMap(Entity::getKey, Function.identity()));
```

### 2. Hibernate Managed Entities

Inside a `@Transactional` method, entities returned from repository queries are
**managed**. Mutating them causes automatic UPDATE at flush time. Only new
entities (created with `new` or `.build()`) need explicit `save()`/`persist()`.

### 3. Filter Ordering in Spring Security

The Security filter chain is ordered. `AnonymousAuthenticationFilter` runs
before custom filters by default. If your custom auth filter needs to override
the anonymous token, check for it explicitly. Use `addFilterBefore` with
`AuthorizationFilter.class` (which is always present) rather than
`UsernamePasswordAuthenticationFilter.class` (which is conditional).

### 4. ObjectProvider for Optional Dependencies

`ObjectProvider<T>` is Spring's recommended way to express optional bean
dependencies in constructors. `getIfAvailable()` returns null if the bean
doesn't exist (e.g., profile-conditional beans).

### 5. Measuring N+1 Impact

To find N+1 queries in your own code:
- Enable SQL logging: `spring.jpa.show-sql=true`
- Enable Hibernate statistics: `spring.jpa.properties.hibernate.generate_statistics=true`
- Look for repeated identical SELECT statements in the logs
- Count: if you see the same query pattern repeated with different parameters,
  you have an N+1 problem

---

## Performance Results

| Metric | Before | After | Improvement |
|---|---|---|---|
| 100 questions | ~90 seconds | ~6.5 seconds | **14x** |
| 200 questions | ~180 seconds (est.) | 12.9 seconds | **14x** |
| DB queries per question | ~450 | ~5 | **90x** |
| Time per question | ~1.4 seconds | ~65ms | **22x** |
