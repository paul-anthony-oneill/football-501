#!/usr/bin/env python3
"""
One-off SQL generator for the Film (Entertainment) category.

Fetches top-revenue movies from the TMDB API and generates a Flyway migration
(V22__seed_film_category.sql) that seeds the Film category with questions,
autocomplete entities, and scored answers.

Usage:
    TMDB_API_KEY=your_key python tmdb_movie_fetcher.py > output/v22_migration.sql

The generated SQL follows the exact pattern of V21__seed_geography_category.sql.
"""

from __future__ import annotations

import json
import os
import re
import sys
import time
import unicodedata
import uuid

import requests
from dotenv import load_dotenv

load_dotenv()

# ── Config ────────────────────────────────────────────────────────────────────

TMDB_API_KEY = os.getenv("TMDB_API_KEY", "")
if not TMDB_API_KEY:
    print("Error: TMDB_API_KEY environment variable is required", file=sys.stderr)
    sys.exit(1)

TMDB_BASE = "https://api.themoviedb.org/3"
MIN_VOTES = 50  # minimum vote_count for a movie to be included
PAGES_TO_FETCH = 20  # ~400 movie IDs (20 per page)
REQUEST_DELAY = 0.15  # seconds between API calls

INVALID_DARTS_SCORES = {163, 166, 169, 172, 173, 175, 176, 178, 179}

# Zone boundaries from DifficultyConstants.java
CHECKOUT_MIN = 1
CHECKOUT_MAX = 19
MID_RANGE_MIN = 20
MID_RANGE_MAX = 99
HIGH_VALUE_MIN = 100
HIGH_VALUE_MAX = 180

# ── UUIDs ─────────────────────────────────────────────────────────────────────

CATEGORY_ID = uuid.uuid4()
QUESTION_IDS = {
    "global": uuid.uuid4(),
    "1990s": uuid.uuid4(),
    "2000s": uuid.uuid4(),
    "2010s": uuid.uuid4(),
    "2020s": uuid.uuid4(),
}


# ── Text normalization ────────────────────────────────────────────────────────

def normalize_name(name: str) -> str:
    """Strip accents and lowercase, matching EntitySearchService.stripAccents()."""
    nfd = unicodedata.normalize("NFD", name)
    stripped = "".join(c for c in nfd if not unicodedata.combining(c))
    replacements = {
        "ø": "o", "Ø": "O",
        "æ": "ae", "Æ": "AE",
        "ł": "l", "Ł": "L",
        "đ": "d", "Đ": "D",
        "œ": "oe", "Œ": "OE",
        "ð": "d", "Ð": "D",
        "þ": "th", "Þ": "TH",
        "ß": "ss",
    }
    for old, new in replacements.items():
        stripped = stripped.replace(old, new)
    return stripped.lower().strip()


def escape_sql(text: str) -> str:
    """Escape single quotes for SQL string literals."""
    return text.replace("'", "''")


def escape_json(text: str) -> str:
    """Escape a string for inclusion in a JSON string literal."""
    return json.dumps(text)[1:-1]  # strip surrounding quotes from json.dumps


# ── TMDB API ──────────────────────────────────────────────────────────────────

def tmdb_get(path: str, params: dict = None) -> dict:
    """Make a GET request to the TMDB API."""
    if params is None:
        params = {}
    params["api_key"] = TMDB_API_KEY
    params["language"] = "en-US"
    url = f"{TMDB_BASE}{path}"
    time.sleep(REQUEST_DELAY)
    resp = requests.get(url, params=params, timeout=30)
    resp.raise_for_status()
    return resp.json()


def fetch_movie_ids() -> list[dict]:
    """Fetch top-revenue movie IDs + basic metadata from TMDB discover endpoint.

    The discover endpoint sorts by revenue but does NOT return the revenue field.
    We collect IDs and basic info here, then fetch revenue via /movie/{id} later.
    """
    movies = []
    seen_ids = set()
    for page in range(1, PAGES_TO_FETCH + 1):
        data = tmdb_get("/discover/movie", {
            "sort_by": "revenue.desc",
            "vote_count.gte": MIN_VOTES,
            "page": page,
        })
        for m in data.get("results", []):
            mid = m["id"]
            if mid not in seen_ids:
                seen_ids.add(mid)
                movies.append({
                    "id": mid,
                    "title": m["title"].strip(),
                    "original_title": m.get("original_title", "").strip(),
                    "release_date": m.get("release_date", ""),
                    "poster_path": m.get("poster_path"),
                    "genre_ids": m.get("genre_ids", []),
                })
        print(f"Page {page}: fetched {len(data.get('results', []))} movie IDs "
              f"(total unique: {len(movies)})", file=sys.stderr)
        if page >= data.get("total_pages", 1):
            break
    return movies


