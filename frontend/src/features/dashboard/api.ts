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

export interface LongTermLearningSummary {
  date: string;
  rangeDays: number;
  currentStreakDays: number;
  longestStreakDays: number;
  reviewedLast7Days: number;
  wordReviewedLast7Days: number;
  noteReviewedLast7Days: number;
  reviewedLast30Days: number;
  wordReviewedLast30Days: number;
  noteReviewedLast30Days: number;
}

export interface LongTermTrendItem {
  date: string;
  wordReviews: number;
  noteReviews: number;
  totalReviews: number;
}

export interface LongTermLoadBucket {
  days: number;
  wordDue: number;
  noteDue: number;
  totalDue: number;
}

export interface LongTermLoadForecast {
  next7Days: LongTermLoadBucket;
  next14Days: LongTermLoadBucket;
  next30Days: LongTermLoadBucket;
}

export interface LongTermDashboard {
  summary: LongTermLearningSummary;
  trend: LongTermTrendItem[];
  loadForecast: LongTermLoadForecast;
}

export function getStudyDashboard(date: string) {
  return getJson<StudyDashboard>(`/dashboard?date=${date}`);
}

export function getLongTermDashboard(date: string, rangeDays = 90) {
  return getJson<LongTermDashboard>(`/dashboard/long-term?date=${date}&rangeDays=${rangeDays}`);
}
