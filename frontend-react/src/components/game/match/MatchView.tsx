"use client";

import React, { useState, useEffect } from 'react';
import EntitySearch from '../EntitySearch';
import HowToPlayPanel from '../HowToPlayPanel';
import DebugPanel from '../DebugPanel';
import LoginButton from '@/components/auth/LoginButton';
import ConfirmDialog from '@/components/ui/ConfirmDialog';

interface StagedAnswer {
  name: string;
  entityId?: string;
}

interface Move {
  answer: string;
  result: string;
  scoreBefore: number;
  scoreAfter: number;
  matchedAnswer?: string;
  scoreValue?: number;
}

interface GameHints {
  /** Remaining unused answers worth exactly 180 points. Shown while score > 180. */
  maxScoresLeft: number;
  /** Remaining unused answers that would win the game in one move. Shown while score ≤ 180. */
  checkoutsLeft: number;
}

interface MatchViewProps {
  score: number;
  question: string;
  turnCount: number;
  moves: Move[];
  onExit: () => void;
  onSubmitAnswer: (answer: string, entityId?: string) => void;
  onPlayAgain: () => void;
  categoryName: string;
  categorySub: string;
  /** Entity type passed to autocomplete (e.g. "footballer", "city"). */
  entityType?: string;
  /** True once the player has checked out (game over). */
  isWin?: boolean;
  /** In-game hint stats from the server. Null until the first response arrives. */
  hints?: GameHints | null;
  /** Disables the answer input while an animation is playing. */
  disabled?: boolean;
  /** Version counter for the score value; used as a React key to trigger flash animation. */
  flashVersion?: number;
  /** Called when the player clicks "SHARE RESULT" in the win overlay. Daily-challenge only. */
  onShare?: () => void;
  /** True while a share copy operation is in progress. */
  sharing?: boolean;
  /** Current game ID, used by DebugPanel to fetch all answers. */
  gameId?: string | null;
  /** Current game type, used by DebugPanel to route API calls. */
  debugGameType?: "solo" | "daily-challenge";
}

