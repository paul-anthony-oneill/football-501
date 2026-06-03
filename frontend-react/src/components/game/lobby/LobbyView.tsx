"use client";

import { useState } from "react";
import CategoryPopup from "./CategoryPopup";
import LoginButton from "@/components/auth/LoginButton";
import { useAuth } from "@/context/AuthContext";
import { CATEGORIES, type CategoryDefinition } from "@/lib/questionHierarchy";
import type { CategoryChallenge } from "@/hooks/useDailyChallenge";

interface Category {
  id: string;
  name: string;
  slug: string;
  description: string;
  theme?: string;
  leader?: { name: string; score: number };
}

interface LobbyViewProps {
  categories: Category[];
  onStartGame: (slug: string, label: string) => void;
  onStartDailyChallenge: (slug: string, label: string) => void;
  onStartTestGame: () => void;
  playerName: string;
  onPlayerNameChange: (name: string) => void;
  gameMode: "solo" | "ranked";
  onGameModeChange: (mode: "solo" | "ranked") => void;
  dailyChallenges: CategoryChallenge[];
  dailyLoading: boolean;
}

/** Look up the full hierarchy definition for a flat category by matching its slug. */
function findHierarchyDef(slug: string): CategoryDefinition | null {
  return CATEGORIES.find((c) => c.id === slug) ?? null;
}

