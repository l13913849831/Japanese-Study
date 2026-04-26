import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, App, Button, DatePicker, Form, Input, InputNumber, Select, Space, Statistic, Table, Tag, Typography } from "antd";
import type { Dayjs } from "dayjs";
import dayjs from "dayjs";
import { useEffect, useMemo, useState } from "react";
import {
  getCardReviews,
  getTodayCards,
  submitCardReview,
  type ReviewCardPayload,
  type ReviewLogItem,
  type ReviewRating,
  type TodayCard
} from "@/features/cards/api";
import { buildReviewSessionSummary, resolveCurrentSessionIndex } from "@/features/review/session";
import { listStudyPlans, type StudyPlan } from "@/features/study-plans/api";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import { useUiStore } from "@/shared/store/useUiStore";

interface SearchFormValues {
  planId?: number;
  date?: Dayjs;
}

interface ReviewFormValues {
  responseTimeMs?: number;
  note?: string;
}

type SessionRowMode = "MAIN" | "REQUEUE" | "WEAK";

interface SessionCardRow {
  rowKey: string;
  cardId: number;
  mode: SessionRowMode;
}

type SessionCardTableRow = TodayCard & {
  rowKey: string;
  queueMode: SessionRowMode;
};

const reviewRatings: Array<{ rating: ReviewRating; label: string; type?: "primary" | "default" }> = [
  { rating: "AGAIN", label: "AGAIN" },
  { rating: "HARD", label: "HARD" },
  { rating: "GOOD", label: "GOOD", type: "primary" },
  { rating: "EASY", label: "EASY" }
];

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

function buildPlanLabel(plan: StudyPlan) {
  return `${plan.name} (${plan.status})`;
}

function resolvePreferredPlanId(plans: StudyPlan[], currentPlanId?: number, selectedPlanId?: number) {
  if (!plans.length) {
    return undefined;
  }

  const candidateIds = [selectedPlanId, currentPlanId];
  for (const candidateId of candidateIds) {
    if (candidateId !== undefined && plans.some((plan) => plan.id === candidateId)) {
      return candidateId;
    }
  }

  return plans[0]?.id;
}

