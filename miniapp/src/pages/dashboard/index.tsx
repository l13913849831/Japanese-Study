import { Button, Text, View } from "@tarojs/components";
import { useQuery } from "@tanstack/react-query";
import { AppPage } from "@/shared/components/AppPage";
import { MetricCard } from "@/shared/components/MetricCard";
import { StateView } from "@/shared/components/StateView";
import { formatLocalDate } from "@/shared/date";
import { getErrorMessage } from "@/shared/errors";
import { useAuthGuard } from "@/shared/hooks/use-auth-guard";
import { openAccount, openCards, openNotesReview, openWeakItems } from "@/shared/routes";
import { getLongTermDashboard, getStudyDashboard } from "@/features/dashboard/api";
import { getNoteDashboard } from "@/features/notes/api";
import { resolveNewWordLoadAdvice } from "@/features/review/newWordLoadAdvice";
import { getWeakItemSummary } from "@/features/weak-items/api";

definePageConfig({
  navigationBarTitleText: "今日学习"
});

export default function DashboardPage() {
  const authenticated = useAuthGuard();
  const date = formatLocalDate();

  const dashboardQuery = useQuery({
    queryKey: ["dashboard", date],
    queryFn: () => getStudyDashboard(date),
    enabled: authenticated
  });
  const noteDashboardQuery = useQuery({
    queryKey: ["noteDashboard", date],
    queryFn: () => getNoteDashboard(date),
    enabled: authenticated
  });
  const weakSummaryQuery = useQuery({
    queryKey: ["weakItemSummary"],
    queryFn: getWeakItemSummary,
    enabled: authenticated
  });
  const longTermQuery = useQuery({
    queryKey: ["dashboard", "long-term", date, 90],
    queryFn: () => getLongTermDashboard(date, 90),
    enabled: authenticated
  });

  if (!authenticated) {
    return null;
  }

  const dashboard = dashboardQuery.data;
  const noteDashboard = noteDashboardQuery.data;
  const weakSummary = weakSummaryQuery.data;
  const longTerm = longTermQuery.data;
  const firstPlan = dashboard?.activePlans.find((plan) => plan.pendingToday > 0) ?? dashboard?.activePlans[0];
  const loading = dashboardQuery.isLoading || noteDashboardQuery.isLoading;
  const error = dashboardQuery.error ?? noteDashboardQuery.error;
  const newWordLoadAdvice = resolveNewWordLoadAdvice({
    plans: dashboard?.activePlans ?? [],
    todayWordPendingCount: dashboard?.overview.pendingDueToday ?? 0,
    todayNotePendingCount: noteDashboard?.overview.dueToday ?? 0,
    next7DayTotalDue: longTerm?.loadForecast.next7Days.totalDue ?? 0,
    next14DayTotalDue: longTerm?.loadForecast.next14Days.totalDue ?? 0,
    next30DayTotalDue: longTerm?.loadForecast.next30Days.totalDue ?? 0,
    reviewedLast7Days: longTerm?.summary.reviewedLast7Days
  });

  return (
    <AppPage title="今日学习" subtitle={`${date} 的学习入口。先处理到期内容，再看弱项。`}>
      {loading ? <StateView title="加载中" body="正在读取今日学习数据。" /> : null}
      {error ? <StateView title="加载失败" body={getErrorMessage(error)} /> : null}

      {dashboard && noteDashboard ? (
        <>
          <View className="metric-grid">
            <MetricCard label="单词待复习" value={dashboard.overview.pendingDueToday} />
            <MetricCard label="知识卡待复习" value={noteDashboard.overview.dueToday} />
            <MetricCard label="今日已完成" value={dashboard.overview.reviewedToday + noteDashboard.overview.reviewedNotes} />
            <MetricCard
              label="弱项"
              value={(weakSummary?.weakWordCount ?? 0) + (weakSummary?.weakNoteCount ?? 0)}
            />
          </View>

          <View className="app-card app-card--accent">
            <Text className="app-card__title">下一步</Text>
            <Text className="app-card__body">
              {firstPlan
                ? `推荐先复习「${firstPlan.planName}」，当前还有 ${firstPlan.pendingToday} 张单词卡。`
                : "当前没有可用的单词计划，可以先处理知识卡或弱项。"}
            </Text>
            <View className="action-row">
              <Button className="primary-button" onClick={() => void openCards(date, firstPlan?.planId)}>
                单词复习
              </Button>
              <Button className="secondary-button" onClick={() => void openNotesReview(date)}>
                知识卡复习
              </Button>
            </View>
          </View>

          <View className="app-card">
            <Text className="app-card__title">长期指标</Text>
            <Text className="app-card__body">
              {longTerm
                ? `连续 ${longTerm.summary.currentStreakDays} 天，近 30 天完成 ${longTerm.summary.reviewedLast30Days} 次复习。未来 7 天预计 ${longTerm.loadForecast.next7Days.totalDue} 项到期。`
                : "长期指标加载中或暂不可用，不影响今天复习。"}
            </Text>
          </View>

          <View className="app-card">
            <Text className="app-card__title">新词负载建议</Text>
            <Text className="app-card__body">{newWordLoadAdvice.title}</Text>
            <Text className="app-card__body">{newWordLoadAdvice.reason}</Text>
            <Text className="app-card__body">{newWordLoadAdvice.detail}</Text>
          </View>

          <View className="app-card">
            <Text className="app-card__title">其他入口</Text>
            <View className="action-row">
              <Button className="secondary-button" onClick={() => void openWeakItems()}>
                弱项
              </Button>
              <Button className="secondary-button" onClick={() => void openAccount()}>
                账号
              </Button>
            </View>
          </View>
        </>
      ) : null}
    </AppPage>
  );
}
