# UX Design Audit Report

**Date:** 2026-06-03
**Scope:** Full frontend application — lobby, gameplay, daily challenges, admin section
**Source:** 30+ files across `src/app/`, `src/components/`, `src/context/`, `src/hooks/`
**Interface type:** Game / Form hybrid — trivia game with admin management
**Technology:** Next.js 16 + React 19 + Tailwind CSS 4

---

## Summary

| Severity | Count |
|----------|-------|
| 4 - Catastrophe | 0 |
| 3 - Major | 4 |
| 2 - Minor | 12 |
| 1 - Cosmetic | 4 |
| **Total findings** | **20** |

---

## Findings (ordered by severity, then ease of fix)

---

### Finding 1 — No visible focus indicators anywhere

- **Severity:** 3 (Major)
- **Ease:** Trivial (single CSS rule)
- **Principle:** H13 — Accessibility, H11 — Affordances and Signifiers
- **Location:** `globals.css` (global)
- **Issue:** No `:focus-visible` rule exists globally. The form components (`TextField`, `Select`, `TextArea`) have `focus:` Tailwind classes, but buttons, links, the EntitySearch input, and category cards have no focus ring. Keyboard users navigating with Tab get zero visual feedback.
- **User impact:** Keyboard-only users cannot tell which element is focused. They may activate the wrong element or miss interactive elements entirely. This makes the game unplayable via keyboard alone.
- **Fix:** Add a global `:focus-visible` rule to `globals.css`:
  ```css
  :focus-visible {
    outline: 2px solid var(--tele-accent);
    outline-offset: 2px;
  }
  ```

### Finding 2 — No confirmation when exiting an active game

- **Severity:** 3 (Major)
- **Ease:** Straightforward (wire existing ConfirmDialog)
- **Principle:** H3 — User Control and Freedom, H5 — Error Prevention
- **Location:** `MatchView.tsx:94`
- **Issue:** The "ESC_EXIT" button calls `onExit()` immediately with no confirmation. An accidental click or tap loses all game progress. Daily challenges are one-attempt-only — this is a data-loss-level event.
- **User impact:** A player accidentally clicks ESC_EXIT mid-game and loses their entire session, including one-attempt daily challenges.
- **Fix:** Wrap `onExit` in a confirmation step. `ConfirmDialog` already exists — add local confirm state to MatchView and show the dialog before calling `onExit()`.

### Finding 3 — No game rules explanation in the UI

- **Severity:** 3 (Major)
- **Ease:** Moderate (new UI component)
- **Principle:** H10 — Help and Documentation, H6 — Recognition Over Recall
- **Location:** Missing from `MatchView.tsx` and `LobbyView.tsx`
- **Issue:** Darts 501 scoring rules (valid finishes, bust scores, checkout range, hint meanings) are never explained. "180s Left" and "Checkouts" hints appear without context.
- **User impact:** First-time players don't understand why certain scores bust, what a checkout is, or how to win. The game's strategic depth is hidden behind unexplained terminology.
- **Fix:** Add a collapsible "How to Play" panel accessible from both lobby and game screens. Explain: scoring system, bust rules, checkout range, and hint meanings.

### Finding 4 — Three incompatible visual design systems

- **Severity:** 3 (Major)
- **Ease:** Moderate (consolidation across files)
- **Principle:** H4 — Consistency and Standards, H8 — Aesthetic and Minimalist Design
- **Location:** Across the entire application
- **Issue:** The app uses three styling approaches: (1) admin uses `var(--color-*)` custom properties, (2) game uses `h-*` utility classes + raw arbitrary values, (3) daily pages mix both. The `@theme` tokens are defined but unused.
- **User impact:** The app feels like separate products stitched together. Moving from lobby to game to admin triggers a complete visual language shift.
- **Fix:** Define missing CSS custom properties that admin components reference. Replace raw hex values in game components with theme tokens where appropriate. Consolidate daily challenge pages to use one consistent system.