export default function LobbyView({
  categories,
  onStartGame,
  onStartDailyChallenge,
  onStartTestGame,
  playerName,
  onPlayerNameChange,
  gameMode,
  onGameModeChange,
  dailyChallenges,
  dailyLoading,
}: LobbyViewProps) {
  const [popupCat, setPopupCat] = useState<CategoryDefinition | null>(null);
  const { user, loading } = useAuth();

  const handleCardClick = (cat: Category) => {
    const def = findHierarchyDef(cat.slug);
    if (def) setPopupCat(def);
  };

  const handlePopupSelect = (slug: string, label: string) => {
    setPopupCat(null);
    onStartGame(slug, label);
  };

  return (
    <div className="h-root min-h-screen relative flex flex-col px-7 md:px-14 py-7 text-h-fg overflow-x-hidden">
      {/* Dart Trajectory Decoration */}
      <div className="h-trajectory absolute inset-0 pointer-events-none z-0">
        <svg
          viewBox="0 0 1440 900"
          className="absolute inset-0 w-full h-full opacity-20"
        >
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
      <header className="h-head relative z-10 flex items-baseline justify-between border-b border-h-rule pb-3.5 mb-9">
        <div className="flex items-baseline gap-6">
          <div className="h-mark">FOOTBALL 501</div>
          <div className="h-kicker hidden sm:block">THE TRIVIA DARTS CHAMPIONSHIP</div>
        </div>
        <LoginButton />
      </header>

      {/* Main */}
      <main className="h-main flex-1 relative z-10 grid grid-cols-1 gap-7.5 max-w-6xl mx-auto w-full">
        {/* Hero */}
        <section className="h-hero grid grid-cols-1 md:grid-cols-[1fr_auto] items-end gap-8 border-b border-h-rule pb-4 animate-in fade-in slide-in-from-bottom-3 duration-700">
          <div className="h-hero-left">
            <h1 className="h-hero-h1 m-0">
              FROM <em>501</em> DOWN<br />TO <em>ZERO</em>
            </h1>
            <div className="h-hero-tag font-plex text-[12px] tracking-[0.2em] text-h-dim uppercase mt-3">
              Pick a category · Drill down · Over 180 busts
            </div>
          </div>
          <div className="h-hero-num hidden md:block">501</div>
        </section>

        {/* Daily Challenges Row */}
        {!dailyLoading && dailyChallenges.length > 0 && (
          <section className="h-section">
            <div className="h-sec-label flex items-baseline gap-3 mb-3">
              <span className="h-sec-num w-5.5 h-5.5 rounded-full bg-h-accent text-h-bg text-[10px] flex items-center justify-center font-plex">
                D
              </span>
              <span className="h-kicker">Today&apos;s Challenges</span>
              <span className="font-plex text-[10px] tracking-widest text-h-dim uppercase ml-auto">
                One attempt · Share with friends
              </span>
            </div>

            <div className="flex gap-3.5 overflow-x-auto pb-2">
              {dailyChallenges.map((dc) => (
                <button
                  key={dc.categorySlug}
                  onClick={() => onStartDailyChallenge(dc.categorySlug, dc.categoryName)}
                  className="daily-card group flex-shrink-0 flex flex-col bg-black border-2 border-h-accent rounded-sm p-5 w-64 transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-h-accent/20 text-left"
                >
                  <div className="flex items-baseline justify-between mb-2">
                    <span className="font-bricolage font-bold text-lg tracking-tight text-h-fg">
                      {dc.categoryName}
                    </span>
                    <span className="font-plex text-[10px] tracking-widest text-h-dim uppercase">
                      Daily
                    </span>
                  </div>
                  <div className="font-vt323 text-[32px] text-h-accent tracking-widest mb-2">
                    TARGET: {dc.startingScore.toString().padStart(3, "0")}
                  </div>
                  <div className="font-plex text-[11px] text-h-dim leading-snug line-clamp-2 mb-4">
                    {dc.questionText || "Loading..."}
                  </div>
                  <div className="mt-auto flex items-center justify-between">
                    <span className="font-plex text-[10px] tracking-widest text-h-accent uppercase">
                      PLAY NOW
                    </span>
                    <span className="font-bricolage font-bold text-lg text-h-accent">→</span>
                  </div>
                </button>
              ))}
            </div>
          </section>
        )}

        {/* Categories Section */}
        <section className="h-section">
          <div className="h-sec-label flex items-baseline gap-3 mb-3">
            <span className="h-sec-num w-5.5 h-5.5 rounded-full bg-h-fg text-h-bg text-[10px] flex items-center justify-center font-plex">
              01
            </span>
            <span className="h-kicker">Choose a Category</span>
          </div>

          <div className="h-cats grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3.5">
            {categories.map((cat, idx) => (
              <button
                key={cat.id}
                onClick={() => handleCardClick(cat)}
                className="h-card group flex flex-col bg-white border border-h-rule rounded-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg"
                style={{ animationDelay: `${idx * 0.08}s` }}
                data-theme={cat.theme ?? "teletext"}
              >
                <div className="h-card-stripe h-2.5 flex overflow-hidden">
                  <i className="flex-1 stripe-1" />
                  <i className="flex-1 stripe-2" />
                  <i className="flex-1 stripe-3" />
                  <i className="flex-1 stripe-4" />
                </div>
                <div className="h-card-body p-4.5 flex-1 flex flex-col">
                  <div className="h-card-name font-bricolage font-bold text-lg leading-tight tracking-tight">
                    {cat.name}
                  </div>
                  <div className="h-card-sub font-plex text-[11px] tracking-wider text-h-dim uppercase mt-1">
                    {cat.description}
                  </div>

                  <div className="h-card-leader mt-auto pt-4 border-t border-dashed border-h-rule flex items-center justify-between">
                    <span className="font-plex text-[10px] tracking-widest text-h-dim uppercase">
                      Tap to browse
                    </span>
                    <span className="font-bricolage font-bold text-sm text-h-accent">
                      →
                    </span>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </section>

        {/* Controls — mode + player name */}
        <section className="h-controls grid grid-cols-1 md:grid-cols-[auto_1fr] gap-6 items-center border-t border-h-rule pt-6">
          <div className="h-mode-pill relative inline-flex p-1 bg-black/5 rounded-full">
            <div
              className="h-mode-pill-thumb absolute top-1 bottom-1 bg-h-fg rounded-full transition-all duration-300 ease-out"
              style={{
                left: gameMode === "solo" ? "4px" : "calc(50% + 2px)",
                width: "calc(50% - 6px)",
              }}
            />
            <button
              onClick={() => onGameModeChange("solo")}
              className={`relative z-10 font-plex text-[12px] tracking-wider px-5.5 py-2.5 uppercase transition-colors duration-200 ${
                gameMode === "solo" ? "text-h-bg" : "text-h-dim"
              }`}
            >
              Practice
            </button>
            <button
              onClick={() => onGameModeChange("ranked")}
              className={`relative z-10 font-plex text-[12px] tracking-wider px-5.5 py-2.5 uppercase transition-colors duration-200 ${
                gameMode === "ranked" ? "text-h-bg" : "text-h-dim"
              }`}
            >
              Ranked
            </button>
          </div>

          <div className="h-names flex items-center gap-3.5 font-plex text-[12px] tracking-wider text-h-dim">
            <span>PLAYER</span>
            <input
              type="text"
              value={playerName}
              onChange={(e) => onPlayerNameChange(e.target.value)}
              className="h-name-field bg-transparent border-0 border-b-1.5 border-h-fg font-bricolage font-bold text-lg px-1.5 py-1 text-h-fg outline-none focus:border-h-accent transition-colors w-36"
              placeholder="GUEST_123"
            />
          </div>

          {!loading && !user && (
            <div className="font-plex text-[10px] tracking-wider text-h-dim">
              Sign in to save your progress
            </div>
          )}

          <button
            onClick={onStartTestGame}
            className="font-plex text-[11px] tracking-widest uppercase px-4 py-2 border border-h-accent text-h-accent rounded-full hover:bg-h-accent hover:text-h-bg transition-colors duration-200"
          >
            Test Mode
          </button>
        </section>
      </main>

      {/* Footer */}
      <footer className="h-foot mt-7 pb-4 border-t border-h-rule flex gap-7 flex-wrap font-plex text-[11px] tracking-widest text-h-dim uppercase relative z-10">
        <div className="flex gap-1.5">
          LIVE MATCHES: <b>1,242</b>
        </div>
        <div className="ml-auto">© 2026 FOOTBALL 501</div>
      </footer>

      {/* Category Drill-Down Popup */}
      {popupCat && (
        <CategoryPopup
          category={popupCat}
          onSelect={handlePopupSelect}
          onClose={() => setPopupCat(null)}
        />
      )}
    </div>
  );
}
