import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, App, Button, DatePicker, Form, Input, InputNumber, Space, Statistic, Table, Tag, Typography } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getMe } from "@/features/auth/api";
import { getStudyDashboard } from "@/features/dashboard/api";
import { getNoteDashboard } from "@/features/notes/api";
import { buildReviewSessionSummary, resolveCurrentSessionIndex } from "@/features/review/session";
import {
  getLearningLineSessionLabel,
  resolveLearningPathState,
  type LearningLine
} from "@/features/review/learningPath";
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
import { getWeakItemSummary } from "@/features/weak-items/api";

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

const SESSION_DATE_FORMAT = "YYYY-MM-DD";

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

const NOTE_QUEUE_MODE_LABELS: Record<SessionQueueMode, string> = {
  MAIN: "主队列",
  RECOVERY: "恢复队列",
  WEAK: "薄弱轮"
};

function sortNotes(items: NoteReviewQueueItem[]) {
  return items.slice().sort((left, right) => {
    const dueCompare = dayjs(left.dueAt).valueOf() - dayjs(right.dueAt).valueOf();
    if (dueCompare !== 0) {
      return dueCompare;
    }
    return left.id - right.id;
  });
}

function normalizeDate(value: string | null | undefined) {
  if (!value) {
    return dayjs().format(SESSION_DATE_FORMAT);
  }
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format(SESSION_DATE_FORMAT) : dayjs().format(SESSION_DATE_FORMAT);
}

function getNoteQueueModeDescription(mode: SessionQueueMode) {
  if (mode === "RECOVERY") {
    return "这个知识点今天答过 AGAIN，所以会在当天再回到你面前一次，确认是否真的恢复。";
  }
  if (mode === "WEAK") {
    return "这个知识点今天多次 AGAIN，已经进入薄弱轮。主队列结束后，再集中补一轮。";
  }
  return "这是今天的知识点主复习流。先按标题回忆，把主队列清完，再决定是否处理薄弱轮。";
}

