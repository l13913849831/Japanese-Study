import { useEffect, useRef, useState } from "react";
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
import { getTodayCards, submitCardReview, type ReviewRating } from "@/features/cards/api";

definePageConfig({
  navigationBarTitleText: "单词复习"
});

const ratingLabels: Array<{ rating: ReviewRating; label: string }> = [
  { rating: "AGAIN", label: "再来" },
  { rating: "HARD", label: "困难" },
  { rating: "GOOD", label: "记得" },
  { rating: "EASY", label: "轻松" }
];

function readPlanId(planId?: string) {
  if (!planId) {
    return undefined;
  }

  const parsed = Number(planId);
  return Number.isFinite(parsed) ? parsed : undefined;
}

export default function CardsPage() {
  const authenticated = useAuthGuard();
  const routeParams = useRouteParams();
  const date = typeof routeParams.date === "string" ? routeParams.date : formatLocalDate();
  const [selectedPlanId, setSelectedPlanId] = useState<number | undefined>(() => readPlanId(routeParams.planId));
  const [currentIndex, setCurrentIndex] = useState(0);
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
    enabled: authenticated && Boolean(selectedPlanId)
  });

  const cards = cardsQuery.data ?? [];
  const currentCard = cards[currentIndex];

  const reviewMutation = useMutation({
    mutationFn: (rating: ReviewRating) => {
      if (!currentCard) {
        throw new Error("当前没有可提交的单词卡");
      }

      return submitCardReview(currentCard.id, {
        rating,
        responseTimeMs: Date.now() - startedAtRef.current,
        sessionAgainCount: 0
      });
    },
    onSuccess: () => {
      startedAtRef.current = Date.now();
      setCurrentIndex((value) => value + 1);
      void queryClient.invalidateQueries({ queryKey: ["todayCards", selectedPlanId, date] });
      void queryClient.invalidateQueries({ queryKey: ["dashboard", date] });
    },
    onError: (error) => {
      void Taro.showToast({ title: getErrorMessage(error), icon: "none" });
    }
  });

  if (!authenticated) {
    return null;
  }

  return (
    <AppPage title="单词复习" subtitle={`${date} 的单词复习。第一版先复用后端今日卡片队列。`}>
      {dashboardQuery.isLoading || cardsQuery.isLoading ? <StateView title="加载中" body="正在读取复习队列。" /> : null}
      {dashboardQuery.error || cardsQuery.error ? (
        <StateView title="加载失败" body={getErrorMessage(dashboardQuery.error ?? cardsQuery.error)} />
      ) : null}

      {!selectedPlanId && !dashboardQuery.isLoading ? (
        <StateView title="没有可用学习计划" body="请先在 Web 端创建并激活学习计划。" />
      ) : null}

      {selectedPlanId && cards.length === 0 && !cardsQuery.isLoading ? (
        <StateView title="今日单词已完成" body="当前计划今天没有待复习单词。" actionText="回到工作台" onAction={relaunchDashboard} />
      ) : null}

      {currentCard ? (
        <View className="app-card app-card--accent">
          <Text className="app-card__title">{currentCard.expression ?? "未命名单词"}</Text>
          <Text className="app-card__body">
            {currentCard.reading ? `${currentCard.reading}\n` : ""}
            {currentCard.meaning ?? "暂无释义"}
          </Text>
          {currentCard.exampleJp ? <Text className="app-card__body">{currentCard.exampleJp}</Text> : null}
          {currentCard.exampleZh ? <Text className="app-card__body">{currentCard.exampleZh}</Text> : null}
          <Text className="app-card__body">
            {currentIndex + 1} / {cards.length}
          </Text>
          <View className="action-row">
            {ratingLabels.map((item) => (
              <Button
                key={item.rating}
                className={item.rating === "AGAIN" ? "danger-button" : "secondary-button"}
                loading={reviewMutation.isPending}
                disabled={reviewMutation.isPending}
                onClick={() => reviewMutation.mutate(item.rating)}
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
