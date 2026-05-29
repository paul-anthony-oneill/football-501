"use client";

import { useState, useCallback, useEffect, useRef } from "react";
import type { HierarchyNode, CategoryDefinition } from "@/lib/questionHierarchy";

interface CategoryPopupProps {
  category: CategoryDefinition;
  onSelect: (slug: string, label: string) => void;
  onClose: () => void;
}

/**
 * Multi-level drill-down popup.
 * Renders the current level's options with slide transitions between levels.
 * "Random" at any level picks randomly from all terminal descendants.
 */
export default function CategoryPopup({ category, onSelect, onClose }: CategoryPopupProps) {
  // Stack of nodes representing the drill-down path. The last entry is the
  // currently-displayed node. We start at the category root.
  const [path, setPath] = useState<HierarchyNode[]>([category.hierarchy]);
  // Slide direction: 1 = deeper, -1 = shallower
  const [slideDir, setSlideDir] = useState<1 | -1>(1);
  // Track previous level key for stable slide animations
  const [animKey, setAnimKey] = useState(0);
  const contentRef = useRef<HTMLDivElement>(null);

  const currentNode = path[path.length - 1];
  const options = currentNode.children ?? [];
  const isAtRoot = path.length === 1;

  const pathRef = useRef(path);
  pathRef.current = path;

  // Escape key — use ref so the listener is stable while reading fresh path
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        if (pathRef.current.length > 1) {
          setPath((prev) => prev.slice(0, -1));
        } else {
          onClose();
        }
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const goTo = useCallback((node: HierarchyNode) => {
    if (node.children && node.children.length > 0) {
      setSlideDir(1);
      setAnimKey((k) => k + 1);
      setPath((prev) => [...prev, node]);
    } else {
      // Terminal node — fire selection
      const slugPath = path.map((n) => n.id).concat(node.id);
      const labelPath = path.map((n) => n.name).concat(node.name);
      onSelect(slugPath.join(":"), labelPath.join(" > "));
    }
  }, [path, onSelect]);

  const goRandom = useCallback(() => {
    const slugPath = path.map((n) => n.id).concat("random");
    const labelPath = path.map((n) => n.name).concat("Random");
    onSelect(slugPath.join(":"), labelPath.join(" > "));
  }, [path, onSelect]);

  const goBack = useCallback(() => {
    if (path.length <= 1) return;
    setSlideDir(-1);
    setAnimKey((k) => k + 1);
    setPath((prev) => prev.slice(0, -1));
  }, [path]);

  // Compute breadcrumb labels
  const breadcrumbs = path.map((n, i) => ({
    label: n.name,
    isLast: i === path.length - 1,
    onClick: i < path.length - 1 ? () => setPath(path.slice(0, i + 1)) : undefined,
  }));

  const theme = category.theme;

  return (
    <div className="pop-backdrop" onClick={onClose}>
      <div
        className="pop"
        data-theme={theme}
        role="dialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Theme stripe */}
        <div className="pop-stripe">
          <i /><i /><i /><i />
        </div>

        {/* Close button */}
        <button className="pop-close" onClick={onClose} aria-label="Close">
          &#x2715;
        </button>

        {/* Header */}
        <div className="pop-head">
          <span className="pop-kicker">Category</span>
          <h2 className="pop-title">{category.name}</h2>
          {!isAtRoot && (
            <nav className="pop-breadcrumbs">
              {breadcrumbs.map((b, i) => (
                <span key={i}>
                  {i > 0 && <span className="pop-crumb-sep">/</span>}
                  {b.onClick ? (
                    <button className="pop-crumb-link" onClick={b.onClick}>
                      {b.label}
                    </button>
                  ) : (
                    <span className="pop-crumb-current">{b.label}</span>
                  )}
                </span>
              ))}
            </nav>
          )}
        </div>

        {/* Content with slide transition */}
        <div className="pop-content-wrap">
          <div
            ref={contentRef}
            className="pop-content"
            key={animKey}
            data-slide={slideDir === 1 ? "in-right" : "in-left"}
          >
            {/* Random option — always at the top */}
            <button className="pop-random" onClick={goRandom}>
              <span className="pop-random-mark">&#x2726;</span>
              <span className="pop-random-text">
                <span className="pop-random-k">Feeling lucky</span>
                <span className="pop-random-t">Random question</span>
                <span className="pop-random-s">
                  Pulls from every set in {currentNode.name}
                </span>
              </span>
              <span className="pop-random-arr">&rarr;</span>
            </button>

            {/* Divider */}
            {options.length > 0 && (
              <div className="pop-or">
                <span>or pick a set</span>
              </div>
            )}

            {/* Options list */}
            <div className="pop-list">
              {options.map((node) => {
                const hasChildren = node.children && node.children.length > 0;
                return (
                  <button
                    key={node.id}
                    className="pop-row"
                    onClick={() => goTo(node)}
                  >
                    <span className="pop-row-main">
                      <span className="pop-row-name">{node.name}</span>
                      {hasChildren && (
                        <span className="pop-row-sub">
                          {node.children!.length} sub-options
                        </span>
                      )}
                    </span>
                    <span className="pop-row-stat">
                      {node.questionCount != null && (
                        <>
                          <span className="pop-row-count">{node.questionCount}</span>
                          <span className="pop-row-count-l">questions</span>
                        </>
                      )}
                    </span>
                    <span className="pop-row-arr">
                      {hasChildren ? "→" : "↵"}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Back button (when not at root) */}
        {!isAtRoot && (
          <div className="pop-foot">
            <button className="pop-back-btn" onClick={goBack}>
              <span>&larr;</span> Back
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