def fetch_movie_details(movie_id: int) -> dict | None:
    """Fetch full movie details including revenue and alternate titles.

    Uses append_to_response to get alternative_titles in the same request.
    """
    try:
        data = tmdb_get(f"/movie/{movie_id}", {
            "append_to_response": "alternative_titles",
        })
    except requests.RequestException as e:
        print(f"  Warning: failed to fetch details for movie {movie_id}: {e}",
              file=sys.stderr)
        return None

    alt_titles = []
    for t in data.get("alternative_titles", {}).get("titles", []):
        if t.get("title") and t["title"].strip():
            alt_titles.append(t["title"].strip())

    return {
        "revenue": data.get("revenue", 0),
        "budget": data.get("budget", 0),
        "genres": [g["name"] for g in data.get("genres", [])],
        "_alternate_titles": alt_titles,
    }


def enrich_movies(movies: list[dict]) -> list[dict]:
    """Fetch revenue + alternate titles for each movie via /movie/{id}.

    Returns only movies with revenue > 0.
    """
    enriched = []
    for i, m in enumerate(movies):
        details = fetch_movie_details(m["id"])
        if details is None:
            continue
        revenue = details["revenue"]
        if revenue <= 0:
            continue
        m["revenue"] = revenue
        m["budget"] = details["budget"]
        m["genres"] = details["genres"]
        m["_alternate_titles"] = details["_alternate_titles"]
        enriched.append(m)
        if (i + 1) % 50 == 0:
            print(f"  Enriched {i + 1}/{len(movies)} movies "
                  f"({len(enriched)} with revenue > 0 so far)", file=sys.stderr)
    return enriched


# ── Score calculation ─────────────────────────────────────────────────────────

def movie_score(revenue: int) -> int:
    """Convert TMDB revenue (USD) to darts score (tens of millions)."""
    return max(1, round(revenue / 10_000_000))


def is_valid_darts(score: int) -> bool:
    """Check if score is a valid darts checkout (1-180, not in invalid set)."""
    return 1 <= score <= 180 and score not in INVALID_DARTS_SCORES


def is_bust(score: int) -> bool:
    """Check if score is a bust (>180)."""
    return score > 180


def decade_from_date(date_str: str) -> str | None:
    """Extract decade label from a release date string (YYYY-MM-DD)."""
    if not date_str:
        return None
    match = re.match(r"(\d{4})", date_str)
    if not match:
        return None
    year = int(match.group(1))
    if year < 1990:
        return None
    elif year < 2000:
        return "1990s"
    elif year < 2010:
        return "2000s"
    elif year < 2020:
        return "2010s"
    else:
        return "2020s"


def zone_counts(answers: list[dict]) -> tuple[int, int, int, int, int]:
    """Calculate zone counts from a list of answer dicts.

    Returns (high, mid, checkout, total_valid, total_score_pool).
    Only counts valid (non-bust, valid-darts) answers.
    """
    high = mid = checkout = total_pool = 0
    for a in answers:
        if not a["is_bust"] and a["is_valid_darts"]:
            score = a["score"]
            total_pool += score
            if CHECKOUT_MIN <= score <= CHECKOUT_MAX:
                checkout += 1
            elif MID_RANGE_MIN <= score <= MID_RANGE_MAX:
                mid += 1
            elif HIGH_VALUE_MIN <= score <= HIGH_VALUE_MAX:
                high += 1
    total_valid = high + mid + checkout
    return high, mid, checkout, total_valid, total_pool


# ── SQL Generation ────────────────────────────────────────────────────────────

def sql_header() -> str:
    return """-- V22: Seed Film (Entertainment) category with movie box office questions
-- Generated by tmdb_movie_fetcher.py from TMDB API data
"""


def sql_category() -> str:
    return f"""-- Film category
INSERT INTO categories (id, name, slug, description, created_at, updated_at) VALUES (
  '{CATEGORY_ID}', 'Film', 'film',
  'Movie box office trivia — name a movie and score its worldwide revenue per $10M',
  NOW(), NOW()
) ON CONFLICT (slug) DO NOTHING;
"""


