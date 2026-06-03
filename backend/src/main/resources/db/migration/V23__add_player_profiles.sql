-- Player profiles for authenticated Supabase users.
-- Created when a player first accesses a protected endpoint with a real JWT.
-- Anonymous players (cookie-based session UUIDs) are NOT tracked here.
-- player_id = Supabase auth.uid from the JWT "sub" claim.
CREATE TABLE player_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id       UUID NOT NULL UNIQUE,
    display_name    VARCHAR(100),
    avatar_url      VARCHAR(500),
    games_played    INTEGER NOT NULL DEFAULT 0,
    games_won       INTEGER NOT NULL DEFAULT 0,
    total_score     INTEGER NOT NULL DEFAULT 0,
    best_score      INTEGER,
    last_active_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_player_profiles_player_id ON player_profiles (player_id);