### Finding 5 — `alert()` used for error feedback

- **Severity:** 2 (Minor)
- **Ease:** Trivial (swap to existing toast system)
- **Principle:** H2 — Match Between System and Real World, H9 — Error Recovery
- **Location:** `app/daily/page.tsx:65`, `app/daily/[category]/page.tsx:65`
- **Issue:** Daily challenge start failures are shown via native `alert()`. This blocks the UI, looks like a system error, and provides no recovery path.
- **User impact:** Users see a jarring browser-native popup with no action beyond "OK."
- **Fix:** Replace `alert(msg)` with `addToast(msg, "error")` using the existing Toast system.

### Finding 6 — EntitySearch input has no visible label

- **Severity:** 2 (Minor)
- **Ease:** Trivial (add attribute)
- **Principle:** H13 — Accessibility, H6 — Recognition Over Recall
- **Location:** `EntitySearch.tsx:149-160`
- **Issue:** The input relies entirely on `placeholder` text for identification. Placeholder disappears on input, has poor contrast, and is not a label substitute.
- **User impact:** Screen reader users cannot identify the field. Users who start typing forget what field they're in.
- **Fix:** Add `aria-label="Search player name"` to the input element.

### Finding 7 — Toast error messages auto-dismiss

- **Severity:** 2 (Minor)
- **Ease:** Trivial (change timeout value)
- **Principle:** H9 — Error Recovery, H1 — Visibility of System Status
- **Location:** `ToastContext.tsx:37`
- **Issue:** All toasts auto-dismiss after 3 seconds. Error messages need more reading time and should not disappear while the user is still processing them.
- **User impact:** Players may miss error feedback entirely if they glance away for 3 seconds.
- **Fix:** Extend error toast timeout to 8 seconds, or make errors persist until manually dismissed. Success/info toasts can keep the 3-second timeout.

### Finding 8 — Match history uses color as the only validity indicator

- **Severity:** 2 (Minor)
- **Ease:** Straightforward (add text labels)
- **Principle:** H13 — Accessibility, H14 — Perceptibility
- **Location:** `MatchView.tsx:228-240`
- **Issue:** Move results in history are distinguished only by text color: green (valid), red (bust), gray (invalid). Color-blind users cannot distinguish these states.
- **User impact:** Color-blind users can't tell which of their answers were valid vs. bust in the history sidebar.
- **Fix:** Add a small text label or icon alongside the colored score value (e.g., "BUST" badge, "✓" for valid).

### Finding 9 — No `prefers-reduced-motion` support

- **Severity:** 2 (Minor)
- **Ease:** Straightforward (media query + JS check)
- **Principle:** H7 — Flexibility and Efficiency, H14 — Perceptibility
- **Location:** `globals.css:94-122`, `AnimatedScorePopup.tsx`
- **Issue:** Multiple animations (score flash, dart trajectory, popup transitions, counting animation) have no reduced-motion fallback. The counting animation runs 2-4 seconds with a sinusoidal wobble.
- **User impact:** Users with vestibular disorders can experience dizziness or nausea from the animations.
- **Fix:** Add `@media (prefers-reduced-motion: reduce)` to disable CSS animations. In `AnimatedScorePopup.tsx`, check the media query and skip the counting phase.

### Finding 10 — Undefined CSS custom properties

- **Severity:** 2 (Minor)
- **Ease:** Straightforward (define missing variables)
- **Principle:** H4 — Consistency and Standards, H8 — Aesthetic and Minimalist Design
- **Location:** `FormModal.tsx:74`, `ToastContext.tsx:45-55`, `Select.tsx:41`, `AdminLayout.tsx:16`, `Sidebar.tsx:18`
- **Issue:** Components reference `var(--color-primary)`, `var(--color-error)`, `var(--color-surface)`, `var(--color-outline)`, `var(--color-primary-container)`, `var(--color-on-surface)`, `var(--color-on-surface-variant)`, `var(--shadow-2)`, `var(--shadow-3)` — none of which are defined in `globals.css`. Undefined custom properties resolve to `initial`, causing invisible backgrounds and missing borders.
- **User impact:** Admin UI elements appear with transparent backgrounds or missing borders, looking broken in subtle ways.
- **Fix:** Define these missing properties in `globals.css` under `:root` or a dedicated admin theme class.

