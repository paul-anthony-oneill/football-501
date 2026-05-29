package com.football501.model;

/**
 * String constants for the {@code entity_type} column in the {@code named_entities}
 * table and for the {@code entity_type} key in question {@code config} JSONB.
 *
 * <p>Always use these constants rather than bare string literals. This ensures:
 * <ul>
 *   <li>Consistency between Java code, stored JSON, and database rows.</li>
 *   <li>Compile-time safety — a typo in a constant name fails to compile.</li>
 *   <li>Easy search for all callsites if a type slug ever needs renaming.</li>
 * </ul>
 *
 * <h3>Adding a new entity type</h3>
 * <ol>
 *   <li>Add a new constant here.</li>
 *   <li>Seed the {@code named_entities} table for that type before activating
 *       any question whose {@code config.entity_type} uses it.</li>
 *   <li>See {@code docs/design/AUTOCOMPLETE_ENTITY_DESIGN.md} for the full guide.</li>
 * </ol>
 *
 * @see com.football501.service.EntitySearchService
 */
public final class EntityType {

    /** A football player (default autocomplete pool). */
    public static final String FOOTBALLER = "footballer";

    /** A city or stadium name (for future question types). */
    public static final String CITY = "city";

    /** A country name (for future question types). */
    public static final String COUNTRY = "country";

    /** A football coach or manager (for future question types). */
    public static final String COACH = "coach";

    private EntityType() {
        // constants-only utility class — do not instantiate
    }
}
