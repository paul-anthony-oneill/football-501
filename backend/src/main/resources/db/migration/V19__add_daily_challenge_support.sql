-- Guardrail: explicit flag for Daily Challenge pool selection.
-- More reliable than inferring from difficulty alone, since some medium questions
-- may not suit a shared daily format.
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS suitable_for_daily BOOLEAN NOT NULL DEFAULT false;

-- Guardrail: every future game mode needs this column. Adding it later would
-- require a migration and code change everywhere matches are created.
ALTER TABLE matches
    ADD COLUMN IF NOT EXISTS game_mode VARCHAR(50) NOT NULL DEFAULT 'STANDARD';

-- One row per category per calendar day.
-- Each category gets its own daily challenge with an independent starting score.
CREATE TABLE daily_challenges (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_date DATE    NOT NULL,
    category_id    UUID    NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    question_id    UUID    NOT NULL REFERENCES questions(id)  ON DELETE RESTRICT,
    starting_score INTEGER NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at     TIMESTAMP  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP  NOT NULL DEFAULT NOW(),
    UNIQUE (challenge_date, category_id)
);

CREATE INDEX idx_daily_challenges_date ON daily_challenges (challenge_date DESC);
CREATE INDEX idx_daily_challenges_category ON daily_challenges (category_id);
