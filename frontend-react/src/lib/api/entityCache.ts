import { apiFetch } from "@/lib/api/client";

export interface CachedEntity {
  id: string;
  name: string;
  normalizedName: string;
  nationality: string;
}

// Mirrors EntitySearchService.NFD_OPAQUE_REPLACEMENTS in Java so that
// client-side normalization matches the pre-computed normalizedName column.
const NFD_OPAQUE: Record<string, string> = {
  ø: "o",  Ø: "o",
  æ: "ae", Æ: "ae",
  ł: "l",  Ł: "l",
  đ: "d",  Đ: "d",
  œ: "oe", Œ: "oe",
  ð: "d",  Ð: "d",
  þ: "th", Þ: "th",
  ß: "ss",
};

export function stripAccents(str: string): string {
  const decomposed = str.normalize("NFD").replace(/\p{Diacritic}/gu, "");
  return Array.from(decomposed)
    .map((c) => NFD_OPAQUE[c] ?? c)
    .join("");
}

// Module-level cache: entityType → resolved array, or in-flight Promise
const cache = new Map<string, CachedEntity[] | Promise<CachedEntity[]>>();

/** Returns the cached entity list synchronously if already loaded, otherwise null. */
export function getEntityCacheSync(entityType: string): CachedEntity[] | null {
  const entry = cache.get(entityType);
  return Array.isArray(entry) ? entry : null;
}

/**
 * Loads all entities for the given type from the server (once per session).
 * Subsequent calls return the same resolved array immediately.
 * Concurrent calls during the first load share the same in-flight Promise.
 */
export async function loadEntityCache(entityType: string): Promise<CachedEntity[]> {
  const entry = cache.get(entityType);
  if (Array.isArray(entry)) return entry;
  if (entry instanceof Promise) return entry;

  const promise = apiFetch(`/api/entities/all?type=${encodeURIComponent(entityType)}`)
    .then((res) => (res.ok ? (res.json() as Promise<CachedEntity[]>) : []))
    .then((data) => {
      cache.set(entityType, data);
      return data;
    })
    .catch(() => {
      cache.delete(entityType); // allow retry on next attempt
      return [] as CachedEntity[];
    });

  cache.set(entityType, promise);
  return promise;
}

/**
 * Filters the cached entity list by query, ranking prefix matches first.
 * Mirrors the ORDER BY in NamedEntityRepository.searchByType.
 */
export function searchEntities(
  entities: CachedEntity[],
  query: string,
  limit = 10
): CachedEntity[] {
  const normalized = stripAccents(query.toLowerCase().trim());
  if (!normalized) return [];

  const prefix: CachedEntity[] = [];
  const substr: CachedEntity[] = [];

  for (const entity of entities) {
    if (prefix.length >= limit && substr.length >= limit) break;
    if (entity.normalizedName.startsWith(normalized)) {
      if (prefix.length < limit) prefix.push(entity);
    } else if (entity.normalizedName.includes(normalized)) {
      if (substr.length < limit) substr.push(entity);
    }
  }

  return [...prefix, ...substr].slice(0, limit);
}
