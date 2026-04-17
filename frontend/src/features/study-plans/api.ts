import { getJson, postJson, putJson } from "@/shared/api/http";
import type { PageResponse } from "@/shared/api/types";

export type StudyPlanStatus = "DRAFT" | "ACTIVE" | "PAUSED" | "ARCHIVED";

export interface StudyPlan {
  id: number;
  name: string;
  wordSetId: number;
  startDate: string;
  dailyNewCount: number;
  reviewOffsets: number[];
  ankiTemplateId?: number;
  mdTemplateId?: number;
  status: StudyPlanStatus;
  createdAt: string;
  updatedAt: string;
}

export function listStudyPlans() {
  return getJson<PageResponse<StudyPlan>>("/study-plans");
}

export function getStudyPlan(id: number) {
  return getJson<StudyPlan>(`/study-plans/${id}`);
}

export interface StudyPlanPayload {
  name: string;
  wordSetId: number;
  startDate: string;
  dailyNewCount: number;
  reviewOffsets: number[];
  ankiTemplateId?: number;
  mdTemplateId?: number;
}

export function createStudyPlan(payload: StudyPlanPayload) {
  return postJson<StudyPlan, StudyPlanPayload>("/study-plans", payload);
}

export function updateStudyPlan(id: number, payload: StudyPlanPayload) {
  return putJson<StudyPlan, StudyPlanPayload>(`/study-plans/${id}`, payload);
}

export function activateStudyPlan(id: number) {
  return postJson<StudyPlan, Record<string, never>>(`/study-plans/${id}/activate`, {});
}

export function pauseStudyPlan(id: number) {
  return postJson<StudyPlan, Record<string, never>>(`/study-plans/${id}/pause`, {});
}

export function archiveStudyPlan(id: number) {
  return postJson<StudyPlan, Record<string, never>>(`/study-plans/${id}/archive`, {});
}
