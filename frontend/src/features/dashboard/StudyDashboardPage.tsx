import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, Col, DatePicker, Progress, Row, Space, Statistic, Table, Tag, Typography } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMe } from "@/features/auth/api";
import { getStudyDashboard, type DashboardPlanSummary, type DashboardTrendItem } from "@/features/dashboard/api";
import {
  getNoteDashboard,
  type NoteDashboardMasteryItem,
  type NoteDashboardTrendItem,
  type NoteMasteryStatus,
  type RecentNoteItem
} from "@/features/notes/api";
import {
  getLearningLineLabel,
  getLearningLinePendingCount,
  getLearningLineSessionLabel,
  resolveLearningPathState,
  type LearningLine
} from "@/features/review/learningPath";
import { getWeakItemSummary } from "@/features/weak-items/api";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import { useUiStore } from "@/shared/store/useUiStore";

const STATUS_COLORS: Record<string, string> = {
  DRAFT: "default",
  ACTIVE: "green",
  PAUSED: "orange",
  ARCHIVED: "red"
};

const NOTE_MASTERY_LABELS: Record<NoteMasteryStatus, string> = {
  UNSTARTED: "未开始",
  LEARNING: "学习中",
  CONSOLIDATING: "巩固中",
  MASTERED: "已掌握"
};

function getTodayReviewedNotes(recentTrend: NoteDashboardTrendItem[], formattedDate: string) {
  return recentTrend.find((item) => item.date === formattedDate)?.reviewedNotes ?? 0;
}

function sumBy<T>(items: T[], getValue: (item: T) => number) {
  return items.reduce((total, item) => total + getValue(item), 0);
}

function findPeakItem<T>(items: T[], getValue: (item: T) => number) {
  return items.reduce<T | undefined>((peak, item) => {
    if (!peak) {
      return item;
    }
    return getValue(item) > getValue(peak) ? item : peak;
  }, undefined);
}

function buildRetrospectiveRows(
  wordTrend: Array<DashboardTrendItem & { totalCards: number }>,
  noteTrend: NoteDashboardTrendItem[]
) {
  const byDate = new Map<string, { date: string; wordReviewed: number; noteReviewed: number; totalReviewed: number }>();

  wordTrend.forEach((item) => {
    byDate.set(item.date, {
      date: item.date,
      wordReviewed: item.reviewedCards,
      noteReviewed: byDate.get(item.date)?.noteReviewed ?? 0,
      totalReviewed: item.reviewedCards + (byDate.get(item.date)?.noteReviewed ?? 0)
    });
  });

  noteTrend.forEach((item) => {
    const current = byDate.get(item.date);
    byDate.set(item.date, {
      date: item.date,
      wordReviewed: current?.wordReviewed ?? 0,
      noteReviewed: item.reviewedNotes,
      totalReviewed: (current?.wordReviewed ?? 0) + item.reviewedNotes
    });
  });

  return Array.from(byDate.values()).sort((left, right) => left.date.localeCompare(right.date));
}

function buildSessionSearch(date: string, planId?: number) {
  const params = new URLSearchParams();
  params.set("date", date);
  if (planId) {
    params.set("planId", String(planId));
  }
  return `?${params.toString()}`;
}

