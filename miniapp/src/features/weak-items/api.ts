import { getJson, postJson } from "@/shared/api/http";
import type { PageResponse } from "@/shared/api/types";

export interface WeakItemSummary {
  weakWordCount: number;
  weakNoteCount: number;
}

export interface WeakWordItem {
  cardId: number;
  planId: number;
  planName: string;
  wordEntryId: number;
  expression: string;
  reading?: string;
  meaning?: string;
  dueDate: string;
  lastReviewRating?: string;
  weakMarkedAt?: string;
}

export interface WeakNoteItem {
  noteId: number;
  title: string;
  tags: string[];
  masteryStatus: string;
  lastReviewRating?: string;
  weakMarkedAt?: string;
}

export interface DismissWeakItemResult {
  dismissed: boolean;
}

export function getWeakItemSummary() {
  return getJson<WeakItemSummary>("/weak-items/summary");
}

export function listWeakWords(page = 1, pageSize = 20) {
  return getJson<PageResponse<WeakWordItem>>(`/weak-items/words?page=${page}&pageSize=${pageSize}`);
}

export function listWeakNotes(page = 1, pageSize = 20) {
  return getJson<PageResponse<WeakNoteItem>>(`/weak-items/notes?page=${page}&pageSize=${pageSize}`);
}

export function dismissWeakWord(cardId: number) {
  return postJson<DismissWeakItemResult, Record<string, never>>(`/weak-items/words/${cardId}/dismiss`, {});
}

export function dismissWeakNote(noteId: number) {
  return postJson<DismissWeakItemResult, Record<string, never>>(`/weak-items/notes/${noteId}/dismiss`, {});
}
