/**
 * Deduplicates consecutive identical whitespace-separated tokens in a nationality string.
 * e.g. "FRA FRA" → "FRA", "ES ES" → "ES"
 */
export function formatNationality(nationality: string): string {
  if (!nationality) return "";
  const tokens = nationality.trim().split(/\s+/);
  const deduped = tokens.filter((tok, i) => i === 0 || tok !== tokens[i - 1]);
  return deduped.join(" ");
}

/**
 * Flags for the UK home nations, which have no ISO 3166-1 alpha-2 code.
 * England, Scotland and Wales have subdivision flag emojis; Northern
 * Ireland does not, so it falls back to the Union Jack.
 */
const HOME_NATION_FLAGS: Record<string, string> = {
  ENG: "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
  SCO: "🏴󠁧󠁢󠁳󠁣󠁴󠁿",
  WAL: "🏴󠁧󠁢󠁷󠁬󠁳󠁿",
  NIR: "🇬🇧",
};

/**
 * Converts a country code to a flag emoji.
 * Handles formats like "esES" (locale style) or "ES" (ISO 3166-1 alpha-2),
 * plus UK home nation codes (ENG, SCO, WAL, NIR).
 * Returns an empty string when no emoji can be produced, so callers can
 * render the flag alongside the text label without duplication.
 */
export function getFlagEmoji(countryCode: string): string {
  if (!countryCode) return "";

  let code = countryCode;

  // Handle locale-like formats e.g. "esES", "frFR" where the code is repeated
  if (code.length === 4 && /^[a-z]{2}[A-Z]{2}$/.test(code)) {
    code = code.substring(2);
  } else if (code.includes("_") && code.split("_").length === 2) {
    const parts = code.split("_");
    if (parts[1].length === 2) {
      code = parts[1];
    }
  }

  code = code.toUpperCase();

  if (HOME_NATION_FLAGS[code]) {
    return HOME_NATION_FLAGS[code];
  }

  if (!/^[A-Z]{2}$/.test(code)) {
    return "";
  }

  const OFFSET = 127397; // 0x1F1E6 - 'A'.charCodeAt(0)
  try {
    return code
      .split("")
      .map((char) => String.fromCodePoint(char.charCodeAt(0) + OFFSET))
      .join("");
  } catch (e) {
    console.warn("Error creating flag emoji for:", countryCode, e);
    return "";
  }
}
