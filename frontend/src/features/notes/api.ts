import { deleteJson, getJson, postFormData, postJson, putJson } from "@/shared/api/http";
import type { PageResponse } from "@/shared/api/types";

export type NoteMasteryStatus = "UNSTARTED" | "LEARNING" | "CONSOLIDATING" | "MASTERED";
export type NoteReviewRating = "AGAIN" | "HARD" | "GOOD" | "EASY";
export type NoteImportSplitMode = "H1" | "H1_H2" | "ALL";

export interface Note {
  id: number;
  title: string;
  content: string;
  tags: string[];
  reviewCount: number;
  masteryStatus: NoteMasteryStatus;
  dueAt: string;
  lastReviewedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SaveNotePayload {
  title: string;
  content: string;
  tags: string[];
}

export interface NoteFilters {
  page?: number;
  pageSize?: number;
  keyword?: string;
  tag?: string;
  masteryStatus?: NoteMasteryStatus;
}

export interface NoteImportPreviewItem {
  itemId: string;
  title: string;
  content: string;
  tags: string[];
  status: "READY" | "ERROR";
  message: string;
}

export interface NoteImportPreviewResult {
  splitMode: NoteImportSplitMode;
  totalItems: number;
  readyCount: number;
  errorCount: number;
  previewItems: NoteImportPreviewItem[];
}

export interface NoteImportPayloadItem {
  title: string;
  content: string;
  tags: string[];
}

export interface NoteImportError {
  itemIndex: number;
  field: string;
  message: string;
}

export interface NoteImportResult {
  importedCount: number;
  skippedCount: number;
  errors: NoteImportError[];
}

export interface ReviewNotePayload {
  rating: NoteReviewRating;
  responseTimeMs?: number;
  note?: string;
}

export interface ReviewNoteResult {
  reviewId: number;
  noteId: number;
  rating: NoteReviewRating;
  masteryStatus: NoteMasteryStatus;
  reviewedAt: string;
  dueAt: string;
}

export interface NoteReviewLogItem {
  id: number;
  noteId: number;
  reviewedAt: string;
  rating: NoteReviewRating;
  responseTimeMs?: number;
  note?: string;
  createdAt: string;
}

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

export interface DeleteNoteResult {
  deleted: boolean;
}

export function listNotes(filters: NoteFilters = {}) {
  const params = new URLSearchParams();
  params.set("page", String(filters.page ?? 1));
  params.set("pageSize", String(filters.pageSize ?? 20));
  if (filters.keyword) {
    params.set("keyword", filters.keyword);
  }
  if (filters.tag) {
    params.set("tag", filters.tag);
  }
  if (filters.masteryStatus) {
    params.set("masteryStatus", filters.masteryStatus);
  }
  return getJson<PageResponse<Note>>(`/notes?${params.toString()}`);
}

export function createNote(payload: SaveNotePayload) {
  return postJson<Note, SaveNotePayload>("/notes", payload);
}

export function updateNote(noteId: number, payload: SaveNotePayload) {
  return putJson<Note, SaveNotePayload>(`/notes/${noteId}`, payload);
}

export function deleteNote(noteId: number) {
  return deleteJson<DeleteNoteResult>(`/notes/${noteId}`);
}

export function previewNoteImport(file: File, splitMode: NoteImportSplitMode, commonTagsText?: string) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("splitMode", splitMode);
  if (commonTagsText?.trim()) {
    formData.append("commonTagsText", commonTagsText.trim());
  }
  return postFormData<NoteImportPreviewResult>("/notes/import/preview", formData);
}

export function importNotes(items: NoteImportPayloadItem[]) {
  return postJson<NoteImportResult, { items: NoteImportPayloadItem[] }>("/notes/import", { items });
}

export function getTodayNoteReviews(date: string) {
  return getJson<NoteReviewQueueItem[]>(`/notes/reviews/today?date=${date}`);
}

export function submitNoteReview(noteId: number, payload: ReviewNotePayload) {
  return postJson<ReviewNoteResult, ReviewNotePayload>(`/notes/${noteId}/reviews`, payload);
}

export function getNoteReviews(noteId: number) {
  return getJson<NoteReviewLogItem[]>(`/notes/${noteId}/reviews`);
}

export function getNoteDashboard(date: string) {
  return getJson<NoteDashboard>(`/notes/dashboard?date=${date}`);
}
