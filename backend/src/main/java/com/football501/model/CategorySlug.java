package com.football501.model;

/**
 * Standard category slug constants for well-known categories seeded at startup.
 *
 * <p>Slugs are stored in the {@code categories} table and referenced from
 * application defaults (e.g. the practice-game controller's default category).
 * Always use these constants rather than bare string literals to prevent typos
 * and make usages easy to find.
 *
 * @see com.football501.controller.PracticeGameController
 */
public final class CategorySlug {

    /** The default football category slug. */
    public static final String FOOTBALL = "football";

    private CategorySlug() {
        // constants-only utility class — do not instantiate
    }
}
