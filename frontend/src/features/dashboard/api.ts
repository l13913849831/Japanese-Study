import { getJson } from "@/shared/api/http";

export interface DashboardOverview {
  date: string;
  activePlanCount: number;
  totalDueToday: number;
  newDueToday: number;
  reviewDueToday: number;
  pendingDueToday: number;
  reviewedToday: number;
}

export interface DashboardPlanSummary {
  planId: number;
  planName: string;
  status: string;
  startDate: string;
  dailyNewCount: number;
  totalCards: number;
  completedCards: number;
  completionRate: number;
  dueToday: number;
  newToday: number;
  reviewToday: number;
  pendingToday: number;
  reviewedToday: number;
}

export interface DashboardTrendItem {
  date: string;
  newCards: number;
  reviewCards: number;
  reviewedCards: number;
}

export interface StudyDashboard {
  overview: DashboardOverview;
  activePlans: DashboardPlanSummary[];
  recentTrend: DashboardTrendItem[];
}

export function getStudyDashboard(date: string) {
  return getJson<StudyDashboard>(`/dashboard?date=${date}`);
}
