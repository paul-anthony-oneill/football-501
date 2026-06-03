package com.trivia501.model;

/**
 * Standard category slug constants for well-known categories seeded at startup.
 *
 * <p>Slugs are stored in the {@code categories} table and referenced from
 * application defaults (e.g. the solo game controller's default category).
 * Always use these constants rather than bare string literals to prevent typos
 * and make usages easy to find.
 *
 * @see com.trivia501.controller.SoloGameController
 */
public final class CategorySlug {

    /** The default football category slug. */
    public static final String FOOTBALL = "football";

    /** Test mode category slug — synthetic answers for frontend flow verification. */
    public static final String TEST = "test";

    /** The Film category slug — movie box office trivia. */
    public static final String FILM = "film";

    private CategorySlug() {
        // constants-only utility class — do not instantiate
    }
}