export default function MatchView({
  score,
  question,
  turnCount,
  moves,
  onExit,
  onSubmitAnswer,
  onPlayAgain,
  categoryName,
  categorySub,
  entityType = "footballer",
  isWin = false,
  hints = null,
  disabled = false,
  flashVersion = 0,
  onShare,
  sharing = false,
  gameId = null,
  debugGameType = "solo",
}: MatchViewProps) {
  const [staged, setStaged] = useState<StagedAnswer | null>(null);
  const [showExitConfirm, setShowExitConfirm] = useState(false);
  const [now, setNow] = useState(new Date());

  useEffect(() => {
    const interval = setInterval(() => setNow(new Date()), 30000);
    return () => clearInterval(interval);
  }, []);

  function handleStage(name: string, entityId?: string) {
    setStaged({ name, entityId });
  }

  function handleThrowDart() {
    if (!staged) return;
    onSubmitAnswer(staged.name, staged.entityId);
    setStaged(null);
  }

  return (
    <div className="game theme-teletext min-h-screen flex flex-col font-vt323 text-lg bg-black text-white relative"
      onKeyDown={(e) => {
        if (e.key === "Enter" && staged && !disabled) {
          handleThrowDart();
        }
      }}
    >
      {/* Teletext Header Status Line */}
      <div className="bg-white text-black px-6 py-0.5 flex justify-between tracking-widest">
        <span>P302 {isWin ? 'GAME COMPLETE' : 'GAME IN PROGRESS'}</span>
        <span>{now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })}</span>
      </div>

      {/* Game Header */}
      <header className="game-top flex items-center justify-between px-8 py-4.5 border-b-2 border-white">
        <button onClick={() => setShowExitConfirm(true)} className="btn-ghost text-tele-cyan border-2 border-tele-cyan px-5 py-2 hover:bg-white hover:text-black transition-colors">
          ESC_EXIT
        </button>
        <div className="game-cat text-center">
          <div className="game-cat-name bg-tele-magenta text-black px-3 py-0.5 inline-block text-[22px]">
            {categoryName.toUpperCase()}
          </div>
          <div className="game-cat-sub text-tele-cyan text-[18px] mt-1 uppercase tracking-wider">
            {categorySub}
          </div>
        </div>
        <div className="game-spacer w-20 flex justify-end">
          <LoginButton />
        </div>
      </header>

      {/* Main Game Area */}
      <main className="game-main flex-1 grid grid-cols-1 lg:grid-cols-[1fr_380px] gap-8 p-8 overflow-hidden">
        <div className="game-left flex flex-col gap-7 min-w-0">

          {/* Main Scoreboard */}
          <div className="sb sb-on border-4 border-tele-accent p-6 bg-black relative shadow-[0_0_0_4px_inset_#ffff00]">
            <div className="sb-label bg-tele-accent text-black px-2 py-0.5 inline-block text-[18px] tracking-widest font-bold">
              POINTS REMAINING
            </div>
            <div className="sb-big text-[160px] text-tele-green leading-none mt-4 font-normal">
              <span key={flashVersion} className="animate-score-flash inline-block">
                {score.toString().padStart(3, '0')}
              </span>
            </div>
            <div className="sb-foot flex items-baseline gap-2 mt-2">
              <span className="sb-foot-k text-tele-cyan text-[18px] tracking-widest uppercase">LAST SCORE</span>
              <span className="sb-foot-v text-tele-magenta text-[22px] font-bold">
                {moves.length > 0 ? moves[0].scoreValue : '--'}
              </span>
            </div>

            {/* ── In-game hints ──────────────────────────────────────────── */}
            {hints !== null && (
              <div className="sb-hints mt-3 pt-3 border-t border-[#333] flex gap-6">
                {/* 180s LEFT — prominent above 180, dimmed at/below */}
                <div className={score > 180 ? '' : 'opacity-30'}>
                  <span className="text-tele-cyan text-[16px] tracking-widest uppercase">
                    180s Left{' '}
                  </span>
                  <span className="text-tele-green text-[22px] font-bold">
                    {hints.maxScoresLeft}
                  </span>
                </div>
                {/* CHECKOUTS — prominent at/below 180, dimmed above */}
                <div className={score <= 180 ? '' : 'opacity-30'}>
                  <span className="text-tele-cyan text-[16px] tracking-widest uppercase">
                    Checkouts{' '}
                  </span>
                  <span
                    className={`text-[22px] font-bold ${
                      score <= 180 && hints.checkoutsLeft > 0
                        ? 'text-tele-accent'
                        : 'text-[#666]'
                    }`}
                  >
                    {hints.checkoutsLeft}
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Prompt & Input */}
          <div className="prompt flex flex-col gap-4">
            <div className="prompt-q text-tele-cyan text-[32px] leading-tight">
              {question || "Loading question..."}
            </div>

            {/* Search input */}
            <div className="ta-wrap relative">
              <div className="ta-input-row flex items-center gap-3.5 bg-black border-2 border-tele-cyan p-0 px-5.5 h-16">
                <span className="ta-caret text-tele-green text-[26px] animate-pulse">{'>'}</span>
                <EntitySearch
                  entityType={entityType}
                  onSelect={handleStage}
                  placeholder="SEARCH PLAYER NAME..."
                  className="teletext-input flex-1 bg-transparent border-0 outline-none text-tele-accent text-[30px] font-vt323 p-0"
                  disabled={disabled}
                />
              </div>
            </div>

            {/* Staged answer card */}
            {staged ? (
              <div className="staged-card flex items-center justify-between border-2 border-tele-green bg-black px-5.5 py-3 gap-4">
                <div className="flex items-center gap-3">
                  <span className="text-tele-green text-[18px] tracking-widest">SELECTED</span>
                  <span className="text-white text-[26px] tracking-wide font-vt323 uppercase">
                    {staged.name}
                  </span>
                </div>
                <button
                  onClick={() => setStaged(null)}
                  className="text-tele-danger text-[22px] hover:opacity-70 transition-opacity leading-none"
                  aria-label="Clear selection"
                >
                  ✕
                </button>
              </div>
            ) : (
              <div className="prompt-sub text-[#555] text-[18px] border-2 border-dashed border-[#333] px-5.5 py-3">
                Select a player from the list to stage your answer
              </div>
            )}

            {/* Throw Dart button */}
            <button
              onClick={handleThrowDart}
              disabled={!staged || disabled}
              className="throw-btn border-2 border-tele-danger text-tele-danger px-6 py-3 text-[28px] tracking-widest font-vt323 transition-all duration-150 hover:bg-white hover:text-black hover:border-white disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-tele-danger disabled:hover:border-tele-danger"
            >
              THROW DART →
            </button>
          </div>
        </div>

        {/* Sidebar / History */}
        <aside className="game-right flex flex-col bg-black border-2 border-white p-5 overflow-hidden">
          <div className="hist-head bg-tele-magenta text-black px-3 py-1 text-[18px] mb-3 uppercase font-bold">
            MATCH HISTORY
          </div>
          <div className="hist-list flex-1 overflow-y-auto pr-2">
            {moves.length === 0 ? (
              <div className="hist-empty text-[#888] text-[18px] text-center py-6 tracking-widest">
                NO DARTS THROWN
              </div>
            ) : (
              moves.map((move, i) => (
                <div key={i} className={`hist-row grid grid-cols-[28px_1fr_auto_70px_60px] gap-3 items-baseline py-2.5 border-b border-[#444] border-dashed ${move.result === 'BUST' ? 'hist-bust' : ''}`}>
                  <span className="hist-i text-[#888] text-[18px]">{(moves.length - i).toString().padStart(2, '0')}</span>
                  <span className={`hist-name text-[18px] uppercase ${move.result === 'BUST' ? 'text-[#888] line-through' : move.result === 'INVALID' ? 'text-tele-danger' : 'text-white'}`}>
                    {move.matchedAnswer || move.answer}
                  </span>
                  <span className={`hist-val text-[22px] font-bold ${move.result === 'BUST' ? 'text-tele-danger' : move.result === 'INVALID' ? 'text-[#888]' : 'text-tele-green'}`}>
                    {move.result === 'INVALID' ? '✗' : move.scoreValue}
                  </span>
                  <span className={`hist-badge text-[14px] tracking-wider ${move.result === 'VALID' ? 'text-tele-green' : move.result === 'BUST' ? 'text-tele-danger' : 'text-[#888]'}`}>
                    {move.result === 'VALID' ? '✓ OK' : move.result === 'BUST' ? 'BUST' : 'INVALID'}
                  </span>
                  <span className="hist-rem text-tele-accent text-[22px] text-right font-bold">
                    {move.scoreAfter}
                  </span>
                </div>
              ))
            )}
          </div>

          <div className="hist-foot mt-4 pt-3 border-t-2 border-white flex justify-between items-baseline text-tele-cyan text-[18px] uppercase">
            <span>TURN COUNT</span>
            <span className="text-tele-accent font-bold">{turnCount.toString().padStart(2, '0')}</span>
          </div>

          <div className="mt-4">
            <HowToPlayPanel variant="teletext" />
          </div>
        </aside>
      </main>

      {/* ── Win Overlay ─────────────────────────────────────────────────────── */}
      {isWin && (
        <div className="absolute inset-0 bg-black/90 flex flex-col items-center justify-center z-50 gap-8 font-vt323">
          <div className="text-tele-green text-[72px] tracking-widest text-center animate-pulse">
            CHECKOUT!
          </div>
          <div className="text-center">
            <div className="text-tele-accent text-[48px] tracking-widest">
              {/* Checkout lands at 0 or within -10..0; always display as 000 */}
              {(score <= 0 ? 0 : score).toString().padStart(3, '0')}
            </div>
            <div className="text-tele-cyan text-[22px] tracking-widest mt-1">
              FINAL SCORE
            </div>
          </div>
          <div className="text-white text-[26px] tracking-widest">
            {turnCount.toString().padStart(2, '0')} TURNS
          </div>
          <div className="flex flex-col gap-4 items-center mt-4">
            {onShare && (
              <button
                onClick={onShare}
                disabled={sharing}
                className="border-2 border-tele-magenta text-tele-magenta px-10 py-4 text-[24px] tracking-widest hover:bg-tele-magenta hover:text-black transition-colors disabled:opacity-50"
              >
                {sharing ? "COPIED!" : "SHARE RESULT"}
              </button>
            )}
            <button
              onClick={onPlayAgain}
              className="border-2 border-tele-cyan text-tele-cyan px-10 py-4 text-[28px] tracking-widest hover:bg-tele-cyan hover:text-black transition-colors"
            >
              PLAY AGAIN
            </button>
            <button
              onClick={() => setShowExitConfirm(true)}
              className="text-[#666] text-[20px] hover:text-white transition-colors tracking-widest"
            >
              EXIT TO LOBBY
            </button>
          </div>
        </div>
      )}

      {/* ── Exit Confirmation Dialog ─────────────────────────────────────────── */}
      <ConfirmDialog
        open={showExitConfirm}
        title="Exit Game?"
        message="Are you sure you want to exit? Your progress in this game will be lost."
        confirmText="Exit"
        cancelText="Stay"
        type="danger"
        onConfirm={() => { setShowExitConfirm(false); onExit(); }}
        onCancel={() => setShowExitConfirm(false)}
      />

      {/* ── Debug Panel (Ctrl+Shift+D) ────────────────────────────────────────── */}
      <DebugPanel gameId={gameId} gameType={debugGameType} />
    </div>
  );
}
