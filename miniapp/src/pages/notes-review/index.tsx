import { useEffect, useMemo, useRef, useState } from "react";
import Taro from "@tarojs/taro";
import { Button, Text, View } from "@tarojs/components";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppPage } from "@/shared/components/AppPage";
import { StateView } from "@/shared/components/StateView";
import { formatLocalDate } from "@/shared/date";
import { getErrorMessage } from "@/shared/errors";
import { useAuthGuard } from "@/shared/hooks/use-auth-guard";
import { useRouteParams } from "@/shared/hooks/use-route-params";
import { relaunchDashboard } from "@/shared/routes";
import {
  getTodayNoteReviews,
  submitNoteReview,
  type NoteReviewQueueItem,
  type NoteReviewRating
} from "@/features/notes/api";
import { buildReviewSessionSummary, resolveCurrentSessionIndex } from "@/features/review/session";

definePageConfig({
  navigationBarTitleText: "知识卡复习"
});

type SessionNoteMode = "MAIN" | "RECOVERY" | "WEAK";

interface SessionNoteRow {
  rowKey: string;
  noteId: number;
  mode: SessionNoteMode;
}

const ratingLabels: Array<{ rating: NoteReviewRating; label: string }> = [
  { rating: "AGAIN", label: "再来" },
  { rating: "HARD", label: "困难" },
  { rating: "GOOD", label: "记得" },
  { rating: "EASY", label: "轻松" }
];

const queueModeLabels: Record<SessionNoteMode, string> = {
  MAIN: "主队列",
  RECOVERY: "恢复",
  WEAK: "薄弱轮"
};

function sortNotes(items: NoteReviewQueueItem[]) {
  return items.slice().sort((left, right) => {
    const dueCompare = Date.parse(left.dueAt) - Date.parse(right.dueAt);
    if (dueCompare !== 0) {
      return dueCompare;
    }
    return left.id - right.id;
  });
}

function buildNoteMap(items: NoteReviewQueueItem[]) {
  return items.reduce<Record<number, NoteReviewQueueItem>>((accumulator, item) => {
    accumulator[item.id] = item;
    return accumulator;
  }, {});
}

function resolveNoteReviewToast(todayAction: "DONE" | "MOVE_TO_RECOVERY_QUEUE" | "MOVE_TO_WEAK_ROUND") {
  if (todayAction === "MOVE_TO_RECOVERY_QUEUE") {
    return "已加入恢复队列";
  }
  if (todayAction === "MOVE_TO_WEAK_ROUND") {
    return "已加入薄弱轮";
  }
  return "已记录";
}

