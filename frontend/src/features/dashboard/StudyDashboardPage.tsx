import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, Col, DatePicker, Progress, Row, Space, Statistic, Table, Tag, Typography } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getStudyDashboard, type DashboardPlanSummary, type DashboardTrendItem } from "@/features/dashboard/api";
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

export function StudyDashboardPage() {
  const navigate = useNavigate();
  const setCurrentPlanId = useUiStore((state) => state.setCurrentPlanId);
  const [selectedDate, setSelectedDate] = useState<Dayjs>(() => dayjs());

  const formattedDate = selectedDate.format("YYYY-MM-DD");
  const dashboardQuery = useQuery({
    queryKey: ["dashboard", formattedDate],
    queryFn: () => getStudyDashboard(formattedDate)
  });

  const dashboard = dashboardQuery.data;
  const activePlans = dashboard?.activePlans ?? [];
  const hasActivePlans = activePlans.length > 0;

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
    navigate("/cards");
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Study Dashboard"
        description="Start from a learner-facing home page: review today's workload, active plans, and the recent study trend."
        extra={
          <Space wrap>
            <DatePicker value={selectedDate} onChange={(value) => value && setSelectedDate(value)} />
            <Button onClick={() => setSelectedDate(dayjs())}>Today</Button>
            <Tag color="gold">dashboard</Tag>
          </Space>
        }
      />

      {dashboardQuery.isLoading ? (
        <StatusState mode="loading" />
      ) : dashboardQuery.isError ? (
        <StatusState mode="error" description={(dashboardQuery.error as Error).message} />
      ) : (
        <>
          <PageSection title="Today's Overview">
            <div className="dashboard-overview-grid">
              <Card size="small">
                <Statistic title="Total Due Today" value={dashboard?.overview.totalDueToday ?? 0} />
              </Card>
              <Card size="small">
                <Statistic title="New Cards" value={dashboard?.overview.newDueToday ?? 0} />
              </Card>
              <Card size="small">
                <Statistic title="Review Cards" value={dashboard?.overview.reviewDueToday ?? 0} />
              </Card>
              <Card size="small">
                <Statistic title="Reviewed Today" value={dashboard?.overview.reviewedToday ?? 0} />
              </Card>
              <Card size="small">
                <Statistic title="Pending Today" value={dashboard?.overview.pendingDueToday ?? 0} />
              </Card>
              <Card size="small">
                <Statistic title="Active Plans" value={dashboard?.overview.activePlanCount ?? 0} />
              </Card>
            </div>
            {!hasActivePlans ? (
              <Alert
                style={{ marginTop: 16 }}
                type="info"
                showIcon
                message="No active study plans yet."
                description="Create and activate at least one study plan to populate the dashboard."
              />
            ) : null}
          </PageSection>

          <PageSection title="Active Plan Summary">
            {!hasActivePlans ? (
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
                          Open Today Cards
                        </Button>
                      </Space>
                    </Space>
                  </Card>
                ))}
              </div>
            )}
          </PageSection>

          <PageSection title="Recent 7-Day Trend">
            {!hasActivePlans ? (
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

          <PageSection title="Cross-Plan Comparison">
            {!hasActivePlans ? (
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
                        Open Cards
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
