import { getJson } from "@/shared/api/http";
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