export default function NotesReviewPage() {
  const authenticated = useAuthGuard();
  const routeParams = useRouteParams();
  const date = typeof routeParams.date === "string" ? routeParams.date : formatLocalDate();
  const [sessionNotesById, setSessionNotesById] = useState<Record<number, NoteReviewQueueItem>>({});
  const [mainQueue, setMainQueue] = useState<SessionNoteRow[]>([]);
  const [weakQueue, setWeakQueue] = useState<SessionNoteRow[]>([]);
  const [completedRowKeys, setCompletedRowKeys] = useState<string[]>([]);
  const [againCountByNoteId, setAgainCountByNoteId] = useState<Record<number, number>>({});
  const [weakRoundStarted, setWeakRoundStarted] = useState(false);
  const [weakRoundSkipped, setWeakRoundSkipped] = useState(false);
  const [currentRowKey, setCurrentRowKey] = useState<string>();
  const [revealed, setRevealed] = useState(false);
  const startedAtRef = useRef(Date.now());
  const queryClient = useQueryClient();

  const notesQuery = useQuery({
    queryKey: ["todayNoteReviews", date],
    queryFn: () => getTodayNoteReviews(date),
    enabled: authenticated,
    refetchOnWindowFocus: false
  });

  const orderedNotes = useMemo(() => sortNotes(notesQuery.data ?? []), [notesQuery.data]);
  const completedRowKeySet = useMemo(() => new Set(completedRowKeys), [completedRowKeys]);
  const activeQueue = weakRoundStarted ? weakQueue : mainQueue;
  const sessionSummary = useMemo(
    () => buildReviewSessionSummary(activeQueue, (item) => !completedRowKeySet.has(item.rowKey)),
    [activeQueue, completedRowKeySet]
  );
  const resolvedCurrentIndex = useMemo(
    () => resolveCurrentSessionIndex(activeQueue, currentRowKey, (item) => item.rowKey, (item) => !completedRowKeySet.has(item.rowKey)),
    [activeQueue, completedRowKeySet, currentRowKey]
  );
  const currentRow = resolvedCurrentIndex === -1 ? undefined : activeQueue[resolvedCurrentIndex];
  const currentNote = currentRow ? sessionNotesById[currentRow.noteId] : undefined;
  const pendingWeakCount = weakQueue.filter((item) => !completedRowKeySet.has(item.rowKey)).length;
  const shouldPromptWeakRound = !weakRoundStarted && !weakRoundSkipped && sessionSummary.pendingCount === 0 && weakQueue.length > 0;

  useEffect(() => {
    if (!notesQuery.isSuccess) {
      return;
    }

    setSessionNotesById(buildNoteMap(orderedNotes));
    setMainQueue(orderedNotes.map((note) => ({ rowKey: `main-${note.id}`, noteId: note.id, mode: "MAIN" })));
    setWeakQueue([]);
    setCompletedRowKeys([]);
    setAgainCountByNoteId({});
    setWeakRoundStarted(false);
    setWeakRoundSkipped(false);
    setCurrentRowKey(undefined);
    setRevealed(false);
    startedAtRef.current = Date.now();
  }, [date, notesQuery.isSuccess, orderedNotes]);

  useEffect(() => {
    const nextRowKey = resolvedCurrentIndex === -1 ? undefined : activeQueue[resolvedCurrentIndex]?.rowKey;
    if (nextRowKey !== currentRowKey) {
      setCurrentRowKey(nextRowKey);
      setRevealed(false);
      startedAtRef.current = Date.now();
    }
  }, [activeQueue, currentRowKey, resolvedCurrentIndex]);

  const reviewMutation = useMutation({
    mutationFn: ({
      noteId,
      rating,
      nextAgainCount
    }: {
      noteId: number;
      queueRowKey: string;
      rating: NoteReviewRating;
      nextAgainCount: number;
    }) =>
      submitNoteReview(noteId, {
        rating,
        responseTimeMs: Date.now() - startedAtRef.current,
        sessionAgainCount: nextAgainCount
      }),
    onSuccess: async (result, variables) => {
      setCompletedRowKeys((previous) => (previous.includes(variables.queueRowKey) ? previous : [...previous, variables.queueRowKey]));

      if (result.rating === "AGAIN") {
        setAgainCountByNoteId((previous) => ({
          ...previous,
          [result.noteId]: variables.nextAgainCount
        }));
      }

      setSessionNotesById((previous) => {
        const current = previous[result.noteId];
        if (!current) {
          return previous;
        }
        return {
          ...previous,
          [result.noteId]: {
            ...current,
            masteryStatus: result.masteryStatus,
            dueAt: result.dueAt,
            lastReviewedAt: result.reviewedAt,
            reviewCount: current.reviewCount + 1
          }
        };
      });

      if (result.todayAction === "MOVE_TO_RECOVERY_QUEUE") {
        setMainQueue((previous) => {
          const rowKey = `recovery-${result.noteId}`;
          return previous.some((item) => item.rowKey === rowKey)
            ? previous
            : [...previous, { rowKey, noteId: result.noteId, mode: "RECOVERY" }];
        });
      }

      if (result.todayAction === "MOVE_TO_WEAK_ROUND") {
        setWeakQueue((previous) => {
          const rowKey = `weak-${result.noteId}`;
          return previous.some((item) => item.rowKey === rowKey)
            ? previous
            : [...previous, { rowKey, noteId: result.noteId, mode: "WEAK" }];
        });
      }

      setCurrentRowKey(undefined);
      setRevealed(false);
      startedAtRef.current = Date.now();
      void Taro.showToast({ title: resolveNoteReviewToast(result.todayAction), icon: "success" });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["notes"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
        queryClient.invalidateQueries({ queryKey: ["noteDashboard"] }),
        queryClient.invalidateQueries({ queryKey: ["weakItemSummary"] }),
        queryClient.invalidateQueries({ queryKey: ["weakNotes"] })
      ]);
    },
    onError: (error) => {
      void Taro.showToast({ title: getErrorMessage(error), icon: "none" });
    }
  });

  function submitReview(rating: NoteReviewRating) {
    if (!currentNote || !currentRow) {
      void Taro.showToast({ title: "当前没有可提交的知识卡", icon: "none" });
      return;
    }

    const nextAgainCount = rating === "AGAIN" ? (againCountByNoteId[currentNote.id] ?? 0) + 1 : againCountByNoteId[currentNote.id] ?? 0;
    reviewMutation.mutate({
      noteId: currentNote.id,
      queueRowKey: currentRow.rowKey,
      rating,
      nextAgainCount
    });
  }

  function startWeakRound() {
    setWeakRoundStarted(true);
    setCurrentRowKey(undefined);
    setRevealed(false);
    startedAtRef.current = Date.now();
  }

  if (!authenticated) {
    return null;
  }

  return (
    <AppPage title="知识卡复习" subtitle={`${date} 的知识卡复习。先回忆标题，再揭示内容并评分。`}>
      {notesQuery.isLoading ? <StateView title="加载中" body="正在读取知识卡队列。" /> : null}
      {notesQuery.error ? <StateView title="加载失败" body={getErrorMessage(notesQuery.error)} /> : null}

      {shouldPromptWeakRound ? (
        <View className="app-card">
          <Text className="app-card__title">主队列已完成</Text>
          <Text className="app-card__body">还有 {pendingWeakCount} 个薄弱知识点可以马上再练一轮。</Text>
          <View className="action-row">
            <Button className="primary-button" onClick={startWeakRound}>
              开始薄弱轮
            </Button>
            <Button className="secondary-button" onClick={() => setWeakRoundSkipped(true)}>
              稍后再说
            </Button>
          </View>
        </View>
      ) : null}

      {sessionSummary.totalCount === 0 && !notesQuery.isLoading ? (
        <StateView title="今日知识卡已完成" body="当前没有到期知识卡。" actionText="回到工作台" onAction={relaunchDashboard} />
      ) : null}

      {sessionSummary.totalCount > 0 && sessionSummary.pendingCount === 0 && !shouldPromptWeakRound ? (
        <StateView title="当前知识卡会话已完成" body="主队列和已选择的薄弱轮都已处理。" actionText="回到工作台" onAction={relaunchDashboard} />
      ) : null}

      {currentNote && currentRow && sessionSummary.pendingCount > 0 ? (
        <View className="app-card app-card--accent">
          <Text className="app-card__title">{currentNote.title}</Text>
          <Text className="app-card__body">
            {currentNote.tags.length > 0 ? `标签：${currentNote.tags.join(" / ")}` : "暂无标签"}
          </Text>
          {revealed ? (
            <Text className="app-card__body">{currentNote.content}</Text>
          ) : (
            <View className="action-row">
              <Button className="primary-button" onClick={() => setRevealed(true)}>
                显示内容
              </Button>
            </View>
          )}
          <Text className="app-card__body">
            {queueModeLabels[currentRow.mode]} · 待处理 {sessionSummary.pendingCount} · 已完成 {sessionSummary.completedCount} · 薄弱轮 {pendingWeakCount}
          </Text>
          {revealed ? (
            <View className="action-row">
              {ratingLabels.map((item) => (
                <Button
                  key={item.rating}
                  className={item.rating === "AGAIN" ? "danger-button" : "secondary-button"}
                  loading={reviewMutation.isPending}
                  disabled={reviewMutation.isPending}
                  onClick={() => submitReview(item.rating)}
                >
                  {item.label}
                </Button>
              ))}
            </View>
          ) : null}
        </View>
      ) : null}
    </AppPage>
  );
}
