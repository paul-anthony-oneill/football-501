"use client";

import { useState, useCallback, useEffect, useRef } from "react";
import dynamic from "next/dynamic";
const LoginButton = dynamic(() => import("@/components/auth/LoginButton"), { ssr: false });
import HowToPlayPanel from "../HowToPlayPanel";
import { useAuth } from "@/context/AuthContext";
import { fetchClubs, type FootballClub, type FootballFilter } from "@/lib/api/footballApi";
import type { CategoryChallenge } from "@/hooks/useDailyChallenge";

// ─── Static data ──────────────────────────────────────────────────────────────

type TargetScore = 501 | 301 | 101 | "random";
const TARGET_OPTIONS: TargetScore[] = [501, 301, 101, "random"];

const LEAGUES = [
  { id: "premier-league",   name: "Premier League" },
  { id: "la-liga",          name: "La Liga" },
  { id: "bundesliga",       name: "Bundesliga" },
  { id: "serie-a",          name: "Serie A" },
  { id: "champions-league", name: "Champions League" },
] as const;
type League = (typeof LEAGUES)[number];

const STAT_TYPES = [
  { id: "goals",                    name: "Goals",                          sub: "Goals scored since 2000" },
  { id: "assists",                  name: "Assists",                        sub: "Goal assists since 2000" },
  { id: "appearances",              name: "Appearances",                    sub: "Games played since 2000" },
  { id: "goals_assists",            name: "Goals + Assists",                sub: "Combined total" },
  { id: "goals_appearances",        name: "Goals + Appearances",            sub: "Combined total" },
  { id: "assists_appearances",      name: "Assists + Appearances",          sub: "Combined total" },
  { id: "goals_assists_appearances", name: "Goals + Assists + Appearances", sub: "All three combined" },
] as const;

const OTHER_CATEGORIES = [
  { id: "film",      name: "Film",      description: "Worldwide box office hits",           color: "#f59e0b" },
  { id: "geography", name: "Geography", description: "Populations, capitals & world facts", color: "#3b82f6" },
];

function resolveTarget(t: TargetScore): number {
  if (t === "random") {
    const opts = [501, 301, 101] as const;
    return opts[Math.floor(Math.random() * opts.length)];
  }
  return t;
}

// ─── Navigation stack types ───────────────────────────────────────────────────

type NavScreen =
  | { id: "root" }
  | { id: "football" }
  | { id: "football-league"; league: League }
  | { id: "football-club";   league: League; club: FootballClub };

// ─── Props ────────────────────────────────────────────────────────────────────

interface LobbyViewProps {
  onStartGame: (slug: string, label: string, targetScore: number, filter?: FootballFilter) => void;
  onStartDailyChallenge: (slug: string, label: string) => void;
  dailyChallenges: CategoryChallenge[];
  dailyLoading: boolean;
}

// ─── Main component ───────────────────────────────────────────────────────────

