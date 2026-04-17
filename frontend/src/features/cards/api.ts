import { getJson, postJson } from "@/shared/api/http";

export interface TodayCard {
  id: number;
  planId: number;
  wordEntryId: number;
  cardType: string;
  sequenceNo: number;
  stageNo: number;
  dueDate: string;
  status: string;
  expression?: string;
  reading?: string;
  meaning?: string;
  exampleJp?: string;
  exampleZh?: string;
}

export type ReviewRating = "AGAIN" | "HARD" | "GOOD" | "EASY";

export interface ReviewCardPayload {
  rating: ReviewRating;
  responseTimeMs?: number;
  note?: string;
}

export interface ReviewCardResult {
  reviewId: number;
  cardId: number;
  rating: ReviewRating;
  cardStatus: string;
  reviewedAt: string;
}

export interface ReviewLogItem {
  id: number;
  cardInstanceId: number;
  reviewedAt: string;
  rating: ReviewRating;
  responseTimeMs?: number;
  note?: string;
  createdAt: string;
}

export function getTodayCards(planId: number, date: string) {
  return getJson<TodayCard[]>(`/study-plans/${planId}/cards/today?date=${date}`);
}

export function submitCardReview(cardId: number, payload: ReviewCardPayload) {
  return postJson<ReviewCardResult, ReviewCardPayload>(`/cards/${cardId}/review`, payload);
}

export function getCardReviews(cardId: number) {
  return getJson<ReviewLogItem[]>(`/cards/${cardId}/reviews`);
}
