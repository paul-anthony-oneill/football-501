"use client";

import React from 'react';
import EntitySearch from '../EntitySearch';

interface Move {
  answer: string;
  result: string;
  scoreBefore: number;
  scoreAfter: number;
  matchedAnswer?: string;
  scoreValue?: number;
}

interface MatchViewProps {
  score: number;
  question: string;
  turnCount: number;
  moves: Move[];
  onExit: () => void;
  onSubmitAnswer: (answer: string) => void;
  categoryName: string;
  categorySub: string;
}

export default function MatchView({
  score,
  question,
  turnCount,
  moves,
  onExit,
  onSubmitAnswer,
  categoryName,
  categorySub,
}: MatchViewProps) {
  return (
    <div className="game theme-teletext min-h-screen flex flex-col font-vt323 text-lg bg-black text-white">
      {/* Teletext Header Status Line */}
      <div className="bg-white text-black px-6 py-0.5 flex justify-between tracking-widest">
        <span>P302 GAME IN PROGRESS</span>
        <span>20:45</span>
      </div>

      {/* Game Header */}
      <header className="game-top flex items-center justify-between px-8 py-4.5 border-b-2 border-white">
        <button onClick={onExit} className="btn-ghost text-tele-cyan border-2 border-tele-cyan px-5 py-2 hover:bg-white hover:text-black transition-colors">
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
        <div className="game-spacer w-20"></div>
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
              {score.toString().padStart(3, '0')}
            </div>
            <div className="sb-foot flex items-baseline gap-2 mt-2">
              <span className="sb-foot-k text-tele-cyan text-[18px] tracking-widest uppercase">LAST SCORE</span>
              <span className="sb-foot-v text-tele-magenta text-[22px] font-bold">
                {moves.length > 0 ? moves[0].scoreValue : '--'}
              </span>
            </div>
          </div>

          {/* Prompt & Input */}
          <div className="prompt flex flex-col gap-4">
            <div className="prompt-q text-tele-cyan text-[32px] leading-tight">
              {question || "Loading question..."}
            </div>
            
            <div className="ta-wrap relative">
              <div className="ta-input-row flex items-center gap-3.5 bg-black border-2 border-tele-cyan p-0 px-5.5 h-16">
                <span className="ta-caret text-tele-green text-[26px] animate-pulse">{'>'}</span>
                <EntitySearch 
                  onSelect={onSubmitAnswer}
                  placeholder="TYPE PLAYER NAME..."
                  className="teletext-input flex-1 bg-transparent border-0 outline-none text-tele-accent text-[30px] font-vt323 p-0"
                />
              </div>
            </div>
            
            <div className="prompt-sub text-white text-[18px]">
              Type a player and press <b className="text-tele-danger">ENTER</b> to score.
            </div>
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
                <div key={i} className={`hist-row grid grid-cols-[28px_1fr_auto_60px] gap-3 items-baseline py-2.5 border-b border-[#444] border-dashed ${move.result === 'BUST' ? 'hist-bust' : ''}`}>
                  <span className="hist-i text-[#888] text-[18px]">{(moves.length - i).toString().padStart(2, '0')}</span>
                  <span className={`hist-name text-[18px] uppercase ${move.result === 'BUST' ? 'text-[#888] line-through' : 'text-white'}`}>
                    {move.answer}
                  </span>
                  <span className={`hist-val text-[22px] font-bold ${move.result === 'BUST' ? 'text-tele-danger' : 'text-tele-green'}`}>
                    {move.scoreValue}
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
        </aside>
      </main>
    </div>
  );
}
