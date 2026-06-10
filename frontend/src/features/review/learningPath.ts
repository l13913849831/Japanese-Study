import type { PreferredLearningOrder } from "@/features/auth/api";

export type LearningLine = "WORD" | "NOTE";
export type LearningPathRiskLevel = "LOW" | "MEDIUM" | "HIGH";

export interface LearningPathCounts {
  wordPendingCount: number;
  notePendingCount: number;
  weakWordCount?: number;
  weakNoteCount?: number;
  next7DayWordDue?: number;
  next7DayNoteDue?: number;
  todayWordReviewedCount?: number;
  todayNoteReviewedCount?: number;
}

export interface LearningPathState {
  recommendedLine?: LearningLine;
  followUpLine?: LearningLine;
  isComplete: boolean;
  recommendedReason: string;
  followUpReason?: string;
  riskLevel: LearningPathRiskLevel;
  riskReason: string;
}

const LEARNING_LINE_LABELS: Record<LearningLine, string> = {
  WORD: "单词线",
  NOTE: "知识点线"
};

const LEARNING_LINE_SESSION_LABELS: Record<LearningLine, string> = {
  WORD: "单词复习",
  NOTE: "知识点复习"
};

const LEARNING_PATH_RISK_LABELS: Record<LearningPathRiskLevel, string> = {
  LOW: "低风险",
  MEDIUM: "中风险",
  HIGH: "高风险"
};

const LEARNING_PATH_RISK_COLORS: Record<LearningPathRiskLevel, string> = {
  LOW: "green",
  MEDIUM: "gold",
  HIGH: "volcano"
};

export function getLearningLineLabel(line: LearningLine) {
  return LEARNING_LINE_LABELS[line];
}

export function getLearningLineSessionLabel(line: LearningLine) {
  return LEARNING_LINE_SESSION_LABELS[line];
}

export function getLearningPathRiskLabel(riskLevel: LearningPathRiskLevel) {
  return LEARNING_PATH_RISK_LABELS[riskLevel];
}

export function getLearningPathRiskColor(riskLevel: LearningPathRiskLevel) {
  return LEARNING_PATH_RISK_COLORS[riskLevel];
}

export function getLearningPathOrder(preferredLearningOrder: PreferredLearningOrder): LearningLine[] {
  return preferredLearningOrder === "NOTE_FIRST" ? ["NOTE", "WORD"] : ["WORD", "NOTE"];
}

export function getLearningLinePendingCount(line: LearningLine, counts: LearningPathCounts) {
  return line === "WORD" ? counts.wordPendingCount : counts.notePendingCount;
}

export function resolveLearningPathState(
  preferredLearningOrder: PreferredLearningOrder,
  counts: LearningPathCounts
): LearningPathState {
  const orderedLines = getLearningPathOrder(preferredLearningOrder);
  const activeLines = orderedLines.filter((line) => getLearningLinePendingCount(line, counts) > 0);
  const recommendedLine = resolveRecommendedLine(orderedLines, counts);
  const followUpLine = activeLines.find((line) => line !== recommendedLine);
  const riskLevel = resolveRiskLevel(counts);

  return {
    recommendedLine,
    followUpLine,
    isComplete: activeLines.length === 0,
    recommendedReason: buildRecommendedReason(recommendedLine, preferredLearningOrder, counts),
    followUpReason: followUpLine ? buildFollowUpReason(followUpLine, counts) : undefined,
    riskLevel,
    riskReason: buildRiskReason(riskLevel, counts)
  };
}

function resolveRecommendedLine(
  orderedLines: LearningLine[],
  counts: LearningPathCounts
): LearningLine | undefined {
  const wordPending = counts.wordPendingCount;
  const notePending = counts.notePendingCount;

  if (wordPending <= 0 && notePending <= 0) {
    return undefined;
  }
  if (wordPending > 0 && notePending <= 0) {
    return "WORD";
  }
  if (notePending > 0 && wordPending <= 0) {
    return "NOTE";
  }

  const pendingDiff = Math.abs(wordPending - notePending);
  if (pendingDiff >= 5) {
    return wordPending > notePending ? "WORD" : "NOTE";
  }

  const weakWordCount = counts.weakWordCount ?? 0;
  const weakNoteCount = counts.weakNoteCount ?? 0;
  if (Math.abs(weakWordCount - weakNoteCount) >= 3) {
    return weakWordCount > weakNoteCount ? "WORD" : "NOTE";
  }

  return orderedLines[0];
}

function resolveRiskLevel(counts: LearningPathCounts): LearningPathRiskLevel {
  const totalPending = counts.wordPendingCount + counts.notePendingCount;
  const totalWeak = (counts.weakWordCount ?? 0) + (counts.weakNoteCount ?? 0);
  const next7DayDue = (counts.next7DayWordDue ?? 0) + (counts.next7DayNoteDue ?? 0);

  if (totalPending >= 30 || totalWeak >= 10 || next7DayDue >= 80) {
    return "HIGH";
  }
  if (totalPending >= 10 || totalWeak > 0 || next7DayDue >= 30) {
    return "MEDIUM";
  }
  return "LOW";
}

function buildRecommendedReason(
  recommendedLine: LearningLine | undefined,
  preferredLearningOrder: PreferredLearningOrder,
  counts: LearningPathCounts
) {
  if (!recommendedLine) {
    return "两条主复习线都已清空，后续只需要按需处理薄弱项或导出复盘材料。";
  }

  const preferredLine = preferredLearningOrder === "NOTE_FIRST" ? "NOTE" : "WORD";
  const pending = getLearningLinePendingCount(recommendedLine, counts);
  const otherLine = recommendedLine === "WORD" ? "NOTE" : "WORD";
  const otherPending = getLearningLinePendingCount(otherLine, counts);

  if (Math.abs(pending - otherPending) >= 5) {
    return `${getLearningLineLabel(recommendedLine)}剩余 ${pending} 项，高于${getLearningLineLabel(otherLine)} ${otherPending} 项，先清更重的一边。`;
  }
  if (recommendedLine !== preferredLine) {
    return `${getLearningLineLabel(recommendedLine)}薄弱项或待处理量更高，暂时覆盖默认偏好。`;
  }
  return `两条线压力接近，按你的默认偏好先走${getLearningLineLabel(recommendedLine)}。`;
}

function buildFollowUpReason(followUpLine: LearningLine, counts: LearningPathCounts) {
  return `${getLearningLineLabel(followUpLine)}还剩 ${getLearningLinePendingCount(followUpLine, counts)} 项，当前线完成后继续接上。`;
}

function buildRiskReason(riskLevel: LearningPathRiskLevel, counts: LearningPathCounts) {
  const totalPending = counts.wordPendingCount + counts.notePendingCount;
  const totalWeak = (counts.weakWordCount ?? 0) + (counts.weakNoteCount ?? 0);
  const next7DayDue = (counts.next7DayWordDue ?? 0) + (counts.next7DayNoteDue ?? 0);
  const todayReviewed = (counts.todayWordReviewedCount ?? 0) + (counts.todayNoteReviewedCount ?? 0);

  if (riskLevel === "HIGH") {
    return `当前待处理 ${totalPending} 项，薄弱项 ${totalWeak} 项，未来 7 天负载 ${next7DayDue} 项，需要优先清主路径。`;
  }
  if (riskLevel === "MEDIUM") {
    return `当前待处理 ${totalPending} 项，薄弱项 ${totalWeak} 项，今天已完成 ${todayReviewed} 项，建议保留一段收尾时间。`;
  }
  return `当前主路径压力低，今天已完成 ${todayReviewed} 项，可以按偏好推进。`;
}
