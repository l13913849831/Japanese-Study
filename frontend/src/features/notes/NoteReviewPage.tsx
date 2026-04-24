import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, App, Button, DatePicker, Form, Input, InputNumber, Space, Statistic, Table, Tag, Typography } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useEffect, useMemo, useState } from "react";
import { buildReviewSessionSummary, resolveCurrentSessionIndex } from "@/features/review/session";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import {
  getNoteReviews,
  getTodayNoteReviews,
  submitNoteReview,
  type NoteMasteryStatus,
  type NoteReviewLogItem,
  type NoteReviewQueueItem,
  type NoteReviewRating,
  type ReviewNotePayload
} from "@/features/notes/api";

interface SearchFormValues {
  date?: Dayjs;
}

interface ReviewFormValues {
  responseTimeMs?: number;
  note?: string;
}

const NOTE_MASTERY_LABELS: Record<NoteMasteryStatus, string> = {
  UNSTARTED: "未开始",
  LEARNING: "学习中",
  CONSOLIDATING: "巩固中",
  MASTERED: "已掌握"
};

const REVIEW_ACTIONS: Array<{ rating: NoteReviewRating; label: string; type?: "primary" | "default" }> = [
  { rating: "AGAIN", label: "不会" },
  { rating: "HARD", label: "吃力" },
  { rating: "GOOD", label: "还行", type: "primary" },
  { rating: "EASY", label: "熟悉" }
];

function sortNotes(items: NoteReviewQueueItem[]) {
  return items.slice().sort((left, right) => {
    const dueCompare = dayjs(left.dueAt).valueOf() - dayjs(right.dueAt).valueOf();
    if (dueCompare !== 0) {
      return dueCompare;
    }
    return left.id - right.id;
  });
}

