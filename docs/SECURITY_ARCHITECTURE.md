# Security Architecture

**Status**: Permissive dev-mode — Spring Security wired, real OAuth deferred  
**Implemented**: Phase 1 (commit 738c13d)  
**Last updated**: 2026-05-27

---

## Overview

Spring Security is fully wired and active but operates in a **permissive dev-mode posture** that keeps local development frictionless. The plumbing is in place and the annotations are enforced; what's missing is a real identity provider.

The two key classes live in `backend/src/main/java/com/trivia501/security/`:

| Class | Role |
|---|---|
| `SecurityConfig` | Filter chain, URL-level access rules, `@EnableMethodSecurity` |
| `DevModeAuthFilter` | Injects a fixed principal on every request (non-prod profiles only) |

---

## Current Identity Model

### DevModeAuthFilter (`@Profile("!prod")`)

Active on every Spring profile **except** `prod`. Runs before `UsernamePasswordAuthenticationFilter` and injects a fixed, fully-authenticated principal:

```java
Principal name  : DevModeAuthFilter.DEV_PLAYER_ID
                  = "00000000-0000-0000-0000-000000000001"
Granted roles   : ROLE_USER, ROLE_ADMIN
```

The filter is idempotent — if a `SecurityContext` is already populated (e.g. by a test harness), it is a no-op.

**In controllers**, identity is read from the `Principal` argument, never from a request parameter:

```java
// Correct — derives identity from the security context
@PostMapping("/start")
public ResponseEntity<GameStateResponse> startGame(Principal principal) {
    UUID playerId = UUID.fromString(principal.getName());
    // ...
}

// WRONG — re-opens identity spoofing vulnerability (removed in Phase 1)
// @RequestParam UUID playerId
```

### SecurityConfig

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // activates @PreAuthorize on admin controllers
public class SecurityConfig {
    // ...
}
```

**Session policy**: `STATELESS` — no HTTP session is created. Every request must carry its own credentials (currently: the dev filter's injected token; in production: a JWT cookie or Bearer token).

**CSRF**: Disabled — this is a stateless REST API consumed by a SPA client. When JWT cookies are introduced, re-evaluate (SameSite=Strict cookies + double-submit token give equivalent protection).

---

## URL-Level Access Policy

| Path pattern | Requirement | Enforced by |
|---|---|---|
| `/api/entities/**` | `permitAll` | `SecurityConfig` URL rule |
| `/api/categories/**` | `permitAll` | `SecurityConfig` URL rule |
| `/actuator/health` | `permitAll` | `SecurityConfig` URL rule |
| `/api/practice/**` | `ROLE_USER` or `ROLE_ADMIN` | `SecurityConfig` URL rule |
| `/api/admin/**` | `ROLE_ADMIN` | URL rule **+** `@PreAuthorize` |
| everything else | `authenticated` | `SecurityConfig` catch-all |

Admin endpoints are double-gated: the URL rule blocks non-admin tokens before the request even reaches the method, and `@PreAuthorize("hasRole('ADMIN')")` at the class level provides defence-in-depth.

---

## Admin Controller Annotations

All five admin controllers carry `@PreAuthorize("hasRole('ADMIN')")` at **class level**:

```java
@RestController
@RequestMapping("/api/admin/questions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionController { ... }
```

This is enforced by `@EnableMethodSecurity` in `SecurityConfig`. The annotations were present as placeholders before Phase 1; Phase 1 made them effective by activating the security framework.

---

## CORS Configuration

A `CorsConfig` bean (in `config/`) provides the `CorsConfigurationSource` that `SecurityConfig` delegates to via `.cors(Customizer.withDefaults())`. For local development it allows `http://localhost:3000` (the Next.js dev server).

---

## What Is Deferred

| Feature | Status | Notes |
|---|---|---|
| JWT validation filter | ❌ Not built | Replaces `DevModeAuthFilter` on `prod` profile |
| OAuth 2.0 social login | ❌ Not built | Google login first; Apple + Facebook later |
| Guest accounts | ❌ Not built | Ephemeral UUID, 24-hour inactivity timeout |
| HTTPOnly cookie token storage | ❌ Not built | Needed once real auth exists |
| CSRF protection | ❌ Disabled | Re-enable with SameSite cookies when JWT cookies are introduced |
| Content Security Policy | ❌ Not built | Add in Next.js `middleware.ts` |
| Rate limiting | ❌ Not built | 100 req/min authenticated, 10/min unauthenticated per IP |

---

## Production Migration Path

When real auth is implemented, the steps are:

1. **Write a `JwtAuthFilter`** that validates a JWT (or OAuth 2.0 access token) and populates the `SecurityContext` with the real player's UUID as the principal name.
2. **Activate it on the `prod` profile** by annotating it `@Profile("prod")` and adding it to the filter chain in `SecurityConfig` before `UsernamePasswordAuthenticationFilter`.
3. **`DevModeAuthFilter` becomes inactive automatically** — it is `@Profile("!prod")`, so it is not created when `prod` is active. No other changes needed.
4. **Re-enable CSRF** if JWT HTTPOnly cookies are used. If Bearer tokens in `Authorization` headers are used instead, CSRF is not applicable.
5. **Add Next.js `middleware.ts`** with a strict Content Security Policy.

No controller code needs to change — controllers already read identity from `Principal.getName()`, which the real filter will populate with the real UUID.

---

## Security in Tests

Tests run under the `test` Spring profile. `DevModeAuthFilter` is active on `test` (it is `@Profile("!prod")`), so:

- No `@WithMockUser` annotation is needed — all requests are automatically authenticated as `DEV_PLAYER_ID` with both `ROLE_USER` and `ROLE_ADMIN`.
- Tests that assert on player-scoped behaviour (e.g. "this game belongs to player X") should use `DevModeAuthFilter.DEV_PLAYER_ID` as the expected player ID so the test value stays in sync with what the filter injects.

---

## Related Files

| File | Purpose |
|---|---|
| `backend/.../security/SecurityConfig.java` | Filter chain + URL rules + `@EnableMethodSecurity` |
| `backend/.../security/DevModeAuthFilter.java` | Dev-mode identity injection |
| `backend/.../config/CorsConfig.java` | CORS configuration (delegated to by SecurityConfig) |
| `backend/.../exception/GlobalExceptionHandler.java` | Returns 401/403 in consistent JSON format |
| `docs/AUDIT_SUMMARY.md` | Original audit findings (§ Security, items 1–3) |
