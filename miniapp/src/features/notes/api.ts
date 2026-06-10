import { getJson, postJson } from "@/shared/api/http";

export type NoteMasteryStatus = "UNSTARTED" | "LEARNING" | "CONSOLIDATING" | "MASTERED";
export type NoteReviewRating = "AGAIN" | "HARD" | "GOOD" | "EASY";

export interface NoteReviewQueueItem {
  id: number;
  title: string;
  content: string;
  tags: string[];
  masteryStatus: NoteMasteryStatus;
  reviewCount: number;
  dueAt: string;
  lastReviewedAt?: string;
}

export interface ReviewNotePayload {
  rating: NoteReviewRating;
  responseTimeMs?: number;
  sessionAgainCount?: number;
  note?: string;
}

export interface ReviewNoteResult {
  reviewId: number;
  noteId: number;
  rating: NoteReviewRating;
  masteryStatus: NoteMasteryStatus;
  reviewedAt: string;
  dueAt: string;
  weak: boolean;
  weakMarkedAt?: string;
  todayAction: "DONE" | "MOVE_TO_RECOVERY_QUEUE" | "MOVE_TO_WEAK_ROUND";
}

export interface NoteDashboardOverview {
  date: string;
  dueToday: number;
  totalNotes: number;
  reviewedNotes: number;
}

export interface NoteDashboardMasteryItem {
  masteryStatus: NoteMasteryStatus;
  count: number;
}

export interface NoteDashboardTrendItem {
  date: string;
  reviewedNotes: number;
}

export interface RecentNoteItem {
  id: number;
  title: string;
  tags: string[];
  masteryStatus: NoteMasteryStatus;
  createdAt: string;
}

export interface NoteDashboard {
  overview: NoteDashboardOverview;
  masteryDistribution: NoteDashboardMasteryItem[];
  recentTrend: NoteDashboardTrendItem[];
  recentNotes: RecentNoteItem[];
}

export function getTodayNoteReviews(date: string) {
  return getJson<NoteReviewQueueItem[]>(`/notes/reviews/today?date=${date}`);
}

export function submitNoteReview(noteId: number, payload: ReviewNotePayload) {
  return postJson<ReviewNoteResult, ReviewNotePayload>(`/notes/${noteId}/reviews`, payload);
}

export function getNoteDashboard(date: string) {
  return getJson<NoteDashboard>(`/notes/dashboard?date=${date}`);
}
