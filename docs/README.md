# Football 501 Documentation

Welcome to the Football 501 documentation. This index is the canonical entry point for navigating the project.

**Last Updated**: 2026-05-27  
**Status**: Audit Fixes Complete (Phase 5 of 5) — Moving to MVP Features

---

## 📚 Documentation Index

### 🚀 Start Here

**New to the project?** Read these in order:

1. **[CLAUDE.md](../CLAUDE.md)** — Project overview, architecture principles, tech stack, common pitfalls, and development workflow. The single most important document.
2. **[Project Log](PROJECT_LOG.md)** — Full chronological record of design sessions and implementation phases, including all 5 audit fix phases.
3. **[Current Implementation](CURRENT_IMPLEMENTATION.md)** — Plain-English explanation of what is built and how each layer works right now.

---

## 📋 Planning & Requirements

- **[PRD.md](PRD.md)** — Product Requirements Document. Full product vision, feature specifications, user stories, and success metrics.
- **[GAME_RULES.md](GAME_RULES.md)** — Authoritative game mechanics: 501 scoring, bust rules, checkout range, close-finish rule, turn timers, question types.
- **[AUDIT_SUMMARY.md](AUDIT_SUMMARY.md)** — Comprehensive security, architectural, and code-quality audit (2026-05-26). All findings implemented across Phases 1–5. Retained as a historical record.

---

## 🏗️ Architecture & Design

### System Architecture

- **[design/TECHNICAL_DESIGN.md](design/TECHNICAL_DESIGN.md)** — High-level system design: client-server architecture, module breakdown, database schema, WebSocket protocol.
- **[design/BACKEND_ARCHITECTURE.md](design/BACKEND_ARCHITECTURE.md)** — Detailed backend architecture. Covers all packages (`engine/`, `security/`, `mapper/`, `service/`, `controller/`), JPA auditing pattern, error handling pattern, identity model, and Mermaid diagrams. Reflects audit Phases 1–5.
- **[design/FRONTEND_ARCHITECTURE.md](design/FRONTEND_ARCHITECTURE.md)** — Detailed frontend architecture. Covers page structure, hook responsibilities (`useGameLoop`, `useQuestionDetail`), component tree, Tailwind theming, and admin page decomposition. Reflects audit Phase 3.
- **[design/DATA_MODEL_RELATIONSHIPS.md](design/DATA_MODEL_RELATIONSHIPS.md)** — Entity-relationship overview and table descriptions.

### Security

- **[SECURITY_ARCHITECTURE.md](SECURITY_ARCHITECTURE.md)** — Current security model: `SecurityConfig`, `DevModeAuthFilter`, URL-level access policy, `@PreAuthorize` on admin controllers, CORS. Includes what is deferred (real OAuth, CSRF, CSP) and the production migration path.

### Game & Scoring

- **[design/DIFFICULTY_SCORING.md](design/DIFFICULTY_SCORING.md)** — Continuous difficulty score (0.00–10.00): formula, zone boundaries, saturation thresholds, viability gate, recalibration endpoint. Implemented in Phase 4.
- **[design/GAME_MODES_STRETCH_GOALS.md](design/GAME_MODES_STRETCH_GOALS.md)** — Designed-but-not-built game modes (Daily Challenge, Rapid Fire, Draft, Category Lock). Includes the architectural guardrails to keep these options open.

### Autocomplete & Entity Search

- **[design/AUTOCOMPLETE_ENTITY_DESIGN.md](design/AUTOCOMPLETE_ENTITY_DESIGN.md)** — The `entities` table, `EntityType` constants, upsert strategy, PostgreSQL trigram search, normalisation contract, and guide for adding new entity types.

### Data Source

- **[design/QUESTION_PIPELINE.md](design/QUESTION_PIPELINE.md)** — How questions move from template → draft → active, including materialisation and auto-exclusion.
- **[design/SCRAPERFC_INTEGRATION.md](design/SCRAPERFC_INTEGRATION.md)** — Python microservice architecture, ScraperFC API reference, database schema mapping, ETL flow.
- **[design/SCRAPING_SERVICE_OPERATIONS.md](design/SCRAPING_SERVICE_OPERATIONS.md)** — Operational guide: service workflows, data flow diagrams, API endpoint specs, error handling.

---

## 🛠️ Implementation Status

### ✅ Complete