export function NoteReviewPage() {
  const [search, setSearch] = useState<{ date: string }>({ date: dayjs().format("YYYY-MM-DD") });
  const [currentNoteId, setCurrentNoteId] = useState<number>();
  const [revealed, setRevealed] = useState(false);
  const [reviewForm] = Form.useForm<ReviewFormValues>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  const todayReviewsQuery = useQuery({
    queryKey: ["todayNoteReviews", search.date],
    queryFn: () => getTodayNoteReviews(search.date)
  });

  const orderedNotes = useMemo(() => sortNotes(todayReviewsQuery.data ?? []), [todayReviewsQuery.data]);
  const sessionSummary = useMemo(
    () => buildReviewSessionSummary(orderedNotes, () => true),
    [orderedNotes]
  );
  const resolvedCurrentIndex = useMemo(
    () => resolveCurrentSessionIndex(orderedNotes, currentNoteId, (item) => item.id, () => true),
    [orderedNotes, currentNoteId]
  );
  const currentNote = resolvedCurrentIndex === -1 ? undefined : orderedNotes[resolvedCurrentIndex];
  const remainingCount = currentNote ? Math.max(sessionSummary.totalCount - resolvedCurrentIndex - 1, 0) : 0;

  useEffect(() => {
    const nextNoteId = resolvedCurrentIndex === -1 ? undefined : orderedNotes[resolvedCurrentIndex]?.id;
    if (nextNoteId !== currentNoteId) {
      setCurrentNoteId(nextNoteId);
      setRevealed(false);
    }
  }, [currentNoteId, orderedNotes, resolvedCurrentIndex]);

  const reviewLogsQuery = useQuery({
    queryKey: ["noteReviewLogs", currentNote?.id],
    queryFn: () => getNoteReviews(currentNote!.id),
    enabled: Boolean(currentNote?.id)
  });

  const reviewMutation = useMutation({
    mutationFn: ({ noteId, payload }: { noteId: number; payload: ReviewNotePayload }) => submitNoteReview(noteId, payload),
    onSuccess: async () => {
      message.success("Review submitted.");
      setRevealed(false);
      reviewForm.resetFields();
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["todayNoteReviews", search.date] }),
        queryClient.invalidateQueries({ queryKey: ["noteReviewLogs", currentNote?.id] }),
        queryClient.invalidateQueries({ queryKey: ["notes"] }),
        queryClient.invalidateQueries({ queryKey: ["noteDashboard"] })
      ]);
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const latestReview = reviewLogsQuery.data?.[0];

  function handleSubmitReview(rating: NoteReviewRating) {
    if (!currentNote) {
      message.warning("No current knowledge point in this session.");
      return;
    }
    const values = reviewForm.getFieldsValue();
    reviewMutation.mutate({
      noteId: currentNote.id,
      payload: {
        rating,
        responseTimeMs: values.responseTimeMs,
        note: values.note?.trim() || undefined
      }
    });
  }

  function moveCurrentNote(offset: number) {
    const nextNote = orderedNotes[resolvedCurrentIndex + offset];
    if (!nextNote) {
      return;
    }
    setCurrentNoteId(nextNote.id);
    setRevealed(false);
    reviewForm.resetFields();
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Note Review Session"
        description="Stay on one knowledge point at a time: recall from the title, reveal the content when needed, score it, then continue forward."
        extra={<Tag color="purple">fsrs</Tag>}
      />

      <PageSection title="Session Setup">
        <Form<SearchFormValues>
          layout="inline"
          initialValues={{ date: dayjs(search.date) }}
          onFinish={(values) => {
            setCurrentNoteId(undefined);
            setRevealed(false);
            reviewForm.resetFields();
            setSearch({ date: values.date?.format("YYYY-MM-DD") ?? dayjs().format("YYYY-MM-DD") });
          }}
        >
          <Form.Item label="Date" name="date" rules={[{ required: true, message: "Select a date." }]}>
            <DatePicker />
          </Form.Item>
          <Button type="primary" htmlType="submit">
            Start Session
          </Button>
        </Form>
      </PageSection>

      {todayReviewsQuery.isLoading ? (
        <StatusState mode="loading" />
      ) : todayReviewsQuery.isError ? (
        <StatusState mode="error" description={(todayReviewsQuery.error as Error).message} />
      ) : (
        <>
          <PageSection title="Session Progress">
            <div className="dashboard-overview-grid">
              <ReviewStat title="Queue Size" value={sessionSummary.totalCount} />
              <ReviewStat title="Current Position" value={currentNote ? resolvedCurrentIndex + 1 : 0} suffix={sessionSummary.totalCount ? `/ ${sessionSummary.totalCount}` : ""} />
              <ReviewStat title="Remaining" value={remainingCount} />
              <ReviewStat title="Revealed" value={revealed ? 1 : 0} suffix={currentNote ? "/ 1" : ""} />
            </div>
            {sessionSummary.totalCount === 0 ? (
              <Alert
                style={{ marginTop: 16 }}
                type="info"
                showIcon
                message="No note is due for the selected date."
                description="Choose another date if you want to inspect a different queue."
              />
            ) : (
              <Alert
                style={{ marginTop: 16 }}
                type="info"
                showIcon
                message="Current flow"
                description="Look at the title first, try to recall actively, reveal the content only when needed, then score and move on."
              />
            )}
          </PageSection>

          <PageSection title="Current Note">
            {!currentNote ? (
              <StatusState mode="empty" description="No current knowledge point is available for this session." />
            ) : (
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <Space wrap>
                  <Tag color="blue">{NOTE_MASTERY_LABELS[currentNote.masteryStatus]}</Tag>
                  <Tag>{`Review ${currentNote.reviewCount}`}</Tag>
                  <Tag>{dayjs(currentNote.dueAt).format("YYYY-MM-DD HH:mm")}</Tag>
                </Space>

                <Typography.Title level={3} style={{ margin: 0 }}>
                  {currentNote.title}
                </Typography.Title>
                <Typography.Text type="secondary">
                  {currentNote.tags.length ? currentNote.tags.join(", ") : "No tags"}
                </Typography.Text>

                {!revealed ? (
                  <Button type="primary" onClick={() => setRevealed(true)}>
                    显示内容
                  </Button>
                ) : (
                  <Typography.Paragraph style={{ whiteSpace: "pre-wrap", marginBottom: 0 }}>
                    {currentNote.content}
                  </Typography.Paragraph>
                )}

                <Space wrap>
                  <Button onClick={() => moveCurrentNote(-1)} disabled={resolvedCurrentIndex <= 0}>
                    Previous
                  </Button>
                  <Button onClick={() => moveCurrentNote(1)} disabled={resolvedCurrentIndex >= orderedNotes.length - 1}>
                    Next
                  </Button>
                </Space>

                <Form<ReviewFormValues> form={reviewForm} layout="vertical">
                  <Form.Item label="Response Time (ms)" name="responseTimeMs">
                    <InputNumber min={0} style={{ width: "100%" }} placeholder="3200" />
                  </Form.Item>
                  <Form.Item label="Review Note" name="note">
                    <Input.TextArea rows={3} placeholder="Optional reflection for this review" />
                  </Form.Item>
                </Form>

                <Space wrap>
                  {REVIEW_ACTIONS.map((action) => (
                    <Button
                      key={action.rating}
                      type={action.type ?? "default"}
                      loading={reviewMutation.isPending}
                      onClick={() => handleSubmitReview(action.rating)}
                    >
                      {action.label}
                    </Button>
                  ))}
                </Space>

                {latestReview ? (
                  <Alert
                    type="success"
                    showIcon
                    message={`Latest review: ${latestReview.rating}`}
                    description={[
                      `Reviewed at: ${dayjs(latestReview.reviewedAt).format("YYYY-MM-DD HH:mm:ss")}`,
                      latestReview.responseTimeMs !== undefined ? `Response time: ${latestReview.responseTimeMs} ms` : undefined,
                      latestReview.note ? `Note: ${latestReview.note}` : undefined
                    ]
                      .filter(Boolean)
                      .join(" | ")}
                  />
                ) : (
                  <Typography.Text type="secondary">No review history for the current note yet.</Typography.Text>
                )}
              </Space>
            )}
          </PageSection>

          <PageSection title="Queue">
            <Table<NoteReviewQueueItem>
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={orderedNotes}
              onRow={(record) => ({
                onClick: () => {
                  setCurrentNoteId(record.id);
                  setRevealed(false);
                  reviewForm.resetFields();
                }
              })}
              rowClassName={(record) => (record.id === currentNote?.id ? "review-session-row-active" : "")}
              columns={[
                {
                  title: "#",
                  width: 70,
                  render: (_, __, index) => index + 1
                },
                { title: "Title", dataIndex: "title" },
                {
                  title: "Mastery",
                  dataIndex: "masteryStatus",
                  width: 120,
                  render: (value: NoteMasteryStatus) => NOTE_MASTERY_LABELS[value]
                },
                {
                  title: "Due At",
                  dataIndex: "dueAt",
                  width: 180,
                  render: (value: string) => dayjs(value).format("YYYY-MM-DD HH:mm")
                }
              ]}
              locale={{
                emptyText: "No knowledge point is due for this queue."
              }}
            />
          </PageSection>

          <PageSection title="Review History">
            {!currentNote ? (
              <StatusState mode="empty" description="No current knowledge point selected." />
            ) : reviewLogsQuery.isLoading ? (
              <StatusState mode="loading" />
            ) : reviewLogsQuery.isError ? (
              <StatusState mode="error" description={(reviewLogsQuery.error as Error).message} />
            ) : (
              <Table<NoteReviewLogItem>
                rowKey="id"
                size="small"
                pagination={false}
                dataSource={reviewLogsQuery.data ?? []}
                columns={[
                  {
                    title: "Reviewed At",
                    dataIndex: "reviewedAt",
                    render: (value: string) => dayjs(value).format("YYYY-MM-DD HH:mm:ss")
                  },
                  { title: "Rating", dataIndex: "rating", width: 100 },
                  {
                    title: "Response Time",
                    dataIndex: "responseTimeMs",
                    render: (value?: number) => (value === undefined ? "-" : `${value} ms`)
                  },
                  {
                    title: "Note",
                    dataIndex: "note",
                    render: (value?: string) => value || "-"
                  }
                ]}
                locale={{
                  emptyText: "No review history for the current note."
                }}
              />
            )}
          </PageSection>
        </>
      )}
    </div>
  );
}

interface ReviewStatProps {
  title: string;
  value: number;
  suffix?: string;
}

function ReviewStat({ title, value, suffix }: ReviewStatProps) {
  return <Statistic title={title} value={value} suffix={suffix} />;
}
