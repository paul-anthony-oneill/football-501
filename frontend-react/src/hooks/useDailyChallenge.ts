"use client";

import { useState, useEffect } from "react";

export interface CategoryChallenge {
  categorySlug: string;
  categoryName: string;
  startingScore: number;
  questionText: string;
  hasChallenge: boolean;
}

export interface DailyChallengeState {
  date: string | null;
  challenges: CategoryChallenge[];
  loading: boolean;
  error: string | null;
}

export function useDailyChallenge(): DailyChallengeState & { refresh: () => void } {
  const [challenges, setChallenges] = useState<CategoryChallenge[]>([]);
  const [date, setDate] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  function fetchStatus() {
    setLoading(true);
    setError(null);

    fetch("/api/daily-challenge/status")
      .then(async (res) => {
        if (!res.ok) throw new Error("Failed to fetch daily challenges");
        return res.json();
      })
      .then((data) => {
        setDate(data.date ?? null);
        setChallenges(data.challenges ?? []);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message || "Error fetching daily challenges");
        setLoading(false);
      });
  }

  useEffect(() => {
    fetchStatus();
  }, []);

  return { date, challenges, loading, error, refresh: fetchStatus };
}