def sql_questions() -> str:
    """Generate INSERT statements for all 5 questions."""
    questions = [
        ("global", "Name a movie — its worldwide box office per $10M is your score",
         '{"entity_type": "film"}'),
        ("1990s", "Name a movie from the 1990s — its worldwide box office per $10M is your score",
         '{"entity_type": "film", "decade": "1990s"}'),
        ("2000s", "Name a movie from the 2000s — its worldwide box office per $10M is your score",
         '{"entity_type": "film", "decade": "2000s"}'),
        ("2010s", "Name a movie from the 2010s — its worldwide box office per $10M is your score",
         '{"entity_type": "film", "decade": "2010s"}'),
        ("2020s", "Name a movie from the 2020s — its worldwide box office per $10M is your score",
         '{"entity_type": "film", "decade": "2020s"}'),
    ]
    lines = ["-- Film questions (1 global + 4 decade-filtered)"]
    for decade, text, config in questions:
        qid = QUESTION_IDS[decade]
        text_esc = escape_sql(text)
        lines.append(f"""INSERT INTO questions (id, category_id, question_text, metric_key, config, min_score, difficulty, status, template_id, template_params, high_value_count, mid_range_count, checkout_count, total_valid_count, total_score_pool, single_question_viable, difficulty_score, difficulty_locked, suitable_for_daily, created_at, updated_at) VALUES (
  '{qid}', '{CATEGORY_ID}',
  '{text_esc}',
  'box_office_millions',
  '{escape_sql(config)}'::jsonb,
  1, 2, 'draft', NULL, '{{}}'::jsonb,
  0, 0, 0, 0, 0, true, 0.00, false, false,
  NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;""")
    return "\n".join(lines)


BATCH_SIZE = 100  # rows per multi-row INSERT


def sql_entities(movies: list[dict]) -> str:
    """Generate batched multi-row INSERTs for all movie title entities."""
    seen = set()
    entities = []

    for m in movies:
        title = m["title"].strip()
        norm = normalize_name(title)
        key = ("film", norm)
        if key not in seen:
            seen.add(key)
            entities.append((title, norm))

        orig = m.get("original_title", "").strip()
        if orig and orig.lower() != title.lower():
            norm_orig = normalize_name(orig)
            key_orig = ("film", norm_orig)
            if key_orig not in seen:
                seen.add(key_orig)
                entities.append((orig, norm_orig))

        for alt in m.get("_alternate_titles", []):
            alt = alt.strip()
            if not alt or alt.lower() == title.lower():
                continue
            norm_alt = normalize_name(alt)
            key_alt = ("film", norm_alt)
            if key_alt not in seen:
                seen.add(key_alt)
                entities.append((alt, norm_alt))

    lines = ["-- Entities (movie titles for autocomplete) — batched multi-row INSERTs"]

    for i in range(0, len(entities), BATCH_SIZE):
        batch = entities[i:i + BATCH_SIZE]
        lines.append("INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at) VALUES")
        rows = []
        for display_name, normalized_name in batch:
            d_esc = escape_sql(display_name)
            n_esc = escape_sql(normalized_name)
            rows.append(f"  (gen_random_uuid(), 'film', '{d_esc}', '{n_esc}', NOW())")
        lines.append(",\n".join(rows))
        lines.append("ON CONFLICT (entity_type, normalized_name) DO NOTHING;")

    lines.append(f"-- Total entities: {len(entities)} ({len(range(0, len(entities), BATCH_SIZE))} batches)")
    return "\n".join(lines)


def sql_answers(movies: list[dict]) -> str:
    """Generate INSERT statements for answers, grouped by question.

    Returns (sql_string, answer_counts_by_question).
    """
    # Group movies by decade
    by_decade: dict[str, list[dict]] = {
        "global": [],
        "1990s": [],
        "2000s": [],
        "2010s": [],
        "2020s": [],
    }

    for m in movies:
        score = movie_score(m["revenue"])
        valid = is_valid_darts(score)
        bust = is_bust(score)
        decade = decade_from_date(m.get("release_date", ""))
        year_str = m.get("release_date", "")[:4] if m.get("release_date") else "?"

        title = m["title"].strip()
        norm_title = normalize_name(title)

        # Use (year) suffix for ambiguous titles in global question
        display_text = title

        metadata = {
            "release_year": year_str,
            "tmdb_id": m["id"],
        }
        if m.get("poster_path"):
            metadata["poster_path"] = m["poster_path"]

        answer = {
            "title": title,
            "display_text": display_text,
            "answer_key": norm_title,
            "score": score,
            "is_valid_darts": valid,
            "is_bust": bust,
            "metadata": metadata,
            "decade": decade,
        }

        # Global question gets all movies
        by_decade["global"].append(answer)

        # Decade question gets only that decade's movies
        if decade:
            by_decade[decade].append(answer)

    lines = []

    for decade in ["global", "1990s", "2000s", "2010s", "2020s"]:
        answers = by_decade[decade]
        qid = QUESTION_IDS[decade]

        # Sort by score descending and de-duplicate by answer_key
        answers.sort(key=lambda a: a["score"], reverse=True)
        seen_keys = set()
        unique_answers = []
        for a in answers:
            if a["answer_key"] not in seen_keys:
                seen_keys.add(a["answer_key"])
                unique_answers.append(a)

        lines.append(f"\n-- Answers for question: {decade} ({len(unique_answers)} unique)")

        for i in range(0, len(unique_answers), BATCH_SIZE):
            batch = unique_answers[i:i + BATCH_SIZE]
            lines.append("INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES")
            rows = []
            for a in batch:
                d_esc = escape_sql(a["display_text"])
                k_esc = escape_sql(a["answer_key"])
                meta_json = escape_sql(json.dumps(a["metadata"], separators=(",", ":")))
                valid_str = "true" if a["is_valid_darts"] else "false"
                bust_str = "true" if a["is_bust"] else "false"
                rows.append(f"  (gen_random_uuid(), '{qid}', '{k_esc}', '{d_esc}', {a['score']}, {valid_str}, {bust_str}, '{meta_json}'::jsonb, NOW())")
            lines.append(",\n".join(rows))
            lines.append("ON CONFLICT (question_id, answer_key) DO NOTHING;")

        lines.append(f"-- {len(unique_answers)} answers for question {qid}")

    return "\n".join(lines)


