import { describe, it, expect } from "vitest";
import { formatNationality, getFlagEmoji } from "@/utils/country";

describe("formatNationality", () => {
  it("returns empty string for empty input", () => {
    expect(formatNationality("")).toBe("");
  });

  it("returns empty string for whitespace-only input", () => {
    expect(formatNationality("   ")).toBe("");
  });

  it("returns single token unchanged", () => {
    expect(formatNationality("FRA")).toBe("FRA");
  });

  it("deduplicates consecutive identical tokens", () => {
    expect(formatNationality("FRA FRA")).toBe("FRA");
  });

  it("deduplicates multiple consecutive duplicates", () => {
    expect(formatNationality("ES ES ES")).toBe("ES");
  });

  it("perserves non-consecutive tokens", () => {
    expect(formatNationality("FRA ES")).toBe("FRA ES");
  });

  it("deduplicates while preserving different tokens", () => {
    expect(formatNationality("FRA FRA ES ES")).toBe("FRA ES");
  });

  it("handles mixed duplicates", () => {
    expect(formatNationality("FRA ES FRA")).toBe("FRA ES FRA");
  });
});

describe("getFlagEmoji", () => {
  it("returns empty string for empty input", () => {
    expect(getFlagEmoji("")).toBe("");
  });

  it("converts ISO code to flag", () => {
    expect(getFlagEmoji("ES")).toBe("🇪🇸");
  });

  it("converts GB to flag", () => {
    expect(getFlagEmoji("GB")).toBe("🇬🇧");
  });

  it("handles lowercase input", () => {
    expect(getFlagEmoji("es")).toBe("🇪🇸");
  });

  it("handles locale-style 4-char codes (esES)", () => {
    expect(getFlagEmoji("esES")).toBe("🇪🇸");
  });

  it("handles locale-style 4-char codes (frFR)", () => {
    expect(getFlagEmoji("frFR")).toBe("🇫🇷");
  });

  it("handles underscore-separated locale codes", () => {
    expect(getFlagEmoji("en_GB")).toBe("🇬🇧");
  });

  it("returns empty string for non-country strings", () => {
    expect(getFlagEmoji("hello")).toBe("");
  });

  it("returns England subdivision flag for ENG", () => {
    expect(getFlagEmoji("ENG")).toBe("🏴󠁧󠁢󠁥󠁮󠁧󠁿");
  });

  it("returns Scotland subdivision flag for SCO", () => {
    expect(getFlagEmoji("SCO")).toBe("🏴󠁧󠁢󠁳󠁣󠁴󠁿");
  });

  it("returns Wales subdivision flag for WAL", () => {
    expect(getFlagEmoji("WAL")).toBe("🏴󠁧󠁢󠁷󠁬󠁳󠁿");
  });

  it("returns Union Jack for NIR (no subdivision emoji exists)", () => {
    expect(getFlagEmoji("NIR")).toBe("🇬🇧");
  });

  it("handles lowercase home nation codes", () => {
    expect(getFlagEmoji("eng")).toBe("🏴󠁧󠁢󠁥󠁮󠁧󠁿");
  });

  it("converts US to flag", () => {
    expect(getFlagEmoji("US")).toBe("🇺🇸");
  });

  it("converts JP to flag", () => {
    expect(getFlagEmoji("JP")).toBe("🇯🇵");
  });
});
