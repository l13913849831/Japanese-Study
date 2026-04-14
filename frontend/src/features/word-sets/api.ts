import { getJson, postJson } from "@/shared/api/http";
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

export function listWordSets() {
  return getJson<PageResponse<WordSet>>("/word-sets");
}

export function createWordSet(payload: CreateWordSetPayload) {
  return postJson<WordSet, CreateWordSetPayload>("/word-sets", payload);
}
