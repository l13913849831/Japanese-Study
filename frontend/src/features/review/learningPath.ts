import type { PreferredLearningOrder } from "@/features/auth/api";

export type LearningLine = "WORD" | "NOTE";

export interface LearningPathCounts {
  wordPendingCount: number;
  notePendingCount: number;
}

export interface LearningPathState {
  recommendedLine?: LearningLine;
  followUpLine?: LearningLine;
  isComplete: boolean;
}

const LEARNING_LINE_LABELS: Record<LearningLine, string> = {
  WORD: "单词线",
  NOTE: "知识点线"
};

const LEARNING_LINE_SESSION_LABELS: Record<LearningLine, string> = {
  WORD: "单词复习",
  NOTE: "知识点复习"
};

export function getLearningLineLabel(line: LearningLine) {
  return LEARNING_LINE_LABELS[line];
}

export function getLearningLineSessionLabel(line: LearningLine) {
  return LEARNING_LINE_SESSION_LABELS[line];
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
  const activeLines = getLearningPathOrder(preferredLearningOrder).filter(
    (line) => getLearningLinePendingCount(line, counts) > 0
  );

  return {
    recommendedLine: activeLines[0],
    followUpLine: activeLines[1],
    isComplete: activeLines.length === 0
  };
}
