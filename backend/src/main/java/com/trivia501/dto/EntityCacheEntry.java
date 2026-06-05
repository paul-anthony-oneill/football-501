package com.trivia501.dto;

import java.util.UUID;

/**
 * Slim projection returned by GET /api/entities/all for client-side caching.
 * Includes the UUID so the frontend can pass it back on answer submission,
 * allowing the backend to do an exact entity lookup instead of fuzzy matching.
 */
public record EntityCacheEntry(UUID id, String name, String normalizedName, String nationality) {}
