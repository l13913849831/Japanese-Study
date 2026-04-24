import { useQuery } from "@tanstack/react-query";
import { Button, Card, DatePicker, Progress, Space, Statistic, Table, Tag, Typography } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import { getNoteDashboard, type NoteDashboardMasteryItem, type NoteMasteryStatus, type NoteDashboardTrendItem, type RecentNoteItem } from "@/features/notes/api";

const NOTE_MASTERY_LABELS: Record<NoteMasteryStatus, string> = {
  UNSTARTED: "未开始",
  LEARNING: "学习中",
  CONSOLIDATING: "巩固中",
  MASTERED: "已掌握"
};

export function NoteDashboardPage() {
  const navigate = useNavigate();
  const [selectedDate, setSelectedDate] = useState<Dayjs>(() => dayjs());
  const formattedDate = selectedDate.format("YYYY-MM-DD");

  const dashboardQuery = useQuery({
    queryKey: ["noteDashboard", formattedDate],
    queryFn: () => getNoteDashboard(formattedDate)
  });

  const masteryRows = useMemo(
    () =>
      (dashboardQuery.data?.masteryDistribution ?? []).map((item) => ({
        ...item,
        label: NOTE_MASTERY_LABELS[item.masteryStatus]
      })),
    [dashboardQuery.data?.masteryDistribution]
  );

  return (
    <div className="page-stack">
      <PageHeader
        title="Note Dashboard"
        description="Track the note-review queue, mastery distribution, recent review trend, and the latest knowledge points."
        extra={
          <Space wrap>
            <DatePicker value={selectedDate} onChange={(value) => value && setSelectedDate(value)} />
            <Button onClick={() => setSelectedDate(dayjs())}>Today</Button>
            <Button type="primary" onClick={() => navigate("/notes/review")}>
              Start Review
            </Button>
            <Tag color="gold">notes dashboard</Tag>
          </Space>
        }
      />

      {dashboardQuery.isLoading ? (
        <StatusState mode="loading" />
      ) : dashboardQuery.isError ? (
        <StatusState mode="error" description={(dashboardQuery.error as Error).message} />
      ) : (
        <>
          <PageSection title="Overview">
            <div className="dashboard-overview-grid">
              <Card size="small">
                <Statistic title="Due Today" value={dashboardQuery.data?.overview.dueToday ?? 0} />
              </Card>
              <Card size="small">
                <Statistic title="Total Notes" value={dashboardQuery.data?.overview.totalNotes ?? 0} />
              </Card>
              <Card size="small">
                <Statistic title="Reviewed Notes" value={dashboardQuery.data?.overview.reviewedNotes ?? 0} />
              </Card>
            </div>
          </PageSection>

          <PageSection title="Mastery Distribution">
            <Table<NoteDashboardMasteryItem & { label: string }>
              rowKey="masteryStatus"
              pagination={false}
              dataSource={masteryRows}
              columns={[
                {
                  title: "Status",
                  dataIndex: "label"
                },
                {
                  title: "Count",
                  dataIndex: "count",
                  width: 120
                },
                {
                  title: "Share",
                  render: (_, record) => {
                    const total = dashboardQuery.data?.overview.totalNotes ?? 0;
                    const percent = total === 0 ? 0 : (record.count / total) * 100;
                    return <Progress percent={Number(percent.toFixed(1))} size="small" strokeColor="#8c5d20" />;
                  }
                }
              ]}
              locale={{
                emptyText: "Mastery distribution will appear after you create notes."
              }}
            />
          </PageSection>

          <PageSection title="Recent 7-Day Review Trend">
            <Table<NoteDashboardTrendItem>
              rowKey="date"
              pagination={false}
              dataSource={dashboardQuery.data?.recentTrend ?? []}
              columns={[
                {
                  title: "Date",
                  dataIndex: "date",
                  render: (value: string) => dayjs(value).format("YYYY-MM-DD")
                },
                {
                  title: "Reviewed Notes",
                  dataIndex: "reviewedNotes"
                }
              ]}
              locale={{
                emptyText: "Trend data will appear after at least one review."
              }}
            />
          </PageSection>

          <PageSection
            title="Recent Notes"
            extra={<Button onClick={() => navigate("/notes")}>Open Notes</Button>}
          >
            <Table<RecentNoteItem>
              rowKey="id"
              pagination={false}
              dataSource={dashboardQuery.data?.recentNotes ?? []}
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
          </PageSection>
        </>
      )}
    </div>
  );
}
