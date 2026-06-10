export type NewWordLoadAdviceAction = "NONE" | "KEEP" | "REDUCE" | "PAUSE";

export interface NewWordLoadAdvicePlan {
  planName: string;
  dailyNewCount: number;
  pendingToday: number;
  reviewedToday: number;
}

export interface NewWordLoadAdviceInput {
  plans: NewWordLoadAdvicePlan[];
  todayWordPendingCount: number;
  todayNotePendingCount: number;
  next7DayTotalDue: number;
  next14DayTotalDue: number;
  next30DayTotalDue: number;
  reviewedLast7Days?: number;
}

export interface NewWordLoadAdvice {
  action: NewWordLoadAdviceAction;
  title: string;
  reason: string;
  detail: string;
}

export function resolveNewWordLoadAdvice(input: NewWordLoadAdviceInput): NewWordLoadAdvice {
  const totalDailyNew = input.plans.reduce((total, plan) => total + plan.dailyNewCount, 0);
  const totalPendingToday = input.todayWordPendingCount + input.todayNotePendingCount;
  const untouchedPlans = input.plans.filter((plan) => plan.pendingToday > 0 && plan.reviewedToday === 0);
  const weeklyNewTarget = totalDailyNew * 7;
  const reduceThreshold = Math.max(weeklyNewTarget, 25);
  const pauseThreshold = Math.max(weeklyNewTarget * 2, 40);

  if (!input.plans.length || totalDailyNew <= 0) {
    return {
      action: "NONE",
      title: "暂无新词负载建议",
      reason: "当前没有激活的新词计划。",
      detail: "先保持复习主路径，创建并激活学习计划后再评估 dailyNewCount。"
    };
  }

  if (totalPendingToday >= 20 || input.next7DayTotalDue >= pauseThreshold) {
    return {
      action: "PAUSE",
      title: "建议暂停新词引入",
      reason: `今天还有 ${totalPendingToday} 项待处理，未来 7 天预计 ${input.next7DayTotalDue} 项到期。`,
      detail: "先清主复习和弱项，等未来 7 天负载回落后再恢复 dailyNewCount。"
    };
  }

  if (totalPendingToday >= 10 || input.next7DayTotalDue >= reduceThreshold || untouchedPlans.length > 0) {
    return {
      action: "REDUCE",
      title: "建议降低新词数量",
      reason: `当前新词日目标合计 ${totalDailyNew}，未来 14 天预计 ${input.next14DayTotalDue} 项到期。`,
      detail: untouchedPlans.length
        ? `有 ${untouchedPlans.length} 个计划今天尚未推进，先降低 dailyNewCount 并清掉积压。`
        : "把 dailyNewCount 临时降到当前的一半，直到今日积压稳定低于 10 项。"
    };
  }

  return {
    action: "KEEP",
    title: "可以保持当前新词节奏",
    reason: `今天剩余 ${totalPendingToday} 项，未来 30 天预计 ${input.next30DayTotalDue} 项到期。`,
    detail: "当前负载没有明显过载信号，可以按原 dailyNewCount 继续。"
  };
}

export function getNewWordLoadAdviceAlertType(action: NewWordLoadAdviceAction) {
  if (action === "PAUSE") {
    return "error";
  }
  if (action === "REDUCE") {
    return "warning";
  }
  if (action === "KEEP") {
    return "success";
  }
  return "info";
}