export function StudyDashboardPage() {
  const navigate = useNavigate();
  const setCurrentPlanId = useUiStore((state) => state.setCurrentPlanId);
  const [selectedDate, setSelectedDate] = useState<Dayjs>(() => dayjs());

  const formattedDate = selectedDate.format("YYYY-MM-DD");
  const dashboardQuery = useQuery({
    queryKey: ["dashboard", formattedDate],
    queryFn: () => getStudyDashboard(formattedDate)
  });
  const noteDashboardQuery = useQuery({
    queryKey: ["noteDashboard", formattedDate],
    queryFn: () => getNoteDashboard(formattedDate)
  });
  const weakItemSummaryQuery = useQuery({
    queryKey: ["weakItemSummary"],
    queryFn: getWeakItemSummary
  });
  const currentUserQuery = useQuery({
    queryKey: ["me"],
    queryFn: getMe,
    retry: false
  });

  const dashboard = dashboardQuery.data;
  const activePlans = dashboard?.activePlans ?? [];
  const hasActivePlans = activePlans.length > 0;
  const noteDashboard = noteDashboardQuery.data;
  const weakItemSummary = weakItemSummaryQuery.data;
  const noteDueToday = noteDashboard?.overview.dueToday ?? 0;
  const noteReviewedToday = getTodayReviewedNotes(noteDashboard?.recentTrend ?? [], formattedDate);
  const todayTotalDue = (dashboard?.overview.totalDueToday ?? 0) + noteDueToday;
  const todayReviewed = (dashboard?.overview.reviewedToday ?? 0) + noteReviewedToday;
  const primaryPlan = activePlans.find((plan) => plan.pendingToday > 0) ?? activePlans[0];
  const preferredLearningOrder = currentUserQuery.data?.preferredLearningOrder ?? "WORD_FIRST";
  const totalWeakItems = (weakItemSummary?.weakWordCount ?? 0) + (weakItemSummary?.weakNoteCount ?? 0);
  const hasWeakItems = totalWeakItems > 0;
  const hasNotes = (noteDashboard?.overview.totalNotes ?? 0) > 0;
  const learningPathState = useMemo(
    () =>
      resolveLearningPathState(preferredLearningOrder, {
        wordPendingCount: dashboard?.overview.pendingDueToday ?? 0,
        notePendingCount: noteDueToday
      }),
    [dashboard?.overview.pendingDueToday, noteDueToday, preferredLearningOrder]
  );
  const noteMasteryRows = useMemo(
    () =>
      (noteDashboard?.masteryDistribution ?? []).map((item) => ({
        ...item,
        label: NOTE_MASTERY_LABELS[item.masteryStatus]
      })),
    [noteDashboard?.masteryDistribution]
  );
  const masteredNotes = (noteDashboard?.masteryDistribution ?? []).find((item) => item.masteryStatus === "MASTERED")?.count ?? 0;

  const trendRows = useMemo(
    () =>
      (dashboard?.recentTrend ?? []).map((item) => ({
        ...item,
        totalCards: item.newCards + item.reviewCards
      })),
    [dashboard?.recentTrend]
  );
  const retrospectiveRows = useMemo(
    () => buildRetrospectiveRows(trendRows, noteDashboard?.recentTrend ?? []),
    [noteDashboard?.recentTrend, trendRows]
  );
  const sevenDayWordReviewed = sumBy(trendRows, (item) => item.reviewedCards);
  const sevenDayNoteReviewed = sumBy(noteDashboard?.recentTrend ?? [], (item) => item.reviewedNotes);
  const sevenDayActiveDays = retrospectiveRows.filter((item) => item.totalReviewed > 0).length;
  const peakWordDay = findPeakItem(trendRows, (item) => item.reviewedCards);
  const peakNoteDay = findPeakItem(noteDashboard?.recentTrend ?? [], (item) => item.reviewedNotes);
  const atRiskPlanCount = activePlans.filter((plan) => plan.pendingToday > 0 && plan.reviewedToday === 0).length;
  const recommendedLine = learningPathState.recommendedLine;
  const followUpLine = learningPathState.followUpLine;

  function openRecommendedLine(line: LearningLine) {
    if (line === "WORD") {
      if (!primaryPlan) {
        navigate("/study-plans");
        return;
      }
      openTodayCards(primaryPlan.planId);
      return;
    }

    startNoteReview();
  }

  function renderPathActionLabel(line: LearningLine) {
    return line === "WORD" ? "进入单词复习" : "进入知识点复习";
  }

  function openTodayCards(planId: number) {
    setCurrentPlanId(planId);
    navigate({
      pathname: "/cards",
      search: buildSessionSearch(formattedDate, planId)
    });
  }

  function startWordReview() {
    if (!primaryPlan) {
      return;
    }
    openTodayCards(primaryPlan.planId);
  }

  function startNoteReview() {
    navigate({
      pathname: "/notes/review",
      search: buildSessionSearch(formattedDate)
    });
  }

  const studyError = dashboardQuery.isError ? (dashboardQuery.error as Error).message : null;
  const noteError = noteDashboardQuery.isError ? (noteDashboardQuery.error as Error).message : null;
  const weakError = weakItemSummaryQuery.isError ? (weakItemSummaryQuery.error as Error).message : null;
  const showInitialLoading = dashboardQuery.isLoading && noteDashboardQuery.isLoading;

  return (
    <div className="page-stack">
      <PageHeader
        title="Today Workbench"
        description="See today's workload across word study and note review, then jump straight into the next action."
        extra={
          <Space wrap>
            <DatePicker value={selectedDate} onChange={(value) => value && setSelectedDate(value)} />
            <Button onClick={() => setSelectedDate(dayjs())}>Today</Button>
            {recommendedLine ? (
              <Button type="primary" onClick={() => openRecommendedLine(recommendedLine)}>
                {renderPathActionLabel(recommendedLine)}
              </Button>
            ) : hasWeakItems ? (
              <Button type="primary" onClick={() => navigate("/weak-items")}>
                去看薄弱项
              </Button>
            ) : (
              <Tag color="green">今日已清空</Tag>
            )}
            {followUpLine ? (
              <Button onClick={() => openRecommendedLine(followUpLine)}>
                然后做{getLearningLineSessionLabel(followUpLine)}
              </Button>
            ) : null}
            <Tag color="gold">主路径</Tag>
          </Space>
        }
      />

      {showInitialLoading ? (
        <StatusState mode="loading" />
      ) : (
        <>
          {studyError || noteError || weakError ? (
            <Alert
              type="warning"
              showIcon
              message="Some workbench data is unavailable."
              description={[
                studyError ? `Word study: ${studyError}` : null,
                noteError ? `Note review: ${noteError}` : null,
                weakError ? `Weak items: ${weakError}` : null
              ]
                .filter(Boolean)
                .join(" | ")}
            />
          ) : null}

          <PageSection title="Today's Overview">
            <div className="dashboard-overview-grid">
              <Card size="small">
                <Statistic title="Total Due Today" value={todayTotalDue} />
              </Card>
              <Card size="small">
                <Statistic title="Word Cards Due" value={dashboard?.overview.totalDueToday ?? 0} />
              </Card>
              <Card size="small">
                <Statistic title="Notes Due" value={noteDueToday} />
              </Card>
              <Card size="small">
                <Statistic title="Reviewed Today" value={todayReviewed} />
              </Card>
              <Card size="small">
                <Statistic title="Active Plans" value={dashboard?.overview.activePlanCount ?? 0} />
              </Card>
              <Card size="small">
                <Statistic title="Total Notes" value={noteDashboard?.overview.totalNotes ?? 0} />
              </Card>
              <Card size="small">
                <Statistic title="Weak Items" value={totalWeakItems} />
              </Card>
            </div>
          </PageSection>

          {!hasActivePlans || !hasNotes ? (
            <PageSection title="回归前置准备">
              <div className="dashboard-plan-grid">
                {!hasActivePlans ? (
                  <Card size="small" title="单词线还没起">
                    <Space direction="vertical" size={12} style={{ width: "100%" }}>
                      <Typography.Text type="secondary">
                        现在没有激活中的学习计划，所以单词主路径、会话闭环和近 7 天单词复盘都不会有有效数据。
                      </Typography.Text>
                      <Space wrap>
                        <Button type="primary" onClick={() => navigate("/study-plans")}>
                          去建学习计划
                        </Button>
                        <Button onClick={() => navigate("/word-sets")}>先准备词库</Button>
                      </Space>
                    </Space>
                  </Card>
                ) : null}

                {!hasNotes ? (
                  <Card size="small" title="知识点线还没起">
                    <Space direction="vertical" size={12} style={{ width: "100%" }}>
                      <Typography.Text type="secondary">
                        当前还没有知识点内容，所以知识点主路径、薄弱轮和近 7 天知识点复盘都还不会出现。
                      </Typography.Text>
                      <Space wrap>
                        <Button type="primary" onClick={() => navigate("/notes")}>
                          去建知识点
                        </Button>
                        <Button onClick={() => navigate("/notes/review")}>打开复习页</Button>
                      </Space>
                    </Space>
                  </Card>
                ) : null}
              </div>
            </PageSection>
          ) : null}

          <PageSection title="Today's Path">
            <div className="learning-path-grid">
              <Card size="small" title="1. 当前推荐动作">
                {recommendedLine ? (
                  <Space direction="vertical" size={12} style={{ width: "100%" }}>
                    <Typography.Title level={4} style={{ margin: 0 }}>
                      {getLearningLineSessionLabel(recommendedLine)}
                    </Typography.Title>
                    <Typography.Text type="secondary">
                      先走{getLearningLineLabel(recommendedLine)}。当前还剩{" "}
                      {getLearningLinePendingCount(recommendedLine, {
                        wordPendingCount: dashboard?.overview.pendingDueToday ?? 0,
                        notePendingCount: noteDueToday
                      })}{" "}
                      个待处理项。
                    </Typography.Text>
                    <Space wrap>
                      <Button type="primary" onClick={() => openRecommendedLine(recommendedLine)}>
                        {renderPathActionLabel(recommendedLine)}
                      </Button>
                      <Tag color="blue">
                        偏好：{preferredLearningOrder === "WORD_FIRST" ? "单词优先" : "知识点优先"}
                      </Tag>
                    </Space>
                  </Space>
                ) : (
                  <Alert
                    type={hasWeakItems ? "info" : "success"}
                    showIcon
                    message={hasWeakItems ? "今天主路径已完成。" : "今天两条学习线都已清空。"}
                    description={
                      hasWeakItems
                        ? `主路径已清空，但还有 ${totalWeakItems} 个薄弱项可选加练。`
                        : "可以直接结束今天的学习。"
                    }
                    action={
                      hasWeakItems ? (
                        <Button size="small" type="primary" onClick={() => navigate("/weak-items")}>
                          打开薄弱项
                        </Button>
                      ) : undefined
                    }
                  />
                )}
              </Card>

              <Card size="small" title="2. 完成后衔接">
                {followUpLine ? (
                  <Space direction="vertical" size={12} style={{ width: "100%" }}>
                    <Typography.Title level={4} style={{ margin: 0 }}>
                      {getLearningLineSessionLabel(followUpLine)}
                    </Typography.Title>
                    <Typography.Text type="secondary">
                      当前推荐线结束后，顺着切到{getLearningLineLabel(followUpLine)}，避免今天只做一半。
                    </Typography.Text>
                    <Button onClick={() => openRecommendedLine(followUpLine)}>
                      预览下一步
                    </Button>
                  </Space>
                ) : (
                  <Alert
                    type={hasWeakItems ? "info" : "success"}
                    showIcon
                    message={learningPathState.isComplete ? "今天可以收尾。" : "当前推荐线做完后，今天主路径就算完成。"}
                    description={
                      hasWeakItems
                        ? `主路径结束后，还可以去处理 ${totalWeakItems} 个薄弱项。`
                        : "这版先收成一条简单主路径：先完成偏好线，再决定是否切到另一条线。"
                    }
                    action={
                      hasWeakItems ? (
                        <Button size="small" onClick={() => navigate("/weak-items")}>
                          去看薄弱项
                        </Button>
                      ) : undefined
                    }
                  />
                )}
              </Card>
            </div>
          </PageSection>

          <PageSection title="近 7 天复盘">
            <div className="dashboard-plan-grid">
              <Card size="small" title="复盘摘要">
                <div className="dashboard-overview-grid">
                  <Card size="small">
                    <Statistic title="单词复习量" value={sevenDayWordReviewed} />
                  </Card>
                  <Card size="small">
                    <Statistic title="知识点复习量" value={sevenDayNoteReviewed} />
                  </Card>
                  <Card size="small">
                    <Statistic title="有学习记录的天数" value={sevenDayActiveDays} suffix="/ 7" />
                  </Card>
                  <Card size="small">
                    <Statistic title="当前薄弱项" value={totalWeakItems} />
                  </Card>
                </div>
              </Card>

              <Card size="small" title="高峰日">
                <Space direction="vertical" size={12} style={{ width: "100%" }}>
                  <Alert
                    type="info"
                    showIcon
                    message={peakWordDay ? `单词线高峰：${peakWordDay.date}` : "单词线高峰：暂无数据"}
                    description={peakWordDay ? `当天完成 ${peakWordDay.reviewedCards} 张卡。` : "激活计划后，这里会出现近 7 天的高峰日。"}
                  />
                  <Alert
                    type="info"
                    showIcon
                    message={peakNoteDay ? `知识点高峰：${peakNoteDay.date}` : "知识点高峰：暂无数据"}
                    description={peakNoteDay ? `当天完成 ${peakNoteDay.reviewedNotes} 个知识点。` : "开始知识点复习后，这里会出现近 7 天的高峰日。"}
                  />
                </Space>
              </Card>
            </div>
          </PageSection>

          <PageSection title="风险信号">
            <div className="dashboard-plan-grid">
              <Card size="small" title="当前积压">
                <Space direction="vertical" size={12} style={{ width: "100%" }}>
                  <Alert
                    type={todayTotalDue > 0 ? "warning" : "success"}
                    showIcon
                    message={todayTotalDue > 0 ? "今天还有积压。" : "今天没有主线积压。"}
                    description={
                      todayTotalDue > 0
                        ? `单词和知识点合计还有 ${todayTotalDue} 个待处理项。`
                        : "主路径已经清空，可以把精力放到薄弱项或复盘上。"
                    }
                  />
                  <Alert
                    type={atRiskPlanCount > 0 ? "warning" : "success"}
                    showIcon
                    message={atRiskPlanCount > 0 ? "有计划今天还没启动。" : "当前激活计划都已有推进。"}
                    description={
                      atRiskPlanCount > 0
                        ? `有 ${atRiskPlanCount} 个激活计划今天还有待办，但还没产生复习记录。`
                        : "当前没有“激活但完全没动”的计划。"
                    }
                  />
                </Space>
              </Card>

              <Card size="small" title="近 7 天明细">
                <Table<{ date: string; wordReviewed: number; noteReviewed: number; totalReviewed: number }>
                  rowKey="date"
                  size="small"
                  pagination={false}
                  dataSource={retrospectiveRows}
                  columns={[
                    {
                      title: "日期",
                      dataIndex: "date",
                      render: (value: string) => dayjs(value).format("YYYY-MM-DD")
                    },
                    {
                      title: "单词",
                      dataIndex: "wordReviewed"
                    },
                    {
                      title: "知识点",
                      dataIndex: "noteReviewed"
                    },
                    {
                      title: "合计",
                      dataIndex: "totalReviewed"
                    }
                  ]}
                  locale={{
                    emptyText: "近 7 天还没有可复盘的数据。"
                  }}
                />
              </Card>
            </div>
          </PageSection>

          <PageSection title="Quick Start">
            <div className="dashboard-plan-grid">
              <Card size="small" title="Word Study">
                <Space direction="vertical" size={12} style={{ width: "100%" }}>
                  <Typography.Text type="secondary">
                    {hasActivePlans
                      ? `Top plan: ${primaryPlan.planName}`
                      : "No active plan yet. Create and activate one first."}
                  </Typography.Text>
                  <Row gutter={[12, 12]}>
                    <Col span={12}>
                      <Statistic title="Due Today" value={dashboard?.overview.totalDueToday ?? 0} />
                    </Col>
                    <Col span={12}>
                      <Statistic title="Pending" value={dashboard?.overview.pendingDueToday ?? 0} />
                    </Col>
                  </Row>
                  <Space wrap>
                    <Button type="primary" onClick={startWordReview} disabled={!primaryPlan}>
                      Continue Session
                    </Button>
                    <Button onClick={() => navigate("/study-plans")}>Open Plans</Button>
                    <Button onClick={() => navigate("/word-sets")}>Open Word Sets</Button>
                    <Button onClick={() => navigate("/weak-items")}>
                      Weak Items {weakItemSummary ? `(${weakItemSummary.weakWordCount})` : ""}
                    </Button>
                  </Space>
                </Space>
              </Card>

              <Card size="small" title="Note Review">
                <Space direction="vertical" size={12} style={{ width: "100%" }}>
                  <Typography.Text type="secondary">
                    Review today's due knowledge points or continue building your note base.
                  </Typography.Text>
                  <Row gutter={[12, 12]}>
                    <Col span={12}>
                      <Statistic title="Due Today" value={noteDueToday} />
                    </Col>
                    <Col span={12}>
                      <Statistic title="Reviewed Today" value={noteReviewedToday} />
                    </Col>
                  </Row>
                  <Space wrap>
                    <Button type="primary" onClick={startNoteReview}>
                      Continue Session
                    </Button>
                    <Button onClick={() => navigate("/notes")}>Open Notes</Button>
                    <Button onClick={() => navigate("/notes/dashboard")}>Open Note Dashboard</Button>
                    <Button onClick={() => navigate("/weak-items")}>
                      Weak Notes {weakItemSummary ? `(${weakItemSummary.weakNoteCount})` : ""}
                    </Button>
                  </Space>
                </Space>
              </Card>
            </div>
          </PageSection>

          <PageSection title="Word Study Line">
            {dashboardQuery.isLoading ? (
              <StatusState mode="loading" />
            ) : dashboardQuery.isError ? (
              <StatusState mode="error" description={studyError ?? undefined} />
            ) : !hasActivePlans ? (
              <StatusState mode="empty" description="No active plans are available for the selected date." />
            ) : (
              <div className="dashboard-plan-grid">
                {activePlans.map((plan) => (
                  <Card
                    key={plan.planId}
                    size="small"
                    title={plan.planName}
                    extra={<Tag color={STATUS_COLORS[plan.status] ?? "default"}>{plan.status}</Tag>}
                  >
                    <Space direction="vertical" style={{ width: "100%" }} size={12}>
                      <Row gutter={[12, 12]}>
                        <Col span={12}>
                          <Statistic title="Due Today" value={plan.dueToday} />
                        </Col>
                        <Col span={12}>
                          <Statistic title="Reviewed Today" value={plan.reviewedToday} />
                        </Col>
                        <Col span={12}>
                          <Statistic title="Daily New Target" value={plan.dailyNewCount} />
                        </Col>
                        <Col span={12}>
                          <Statistic title="Pending Today" value={plan.pendingToday} />
                        </Col>
                      </Row>
                      <div>
                        <Typography.Text type="secondary">Progress</Typography.Text>
                        <Progress
                          percent={Number(plan.completionRate.toFixed(1))}
                          size="small"
                          strokeColor="#8c5d20"
                        />
                        <Typography.Text type="secondary">
                          {plan.completedCards} / {plan.totalCards} cards completed
                        </Typography.Text>
                      </div>
                      <Space wrap>
                        <Typography.Text type="secondary">Start date: {plan.startDate}</Typography.Text>
                        <Button type="primary" onClick={() => openTodayCards(plan.planId)}>
                          Open Session
                        </Button>
                      </Space>
                    </Space>
                  </Card>
                ))}
              </div>
            )}
          </PageSection>

          <PageSection title="Word Study Trend">
            {dashboardQuery.isLoading ? (
              <StatusState mode="loading" />
            ) : dashboardQuery.isError ? (
              <StatusState mode="error" description={studyError ?? undefined} />
            ) : !hasActivePlans ? (
              <StatusState mode="empty" description="Trend data will appear after you activate a study plan." />
            ) : (
              <Table<DashboardTrendItem & { totalCards: number }>
                rowKey="date"
                pagination={false}
                size="small"
                dataSource={trendRows}
                columns={[
                  {
                    title: "Date",
                    dataIndex: "date",
                    render: (value: string) => dayjs(value).format("YYYY-MM-DD")
                  },
                  { title: "New", dataIndex: "newCards" },
                  { title: "Review", dataIndex: "reviewCards" },
                  { title: "Total Due", dataIndex: "totalCards" },
                  { title: "Reviewed", dataIndex: "reviewedCards" }
                ]}
              />
            )}
          </PageSection>

          <PageSection title="Note Review Line">
            {noteDashboardQuery.isLoading ? (
              <StatusState mode="loading" />
            ) : noteDashboardQuery.isError ? (
              <StatusState mode="error" description={noteError ?? undefined} />
            ) : (
              <div className="dashboard-plan-grid">
                <Card size="small" title="Overview">
                  <Space direction="vertical" size={12} style={{ width: "100%" }}>
                    <Row gutter={[12, 12]}>
                      <Col span={12}>
                        <Statistic title="Due Today" value={noteDashboard?.overview.dueToday ?? 0} />
                      </Col>
                      <Col span={12}>
                        <Statistic title="Reviewed Today" value={noteReviewedToday} />
                      </Col>
                      <Col span={12}>
                        <Statistic title="Reviewed Notes" value={noteDashboard?.overview.reviewedNotes ?? 0} />
                      </Col>
                      <Col span={12}>
                        <Statistic title="Mastered Notes" value={masteredNotes} />
                      </Col>
                    </Row>
                    <Space wrap>
                      <Button type="primary" onClick={startNoteReview}>
                        Open Session
                      </Button>
                      <Button onClick={() => navigate("/notes")}>Manage Notes</Button>
                    </Space>
                  </Space>
                </Card>

                <Card size="small" title="Mastery Distribution">
                  <Table<NoteDashboardMasteryItem & { label: string }>
                    rowKey="masteryStatus"
                    size="small"
                    pagination={false}
                    dataSource={noteMasteryRows}
                    columns={[
                      {
                        title: "Status",
                        dataIndex: "label"
                      },
                      {
                        title: "Count",
                        dataIndex: "count",
                        width: 120
                      }
                    ]}
                    locale={{
                      emptyText: "Create notes to populate mastery distribution."
                    }}
                  />
                </Card>
              </div>
            )}
          </PageSection>

          <PageSection title="Recent Note Activity">
            {noteDashboardQuery.isLoading ? (
              <StatusState mode="loading" />
            ) : noteDashboardQuery.isError ? (
              <StatusState mode="error" description={noteError ?? undefined} />
            ) : (
              <Table<RecentNoteItem>
                rowKey="id"
                pagination={false}
                dataSource={noteDashboard?.recentNotes ?? []}
                columns={[
                  {
                    title: "Title",
                    dataIndex: "title"
                  },
                  {
                    title: "Tags",
                    dataIndex: "tags",
                    render: (tags: string[]) => (tags.length ? tags.join(", ") : "-")
                  },
                  {
                    title: "Mastery",
                    dataIndex: "masteryStatus",
                    render: (value: NoteMasteryStatus) => NOTE_MASTERY_LABELS[value]
                  },
                  {
                    title: "Created At",
                    dataIndex: "createdAt",
                    render: (value: string) => dayjs(value).format("YYYY-MM-DD HH:mm")
                  }
                ]}
                locale={{
                  emptyText: "No notes yet."
                }}
              />
            )}
          </PageSection>

          <PageSection title="Cross-Plan Comparison">
            {dashboardQuery.isLoading ? (
              <StatusState mode="loading" />
            ) : dashboardQuery.isError ? (
              <StatusState mode="error" description={studyError ?? undefined} />
            ) : !hasActivePlans ? (
              <StatusState mode="empty" description="Activate multiple plans to compare their current workload." />
            ) : (
              <Table<DashboardPlanSummary>
                rowKey="planId"
                pagination={false}
                dataSource={activePlans}
                columns={[
                  {
                    title: "Plan",
                    dataIndex: "planName"
                  },
                  {
                    title: "Due Today",
                    dataIndex: "dueToday"
                  },
                  {
                    title: "New / Review",
                    render: (_, record) => `${record.newToday} / ${record.reviewToday}`
                  },
                  {
                    title: "Reviewed Today",
                    dataIndex: "reviewedToday"
                  },
                  {
                    title: "Completion",
                    render: (_, record) => (
                      <Progress
                        percent={Number(record.completionRate.toFixed(1))}
                        size="small"
                        strokeColor="#8c5d20"
                      />
                    )
                  },
                  {
                    title: "Action",
                    render: (_, record) => (
                      <Button type="link" onClick={() => openTodayCards(record.planId)}>
                        Open Session
                      </Button>
                    )
                  }
                ]}
              />
            )}
          </PageSection>
        </>
      )}
    </div>
  );
}