def sql_viability_updates(movies: list[dict]) -> str:
    """Generate UPDATE statements to set zone counts and activate questions."""
    # Build the same answer grouping as sql_answers
    by_decade = {"global": [], "1990s": [], "2000s": [], "2010s": [], "2020s": []}

    for m in movies:
        score = movie_score(m["revenue"])
        valid = is_valid_darts(score)
        bust = is_bust(score)
        decade = decade_from_date(m.get("release_date", ""))

        answer = {"score": score, "is_valid_darts": valid, "is_bust": bust, "decade": decade}
        by_decade["global"].append(answer)
        if decade:
            by_decade[decade].append(answer)

    decade_labels = {
        "global": "All movies",
        "1990s": "1990s movies (1990–1999)",
        "2000s": "2000s movies (2000–2009)",
        "2010s": "2010s movies (2010–2019)",
        "2020s": "2020s movies (2020+)",
    }

    lines = []
    for decade in ["global", "1990s", "2000s", "2010s", "2020s"]:
        answers = by_decade[decade]
        high, mid, checkout, total_valid, total_pool = zone_counts(answers)
        viable = total_pool >= 501
        qid = QUESTION_IDS[decade]
        label = decade_labels[decade]

        lines.append(f"\n-- Question: {label}")
        lines.append(f"-- Valid answers: {total_valid}, Score pool: {total_pool}, Viable: {viable}")
        lines.append(f"-- Zones: high={high}, mid={mid}, checkout={checkout}")
        reason = 'NULL' if viable else "'score_pool_insufficient'"
        lines.append(f"""UPDATE questions SET
  high_value_count = {high},
  mid_range_count = {mid},
  checkout_count = {checkout},
  total_valid_count = {total_valid},
  total_score_pool = {total_pool},
  single_question_viable = {'true' if viable else 'false'},
  viability_exclusion_reason = {reason},
  status = 'active',
  difficulty_score = 0.00,
  updated_at = NOW()
WHERE id = '{qid}';""")

    return "\n".join(lines)


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    print("Phase 1: Fetching movie IDs from TMDB discover...", file=sys.stderr)
    movie_ids = fetch_movie_ids()
    print(f"Movie IDs fetched: {len(movie_ids)}", file=sys.stderr)

    print("Phase 2: Fetching revenue + alternate titles for each movie...", file=sys.stderr)
    movies = enrich_movies(movie_ids)
    print(f"Movies with revenue > 0: {len(movies)}", file=sys.stderr)

    # Stats
    scores = [movie_score(m["revenue"]) for m in movies]
    valid = [s for s in scores if is_valid_darts(s)]
    bust = [s for s in scores if is_bust(s)]
    print(f"Scores: {len(valid)} valid, {len(bust)} bust "
          f"(range {min(scores)}–{max(scores)})", file=sys.stderr)

    decades = {}
    for m in movies:
        d = decade_from_date(m.get("release_date", ""))
        if d:
            decades[d] = decades.get(d, 0) + 1
    print(f"By decade: {decades}", file=sys.stderr)

    # Generate SQL
    print("Generating SQL...", file=sys.stderr)
    sql = []
    sql.append(sql_header())
    sql.append(sql_category())
    sql.append(sql_questions())
    sql.append(sql_entities(movies))
    sql.append(sql_viability_updates(movies))
    sql.append(sql_answers(movies))
    sql.append("")  # trailing newline

    full_sql = "\n".join(sql)
    print(full_sql)


if __name__ == "__main__":
    main()
