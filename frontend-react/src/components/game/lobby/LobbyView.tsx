"use client";

import React from 'react';

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
  selectedCategorySlug: string;
  onSelectCategory: (slug: string) => void;
  onStartGame: () => void;
  playerName: string;
  onPlayerNameChange: (name: string) => void;
  gameMode: 'practice' | 'ranked';
  onGameModeChange: (mode: 'practice' | 'ranked') => void;
}

export default function LobbyView({
  categories,
  selectedCategorySlug,
  onSelectCategory,
  onStartGame,
  playerName,
  onPlayerNameChange,
  gameMode,
  onGameModeChange,
}: LobbyViewProps) {
  return (
    <div className="h-root min-h-screen relative flex flex-col p-7 md:p-14 text-h-fg overflow-x-hidden">
      {/* Dart Trajectory Decoration */}
      <div className="h-trajectory absolute inset-0 pointer-events-none z-0">
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
      <header className="h-head relative z-10 flex items-baseline justify-between border-b border-h-rule pb-3.5 mb-9">
        <div className="h-mark">FOOTBALL 501</div>
        <div className="h-kicker">THE TRIVIA DARTS CHAMPIONSHIP</div>
      </header>

      {/* Main Hero */}
      <main className="h-main flex-1 relative z-10 grid grid-cols-1 gap-7.5 max-w-6xl mx-auto w-full">
        <section className="h-hero grid grid-cols-1 md:grid-cols-[1fr_auto] items-end gap-8 border-b border-h-rule pb-4 animate-in fade-in slide-in-from-bottom-3 duration-700">
          <div className="h-hero-left">
            <h1 className="h-hero-h1 m-0">
              PLAY THE<br />DAILY <em>501</em>
            </h1>
            <div className="h-hero-tag font-plex text-[12px] tracking-[0.2em] text-h-dim uppercase mt-3">
              Global Challenge • New Every 24h
            </div>
          </div>
          <div className="h-hero-num hidden md:block">501</div>
        </section>

        {/* Categories Section */}
        <section className="h-section">
          <div className="h-sec-label flex items-baseline gap-3 mb-3">
            <span className="h-sec-num w-5.5 h-5.5 rounded-full bg-h-fg text-h-bg text-[10px] flex items-center justify-center font-plex">01</span>
            <span className="h-kicker">Select Category</span>
          </div>

          <div className="h-cats grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3.5">
            {categories.map((cat, idx) => (
              <button
                key={cat.id}
                onClick={() => onSelectCategory(cat.slug)}
                className={`h-card group flex flex-col bg-white border rounded-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg ${
                  selectedCategorySlug === cat.slug 
                    ? 'border-h-fg ring-1 ring-h-fg shadow-md' 
                    : 'border-h-rule'
                }`}
                style={{ animationDelay: `${idx * 0.08}s` }}
              >
                <div className="h-card-stripe h-2.5 flex overflow-hidden">
                  <i className="flex-1 bg-tele-cyan"></i>
                  <i className="flex-1 bg-tele-accent"></i>
                  <i className="flex-1 bg-tele-magenta"></i>
                  <i className="flex-1 bg-tele-green"></i>
                </div>
                <div className="h-card-body p-4.5 flex-1 flex flex-col">
                  <div className="h-card-name font-bricolage font-bold text-lg leading-tight tracking-tight">
                    {cat.name}
                  </div>
                  <div className="h-card-sub font-plex text-[11px] tracking-wider text-h-dim uppercase mt-1">
                    {cat.description}
                  </div>
                  
                  <div className="h-card-leader mt-auto pt-4 border-t border-dashed border-h-rule flex items-baseline justify-between gap-3">
                    <div className="h-card-num font-bricolage font-extrabold text-4xl leading-none tracking-tight text-h-fg">
                      {cat.leader?.score || '---'}
                    </div>
                    <div className="h-card-leader-name font-plex text-[11px] tracking-tight text-h-fg uppercase text-right leading-tight max-w-[50%]">
                      <span className="text-h-accent block text-[9px] tracking-widest mb-0.5">TOP SCORE</span>
                      {cat.leader?.name || 'No Data'}
                    </div>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </section>

        {/* Controls */}
        <section className="h-controls grid grid-cols-1 md:grid-cols-[auto_1fr_auto] gap-6 items-center">
          <div className="h-mode-pill relative inline-flex p-1 bg-black/5 rounded-full">
            <div 
              className="h-mode-pill-thumb absolute top-1 bottom-1 bg-h-fg rounded-full transition-all duration-300 ease-out"
              style={{ 
                left: gameMode === 'practice' ? '4px' : 'calc(50% + 2px)',
                width: 'calc(50% - 6px)'
              }}
            />
            <button 
              onClick={() => onGameModeChange('practice')}
              className={`relative z-10 font-plex text-[12px] tracking-wider px-5.5 py-2.5 uppercase transition-colors duration-200 ${gameMode === 'practice' ? 'text-h-bg' : 'text-h-dim'}`}
            >
              Practice
            </button>
            <button 
              onClick={() => onGameModeChange('ranked')}
              className={`relative z-10 font-plex text-[12px] tracking-wider px-5.5 py-2.5 uppercase transition-colors duration-200 ${gameMode === 'ranked' ? 'text-h-bg' : 'text-h-dim'}`}
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

          <button 
            onClick={onStartGame}
            className="h-cta group appearance-none border-0 cursor-pointer font-plex text-[13px] tracking-widest uppercase bg-h-fg text-h-bg px-7 py-4.5 rounded-sm inline-flex items-center gap-3 self-end hover:bg-h-accent transition-all hover:-translate-y-0.5"
          >
            Start Match
            <span className="h-cta-arr transition-transform group-hover:translate-x-1">→</span>
          </button>
        </section>
      </main>

      {/* Footer */}
      <footer className="h-foot mt-7 pb-4 border-t border-h-rule flex gap-7 flex-wrap font-plex text-[11px] tracking-widest text-h-dim uppercase relative z-10">
        <div className="flex gap-1.5">LIVE MATCHES: <b>1,242</b></div>
        <div className="flex gap-1.5">DAILY CHALLENGE: <b>OPEN</b></div>
        <div className="ml-auto">© 2026 FOOTBALL 501</div>
      </footer>
    </div>
  );
}