export default function LobbyView({
  onStartGame,
  onStartDailyChallenge,
  dailyChallenges,
  dailyLoading,
}: LobbyViewProps) {
  const [target, setTarget] = useState<TargetScore>(501);
  const { user, loading } = useAuth();
  const isAdmin = !loading && user?.app_metadata?.role === "admin";

  // Navigation stack — last entry is the currently visible screen
  const [stack, setStack] = useState<NavScreen[]>([{ id: "root" }]);
  // Animation direction: 1 = sliding in from right (push), -1 = sliding in from left (pop)
  const [slideDir, setSlideDir] = useState<1 | -1>(1);
  const [animKey, setAnimKey] = useState(0);

  const push = useCallback((screen: NavScreen) => {
    setSlideDir(1);
    setAnimKey((k) => k + 1);
    setStack((s) => [...s, screen]);
  }, []);

  const pop = useCallback(() => {
    if (stack.length <= 1) return;
    setSlideDir(-1);
    setAnimKey((k) => k + 1);
    setStack((s) => s.slice(0, -1));
  }, [stack.length]);

  const startGame = useCallback(
    (slug: string, label: string, filter?: FootballFilter) => {
      onStartGame(slug, label, resolveTarget(target), filter);
    },
    [onStartGame, target],
  );

  const displayTarget = target === "random" ? "RND" : String(target);
  const currentScreen = stack[stack.length - 1];

  // Build breadcrumb label for the right-column header
  const breadcrumb = stack
    .slice(1)
    .map((s) => {
      if (s.id === "football") return "Football";
      if (s.id === "football-league") return s.league.name;
      if (s.id === "football-club") return s.club.name;
      return "";
    })
    .join(" › ");

  return (
    <div className="relative min-h-screen bg-[#f5f1e8] text-[#18171a] flex flex-col font-hanken overflow-hidden">
      {/* Dart trajectory — draws in on page load */}
      <div className="absolute inset-0 pointer-events-none z-0" aria-hidden="true">
        <svg viewBox="0 0 1440 900" className="absolute inset-0 w-full h-full opacity-20">
          <path
            d="M-100,600 C300,500 800,800 1540,100"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.5"
            className="animate-draw"
          />
        </svg>
      </div>

      {/* Header */}
      <header className="relative flex items-center justify-between px-6 md:px-10 py-4 border-b border-[rgba(24,23,26,0.1)]">
        <div className="flex items-center gap-4">
          <span className="font-plex text-[11px] tracking-[0.2em] bg-[#18171a] text-[#f5f1e8] px-2.5 py-1">
            TRIVIA 501
          </span>
          <span className="font-plex text-[10px] tracking-widest text-[#6f6a62] uppercase hidden sm:block">
            The Trivia Darts Championship
          </span>
        </div>
        <div className="flex items-center gap-3">
          {isAdmin && (
            <a
              href="/admin"
              className="font-plex text-[10px] tracking-widest text-[#6f6a62] uppercase hover:text-[#18171a] transition-colors"
            >
              Admin
            </a>
          )}
          <LoginButton />
        </div>
      </header>

      {/* Two-column layout */}
      <main className="flex-1 flex flex-col md:flex-row">

        {/* ── Left column: target score + daily challenges ── */}
        <div className="flex-1 flex flex-col px-6 md:px-10 py-8 md:border-r border-[rgba(24,23,26,0.1)]">

          {/* Target Score */}
          <div className="mb-10">
            <div className="font-plex text-[10px] tracking-[0.25em] text-[#6f6a62] uppercase mb-4">
              Target Score
            </div>
            <div className="flex gap-2 mb-6 flex-wrap">
              {TARGET_OPTIONS.map((opt) => (
                <button
                  key={opt}
                  onClick={() => setTarget(opt)}
                  className={`font-plex text-[11px] tracking-widest px-5 py-2.5 rounded-full border transition-all duration-200 ${
                    target === opt
                      ? "bg-[#18171a] text-[#f5f1e8] border-[#18171a]"
                      : "bg-transparent text-[#6f6a62] border-[rgba(24,23,26,0.2)] hover:border-[#18171a] hover:text-[#18171a]"
                  }`}
                >
                  {opt === "random" ? "RND" : opt}
                </button>
              ))}
            </div>
            <div
              className="font-bricolage font-extrabold leading-none tracking-tight text-[#18171a] select-none transition-all duration-300"
              style={{ fontSize: "clamp(72px, 11vw, 160px)", fontVariationSettings: "'wdth' 80" }}
            >
              {displayTarget}
            </div>
            {target === "random" && (
              <div className="font-plex text-[10px] tracking-widest text-[#6f6a62] uppercase mt-2">
                Picked randomly when you start
              </div>
            )}
          </div>

          {/* Daily Challenges */}
          {!dailyLoading && dailyChallenges.length > 0 && (
            <div className="mb-8">
              <div className="flex items-center gap-3 mb-3">
                <span className="w-5 h-5 rounded-full bg-[#e84545] text-white text-[9px] flex items-center justify-center font-plex font-bold">
                  D
                </span>
                <span className="font-plex text-[10px] tracking-widest text-[#6f6a62] uppercase">
                  Today&apos;s Challenges
                </span>
                <span className="ml-auto font-plex text-[9px] tracking-widest text-[#6f6a62]/60 uppercase hidden sm:block">
                  One attempt · Share with friends
                </span>
              </div>
              <div className="flex gap-3 overflow-x-auto pb-2">
                {dailyChallenges.map((dc) => (
                  <button
                    key={dc.categorySlug}
                    onClick={() => onStartDailyChallenge(dc.categorySlug, dc.categoryName)}
                    className="flex-shrink-0 flex flex-col bg-[#18171a] rounded-sm p-4 w-52 text-left hover:-translate-y-0.5 transition-transform duration-200"
                  >
                    <div className="flex items-baseline justify-between mb-1">
                      <span className="font-bricolage font-bold text-sm text-[#f5f1e8]">{dc.categoryName}</span>
                      <span className="font-plex text-[9px] tracking-widest text-[#f5f1e8]/50">DAILY</span>
                    </div>
                    <div className="font-vt323 text-[24px] text-[#e84545] tracking-widest mb-1">
                      TARGET: {dc.startingScore.toString().padStart(3, "0")}
                    </div>
                    <div className="font-plex text-[10px] text-[#f5f1e8]/60 leading-snug line-clamp-2 mb-3">
                      {dc.questionText || "Loading..."}
                    </div>
                    <div className="mt-auto flex items-center justify-between">
                      <span className="font-plex text-[9px] tracking-widest text-[#e84545] uppercase">PLAY NOW</span>
                      <span className="font-bricolage font-bold text-[#e84545]">→</span>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )}

        </div>

        {/* ── Right column: drill-down navigator ── */}
        <div className="w-full md:w-96 lg:w-[440px] flex flex-col px-6 md:px-8 py-8 overflow-hidden relative bg-[#f5f1e8]">

          {/* Back + breadcrumb */}
          {stack.length > 1 && (
            <button
              onClick={pop}
              className="flex items-center gap-2 font-plex text-[10px] tracking-widest text-[#6f6a62] uppercase mb-4 hover:text-[#18171a] transition-colors self-start"
            >
              ← {breadcrumb || "Back"}
            </button>
          )}

          {/* Animated screen area */}
          <div
            key={animKey}
            className="flex-1"
            style={{
              animation: `slideIn${slideDir === 1 ? "Right" : "Left"} 220ms ease-out both`,
            }}
          >
            <NavScreenRenderer
              screen={currentScreen}
              onPush={push}
              onStartGame={startGame}
            />
          </div>
        </div>
      </main>

      {/* Slide-in keyframes injected once */}
      <style>{`
        @keyframes slideInRight { from { transform: translateX(28px); opacity: 0; } to { transform: none; opacity: 1; } }
        @keyframes slideInLeft  { from { transform: translateX(-28px); opacity: 0; } to { transform: none; opacity: 1; } }
      `}</style>
    </div>
  );
}

// ─── Screen renderer (delegates to the right component per screen) ────────────

function NavScreenRenderer({
  screen,
  onPush,
  onStartGame,
}: {
  screen: NavScreen;
  onPush: (s: NavScreen) => void;
  onStartGame: (slug: string, label: string, filter?: FootballFilter) => void;
}) {
  if (screen.id === "root") {
    return <RootScreen onPush={onPush} onStartGame={onStartGame} />;
  }
  if (screen.id === "football") {
    return <FootballScreen onPush={onPush} onStartGame={onStartGame} />;
  }
  if (screen.id === "football-league") {
    return <LeagueScreen league={screen.league} onPush={onPush} onStartGame={onStartGame} />;
  }
  if (screen.id === "football-club") {
    return <ClubScreen league={screen.league} club={screen.club} onStartGame={onStartGame} />;
  }
  return null;
}

// ─── Screen: root — all categories ───────────────────────────────────────────

function RootScreen({
  onPush,
  onStartGame,
}: {
  onPush: (s: NavScreen) => void;
  onStartGame: (slug: string, label: string, filter?: FootballFilter) => void;
}) {
  return (
    <>
      <div className="font-plex text-[10px] tracking-[0.25em] text-[#6f6a62] uppercase mb-5">
        Choose Your Board
      </div>

      {/* Football — drill-down */}
      <NavRow
        accentColor="#22c55e"
        name="Football"
        sub="Goals · assists · 5 leagues"
        onClick={() => onPush({ id: "football" })}
        hasChildren
      />

      {/* Other categories — one click */}
      {OTHER_CATEGORIES.map((cat) => (
        <NavRow
          key={cat.id}
          accentColor={cat.color}
          name={cat.name}
          sub={cat.description}
          onClick={() => onStartGame(cat.id, cat.name)}
        />
      ))}

      <div className="mt-8">
        <HowToPlayPanel variant="home" />
      </div>
    </>
  );
}

// ─── Screen: football root ────────────────────────────────────────────────────

function FootballScreen({
  onPush,
  onStartGame,
}: {
  onPush: (s: NavScreen) => void;
  onStartGame: (slug: string, label: string, filter?: FootballFilter) => void;
}) {
  return (
    <>
      <div className="font-plex text-[10px] tracking-[0.25em] text-[#6f6a62] uppercase mb-5">Football</div>

      <NavRow
        icon="✦"
        name="Random Question"
        sub="Any club, any league, any stat"
        onClick={() => onStartGame("football", "Football — Random", { scope: "random_any" })}
      />

      <NavRow
        icon="✦"
        name="Random League Question"
        sub="League-wide stat, picked at random"
        onClick={() => onStartGame("football", "Football — Random League", { scope: "random_league_level" })}
      />

      <div className="py-3 flex items-center gap-3">
        <div className="flex-1 border-t border-[rgba(24,23,26,0.1)]" />
        <span className="font-plex text-[9px] tracking-widest text-[#6f6a62] uppercase">or pick a league</span>
        <div className="flex-1 border-t border-[rgba(24,23,26,0.1)]" />
      </div>

      {LEAGUES.map((league) => (
        <NavRow
          key={league.id}
          accentColor="#22c55e"
          name={league.name}
          onClick={() => onPush({ id: "football-league", league })}
          hasChildren
        />
      ))}
    </>
  );
}

// ─── Screen: league ───────────────────────────────────────────────────────────

function LeagueScreen({
  league,
  onPush,
  onStartGame,
}: {
  league: League;
  onPush: (s: NavScreen) => void;
  onStartGame: (slug: string, label: string, filter?: FootballFilter) => void;
}) {
  const [clubs, setClubs] = useState<FootballClub[]>([]);
  const [loadingClubs, setLoadingClubs] = useState(true);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    setLoadingClubs(true);
    fetchClubs(league.id)
      .then((data) => { if (mounted.current) { setClubs(data); setLoadingClubs(false); } })
      .catch(() => { if (mounted.current) setLoadingClubs(false); });
    return () => { mounted.current = false; };
  }, [league.id]);

  return (
    <>
      <div className="font-plex text-[10px] tracking-[0.25em] text-[#6f6a62] uppercase mb-5">{league.name}</div>

      {/* League-scope questions */}
      <NavRow
        icon="✦"
        name="League Questions"
        sub={`Stats across the full ${league.name}`}
        onClick={() => onStartGame(`football:${league.id}`, `Football › ${league.name} › League`, {
          scope: "league", league: league.id,
        })}
      />

      {/* Stat type drill-down for league */}
      {STAT_TYPES.map((stat) => (
        <NavRow
          key={`league-${stat.id}`}
          name={stat.name}
          sub={stat.sub}
          small
          onClick={() => onStartGame(
            `football:${league.id}:league:${stat.id}`,
            `Football › ${league.name} › ${stat.name}`,
            { scope: "league", league: league.id, statType: stat.id },
          )}
        />
      ))}

      <div className="py-3 flex items-center gap-3">
        <div className="flex-1 border-t border-[rgba(24,23,26,0.1)]" />
        <span className="font-plex text-[9px] tracking-widest text-[#6f6a62] uppercase">or pick a club</span>
        <div className="flex-1 border-t border-[rgba(24,23,26,0.1)]" />
      </div>

      <NavRow
        icon="✦"
        name="Random Club"
        sub={`Any club from the ${league.name}`}
        onClick={() => onStartGame(`football:${league.id}:random`, `Football › ${league.name} › Random Club`, {
          scope: "random_club_level", league: league.id,
        })}
      />

      {loadingClubs ? (
        <div className="font-plex text-[10px] tracking-widest text-[#6f6a62] py-4 animate-pulse">
          Loading clubs…
        </div>
      ) : clubs.length === 0 ? (
        <div className="font-plex text-[10px] tracking-widest text-[#6f6a62] py-4">
          No clubs available yet — data coming soon.
        </div>
      ) : (
        clubs.map((club) => (
          <NavRow
            key={club.id}
            accentColor="#22c55e"
            name={club.name}
            onClick={() => onPush({ id: "football-club", league, club })}
            hasChildren
          />
        ))
      )}
    </>
  );
}

// ─── Screen: club stat picker ─────────────────────────────────────────────────

function ClubScreen({
  league,
  club,
  onStartGame,
}: {
  league: League;
  club: FootballClub;
  onStartGame: (slug: string, label: string, filter?: FootballFilter) => void;
}) {
  return (
    <>
      <div className="font-plex text-[10px] tracking-[0.25em] text-[#6f6a62] uppercase mb-5">{club.name}</div>

      <NavRow
        icon="✦"
        name="Random Question"
        sub="Any stat type for this club"
        onClick={() => onStartGame(
          `football:${league.id}:${club.id}`,
          `Football › ${league.name} › ${club.name}`,
          { scope: "club", league: league.id, club: club.id },
        )}
      />

      <div className="py-3 flex items-center gap-3">
        <div className="flex-1 border-t border-[rgba(24,23,26,0.1)]" />
        <span className="font-plex text-[9px] tracking-widest text-[#6f6a62] uppercase">or pick a stat</span>
        <div className="flex-1 border-t border-[rgba(24,23,26,0.1)]" />
      </div>

      {STAT_TYPES.map((stat) => (
        <NavRow
          key={stat.id}
          name={stat.name}
          sub={stat.sub}
          small
          onClick={() => onStartGame(
            `football:${league.id}:${club.id}:${stat.id}`,
            `Football › ${league.name} › ${club.name} › ${stat.name}`,
            { scope: "club", league: league.id, club: club.id, statType: stat.id },
          )}
        />
      ))}
    </>
  );
}

// ─── Shared row component ─────────────────────────────────────────────────────

function NavRow({
  accentColor,
  icon,
  name,
  sub,
  onClick,
  hasChildren = false,
  small = false,
}: {
  accentColor?: string;
  icon?: string;
  name: string;
  sub?: string;
  onClick: () => void;
  hasChildren?: boolean;
  small?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      className="group flex items-center gap-4 py-3.5 border-b border-[rgba(24,23,26,0.08)] hover:bg-[rgba(24,23,26,0.025)] transition-colors text-left w-full"
    >
      {icon ? (
        <span className="text-[#22c55e] font-bricolage font-bold text-lg leading-none flex-shrink-0 w-4 text-center">
          {icon}
        </span>
      ) : accentColor ? (
        <div className="w-0.5 self-stretch rounded-full flex-shrink-0" style={{ backgroundColor: accentColor }} />
      ) : (
        <div className="w-4 flex-shrink-0" />
      )}

      <div className="flex-1 min-w-0">
        <div className={`font-bricolage font-bold leading-tight ${small ? "text-base" : "text-xl"}`}>{name}</div>
        {sub && (
          <div className="font-plex text-[10px] tracking-wider text-[#6f6a62] uppercase mt-0.5">{sub}</div>
        )}
      </div>

      <span className="font-bricolage font-bold text-base text-[#6f6a62] group-hover:text-[#18171a] group-hover:translate-x-0.5 transition-all flex-shrink-0">
        {hasChildren ? "→" : "↵"}
      </span>
    </button>
  );
}
