import { deleteJson, getJson, postFormData, postJson, putJson } from "@/shared/api/http";
import type { PageResponse } from "@/shared/api/types";

export interface WordSet {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWordSetPayload {
  name: string;
  description?: string;
}

export interface WordEntry {
  id: number;
  wordSetId: number;
  expression: string;
  reading?: string;
  meaning: string;
  partOfSpeech?: string;
  exampleJp?: string;
  exampleZh?: string;
  level?: string;
  tags: string[];
  sourceOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface WordEntryPayload {
  expression: string;
  reading?: string;
  meaning: string;
  partOfSpeech?: string;
  exampleJp?: string;
  exampleZh?: string;
  level?: string;
  tags: string[];
}

export interface WordEntryFilters {
  page?: number;
  pageSize?: number;
  keyword?: string;
  level?: string;
  tag?: string;
}

export interface DeleteWordEntryResult {
  deleted: boolean;
}

export interface WordEntryImportError {
  lineNumber: number;
  field: string;
  message: string;
}

export interface WordEntryImportResult {
  importedCount: number;
  skippedCount: number;
  errors: WordEntryImportError[];
}

export function listWordSets() {
  return getJson<PageResponse<WordSet>>("/word-sets");
}

export function createWordSet(payload: CreateWordSetPayload) {
  return postJson<WordSet, CreateWordSetPayload>("/word-sets", payload);
}

export function listWordEntries(wordSetId: number, filters: WordEntryFilters = {}) {
  const params = new URLSearchParams();
  params.set("page", String(filters.page ?? 1));
  params.set("pageSize", String(filters.pageSize ?? 20));

  if (filters.keyword) {
    params.set("keyword", filters.keyword);
  }
  if (filters.level) {
    params.set("level", filters.level);
  }
  if (filters.tag) {
    params.set("tag", filters.tag);
  }

  return getJson<PageResponse<WordEntry>>(`/word-sets/${wordSetId}/words?${params.toString()}`);
}

export function importWordEntries(wordSetId: number, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return postFormData<WordEntryImportResult>(`/word-sets/${wordSetId}/import`, formData);
}

export function createWordEntry(wordSetId: number, payload: WordEntryPayload) {
  return postJson<WordEntry, WordEntryPayload>(`/word-sets/${wordSetId}/words`, payload);
}

export function updateWordEntry(wordId: number, payload: WordEntryPayload) {
  return putJson<WordEntry, WordEntryPayload>(`/words/${wordId}`, payload);
}

export function deleteWordEntry(wordId: number) {
  return deleteJson<DeleteWordEntryResult>(`/words/${wordId}`);
}