### Finding 11 — Design tokens defined but not applied

- **Severity:** 2 (Minor)
- **Ease:** Moderate (pervasive changes)
- **Principle:** H4 — Consistency and Standards, H8 — Aesthetic and Minimalist Design
- **Location:** `globals.css:3-29` (definitions), all components
- **Issue:** Spacing scale (`--spacing-xs` through `--spacing-xxl`), radius tokens (`--radius-xs` through `--radius-md`), and type scale utilities exist but are unused. Components use hardcoded arbitrary values (`py-4.5`, `px-5.5`, `gap-7.5`).
- **User impact:** Spacing is inconsistent across pages. Padding and margins vary randomly, creating subtle visual disorder.
- **Fix:** Apply existing tokens in components via Tailwind arbitrary property syntax (e.g., `p-[var(--spacing-md)]`). Replace one-off spacing values with token references.

### Finding 12 — ConfirmDialog and FormModal don't trap focus

- **Severity:** 2 (Minor)
- **Ease:** Moderate (focus trap logic)
- **Principle:** H13 — Accessibility, H3 — User Control and Freedom
- **Location:** `ConfirmDialog.tsx`, `FormModal.tsx`
- **Issue:** No focus trap — Tab moves focus to elements behind the modal. No Escape key handler to close. Only overlay click-to-dismiss works.
- **User impact:** Keyboard users can tab out of modals and interact with page elements behind them, potentially causing data issues.
- **Fix:** Add Escape key handler. Implement basic focus trap: move focus to first focusable element on open, wrap Tab/Shift+Tab within the modal.

### Finding 13 — Admin sidebar has no responsive behavior

- **Severity:** 2 (Minor)
- **Ease:** Moderate (new collapse pattern)
- **Principle:** H12 — Structure, H13 — Accessibility
- **Location:** `AdminLayout.tsx:16`, `Sidebar.tsx:18`
- **Issue:** Sidebar is fixed at 250px with no collapse mechanism. On tablets, the sidebar consumes most horizontal space.
- **User impact:** Admin users on iPads or small laptops have barely any content space.
- **Fix:** Add a collapsible toggle at a breakpoint (e.g., below 1024px). Use a slide-out drawer pattern for mobile.

### Finding 14 — Hardcoded fake status data

- **Severity:** 2 (Minor)
- **Ease:** Straightforward (remove or wire up)
- **Principle:** H1 — Visibility of System Status
- **Location:** `MatchView.tsx:88-89`, `LobbyView.tsx:253`
- **Issue:** Teletext header shows hardcoded "20:45" and lobby footer shows "LIVE MATCHES: 1,242." Neither is real data.
- **User impact:** Users notice the data never changes. Fake status trains users to ignore real status indicators.
- **Fix:** Remove hardcoded fake data. Replace the time with the current time via `new Date()`. Replace the live matches count with real data or remove it.

### Finding 15 — No keyboard path to throw a dart

- **Severity:** 2 (Minor)
- **Ease:** Straightforward (add key handler)
- **Principle:** H7 — Flexibility and Efficiency, H13 — Accessibility
- **Location:** `MatchView.tsx:206-212`
- **Issue:** After selecting a player via keyboard (arrow keys + Enter in EntitySearch), the user must click "THROW DART" with a mouse. Enter when staged does nothing.
- **User impact:** Keyboard users complete the search flow successfully but hit a dead end — they must switch to mouse to submit.
- **Fix:** Add a `onKeyDown` handler at the MatchView level: pressing Enter when `staged` is set calls `handleThrowDart()`.

### Finding 16 — Daily challenge cards scroll area has no affordance