export function NoteReviewPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [searchForm] = Form.useForm<SearchFormValues>();
  const [reviewForm] = Form.useForm<ReviewFormValues>();
  const [search, setSearch] = useState<{ date: string }>(() => ({
    date: normalizeDate(searchParams.get("date"))
  }));
  const [mainQueue, setMainQueue] = useState<SessionNoteRow[]>([]);
  const [weakQueue, setWeakQueue] = useState<SessionNoteRow[]>([]);
  const [completedRowKeys, setCompletedRowKeys] = useState<string[]>([]);
  const [againCountByNoteId, setAgainCountByNoteId] = useState<Record<number, number>>({});
  const [sessionNotesById, setSessionNotesById] = useState<Record<number, NoteReviewQueueItem>>({});
  const [weakRoundStarted, setWeakRoundStarted] = useState(false);
  const [weakRoundSkipped, setWeakRoundSkipped] = useState(false);
  const [currentRowKey, setCurrentRowKey] = useState<string>();
  const [revealed, setRevealed] = useState(false);
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  useEffect(() => {
    searchForm.setFieldsValue({ date: dayjs(search.date) });
  }, [search.date, searchForm]);

  useEffect(() => {
    const nextParams = new URLSearchParams();
    nextParams.set("date", search.date);
    if (nextParams.toString() !== searchParams.toString()) {
      setSearchParams(nextParams, { replace: true });
    }
  }, [search.date, searchParams, setSearchParams]);

  const todayReviewsQuery = useQuery({
    queryKey: ["todayNoteReviews", search.date],
    queryFn: () => getTodayNoteReviews(search.date),
    refetchOnWindowFocus: false
  });
  const dashboardQuery = useQuery({
    queryKey: ["dashboard", search.date],
    queryFn: () => getStudyDashboard(search.date)
  });
  const noteDashboardQuery = useQuery({
    queryKey: ["noteDashboard", search.date],
    queryFn: () => getNoteDashboard(search.date)
  });
  const currentUserQuery = useQuery({
    queryKey: ["me"],
    queryFn: getMe,
    retry: false
  });
  const weakItemSummaryQuery = useQuery({
    queryKey: ["weakItemSummary"],
    queryFn: getWeakItemSummary
  });

  const orderedNotes = useMemo(() => sortNotes(todayReviewsQuery.data ?? []), [todayReviewsQuery.data]);
  const completedRowKeySet = useMemo(() => new Set(completedRowKeys), [completedRowKeys]);
  const activeQueue = weakRoundStarted ? weakQueue : mainQueue;
  const queueRows = useMemo(
    () =>
      activeQueue
        .map((item) => ({
          ...sessionNotesById[item.noteId],
          rowKey: item.rowKey,
          queueMode: item.mode
        }))
        .filter((item): item is SessionNoteTableRow => Boolean(item?.id)),
    [activeQueue, sessionNotesById]
  );
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
  const currentQueuePosition = currentNote ? resolvedCurrentIndex + 1 : 0;
  const currentPendingPosition = currentNote
    ? activeQueue
        .filter((item) => !completedRowKeySet.has(item.rowKey) || item.rowKey === currentRow?.rowKey)
        .findIndex((item) => item.rowKey === currentRow?.rowKey) + 1
    : 0;
  const remainingCount = currentNote ? Math.max(sessionSummary.totalCount - resolvedCurrentIndex - 1, 0) : 0;
  const preferredLearningOrder = currentUserQuery.data?.preferredLearningOrder ?? "WORD_FIRST";
  const learningPathState = useMemo(
    () =>
      resolveLearningPathState(preferredLearningOrder, {
        wordPendingCount: dashboardQuery.data?.overview.pendingDueToday ?? 0,
        notePendingCount: noteDashboardQuery.data?.overview.dueToday ?? 0
      }),
    [
      dashboardQuery.data?.overview.pendingDueToday,
      noteDashboardQuery.data?.overview.dueToday,
      preferredLearningOrder
    ]
  );
  const recommendedLine = learningPathState.recommendedLine;
  const recommendedWordPlanId =
    dashboardQuery.data?.activePlans.find((plan) => plan.pendingToday > 0)?.planId ?? dashboardQuery.data?.activePlans[0]?.planId;
  const totalWeakItems = (weakItemSummaryQuery.data?.weakWordCount ?? 0) + (weakItemSummaryQuery.data?.weakNoteCount ?? 0);
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
    setRevealed(false);
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

  function handleNextLearningAction(line: LearningLine) {
    if (line === "WORD") {
      if (recommendedWordPlanId) {
        navigate(`/cards?planId=${recommendedWordPlanId}&date=${search.date}`);
        return;
      }
      navigate("/dashboard");
      return;
    }

    navigate("/dashboard");
  }

  function openClosureExport() {
    const params = new URLSearchParams();
    if (recommendedWordPlanId) {
      params.set("planId", String(recommendedWordPlanId));
    }
    params.set("targetDate", search.date);
    params.set("source", "closure");
    navigate(`/export-jobs?${params.toString()}`);
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Note Review Session"
        description="Keep one knowledge point in focus: recall from the title first, reveal only when needed, score it, then continue forward."
        extra={
          <Space wrap>
            <Tag color="gold">{search.date}</Tag>
            <Tag color="purple">fsrs</Tag>
          </Space>
        }
      />

      <div className="review-session-layout">
        <div className="review-session-main">
          <PageSection title="Current Note">
            {todayReviewsQuery.isLoading ? (
              <StatusState mode="loading" />
            ) : todayReviewsQuery.isError ? (
              <StatusState mode="error" description={(todayReviewsQuery.error as Error).message} />
            ) : sessionSummary.totalCount === 0 ? (
              <StatusState
                mode="empty"
                description={
                  weakRoundStarted
                    ? "Today's main queue and weak round are both complete."
                    : "No knowledge point is due for the selected date."
                }
              />
            ) : !currentNote ? (
              <StatusState mode="empty" description="No current knowledge point is available for this session." />
            ) : (
              <div className="review-session-focus">
                <Space wrap>
                  <Tag color={currentRow?.mode === "WEAK" ? "volcano" : currentRow?.mode === "RECOVERY" ? "cyan" : "blue"}>
                    {currentRow ? NOTE_QUEUE_MODE_LABELS[currentRow.mode] : "-"}
                  </Tag>
                  <Tag color="blue">{NOTE_MASTERY_LABELS[currentNote.masteryStatus]}</Tag>
                  <Tag>{`Review ${currentNote.reviewCount}`}</Tag>
                  <Tag>{dayjs(currentNote.dueAt).format("YYYY-MM-DD HH:mm")}</Tag>
                </Space>

                <div className="review-session-copy">
                  <Typography.Text type="secondary">先看标题，先回忆，再决定是否展开</Typography.Text>
                  <Typography.Title level={2} style={{ margin: 0 }}>
                    {currentNote.title}
                  </Typography.Title>
                  <Typography.Text type="secondary">
                    {currentNote.tags.length ? currentNote.tags.join(", ") : "No tags"}
                  </Typography.Text>
                </div>

                {!revealed ? (
                  <Alert
                    type="info"
                    showIcon
                    message="先主动回忆"
                    description="想过一遍再展开内容。评分动作会保持在同一个位置，不需要跳回列表。"
                    action={
                      <Button type="primary" onClick={() => setRevealed(true)}>
                        显示内容
                      </Button>
                    }
                  />
                ) : (
                  <div className="review-session-answer">
                    <Space wrap>
                      <Button onClick={() => setRevealed(false)}>收起内容</Button>
                    </Space>
                    <Typography.Paragraph style={{ whiteSpace: "pre-wrap", marginBottom: 0 }}>
                      {currentNote.content}
                    </Typography.Paragraph>
                  </div>
                )}

                <Form<ReviewFormValues> form={reviewForm} layout="vertical">
                  <div className="review-session-form-grid">
                    <Form.Item label="Response Time (ms)" name="responseTimeMs">
                      <InputNumber min={0} style={{ width: "100%" }} placeholder="3200" />
                    </Form.Item>
                    <Form.Item label="Review Note" name="note">
                      <Input.TextArea rows={3} placeholder="Optional reflection for this review" />
                    </Form.Item>
                  </div>
                </Form>

                <div className="review-session-rating-grid">
                  {REVIEW_ACTIONS.map((action) => (
                    <Button
                      key={action.rating}
                      size="large"
                      type={action.type ?? "default"}
                      loading={reviewMutation.isPending}
                      onClick={() => handleSubmitReview(action.rating)}
                    >
                      {action.label}
                    </Button>
                  ))}
                </div>
              </div>
            )}
          </PageSection>
        </div>

        <div className="review-session-side">
          <PageSection title="Session Rail">
            <div className="review-session-side-stack">
              <div className="dashboard-overview-grid">
                <ReviewStat title="Queue" value={sessionSummary.totalCount} />
                <ReviewStat title="Pending" value={sessionSummary.pendingCount} />
                <ReviewStat title="Remaining" value={remainingCount} />
                <ReviewStat title="Weak Round" value={pendingWeakCount} />
                <ReviewStat title="Position" value={currentQueuePosition} suffix={sessionSummary.totalCount ? `/ ${sessionSummary.totalCount}` : ""} />
              </div>

              {shouldPromptWeakRound ? (
                <Alert
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
              ) : sessionSummary.pendingCount === 0 && sessionSummary.totalCount > 0 ? (
                <Alert
                  type="success"
                  showIcon
                  message="当前知识点会话已完成。"
                  description={
                    <Space wrap>
                      <Typography.Text>
                        {learningPathState.recommendedLine === "WORD"
                          ? "下一步建议切到单词线，把今天的主路径走完。"
                          : learningPathState.isComplete
                            ? totalWeakItems > 0
                              ? `今天主路径已清空，另外还有 ${totalWeakItems} 个薄弱项可选加练。`
                              : "今天两条学习线都已清空，可以回工作台收尾。"
                            : "如果还要继续知识点线，先回工作台确认剩余任务。"}
                      </Typography.Text>
                      {recommendedLine ? (
                        <Button
                          type="primary"
                          size="small"
                          onClick={() => handleNextLearningAction(recommendedLine)}
                        >
                          {recommendedLine === "WORD"
                            ? `去看${getLearningLineSessionLabel(recommendedLine)}`
                            : "回工作台继续知识点线"}
                        </Button>
                      ) : totalWeakItems > 0 ? (
                        <Button type="primary" size="small" onClick={() => navigate("/weak-items")}>
                          去看薄弱项
                        </Button>
                      ) : (
                        <Button type="primary" size="small" onClick={() => navigate("/dashboard")}>
                          回工作台
                        </Button>
                      )}
                      <Button size="small" onClick={openClosureExport}>
                        导出复盘材料
                      </Button>
                    </Space>
                  }
                />
              ) : (
                <Alert
                  type="info"
                  showIcon
                  message={currentRow ? `当前层级：${NOTE_QUEUE_MODE_LABELS[currentRow.mode]}` : "当前处于标题回忆主路径"}
                  description={
                    currentRow
                      ? `${getNoteQueueModeDescription(currentRow.mode)} 当前待处理位置 ${currentPendingPosition} / ${sessionSummary.pendingCount}。`
                      : revealed
                        ? "Content is open. Score it and the session will advance automatically."
                        : "Recall from the title first. Reveal only when you need the full content."
                  }
                />
              )}

              <Form<SearchFormValues>
                form={searchForm}
                layout="vertical"
                onFinish={(values) => {
                  setCurrentRowKey(undefined);
                  setRevealed(false);
                  reviewForm.resetFields();
                  setSearch({ date: values.date?.format(SESSION_DATE_FORMAT) ?? dayjs().format(SESSION_DATE_FORMAT) });
                }}
              >
                <Form.Item label="Date" name="date" rules={[{ required: true, message: "Select a date." }]}>
                  <DatePicker />
                </Form.Item>
                <Space wrap>
                  <Button type="primary" htmlType="submit">
                    Refresh Session
                  </Button>
                  <Button
                    onClick={() => {
                      setCurrentRowKey(undefined);
                      setRevealed(false);
                      reviewForm.resetFields();
                      setSearch({ date: dayjs().format(SESSION_DATE_FORMAT) });
                    }}
                  >
                    Back To Today
                  </Button>
                </Space>
              </Form>

              <Space wrap>
                <Button onClick={() => moveCurrentNote(-1)} disabled={resolvedCurrentIndex <= 0}>
                  Previous
                </Button>
                <Button onClick={() => moveCurrentNote(1)} disabled={resolvedCurrentIndex >= activeQueue.length - 1}>
                  Next
                </Button>
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
              ) : currentNote ? (
                <Typography.Text type="secondary">No review history for the current note yet.</Typography.Text>
              ) : null}
            </div>
          </PageSection>
        </div>
      </div>

      <div className="review-session-support-grid">
        <PageSection title="Queue Assistant">
          <Table<SessionNoteTableRow>
            rowKey="rowKey"
            size="small"
            pagination={false}
            dataSource={queueRows}
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
