import { deleteJson, getJson, postJson } from "@/shared/api/http";

export type LearningLinkSource = "MANUAL" | "REVIEW";

export interface LearningLink {
  linkId: number;
  wordEntryId: number;
  expression: string;
  reading?: string;
  noteId: number;
  noteTitle: string;
  noteTags: string[];
  source: LearningLinkSource;
  createdAt: string;
}

export interface CreateLearningLinkPayload {
  wordEntryId: number;
  noteId: number;
  source: LearningLinkSource;
}

export interface DeleteLearningLinkResult {
  deleted: boolean;
}

export function createLearningLink(payload: CreateLearningLinkPayload) {
  return postJson<LearningLink, CreateLearningLinkPayload>("/learning-links", payload);
}

export function listLearningLinksByWordEntry(wordEntryId: number) {
  return getJson<LearningLink[]>(`/learning-links/words/${wordEntryId}`);
}

export function listLearningLinksByNote(noteId: number) {
  return getJson<LearningLink[]>(`/learning-links/notes/${noteId}`);
}

export function deleteLearningLink(linkId: number) {
  return deleteJson<DeleteLearningLinkResult>(`/learning-links/${linkId}`);
}