- **Severity:** 2 (Minor)
- **Ease:** Straightforward (add visual indicator)
- **Principle:** H11 — Affordances and Signifiers, H12 — Structure
- **Location:** `LobbyView.tsx:117`
- **Issue:** Horizontal scroll container for daily cards uses `overflow-x-auto` with no scrollbar styling, no fade indicators, and no hint that more content exists.
- **User impact:** Users may not realize there are more daily challenges beyond the visible 2-3 cards.
- **Fix:** Add a gradient fade at the right edge when content overflows. Style scrollbar to be visible. Add a subtle "scroll for more" indicator.

### Finding 17 — "LIVE MATCHES" looks interactive but isn't

- **Severity:** 2 (Minor)
- **Ease:** Trivial (change styling)
- **Principle:** H11 — Affordances and Signifiers
- **Location:** `LobbyView.tsx:252-254`
- **Issue:** The footer "LIVE MATCHES: 1,242" uses bold styling and resembles interactive elements. It's a false affordance.
- **User impact:** Users may click/tap expecting live match data. When nothing happens, the interface feels broken.
- **Fix:** Remove the bold `<b>` tag on the number. Use the same muted style as the surrounding footer text.

### Finding 18 — Game status line doesn't reflect actual state

- **Severity:** 1 (Cosmetic)
- **Ease:** Trivial (conditional text)
- **Principle:** H1 — Visibility of System Status
- **Location:** `MatchView.tsx:88`
- **Issue:** Teletext header always shows "P302 GAME IN PROGRESS" regardless of game completion.
- **User impact:** Cosmetic — the win overlay covers the header anyway, but the inconsistency is noticeable.
- **Fix:** Show "GAME COMPLETE" when `isWin` is true.

### Finding 19 — Silent game restore failure

- **Severity:** 1 (Cosmetic)
- **Ease:** Trivial (add toast)
- **Principle:** H1 — Visibility of System Status, H9 — Error Recovery
- **Location:** `useGameLoop.ts:209-212`
- **Issue:** When game restore fails, the user is silently dropped back to the lobby with no explanation.
- **User impact:** Users who refreshed expecting to resume are confused when they're suddenly at the lobby.
- **Fix:** Show a toast: "Your previous game session expired."

### Finding 20 — Emoji read literally by screen readers

- **Severity:** 1 (Cosmetic)
- **Ease:** Trivial (add aria-hidden)
- **Principle:** H13 — Accessibility
- **Location:** `ToastContext.tsx:41`, `Sidebar.tsx:7-10`
- **Issue:** Emoji in UI text (✅, ❌, ℹ️, 📁, ❓) are read aloud by screen readers as Unicode descriptions, creating noisy announcements.
- **User impact:** Screen reader users hear "check mark button" before every success toast message.
- **Fix:** Wrap emoji spans in `<span aria-hidden="true">`.

---

## Strengths

1. **Strong game feel** (H1, H14): Score flash animation, counting popup, BUST/CHECKOUT states create dramatic, satisfying feedback. The teletext aesthetic is distinctive and memorable.

2. **Excellent keyboard navigation in EntitySearch** (H7, H13): Arrow keys navigate, Enter selects, Escape dismisses — well-implemented keyboard interaction for autocomplete.

3. **Solid error prevention on game actions** (H5): Throw Dart button disabled when no player staged, during animations, and during API calls. Double-submission is prevented. EntitySearch enforces 4-character minimum.

4. **Thoughtful popup design** (H3, H12): CategoryPopup has overlay dismiss, Escape handler, close button, breadcrumbs, directional slide transitions, and back button. Multiple escape routes for every state.

5. **Game state persistence** (H3, H15): Game state survives browser refreshes via sessionStorage. Restore flow checks with server and gracefully handles stale state.

6. **Clear typographic personality** (H8): Font choices (Bricolage Grotesque for display, VT323 for teletext, IBM Plex Mono for metadata) create a distinctive editorial/game aesthetic.
