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

type SessionQueueMode = "MAIN" | "RECOVERY" | "WEAK";

interface SessionNoteRow {
  rowKey: string;
  noteId: number;
  mode: SessionQueueMode;
}

type SessionNoteTableRow = NoteReviewQueueItem & {
  rowKey: string;
  queueMode: SessionQueueMode;
};

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
  const [searchForm] = Form.useForm<SearchFormValues>();
  const [search, setSearch] = useState<{ date: string }>({ date: dayjs().format("YYYY-MM-DD") });
  const [mainQueue, setMainQueue] = useState<SessionNoteRow[]>([]);
  const [weakQueue, setWeakQueue] = useState<SessionNoteRow[]>([]);
  const [completedRowKeys, setCompletedRowKeys] = useState<string[]>([]);
  const [againCountByNoteId, setAgainCountByNoteId] = useState<Record<number, number>>({});
  const [sessionNotesById, setSessionNotesById] = useState<Record<number, NoteReviewQueueItem>>({});
  const [weakRoundStarted, setWeakRoundStarted] = useState(false);
  const [weakRoundSkipped, setWeakRoundSkipped] = useState(false);
  const [currentRowKey, setCurrentRowKey] = useState<string>();
  const [revealed, setRevealed] = useState(false);
  const [reviewForm] = Form.useForm<ReviewFormValues>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  const todayReviewsQuery = useQuery({
    queryKey: ["todayNoteReviews", search.date],
    queryFn: () => getTodayNoteReviews(search.date),
    refetchOnWindowFocus: false
  });

  const orderedNotes = useMemo(() => sortNotes(todayReviewsQuery.data ?? []), [todayReviewsQuery.data]);
  const completedRowKeySet = useMemo(() => new Set(completedRowKeys), [completedRowKeys]);
  const activeQueue = weakRoundStarted ? weakQueue : mainQueue;
  const pendingWeakCount = weakQueue.filter((item) => !completedRowKeySet.has(item.rowKey)).length;
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
  const remainingCount = currentNote ? Math.max(sessionSummary.totalCount - resolvedCurrentIndex - 1, 0) : 0;
  const shouldPromptWeakRound = !weakRoundStarted && !weakRoundSkipped && sessionSummary.pendingCount === 0 && weakQueue.length > 0;

  useEffect(() => {
    const nextNotesById = orderedNotes.reduce<Record<number, NoteReviewQueueItem>>((accumulator, item) => {
      accumulator[item.id] = item;
      return accumulator;
    }, {});
    setSessionNotesById(nextNotesById);
    setMainQueue(orderedNotes.map((item) => ({ rowKey: `main-${item.id}`, noteId: item.id, mode: "MAIN" })));
    setWeakQueue([]);
    setCompletedRowKeys([]);
    setAgainCountByNoteId({});
    setWeakRoundStarted(false);
    setWeakRoundSkipped(false);
    setCurrentRowKey(undefined);
  }, [orderedNotes, search.date]);

  useEffect(() => {
    const nextRowKey = resolvedCurrentIndex === -1 ? undefined : activeQueue[resolvedCurrentIndex]?.rowKey;
    if (nextRowKey !== currentRowKey) {
      setCurrentRowKey(nextRowKey);
      setRevealed(false);
    }
  }, [activeQueue, currentRowKey, resolvedCurrentIndex]);

  const reviewLogsQuery = useQuery({
    queryKey: ["noteReviewLogs", currentNote?.id],
    queryFn: () => getNoteReviews(currentNote!.id),
    enabled: Boolean(currentNote?.id)
  });

  const reviewMutation = useMutation({
    mutationFn: ({
      noteId,
      payload
    }: {
      noteId: number;
      payload: ReviewNotePayload;
      queueRowKey: string;
      nextAgainCount: number;
    }) => submitNoteReview(noteId, payload),
    onSuccess: async (result, variables) => {
      setCompletedRowKeys((prev) => (prev.includes(variables.queueRowKey) ? prev : [...prev, variables.queueRowKey]));
      if (result.rating === "AGAIN") {
        setAgainCountByNoteId((prev) => ({
          ...prev,
          [result.noteId]: variables.nextAgainCount
        }));
      }
      setSessionNotesById((prev) => {
        const current = prev[result.noteId];
        if (!current) {
          return prev;
        }
        return {
          ...prev,
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
        setMainQueue((prev) => {
          const rowKey = `recovery-${result.noteId}`;
          return prev.some((item) => item.rowKey === rowKey)
            ? prev
            : [...prev, { rowKey, noteId: result.noteId, mode: "RECOVERY" }];
        });
      }
      if (result.todayAction === "MOVE_TO_WEAK_ROUND") {
        setWeakQueue((prev) => {
          const rowKey = `weak-${result.noteId}`;
          return prev.some((item) => item.rowKey === rowKey)
            ? prev
            : [...prev, { rowKey, noteId: result.noteId, mode: "WEAK" }];
        });
      }
      message.success(resolveNoteReviewMessage(result.todayAction));
      setRevealed(false);
      reviewForm.resetFields();
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["noteReviewLogs", variables.noteId] }),
        queryClient.invalidateQueries({ queryKey: ["notes"] }),
        queryClient.invalidateQueries({ queryKey: ["noteDashboard"] }),
        queryClient.invalidateQueries({ queryKey: ["weakItemSummary"] }),
        queryClient.invalidateQueries({ queryKey: ["weakNotes"] })
      ]);
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const latestReview = reviewLogsQuery.data?.[0];

  useEffect(() => {
    searchForm.setFieldsValue({ date: dayjs(search.date) });
  }, [search.date, searchForm]);

  function handleSubmitReview(rating: NoteReviewRating) {
    if (!currentNote || !currentRow) {
      message.warning("No current knowledge point in this session.");
      return;
    }
    const values = reviewForm.getFieldsValue();
    const nextAgainCount = rating === "AGAIN" ? (againCountByNoteId[currentNote.id] ?? 0) + 1 : againCountByNoteId[currentNote.id] ?? 0;
    reviewMutation.mutate({
      noteId: currentNote.id,
      queueRowKey: currentRow.rowKey,
      nextAgainCount,
      payload: {
        rating,
        responseTimeMs: values.responseTimeMs,
        sessionAgainCount: nextAgainCount,
        note: values.note?.trim() || undefined
      }
    });
  }

  function moveCurrentNote(offset: number) {
    const nextRow = activeQueue[resolvedCurrentIndex + offset];
    if (!nextRow) {
      return;
    }
    setCurrentRowKey(nextRow.rowKey);
    setRevealed(false);
    reviewForm.resetFields();
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Note Review Session"
        description="默认进入今天的知识点会话。先看标题回忆，需要时再展开内容，然后评分继续。"
        extra={<Tag color="purple">fsrs</Tag>}
      />

      {todayReviewsQuery.isLoading ? (
        <StatusState mode="loading" />
      ) : todayReviewsQuery.isError ? (
        <StatusState mode="error" description={(todayReviewsQuery.error as Error).message} />
      ) : (
        <>
          <div className="session-focus-layout">
            <div className="session-focus-main">
              <PageSection title="Current Note">
                {!currentNote ? (
                  <StatusState mode="empty" description="今天没有可继续的知识点，或者这轮会话已经走完。"/>
                ) : (
                  <div className="session-primary-stack">
                    <div className="dashboard-overview-grid">
                      <ReviewStat title="Current" value={currentNote ? resolvedCurrentIndex + 1 : 0} suffix={sessionSummary.totalCount ? `/ ${sessionSummary.totalCount}` : ""} />
                      <ReviewStat title="Remaining" value={remainingCount} />
                      <ReviewStat title="Weak Round" value={pendingWeakCount} />
                      <ReviewStat title="Revealed" value={revealed ? 1 : 0} suffix={currentNote ? "/ 1" : ""} />
                    </div>

                    <div className="session-meta-stack">
                      <Space wrap>
                        <Tag color={currentRow?.mode === "WEAK" ? "volcano" : currentRow?.mode === "RECOVERY" ? "cyan" : "blue"}>
                          {currentRow?.mode}
                        </Tag>
                        <Tag color="blue">{NOTE_MASTERY_LABELS[currentNote.masteryStatus]}</Tag>
                        <Tag>{`Review ${currentNote.reviewCount}`}</Tag>
                        <Tag>{dayjs(currentNote.dueAt).format("YYYY-MM-DD HH:mm")}</Tag>
                      </Space>

                      <div>
                        <Typography.Title level={2} style={{ margin: 0 }}>
                          {currentNote.title}
                        </Typography.Title>
                        <Typography.Text type="secondary">
                          {currentNote.tags.length ? currentNote.tags.join(", ") : "No tags"}
                        </Typography.Text>
                      </div>

                      {!revealed ? (
                        <Button type="primary" onClick={() => setRevealed(true)}>
                          显示内容
                        </Button>
                      ) : (
                        <Typography.Paragraph style={{ whiteSpace: "pre-wrap", marginBottom: 0 }}>
                          {currentNote.content}
                        </Typography.Paragraph>
                      )}
                    </div>

                    <div className="session-action-row">
                      <Button onClick={() => moveCurrentNote(-1)} disabled={resolvedCurrentIndex <= 0}>
                        Previous
                      </Button>
                      <Button onClick={() => moveCurrentNote(1)} disabled={resolvedCurrentIndex >= activeQueue.length - 1}>
                        Next
                      </Button>
                    </div>

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
                  </div>
                )}
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
            </div>

            <div className="session-focus-side">
              <PageSection title="Session Progress">
                <div className="dashboard-overview-grid">
                  <ReviewStat title="Queue Size" value={sessionSummary.totalCount} />
                  <ReviewStat title="Pending" value={sessionSummary.pendingCount} />
                </div>
                {sessionSummary.totalCount === 0 ? (
                  <Alert
                    style={{ marginTop: 16 }}
                    type={weakRoundStarted ? "success" : "info"}
                    showIcon
                    message={weakRoundStarted ? "薄弱轮已完成。" : "今天没有待复习知识点。"}
                    description={weakRoundStarted ? "今日主队列和薄弱轮都已完成。" : "可以换个日期，或者回工作台继续别的学习线。"}
                  />
                ) : shouldPromptWeakRound ? (
                  <Alert
                    style={{ marginTop: 16 }}
                    type="success"
                    showIcon
                    message="主队列已完成。"
                    description={
                      <Space wrap>
                        <Typography.Text>还有 {weakQueue.length} 个薄弱知识点可再练一轮。</Typography.Text>
                        <Button type="primary" size="small" onClick={() => setWeakRoundStarted(true)}>
                          开始薄弱轮
                        </Button>
                        <Button size="small" onClick={() => setWeakRoundSkipped(true)}>
                          稍后再说
                        </Button>
                      </Space>
                    }
                  />
                ) : sessionSummary.pendingCount === 0 ? (
                  <Alert
                    style={{ marginTop: 16 }}
                    type="success"
                    showIcon
                    message="这轮会话已完成。"
                    description="当前队列里的知识点都已经评完分。"
                  />
                ) : (
                  <Alert
                    style={{ marginTop: 16 }}
                    type="info"
                    showIcon
                    message={weakRoundStarted ? "正在进行薄弱轮。" : "当前流程"}
                    description={
                      weakRoundStarted
                        ? "先把刚才没稳住的知识点再过一遍。"
                        : "先看标题回忆，需要时再展开内容，评分后系统会自动推进。"
                    }
                  />
                )}

                {latestReview ? (
                  <Alert
                    style={{ marginTop: 16 }}
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
                ) : null}
              </PageSection>

              <PageSection title="Session Controls">
                <Form<SearchFormValues>
                  form={searchForm}
                  layout="vertical"
                  onFinish={(values) => {
                    setCurrentRowKey(undefined);
                    setRevealed(false);
                    reviewForm.resetFields();
                    setSearch({ date: values.date?.format("YYYY-MM-DD") ?? dayjs().format("YYYY-MM-DD") });
                  }}
                >
                  <Form.Item label="Date" name="date" rules={[{ required: true, message: "Select a date." }]}>
                    <DatePicker style={{ width: "100%" }} />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block>
                    Reload Session
                  </Button>
                </Form>
              </PageSection>
            </div>
          </div>

          <PageSection title="Session Queue">
            <Table<SessionNoteTableRow>
              rowKey="rowKey"
              size="small"
              pagination={false}
              dataSource={activeQueue.map((item) => ({
                ...sessionNotesById[item.noteId],
                rowKey: item.rowKey,
                queueMode: item.mode
              })).filter((item): item is SessionNoteTableRow => Boolean(item?.id))}
              onRow={(record) => ({
                onClick: () => {
                  setCurrentRowKey(record.rowKey);
                  setRevealed(false);
                  reviewForm.resetFields();
                }
              })}
              rowClassName={(record) => (record.rowKey === currentRow?.rowKey ? "review-session-row-active" : "")}
              columns={[
                {
                  title: "#",
                  width: 70,
                  render: (_, __, index) => index + 1
                },
                { title: "Title", dataIndex: "title" },
                {
                  title: "Queue",
                  dataIndex: "queueMode",
                  width: 120,
                  render: (value: SessionQueueMode) => <Tag color={value === "WEAK" ? "volcano" : value === "RECOVERY" ? "cyan" : "default"}>{value}</Tag>
                },
                {
                  title: "Mastery",
                  dataIndex: "masteryStatus",
                  width: 120,
                  render: (value: NoteMasteryStatus) => NOTE_MASTERY_LABELS[value]
                },
                {
                  title: "Status",
                  dataIndex: "rowKey",
                  width: 120,
                  render: (value: string) => (
                    <Tag color={completedRowKeySet.has(value) ? "green" : "default"}>
                      {completedRowKeySet.has(value) ? "DONE" : "PENDING"}
                    </Tag>
                  )
                }
              ]}
              locale={{
                emptyText: "No knowledge point is due for this queue."
              }}
            />
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

function resolveNoteReviewMessage(todayAction: "DONE" | "MOVE_TO_RECOVERY_QUEUE" | "MOVE_TO_WEAK_ROUND") {
  if (todayAction === "MOVE_TO_RECOVERY_QUEUE") {
    return "已加入今日待回顾队列。";
  }
  if (todayAction === "MOVE_TO_WEAK_ROUND") {
    return "已加入薄弱知识点再练一轮。";
  }
  return "已记录评分。";
}