export function TodayCardsPage() {
  const currentPlanId = useUiStore((state) => state.currentPlanId);
  const setCurrentPlanId = useUiStore((state) => state.setCurrentPlanId);
  const [searchForm] = Form.useForm<SearchFormValues>();
  const [search, setSearch] = useState<{ planId?: number; date: string }>({
    planId: currentPlanId,
    date: dayjs().format("YYYY-MM-DD")
  });
  const [mainQueue, setMainQueue] = useState<SessionCardRow[]>([]);
  const [weakQueue, setWeakQueue] = useState<SessionCardRow[]>([]);
  const [completedRowKeys, setCompletedRowKeys] = useState<string[]>([]);
  const [againCountByCardId, setAgainCountByCardId] = useState<Record<number, number>>({});
  const [weakRoundStarted, setWeakRoundStarted] = useState(false);
  const [weakRoundSkipped, setWeakRoundSkipped] = useState(false);
  const [currentRowKey, setCurrentRowKey] = useState<string>();
  const [reviewForm] = Form.useForm<ReviewFormValues>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  const studyPlansQuery = useQuery({
    queryKey: ["studyPlans"],
    queryFn: listStudyPlans
  });

  const enabled = Boolean(search.planId && search.date);
  const cardsQuery = useQuery({
    queryKey: ["todayCards", search.planId, search.date],
    queryFn: () => getTodayCards(search.planId!, search.date),
    enabled,
    refetchOnWindowFocus: false
  });

  const orderedCards = useMemo(() => sortCards(cardsQuery.data ?? []), [cardsQuery.data]);
  const cardById = useMemo(() => new Map(orderedCards.map((card) => [card.id, card])), [orderedCards]);
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
  const currentCard = currentRow ? cardById.get(currentRow.cardId) : undefined;
  const currentQueuePosition = currentCard ? resolvedCurrentIndex + 1 : 0;
  const currentPendingPosition = currentCard
    ? activeQueue
        .filter((item) => !completedRowKeySet.has(item.rowKey) || item.rowKey === currentRow?.rowKey)
        .findIndex((item) => item.rowKey === currentRow?.rowKey) + 1
    : 0;
  const shouldPromptWeakRound = !weakRoundStarted && !weakRoundSkipped && sessionSummary.pendingCount === 0 && weakQueue.length > 0;

  useEffect(() => {
    if (!enabled) {
      setMainQueue([]);
      setWeakQueue([]);
      setCompletedRowKeys([]);
      setAgainCountByCardId({});
      setWeakRoundStarted(false);
      setWeakRoundSkipped(false);
      setCurrentRowKey(undefined);
      return;
    }
    if (!orderedCards.length && !cardsQuery.isSuccess) {
      return;
    }
    setMainQueue(orderedCards.map((card) => ({ rowKey: `main-${card.id}`, cardId: card.id, mode: "MAIN" })));
    setWeakQueue([]);
    setCompletedRowKeys([]);
    setAgainCountByCardId({});
    setWeakRoundStarted(false);
    setWeakRoundSkipped(false);
    setCurrentRowKey(undefined);
  }, [cardsQuery.isSuccess, enabled, orderedCards, search.date, search.planId]);

  useEffect(() => {
    const nextRowKey = resolvedCurrentIndex === -1 ? undefined : activeQueue[resolvedCurrentIndex]?.rowKey;
    if (nextRowKey !== currentRowKey) {
      setCurrentRowKey(nextRowKey);
    }
  }, [activeQueue, currentRowKey, resolvedCurrentIndex]);

  const reviewsQuery = useQuery({
    queryKey: ["cardReviews", currentCard?.id],
    queryFn: () => getCardReviews(currentCard!.id),
    enabled: Boolean(currentCard?.id)
  });

  const reviewMutation = useMutation({
    mutationFn: ({
      cardId,
      payload
    }: {
      cardId: number;
      payload: ReviewCardPayload;
      queueRowKey: string;
      nextAgainCount: number;
    }) => submitCardReview(cardId, payload),
    onSuccess: async (result, variables) => {
      setCompletedRowKeys((prev) => (prev.includes(variables.queueRowKey) ? prev : [...prev, variables.queueRowKey]));
      if (result.rating === "AGAIN") {
        setAgainCountByCardId((prev) => ({
          ...prev,
          [result.cardId]: variables.nextAgainCount
        }));
      }
      if (result.todayAction === "REQUEUE_TODAY") {
        setMainQueue((prev) => {
          const rowKey = `requeue-${result.cardId}`;
          return prev.some((item) => item.rowKey === rowKey)
            ? prev
            : [...prev, { rowKey, cardId: result.cardId, mode: "REQUEUE" }];
        });
      }
      if (result.todayAction === "MOVE_TO_WEAK_ROUND") {
        setWeakQueue((prev) => {
          const rowKey = `weak-${result.cardId}`;
          return prev.some((item) => item.rowKey === rowKey)
            ? prev
            : [...prev, { rowKey, cardId: result.cardId, mode: "WEAK" }];
        });
      }
      message.success(resolveCardReviewMessage(result.todayAction));
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["cardReviews", variables.cardId] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
        queryClient.invalidateQueries({ queryKey: ["weakItemSummary"] }),
        queryClient.invalidateQueries({ queryKey: ["weakWords"] })
      ]);
      reviewForm.resetFields();
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const latestReview = reviewsQuery.data?.[0];
  const allPlans = studyPlansQuery.data?.items ?? [];
  const activePlans = allPlans.filter((plan) => plan.status === "ACTIVE");
  const planOptions = activePlans.length > 0 ? activePlans : allPlans;
  const preferredPlanId = resolvePreferredPlanId(planOptions, currentPlanId, search.planId);
  const hasPlanOptions = planOptions.length > 0;

  useEffect(() => {
    if (studyPlansQuery.isLoading) {
      return;
    }

    if (preferredPlanId === search.planId && preferredPlanId === currentPlanId) {
      return;
    }

    setSearch((prev) => {
      if (prev.planId === preferredPlanId) {
        return prev;
      }
      return {
        ...prev,
        planId: preferredPlanId
      };
    });
    setCurrentPlanId(preferredPlanId);
  }, [currentPlanId, preferredPlanId, search.planId, setCurrentPlanId, studyPlansQuery.isLoading]);

  useEffect(() => {
    searchForm.setFieldsValue({
      planId: search.planId,
      date: dayjs(search.date)
    });
  }, [search.date, search.planId, searchForm]);

  function handleReviewSubmit(rating: ReviewRating) {
    if (!currentCard || !currentRow) {
      message.warning("No current card in this session.");
      return;
    }

    const values = reviewForm.getFieldsValue();
    const nextAgainCount = rating === "AGAIN" ? (againCountByCardId[currentCard.id] ?? 0) + 1 : againCountByCardId[currentCard.id] ?? 0;
    reviewMutation.mutate({
      cardId: currentCard.id,
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

  function moveCurrentCard(offset: number) {
    const nextRow = activeQueue[resolvedCurrentIndex + offset];
    if (!nextRow) {
      return;
    }
    setCurrentRowKey(nextRow.rowKey);
    reviewForm.resetFields();
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Word Review Session"
        description="默认接上今天这组卡片，盯住当前一张，评分后继续往前走。"
        extra={<Tag color="purple">card_instance / review_log</Tag>}
      />

      {studyPlansQuery.isLoading ? (
        <StatusState mode="loading" />
      ) : !hasPlanOptions ? (
        <StatusState
          mode="empty"
          description="还没有可用学习计划。先去创建并激活一个计划，再回来开始今天复习。"
        />
      ) : !enabled ? (
        <StatusState mode="empty" description="当前没有可进入的复习会话。换个计划或日期再试。" />
      ) : cardsQuery.isLoading ? (
        <StatusState mode="loading" />
      ) : cardsQuery.isError ? (
        <StatusState mode="error" description={(cardsQuery.error as Error).message} />
      ) : (
        <>
          <div className="session-focus-layout">
            <div className="session-focus-main">
              <PageSection title="Current Card">
                {!currentCard ? (
                  <StatusState mode="empty" description="今天这组卡片已经走完，或者当前没有可继续的卡。"/>
                ) : (
                  <div className="session-primary-stack">
                    <div className="dashboard-overview-grid">
                      <CardStat title="Current" value={currentPendingPosition || 0} suffix={sessionSummary.pendingCount ? `/ ${sessionSummary.pendingCount}` : ""} />
                      <CardStat title="Total Due" value={sessionSummary.totalCount} />
                      <CardStat title="Completed" value={sessionSummary.completedCount} />
                      <CardStat title="Weak Round" value={pendingWeakCount} />
                    </div>

                    <div className="session-meta-stack">
                      <Space wrap>
                        <Tag color={currentRow?.mode === "WEAK" ? "volcano" : currentRow?.mode === "REQUEUE" ? "cyan" : "gold"}>
                          {currentRow?.mode}
                        </Tag>
                        <Tag>{currentCard.cardType}</Tag>
                        <Tag>Stage {currentCard.stageNo}</Tag>
                        <Tag>Due {currentCard.dueDate}</Tag>
                      </Space>

                      <div>
                        <Typography.Title level={2} style={{ margin: 0 }}>
                          {currentCard.expression || "-"}
                        </Typography.Title>
                        <Typography.Text type="secondary">
                          {currentCard.reading || "No reading"}
                        </Typography.Text>
                      </div>

                      <Typography.Paragraph style={{ marginBottom: 0 }}>
                        {currentCard.meaning || "No meaning"}
                      </Typography.Paragraph>

                      {currentCard.exampleJp ? (
                        <Typography.Paragraph style={{ marginBottom: 0 }}>
                          <Typography.Text strong>Example JP: </Typography.Text>
                          {currentCard.exampleJp}
                        </Typography.Paragraph>
                      ) : null}
                      {currentCard.exampleZh ? (
                        <Typography.Paragraph style={{ marginBottom: 0 }}>
                          <Typography.Text strong>Example ZH: </Typography.Text>
                          {currentCard.exampleZh}
                        </Typography.Paragraph>
                      ) : null}
                    </div>

                    <div className="session-action-row">
                      <Button onClick={() => moveCurrentCard(-1)} disabled={resolvedCurrentIndex <= 0}>
                        Previous
                      </Button>
                      <Button onClick={() => moveCurrentCard(1)} disabled={resolvedCurrentIndex >= activeQueue.length - 1}>
                        Next
                      </Button>
                    </div>

                    <Form<ReviewFormValues> form={reviewForm} layout="vertical">
                      <Form.Item label="Response Time (ms)" name="responseTimeMs">
                        <InputNumber min={0} style={{ width: "100%" }} placeholder="3200" />
                      </Form.Item>
                      <Form.Item label="Note" name="note">
                        <Input.TextArea rows={3} placeholder="Optional note for this review result" />
                      </Form.Item>
                    </Form>

                    <Space wrap>
                      {reviewRatings.map((item) => (
                        <Button
                          key={item.rating}
                          type={item.type ?? "default"}
                          loading={reviewMutation.isPending}
                          onClick={() => handleReviewSubmit(item.rating)}
                        >
                          {item.label}
                        </Button>
                      ))}
                    </Space>
                  </div>
                )}
              </PageSection>

              <PageSection title="Review History">
                {!currentCard ? (
                  <StatusState mode="empty" description="No current card selected." />
                ) : reviewsQuery.isLoading ? (
                  <StatusState mode="loading" />
                ) : reviewsQuery.isError ? (
                  <StatusState mode="error" description={(reviewsQuery.error as Error).message} />
                ) : (
                  <Table<ReviewLogItem>
                    rowKey="id"
                    pagination={false}
                    size="small"
                    dataSource={reviewsQuery.data ?? []}
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
                        width: 140,
                        render: (value?: number) => (value === undefined ? "-" : `${value} ms`)
                      },
                      {
                        title: "Note",
                        dataIndex: "note",
                        render: (value?: string) => value || "-"
                      }
                    ]}
                    locale={{
                      emptyText: "No review history for the current card."
                    }}
                  />
                )}
              </PageSection>
            </div>

            <div className="session-focus-side">
              <PageSection title="Session Progress">
                <div className="dashboard-overview-grid">
                  <CardStat title="Pending" value={sessionSummary.pendingCount} />
                  <CardStat title="Position" value={currentQueuePosition} suffix={sessionSummary.totalCount ? `/ ${sessionSummary.totalCount}` : ""} />
                </div>
                {sessionSummary.totalCount === 0 ? (
                  <Alert
                    style={{ marginTop: 16 }}
                    type={weakRoundStarted ? "success" : "info"}
                    showIcon
                    message={weakRoundStarted ? "薄弱轮已完成。" : "今天没有待复习卡片。"}
                    description={weakRoundStarted ? "主队列和薄弱轮都已完成。" : "可以换个计划，或者回工作台看别的学习线。"}
                  />
                ) : shouldPromptWeakRound ? (
                  <Alert
                    style={{ marginTop: 16 }}
                    type="success"
                    showIcon
                    message="主队列已完成。"
                    description={
                      <Space wrap>
                        <Typography.Text>还有 {weakQueue.length} 个薄弱项可再练一轮。</Typography.Text>
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
                    description="当前队列里的卡片都已经评完分。"
                  />
                ) : (
                  <Alert
                    style={{ marginTop: 16 }}
                    type="info"
                    showIcon
                    message={weakRoundStarted ? "正在进行薄弱轮。" : "当前流程"}
                    description={
                      weakRoundStarted
                        ? `先把薄弱项过完。当前待处理位置：${currentPendingPosition} / ${sessionSummary.pendingCount}`
                        : `看当前卡片，直接评分，系统会自动推进。当前待处理位置：${currentPendingPosition} / ${sessionSummary.pendingCount}`
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
                    const nextPlanId = values.planId;
                    setCurrentPlanId(nextPlanId);
                    reviewForm.resetFields();
                    setSearch({
                      planId: nextPlanId,
                      date: values.date?.format("YYYY-MM-DD") ?? dayjs().format("YYYY-MM-DD")
                    });
                    setCurrentRowKey(undefined);
                  }}
                >
                  <Form.Item label="Plan" name="planId" rules={[{ required: true, message: "Select a study plan." }]}>
                    <Select
                      loading={studyPlansQuery.isLoading}
                      options={planOptions.map((plan) => ({
                        label: buildPlanLabel(plan),
                        value: plan.id
                      }))}
                      placeholder="Select a study plan"
                    />
                  </Form.Item>
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
            <Table<SessionCardTableRow>
              rowKey="rowKey"
              size="small"
              pagination={false}
              dataSource={activeQueue.map((item) => ({
                ...cardById.get(item.cardId),
                rowKey: item.rowKey,
                queueMode: item.mode
              })).filter((item): item is SessionCardTableRow => Boolean(item?.id))}
              onRow={(record) => ({
                onClick: () => {
                  setCurrentRowKey(record.rowKey);
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
                {
                  title: "Expression",
                  dataIndex: "expression",
                  render: (value?: string) => value || "-"
                },
                {
                  title: "Type",
                  dataIndex: "cardType",
                  width: 100
                },
                {
                  title: "Queue",
                  dataIndex: "queueMode",
                  width: 110,
                  render: (value: SessionRowMode) => <Tag color={value === "WEAK" ? "volcano" : value === "REQUEUE" ? "cyan" : "default"}>{value}</Tag>
                },
                {
                  title: "Status",
                  dataIndex: "rowKey",
                  width: 110,
                  render: (value: string) => (
                    <Tag color={completedRowKeySet.has(value) ? "green" : "default"}>
                      {completedRowKeySet.has(value) ? "DONE" : "PENDING"}
                    </Tag>
                  )
                }
              ]}
              locale={{
                emptyText: "No cards in this queue."
              }}
            />
          </PageSection>
        </>
      )}
    </div>
  );
}

interface CardStatProps {
  title: string;
  value: number;
  suffix?: string;
}

function CardStat({ title, value, suffix }: CardStatProps) {
  return (
    <div style={{ minWidth: 0 }}>
      <Statistic title={title} value={value} suffix={suffix} />
    </div>
  );
}

function resolveCardReviewMessage(todayAction: "DONE" | "REQUEUE_TODAY" | "MOVE_TO_WEAK_ROUND") {
  if (todayAction === "REQUEUE_TODAY") {
    return "已回到今日队尾。";
  }
  if (todayAction === "MOVE_TO_WEAK_ROUND") {
    return "已加入薄弱项再练一轮。";
  }
  return "已记录评分。";
}
