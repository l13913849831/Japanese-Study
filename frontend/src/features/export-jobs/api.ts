import { downloadFile, getJson, postJson } from "@/shared/api/http";
import type { PageResponse } from "@/shared/api/types";

export interface ExportJob {
  id: number;
  planId: number;
  exportType: string;
  targetDate?: string;
  fileName?: string;
  filePath?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export function listExportJobs() {
  return getJson<PageResponse<ExportJob>>("/export-jobs");
}

export interface CreateExportJobPayload {
  planId: number;
  exportType: string;
  targetDate: string;
}

export interface ExportJobPreflight {
  planId: number;
  planName: string;
  exportType: string;
  targetDate: string;
  totalCards: number;
  newCards: number;
  reviewCards: number;
  markdownTemplateName: string | null;
  creatable: boolean;
  message: string;
}

export function createExportJob(payload: CreateExportJobPayload) {
  return postJson<ExportJob, CreateExportJobPayload>("/export-jobs", payload);
}

export function preflightExportJob(payload: CreateExportJobPayload) {
  return postJson<ExportJobPreflight, CreateExportJobPayload>("/export-jobs/preflight", payload);
}

export function downloadExportJob(id: number) {
  return downloadFile(`/export-jobs/${id}/download`);
}
