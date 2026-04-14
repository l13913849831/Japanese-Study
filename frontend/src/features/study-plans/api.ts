import { getJson } from "@/shared/api/http";
import type { PageResponse } from "@/shared/api/types";

export interface StudyPlan {
  id: number;
  name: string;
  wordSetId: number;
  startDate: string;
  dailyNewCount: number;
  reviewOffsets: number[];
  ankiTemplateId?: number;
  mdTemplateId?: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export function listStudyPlans() {
  return getJson<PageResponse<StudyPlan>>("/study-plans");
}
