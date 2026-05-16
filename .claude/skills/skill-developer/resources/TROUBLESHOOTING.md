# Troubleshooting - Skill Activation Issues

Complete debugging guide for skill activation problems.

## Table of Contents

- [Skill Not Triggering](#skill-not-triggering)
  - [UserPromptSubmit Not Suggesting](#userpromptsubmit-not-suggesting)
  - [PreToolUse Not Blocking](#pretooluse-not-blocking)
- [False Positives](#false-positives)
- [Hook Not Executing](#hook-not-executing)
- [Performance Issues](#performance-issues)

---

## Skill Not Triggering

### UserPromptSubmit Not Suggesting

**Symptoms:** Ask a question, but no skill suggestion appears in output.

**Common Causes:**

####  1. Keywords Don't Match

**Check:**
- Look at `promptTriggers.keywords` in skill-rules.json
- Are the keywords actually in your prompt?
- Remember: case-insensitive substring matching

**Example:**
```json
"keywords": ["svelte", "component"]
```
- "how does the svelte store work?" → Matches "svelte"
- "create a new component" → Matches "component"
- "how does it work?" → No match

**Fix:** Add more keyword variations to skill-rules.json

#### 2. Intent Patterns Too Specific

**Check:**
- Look at `promptTriggers.intentPatterns`
- Test regex at https://regex101.com/
- May need broader patterns

**Example:**
```json
"intentPatterns": [
  "(create|add).*?(spring.*?service)"  // Too specific
]
```
- "create a spring service" → Matches
- "add a new service" → Doesn't match (missing "spring")

**Fix:** Broaden the pattern:
```json
"intentPatterns": [
  "(create|add).*?(service|controller|repository)"  // Better
]
```

#### 3. Typo in Skill Name

**Check:**
- Skill name in SKILL.md frontmatter
- Skill name in skill-rules.json
- Must match exactly

#### 4. JSON Syntax Error

**Check:**
```bash
cat .claude/skills/skill-rules.json | jq .
```

If invalid JSON, jq will show the error.

**Fix:** Correct JSON syntax, validate with jq

#### Debug Command

Test the hook manually:

```bash
echo '{"session_id":"debug","prompt":"your test prompt here"}' | \
  npx tsx .claude/hooks/skill-activation-prompt.ts
```

Expected: Your skill should appear in the output.

---

### PreToolUse Not Blocking

**Symptoms:** Edit a file that should trigger a guardrail, but no block occurs.

**Common Causes:**

#### 1. File Path Doesn't Match Patterns

**Check:**
- File path being edited
- `fileTriggers.pathPatterns` in skill-rules.json
- Glob pattern syntax

**Example:**
```json
"pathPatterns": [
  "backend/src/main/**/*.java"
]
```
- Editing: `backend/src/main/java/GameService.java` → Matches
- Editing: `backend/src/test/GameServiceTest.java` → Add exclusion!

**Fix:** Adjust glob patterns or add the missing path

#### 2. Excluded by pathExclusions

**Check:**
- Are you editing a test file?
- Look at `fileTriggers.pathExclusions`

**Example:**
```json
"pathExclusions": [
  "**/*Test.java",
  "**/test/**/*.java"
]
```
- Editing: `GameServiceTest.java` → Excluded
- Editing: `GameService.java` → Not excluded

#### 3. Session Already Used Skill

**Check session state:**
```bash
ls .claude/hooks/state/
cat .claude/hooks/state/skills-used-{session-id}.json
```

If the skill is in `skills_used`, it won't block again in this session.

**Fix:** Delete the state file to reset:
```bash
rm .claude/hooks/state/skills-used-{session-id}.json
```

#### 4. Environment Variable Override

**Check:**
```bash
echo $SKIP_DB_VERIFICATION
echo $SKIP_SKILL_GUARDRAILS
```

If set, the skill is disabled.

**Fix:** Unset the environment variable:
```bash
unset SKIP_SKILL_GUARDRAILS
```

---

## False Positives

**Symptoms:** Skill triggers when it shouldn't.

**Common Causes & Solutions:**

### 1. Keywords Too Generic

**Problem:**
```json
"keywords": ["user", "system", "create"]  // Too broad
```

**Solution:** Make keywords more specific
```json
"keywords": [
  "spring boot",
  "sveltekit",
  "game engine"
]
```

### 2. Intent Patterns Too Broad

**Problem:**
```json
"intentPatterns": [
  "(create)"  // Matches everything with "create"
]
```

**Solution:** Add context to patterns
```json
"intentPatterns": [
  "(create|add).*?(controller|service|repository)"  // More specific
]
```

---

## Hook Not Executing

**Symptoms:** Hook doesn't run at all - no suggestion, no block.

**Common Causes:**

### 1. Hook Not Registered

**Check `.claude/settings.json`:**
```bash
cat .claude/settings.json | jq '.hooks.UserPromptSubmit'
```

Expected: Hook entries present

### 2. Bash Wrapper Not Executable

**Check:**
```bash
ls -l .claude/hooks/*.sh
```

Expected: `-rwxr-xr-x` (executable)

**Fix:**
```bash
chmod +x .claude/hooks/*.sh
```

### 3. npx/tsx Not Available

**Check:**
```bash
npx tsx --version
```

Expected: Version number

**Fix:** Install dependencies:
```bash
cd .claude/hooks
npm install
```

### 4. TypeScript Compilation Error

**Check:**
```bash
cd .claude/hooks
npx tsc --noEmit skill-activation-prompt.ts
```

Expected: No output (no errors)

---

## Performance Issues

**Symptoms:** Hooks are slow, noticeable delay before prompt/edit.

**Solutions:**

### 1. Too Many Patterns

**Solution:** Reduce patterns
- Combine similar patterns
- Remove redundant patterns
- Use more specific patterns (faster matching)

### 2. Too Many Files Checked

**Solution:** Be more specific
```json
"pathPatterns": [
  "backend/src/main/java/**/*.java",  // Specific path
  "frontend/src/lib/**/*.ts"          // Not all .ts files
]
```

### Measure Performance

```bash
# UserPromptSubmit
time echo '{"prompt":"test"}' | npx tsx .claude/hooks/skill-activation-prompt.ts
```

**Target metrics:**
- UserPromptSubmit: < 100ms
- PreToolUse: < 200ms

---

**Related Files:**
- [SKILL.md](../SKILL.md) - Main skill guide
- [HOOK_MECHANISMS.md](HOOK_MECHANISMS.md) - How hooks work
- [SKILL_RULES_REFERENCE.md](SKILL_RULES_REFERENCE.md) - Configuration reference
