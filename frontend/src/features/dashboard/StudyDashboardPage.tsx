import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, Col, DatePicker, Progress, Row, Space, Statistic, Table, Tag, Typography } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getStudyDashboard, type DashboardPlanSummary, type DashboardTrendItem } from "@/features/dashboard/api";
import {
  getNoteDashboard,
  type NoteDashboardMasteryItem,
  type NoteDashboardTrendItem,
  type NoteMasteryStatus,
  type RecentNoteItem
} from "@/features/notes/api";
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

  const dashboard = dashboardQuery.data;
  const activePlans = dashboard?.activePlans ?? [];
  const hasActivePlans = activePlans.length > 0;
  const noteDashboard = noteDashboardQuery.data;
  const weakItemSummary = weakItemSummaryQuery.data;
  const noteDueToday = noteDashboard?.overview.dueToday ?? 0;
  const noteReviewedToday = getTodayReviewedNotes(noteDashboard?.recentTrend ?? [], formattedDate);
  const todayTotalDue = (dashboard?.overview.totalDueToday ?? 0) + noteDueToday;
  const todayReviewed = (dashboard?.overview.reviewedToday ?? 0) + noteReviewedToday;
  const primaryPlan = activePlans[0];
  const totalWeakItems = (weakItemSummary?.weakWordCount ?? 0) + (weakItemSummary?.weakNoteCount ?? 0);
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
            <Button type="primary" onClick={startWordReview} disabled={!primaryPlan}>
              Continue Word Session
            </Button>
            <Button onClick={startNoteReview}>Continue Note Session</Button>
            <Tag color="gold">workbench</Tag>
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
