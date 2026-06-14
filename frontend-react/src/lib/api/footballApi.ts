import { apiFetch } from "./client";

export interface FootballClub {
  id: string;
  name: string;
}

export interface FootballFilter {
  scope: "random_any" | "random_league_level" | "random_club_level" | "league" | "club";
  league?: string;
  club?: string;
  statType?: string;
}

export async function fetchClubs(leagueSlug: string): Promise<FootballClub[]> {
  const res = await apiFetch(`/api/football/clubs?league=${encodeURIComponent(leagueSlug)}`);
  if (!res.ok) throw new Error(`Failed to fetch clubs for ${leagueSlug}: ${res.status}`);
  return res.json();
}
