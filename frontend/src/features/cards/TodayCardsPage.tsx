import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, App, Button, DatePicker, Form, Input, InputNumber, Select, Space, Statistic, Table, Tag, Typography } from "antd";
import type { Dayjs } from "dayjs";
import dayjs from "dayjs";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
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

const SESSION_DATE_FORMAT = "YYYY-MM-DD";

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

function parsePlanId(value: string | null) {
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined;
}

function normalizeDate(value: string | null | undefined) {
  if (!value) {
    return dayjs().format(SESSION_DATE_FORMAT);
  }
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format(SESSION_DATE_FORMAT) : dayjs().format(SESSION_DATE_FORMAT);
}

function buildSearchParams(planId: number | undefined, date: string) {
  const params = new URLSearchParams();
  if (planId) {
    params.set("planId", String(planId));
  }
  params.set("date", date);
  return params;
}

export function TodayCardsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const currentPlanId = useUiStore((state) => state.currentPlanId);
  const setCurrentPlanId = useUiStore((state) => state.setCurrentPlanId);
  const [searchForm] = Form.useForm<SearchFormValues>();
  const [reviewForm] = Form.useForm<ReviewFormValues>();
  const [search, setSearch] = useState<{ planId?: number; date: string }>(() => ({
    planId: parsePlanId(searchParams.get("planId")) ?? currentPlanId,
    date: normalizeDate(searchParams.get("date"))
  }));
  const [mainQueue, setMainQueue] = useState<SessionCardRow[]>([]);
  const [weakQueue, setWeakQueue] = useState<SessionCardRow[]>([]);
  const [completedRowKeys, setCompletedRowKeys] = useState<string[]>([]);
  const [againCountByCardId, setAgainCountByCardId] = useState<Record<number, number>>({});
  const [weakRoundStarted, setWeakRoundStarted] = useState(false);
  const [weakRoundSkipped, setWeakRoundSkipped] = useState(false);
  const [currentRowKey, setCurrentRowKey] = useState<string>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  const studyPlansQuery = useQuery({
    queryKey: ["studyPlans"],
    queryFn: listStudyPlans
  });

  const allPlans = studyPlansQuery.data?.items ?? [];
  const activePlans = allPlans.filter((plan) => plan.status === "ACTIVE");
  const currentStoredPlan = currentPlanId ? allPlans.find((plan) => plan.id === currentPlanId) : undefined;
  const selectedPlan = search.planId ? allPlans.find((plan) => plan.id === search.planId) : undefined;
  const autoPlanId = currentStoredPlan?.id ?? activePlans[0]?.id;
  const canAutoStart = Boolean(selectedPlan || autoPlanId);
  const enabled = Boolean(search.planId && search.date);

  useEffect(() => {
    if (selectedPlan || studyPlansQuery.isLoading) {
      return;
    }
    if (!autoPlanId) {
      if (search.planId !== undefined) {
        setSearch((previous) => ({ ...previous, planId: undefined }));
      }
      return;
    }
    setCurrentPlanId(autoPlanId);
    setSearch((previous) => (previous.planId === autoPlanId ? previous : { ...previous, planId: autoPlanId }));
  }, [autoPlanId, search.planId, selectedPlan, setCurrentPlanId, studyPlansQuery.isLoading]);

  useEffect(() => {
    searchForm.setFieldsValue({
      planId: search.planId,
      date: dayjs(search.date)
    });
  }, [search.date, search.planId, searchForm]);

  useEffect(() => {
    const nextParams = buildSearchParams(search.planId, search.date).toString();
    if (nextParams !== searchParams.toString()) {
      setSearchParams(buildSearchParams(search.planId, search.date), { replace: true });
    }
  }, [search.date, search.planId, searchParams, setSearchParams]);

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
  const queueRows = useMemo(
    () =>
      activeQueue
        .map((item) => ({
          ...cardById.get(item.cardId),
          rowKey: item.rowKey,
          queueMode: item.mode
        }))
        .filter((item): item is SessionCardTableRow => Boolean(item?.id)),
    [activeQueue, cardById]
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
      setCompletedRowKeys((previous) => (previous.includes(variables.queueRowKey) ? previous : [...previous, variables.queueRowKey]));
      if (result.rating === "AGAIN") {
        setAgainCountByCardId((previous) => ({
          ...previous,
          [result.cardId]: variables.nextAgainCount
        }));
      }
      if (result.todayAction === "REQUEUE_TODAY") {
        setMainQueue((previous) => {
          const rowKey = `requeue-${result.cardId}`;
          return previous.some((item) => item.rowKey === rowKey)
            ? previous
            : [...previous, { rowKey, cardId: result.cardId, mode: "REQUEUE" }];
        });
      }
      if (result.todayAction === "MOVE_TO_WEAK_ROUND") {
        setWeakQueue((previous) => {
          const rowKey = `weak-${result.cardId}`;
          return previous.some((item) => item.rowKey === rowKey)
            ? previous
            : [...previous, { rowKey, cardId: result.cardId, mode: "WEAK" }];
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
  const planOptions = allPlans.map((plan) => ({
    label: buildPlanLabel(plan),
    value: plan.id
  }));

  function updateSearch(nextSearch: { planId?: number; date: string }) {
    setCurrentPlanId(nextSearch.planId);
    setSearch(nextSearch);
    setCurrentRowKey(undefined);
    reviewForm.resetFields();
  }

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
        description="Open the current plan directly, stay on the current card, and only drop into queue details when you need manual control."
        extra={
          <Space wrap>
            <Tag color="gold">{search.date}</Tag>
            {selectedPlan ? <Tag color="blue">{selectedPlan.name}</Tag> : null}
            <Tag color="purple">card_instance / review_log</Tag>
          </Space>
        }
      />

      {studyPlansQuery.isLoading && !canAutoStart ? (
        <StatusState mode="loading" />
      ) : !canAutoStart ? (
        <PageSection title="Session Entry">
          <StatusState
            mode="empty"
            description="No active study plan is ready. Create or activate a plan first, then this page will open the current session directly."
            extra={
              <Space wrap>
                <Button type="primary" onClick={() => navigate("/study-plans")}>
                  Open Study Plans
                </Button>
                <Button onClick={() => navigate("/word-sets")}>Open Word Sets</Button>
              </Space>
            }
          />
        </PageSection>
      ) : (
        <>
          <div className="review-session-layout">
            <div className="review-session-main">
              <PageSection title="Current Card">
                {cardsQuery.isLoading ? (
                  <StatusState mode="loading" />
                ) : cardsQuery.isError ? (
                  <StatusState mode="error" description={(cardsQuery.error as Error).message} />
                ) : sessionSummary.totalCount === 0 ? (
                  <StatusState
                    mode="empty"
                    description={
                      weakRoundStarted
                        ? "Today's main queue and weak round are both complete."
                        : "No cards are due for this plan on the selected date."
                    }
                  />
                ) : !currentCard ? (
                  <StatusState mode="empty" description="No current card is available for this session." />
                ) : (
                  <div className="review-session-focus">
                    <Space wrap>
                      <Tag color={currentRow?.mode === "WEAK" ? "volcano" : currentRow?.mode === "REQUEUE" ? "cyan" : "gold"}>
                        {currentRow?.mode}
                      </Tag>
                      <Tag>{currentCard.cardType}</Tag>
                      <Tag>Stage {currentCard.stageNo}</Tag>
                      <Tag>Due {currentCard.dueDate}</Tag>
                    </Space>

                    <div className="review-session-copy">
                      <Typography.Text type="secondary">当前题目</Typography.Text>
                      <Typography.Title level={2} style={{ margin: 0 }}>
                        {currentCard.expression || "-"}
                      </Typography.Title>
                      <Typography.Text type="secondary">{currentCard.reading || "No reading"}</Typography.Text>
                    </div>

                    <div className="review-session-answer">
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

                    <Form<ReviewFormValues> form={reviewForm} layout="vertical">
                      <div className="review-session-form-grid">
                        <Form.Item label="Response Time (ms)" name="responseTimeMs">
                          <InputNumber min={0} style={{ width: "100%" }} placeholder="3200" />
                        </Form.Item>
                        <Form.Item label="Note" name="note">
                          <Input.TextArea rows={3} placeholder="Optional note for this review result" />
                        </Form.Item>
                      </div>
                    </Form>

                    <div className="review-session-rating-grid">
                      {reviewRatings.map((item) => (
                        <Button
                          key={item.rating}
                          size="large"
                          type={item.type ?? "default"}
                          loading={reviewMutation.isPending}
                          onClick={() => handleReviewSubmit(item.rating)}
                        >
                          {item.label}
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
                    <CardStat title="Queue" value={sessionSummary.totalCount} />
                    <CardStat title="Pending" value={sessionSummary.pendingCount} />
                    <CardStat title="Completed" value={sessionSummary.completedCount} />
                    <CardStat title="Weak Round" value={pendingWeakCount} />
                    <CardStat title="Position" value={currentQueuePosition} suffix={sessionSummary.totalCount ? `/ ${sessionSummary.totalCount}` : ""} />
                  </div>

                  {shouldPromptWeakRound ? (
                    <Alert
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
                  ) : sessionSummary.pendingCount === 0 && sessionSummary.totalCount > 0 ? (
                    <Alert type="success" showIcon message="This session is complete." description="All cards in the current queue have been reviewed." />
                  ) : (
                    <Alert
                      type="info"
                      showIcon
                      message={weakRoundStarted ? "当前处于薄弱轮" : "当前处于主复习流"}
                      description={
                        weakRoundStarted
                          ? `Pending position: ${currentPendingPosition} / ${sessionSummary.pendingCount}`
                          : "Submit a rating and the session will advance automatically. Manual jumps stay in the helper area."
                      }
                    />
                  )}

                  <Form<SearchFormValues>
                    form={searchForm}
                    layout="vertical"
                    onFinish={(values) => {
                      updateSearch({
                        planId: values.planId,
                        date: values.date?.format(SESSION_DATE_FORMAT) ?? dayjs().format(SESSION_DATE_FORMAT)
                      });
                    }}
                  >
                    <div className="review-session-form-grid">
                      <Form.Item label="Plan" name="planId" rules={[{ required: true, message: "Select a study plan." }]}>
                        <Select loading={studyPlansQuery.isLoading} options={planOptions} placeholder="Select a study plan" />
                      </Form.Item>
                      <Form.Item label="Date" name="date" rules={[{ required: true, message: "Select a date." }]}>
                        <DatePicker />
                      </Form.Item>
                    </div>
                    <Space wrap>
                      <Button type="primary" htmlType="submit">
                        Refresh Session
                      </Button>
                      <Button
                        onClick={() =>
                          updateSearch({
                            planId: selectedPlan?.id ?? autoPlanId,
                            date: dayjs().format(SESSION_DATE_FORMAT)
                          })
                        }
                      >
                        Back To Today
                      </Button>
                    </Space>
                  </Form>

                  <Space wrap>
                    <Button onClick={() => moveCurrentCard(-1)} disabled={resolvedCurrentIndex <= 0}>
                      Previous
                    </Button>
                    <Button onClick={() => moveCurrentCard(1)} disabled={resolvedCurrentIndex >= activeQueue.length - 1}>
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
                  ) : currentCard ? (
                    <Typography.Text type="secondary">No review has been submitted for this card yet.</Typography.Text>
                  ) : null}
                </div>
              </PageSection>
            </div>
          </div>

          <div className="review-session-support-grid">
            <PageSection title="Queue Assistant">
              <Table<SessionCardTableRow>
                rowKey="rowKey"
                size="small"
                pagination={false}
                dataSource={queueRows}
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
  return <Statistic title={title} value={value} suffix={suffix} />;
}

function resolveCardReviewMessage(todayAction: "DONE" | "REQUEUE_TODAY" | "MOVE_TO_WEAK_ROUND") {
  if (todayAction === "REQUEUE_TODAY") {
    return "This card was added back to today's queue.";
  }
  if (todayAction === "MOVE_TO_WEAK_ROUND") {
    return "This card was moved to the weak-item round.";
  }
  return "Review recorded.";
}
