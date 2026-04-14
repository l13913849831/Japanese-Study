import { getJson } from "@/shared/api/http";

export interface TodayCard {
  id: number;
  planId: number;
  wordEntryId: number;
  cardType: string;
  sequenceNo: number;
  stageNo: number;
  dueDate: string;
  status: string;
  expression?: string;
  reading?: string;
  meaning?: string;
  exampleJp?: string;
  exampleZh?: string;
}

export function getTodayCards(planId: number, date: string) {
  return getJson<TodayCard[]>(`/study-plans/${planId}/cards/today?date=${date}`);
}
