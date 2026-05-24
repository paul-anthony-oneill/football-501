/**
 * Converts a country code to a flag emoji.
 * Handles formats like "esES" (locale style) or "ES" (ISO 3166-1 alpha-2).
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

  if (!/^[A-Z]{2}$/.test(code)) {
    return countryCode;
  }

  const OFFSET = 127397; // 0x1F1E6 - 'A'.charCodeAt(0)
  try {
    return code
      .split("")
      .map((char) => String.fromCodePoint(char.charCodeAt(0) + OFFSET))
      .join("");
  } catch (e) {
    console.warn("Error creating flag emoji for:", countryCode, e);
    return countryCode;
  }
}