| Area | Details |
|---|---|
| Spring Security | `SecurityConfig` + `DevModeAuthFilter`; `@PreAuthorize` on admin controllers |
| Identity model | `Principal.getName()` — no `@RequestParam playerId` |
| JPA Auditing | `@EnableJpaAuditing`, `@CreatedDate`/`@LastModifiedDate` on all models |
| Backfill upsert | `INSERT … ON CONFLICT DO NOTHING` — race-safe, restartable |
| Normalisation contract | Integration test: Java `Normalizer` vs PostgreSQL `unaccent()` |
| Game State Machine | `GameStateMachine` — pure transition coordinator in `engine/` |
| Frontend game hook | `useGameLoop` — owns all game state and API calls |
| Admin page decomposition | `questions/[id]/page.tsx` 622 → 113 lines; `useQuestionDetail`, `QuestionMetaPanel`, `AnswerTableSection` |
| Difficulty scoring | `DifficultyConstants`, `DifficultyCalculator`, V13 migration, recalibration endpoint |
| Viability gate | Auto-exclusion at materialisation; `viability_exclusion_reason` populated |
| MapStruct mappers | `AnswerMapper`, `CategoryMapper` — compile-time DTO mapping |
| GlobalExceptionHandler | Central `@RestControllerAdvice`; consistent error JSON across all controllers |
| EntityType constants | `EntityType.FOOTBALLER` etc. — no bare `"footballer"` string literals |
| SvelteKit deleted | `frontend/` removed; `frontend-react/` is the only frontend |

### ⏳ Remaining MVP Work

| Area | Priority | Notes |
|---|---|---|
| Real authentication | 🔴 High | Replace `DevModeAuthFilter` with JWT filter; OAuth 2.0 (Google first) + guest accounts |
| Data population | 🔴 High | Python scraper must run and populate `questions`, `answers`, `entities` |
| WebSocket multiplayer | 🟡 Medium | STOMP broker config; `GameStateMachine` is ready to receive WebSocket events |
| Matchmaking | 🟡 Medium | Queue entry/exit; skill-based pairing; lobby → game handoff |
| Player profiles & ranking UI | 🟡 Medium | MMR, league points, rank badge — backend models exist, no frontend |
| Turn timer | 🟡 Medium | 45s→30s→15s→forfeit; server-side enforcement + client display |
| Daily Challenge | 🟢 Low | Tables exist; needs scheduler, service, frontend page |
| CSP + rate limiting | 🟢 Low | Next.js `middleware.ts` CSP; Spring rate limiting |

---

## 📖 Quick Reference

| Question | Where to look |
|---|---|
| How does the scoring system work? | [GAME_RULES.md](GAME_RULES.md) |
| How does security / auth work? | [SECURITY_ARCHITECTURE.md](SECURITY_ARCHITECTURE.md) |
| How does autocomplete avoid revealing answers? | [AUTOCOMPLETE_ENTITY_DESIGN.md](design/AUTOCOMPLETE_ENTITY_DESIGN.md) |
| How is question difficulty computed? | [DIFFICULTY_SCORING.md](design/DIFFICULTY_SCORING.md) |
| How does game state transition work? | [BACKEND_ARCHITECTURE.md](design/BACKEND_ARCHITECTURE.md) — GameStateMachine section |
| How does the frontend own game state? | [FRONTEND_ARCHITECTURE.md](design/FRONTEND_ARCHITECTURE.md) — useGameLoop section |
| What audit findings were fixed? | [AUDIT_SUMMARY.md](AUDIT_SUMMARY.md) + [PROJECT_LOG.md](PROJECT_LOG.md) |
| How does the data scraper work? | [SCRAPERFC_INTEGRATION.md](design/SCRAPERFC_INTEGRATION.md) |
| What game modes are planned? | [GAME_MODES_STRETCH_GOALS.md](design/GAME_MODES_STRETCH_GOALS.md) |

---

## 🔗 External Resources

- **ScraperFC**: https://scraperfc.readthedocs.io/
- **FBref**: https://fbref.com/
- **Spring Boot**: https://spring.io/projects/spring-boot
- **Next.js**: https://nextjs.org/docs
- **PostgreSQL trigrams**: https://www.postgresql.org/docs/current/pgtrgm.html
- **MapStruct**: https://mapstruct.org/documentation/stable/reference/html/
