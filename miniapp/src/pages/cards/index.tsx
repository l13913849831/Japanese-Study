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
import { getStudyDashboard } from "@/features/dashboard/api";
import { getTodayCards, submitCardReview, type ReviewRating, type TodayCard } from "@/features/cards/api";
import { buildReviewSessionSummary, resolveCurrentSessionIndex } from "@/features/review/session";

definePageConfig({
  navigationBarTitleText: "单词复习"
});

type SessionCardMode = "MAIN" | "REQUEUE" | "WEAK";

interface SessionCardRow {
  rowKey: string;
  cardId: number;
  mode: SessionCardMode;
}

const ratingLabels: Array<{ rating: ReviewRating; label: string }> = [
  { rating: "AGAIN", label: "再来" },
  { rating: "HARD", label: "困难" },
  { rating: "GOOD", label: "记得" },
  { rating: "EASY", label: "轻松" }
];

const queueModeLabels: Record<SessionCardMode, string> = {
  MAIN: "主队列",
  REQUEUE: "回捞",
  WEAK: "薄弱轮"
};

function readPlanId(planId?: string) {
  if (!planId) {
    return undefined;
  }

  const parsed = Number(planId);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function sortCards(items: TodayCard[]) {
  return items.slice().sort((left, right) => {
    if (left.sequenceNo !== right.sequenceNo) {
      return left.sequenceNo - right.sequenceNo;
    }
    if (left.stageNo !== right.stageNo) {
      return left.stageNo - right.stageNo;
    }
    return left.id - right.id;
  });
}

function buildCardMap(items: TodayCard[]) {
  return items.reduce<Record<number, TodayCard>>((accumulator, item) => {
    accumulator[item.id] = item;
    return accumulator;
  }, {});
}

function resolveCardReviewToast(todayAction: "DONE" | "REQUEUE_TODAY" | "MOVE_TO_WEAK_ROUND") {
  if (todayAction === "REQUEUE_TODAY") {
    return "已加入今日回捞";
  }
  if (todayAction === "MOVE_TO_WEAK_ROUND") {
    return "已加入薄弱轮";
  }
  return "已记录";
}

export default function CardsPage() {
  const authenticated = useAuthGuard();
  const routeParams = useRouteParams();
  const date = typeof routeParams.date === "string" ? routeParams.date : formatLocalDate();
  const [selectedPlanId, setSelectedPlanId] = useState<number | undefined>(() => readPlanId(routeParams.planId));
  const [sessionCardsById, setSessionCardsById] = useState<Record<number, TodayCard>>({});
  const [mainQueue, setMainQueue] = useState<SessionCardRow[]>([]);
  const [weakQueue, setWeakQueue] = useState<SessionCardRow[]>([]);
  const [completedRowKeys, setCompletedRowKeys] = useState<string[]>([]);
  const [againCountByCardId, setAgainCountByCardId] = useState<Record<number, number>>({});
  const [weakRoundStarted, setWeakRoundStarted] = useState(false);
  const [weakRoundSkipped, setWeakRoundSkipped] = useState(false);
  const [currentRowKey, setCurrentRowKey] = useState<string>();
  const startedAtRef = useRef(Date.now());
  const queryClient = useQueryClient();

  const dashboardQuery = useQuery({
    queryKey: ["dashboard", date],
    queryFn: () => getStudyDashboard(date),
    enabled: authenticated
  });

  useEffect(() => {
    if (selectedPlanId || !dashboardQuery.data) {
      return;
    }

    const firstPlan =
      dashboardQuery.data.activePlans.find((plan) => plan.pendingToday > 0) ?? dashboardQuery.data.activePlans[0];
    setSelectedPlanId(firstPlan?.planId);
  }, [dashboardQuery.data, selectedPlanId]);

  const cardsQuery = useQuery({
    queryKey: ["todayCards", selectedPlanId, date],
    queryFn: () => getTodayCards(selectedPlanId ?? 0, date),
    enabled: authenticated && Boolean(selectedPlanId),
    refetchOnWindowFocus: false
  });

  const orderedCards = useMemo(() => sortCards(cardsQuery.data ?? []), [cardsQuery.data]);
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
  const currentCard = currentRow ? sessionCardsById[currentRow.cardId] : undefined;
  const pendingWeakCount = weakQueue.filter((item) => !completedRowKeySet.has(item.rowKey)).length;
  const shouldPromptWeakRound = !weakRoundStarted && !weakRoundSkipped && sessionSummary.pendingCount === 0 && weakQueue.length > 0;

  useEffect(() => {
    if (!selectedPlanId || !cardsQuery.isSuccess) {
      if (!selectedPlanId) {
        setSessionCardsById({});
        setMainQueue([]);
        setWeakQueue([]);
        setCompletedRowKeys([]);
        setAgainCountByCardId({});
        setWeakRoundStarted(false);
        setWeakRoundSkipped(false);
        setCurrentRowKey(undefined);
      }
      return;
    }

    setSessionCardsById(buildCardMap(orderedCards));
    setMainQueue(orderedCards.map((card) => ({ rowKey: `main-${card.id}`, cardId: card.id, mode: "MAIN" })));
    setWeakQueue([]);
    setCompletedRowKeys([]);
    setAgainCountByCardId({});
    setWeakRoundStarted(false);
    setWeakRoundSkipped(false);
    setCurrentRowKey(undefined);
    startedAtRef.current = Date.now();
  }, [cardsQuery.isSuccess, date, orderedCards, selectedPlanId]);

  useEffect(() => {
    const nextRowKey = resolvedCurrentIndex === -1 ? undefined : activeQueue[resolvedCurrentIndex]?.rowKey;
    if (nextRowKey !== currentRowKey) {
      setCurrentRowKey(nextRowKey);
      startedAtRef.current = Date.now();
    }
  }, [activeQueue, currentRowKey, resolvedCurrentIndex]);

  const reviewMutation = useMutation({
    mutationFn: ({
      cardId,
      rating,
      nextAgainCount
    }: {
      cardId: number;
      queueRowKey: string;
      rating: ReviewRating;
      nextAgainCount: number;
    }) =>
      submitCardReview(cardId, {
        rating,
        responseTimeMs: Date.now() - startedAtRef.current,
        sessionAgainCount: nextAgainCount
      }),
    onSuccess: async (result, variables) => {
      setCompletedRowKeys((previous) => (previous.includes(variables.queueRowKey) ? previous : [...previous, variables.queueRowKey]));

      if (result.rating === "AGAIN") {
        setAgainCountByCardId((previous) => ({
          ...previous,
          [result.cardId]: variables.nextAgainCount
        }));
      }

      if (result.todayAction === "REQUEUE_TODAY") {
        setMainQueue((previous) => {
          const rowKey = `requeue-${result.cardId}`;
          return previous.some((item) => item.rowKey === rowKey)
            ? previous
            : [...previous, { rowKey, cardId: result.cardId, mode: "REQUEUE" }];
        });
      }

      if (result.todayAction === "MOVE_TO_WEAK_ROUND") {
        setWeakQueue((previous) => {
          const rowKey = `weak-${result.cardId}`;
          return previous.some((item) => item.rowKey === rowKey)
            ? previous
            : [...previous, { rowKey, cardId: result.cardId, mode: "WEAK" }];
        });
      }

      setCurrentRowKey(undefined);
      startedAtRef.current = Date.now();
      void Taro.showToast({ title: resolveCardReviewToast(result.todayAction), icon: "success" });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
        queryClient.invalidateQueries({ queryKey: ["noteDashboard"] }),
        queryClient.invalidateQueries({ queryKey: ["weakItemSummary"] }),
        queryClient.invalidateQueries({ queryKey: ["weakWords"] })
      ]);
    },
    onError: (error) => {
      void Taro.showToast({ title: getErrorMessage(error), icon: "none" });
    }
  });

  function submitReview(rating: ReviewRating) {
    if (!currentCard || !currentRow) {
      void Taro.showToast({ title: "当前没有可提交的单词卡", icon: "none" });
      return;
    }

    const nextAgainCount = rating === "AGAIN" ? (againCountByCardId[currentCard.id] ?? 0) + 1 : againCountByCardId[currentCard.id] ?? 0;
    reviewMutation.mutate({
      cardId: currentCard.id,
      queueRowKey: currentRow.rowKey,
      rating,
      nextAgainCount
    });
  }

  function startWeakRound() {
    setWeakRoundStarted(true);
    setCurrentRowKey(undefined);
    startedAtRef.current = Date.now();
  }

  if (!authenticated) {
    return null;
  }

  return (
    <AppPage title="单词复习" subtitle={`${date} 的单词复习。答错会在本轮回捞，连续答错会进入薄弱轮。`}>
      {dashboardQuery.isLoading || cardsQuery.isLoading ? <StateView title="加载中" body="正在读取复习队列。" /> : null}
      {dashboardQuery.error || cardsQuery.error ? (
        <StateView title="加载失败" body={getErrorMessage(dashboardQuery.error ?? cardsQuery.error)} />
      ) : null}

      {!selectedPlanId && !dashboardQuery.isLoading ? (
        <StateView title="没有可用学习计划" body="请先在 Web 端创建并激活学习计划。" />
      ) : null}

      {shouldPromptWeakRound ? (
        <View className="app-card">
          <Text className="app-card__title">主队列已完成</Text>
          <Text className="app-card__body">还有 {pendingWeakCount} 个薄弱项可以马上再练一轮。</Text>
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

      {selectedPlanId && sessionSummary.totalCount === 0 && !cardsQuery.isLoading ? (
        <StateView title="今日单词已完成" body="当前计划今天没有待复习单词。" actionText="回到工作台" onAction={relaunchDashboard} />
      ) : null}

      {sessionSummary.totalCount > 0 && sessionSummary.pendingCount === 0 && !shouldPromptWeakRound ? (
        <StateView title="当前单词会话已完成" body="主队列和已选择的薄弱轮都已处理。" actionText="回到工作台" onAction={relaunchDashboard} />
      ) : null}

      {currentCard && currentRow && sessionSummary.pendingCount > 0 ? (
        <View className="app-card app-card--accent">
          <Text className="app-card__title">{currentCard.expression ?? "未命名单词"}</Text>
          <Text className="app-card__body">
            {currentCard.reading ? `${currentCard.reading}\n` : ""}
            {currentCard.meaning ?? "暂无释义"}
          </Text>
          {currentCard.exampleJp ? <Text className="app-card__body">{currentCard.exampleJp}</Text> : null}
          {currentCard.exampleZh ? <Text className="app-card__body">{currentCard.exampleZh}</Text> : null}
          <Text className="app-card__body">
            {queueModeLabels[currentRow.mode]} · 待处理 {sessionSummary.pendingCount} · 已完成 {sessionSummary.completedCount} · 薄弱轮 {pendingWeakCount}
          </Text>
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
        </View>
      ) : null}
    </AppPage>
  );
}
