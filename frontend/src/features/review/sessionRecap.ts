import type { ReviewRating } from "@/features/cards/api";

export type ReviewRecapRating = ReviewRating;

export interface ReviewSessionRecapEvent {
  rating: ReviewRecapRating;
  responseTimeMs?: number;
  queueMode: string;
  todayAction: string;
  reviewedAtMs: number;
}

export interface ReviewSessionRecapSummary {
  totalCount: number;
  completedCount: number;
  ratingCounts: Record<ReviewRecapRating, number>;
  averageResponseTimeMs?: number;
  requeueCount: number;
  recoveryCount: number;
  weakAddedCount: number;
  sessionDurationMs: number;
}

const REVIEW_RATINGS: ReviewRecapRating[] = ["AGAIN", "HARD", "GOOD", "EASY"];

export function buildReviewSessionRecap(
  events: ReviewSessionRecapEvent[],
  totalCount: number,
  sessionStartedAtMs: number,
  nowMs = Date.now()
): ReviewSessionRecapSummary {
  const responseTimes = events
    .map((event) => event.responseTimeMs)
    .filter((value): value is number => typeof value === "number" && Number.isFinite(value));

  return {
    totalCount,
    completedCount: events.length,
    ratingCounts: REVIEW_RATINGS.reduce<Record<ReviewRecapRating, number>>((accumulator, rating) => {
      accumulator[rating] = events.filter((event) => event.rating === rating).length;
      return accumulator;
    }, { AGAIN: 0, HARD: 0, GOOD: 0, EASY: 0 }),
    averageResponseTimeMs: responseTimes.length
      ? Math.round(responseTimes.reduce((total, value) => total + value, 0) / responseTimes.length)
      : undefined,
    requeueCount: events.filter((event) => event.todayAction === "REQUEUE_TODAY").length,
    recoveryCount: events.filter((event) => event.todayAction === "MOVE_TO_RECOVERY_QUEUE").length,
    weakAddedCount: events.filter((event) => event.todayAction === "MOVE_TO_WEAK_ROUND").length,
    sessionDurationMs: Math.max(nowMs - sessionStartedAtMs, 0)
  };
}

export function formatDurationMs(durationMs: number) {
  const totalSeconds = Math.max(Math.round(durationMs / 1000), 0);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`;
}
