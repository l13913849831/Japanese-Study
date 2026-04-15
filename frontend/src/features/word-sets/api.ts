import { getJson, postFormData, postJson } from "@/shared/api/http";
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

export function listWordEntries(wordSetId: number) {
  return getJson<PageResponse<WordEntry>>(`/word-sets/${wordSetId}/words`);
}

export function importWordEntries(wordSetId: number, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return postFormData<WordEntryImportResult>(`/word-sets/${wordSetId}/import`, formData);
}
