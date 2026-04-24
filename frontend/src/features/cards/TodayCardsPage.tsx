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

function isPendingCard(card: TodayCard) {
  return card.status === "PENDING";
}

function buildPlanLabel(plan: StudyPlan) {
  return `${plan.name} (${plan.status})`;
}

export function TodayCardsPage() {
  const currentPlanId = useUiStore((state) => state.currentPlanId);
  const setCurrentPlanId = useUiStore((state) => state.setCurrentPlanId);
  const [search, setSearch] = useState<{ planId?: number; date: string }>({
    planId: currentPlanId,
    date: dayjs().format("YYYY-MM-DD")
  });
  const [currentCardId, setCurrentCardId] = useState<number>();
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
    enabled
  });

  const orderedCards = useMemo(() => sortCards(cardsQuery.data ?? []), [cardsQuery.data]);
  const sessionSummary = useMemo(() => buildReviewSessionSummary(orderedCards, isPendingCard), [orderedCards]);
  const resolvedCurrentIndex = useMemo(
    () => resolveCurrentSessionIndex(orderedCards, currentCardId, (item) => item.id, isPendingCard),
    [orderedCards, currentCardId]
  );
  const currentCard = resolvedCurrentIndex === -1 ? undefined : orderedCards[resolvedCurrentIndex];
  const currentQueuePosition = currentCard ? resolvedCurrentIndex + 1 : 0;
  const currentPendingPosition = currentCard
    ? orderedCards.filter((item) => isPendingCard(item) || item.id === currentCard.id).findIndex((item) => item.id === currentCard.id) + 1
    : 0;

  useEffect(() => {
    const nextCardId = resolvedCurrentIndex === -1 ? undefined : orderedCards[resolvedCurrentIndex]?.id;
    if (nextCardId !== currentCardId) {
      setCurrentCardId(nextCardId);
    }
  }, [currentCardId, orderedCards, resolvedCurrentIndex]);

  const reviewsQuery = useQuery({
    queryKey: ["cardReviews", currentCard?.id],
    queryFn: () => getCardReviews(currentCard!.id),
    enabled: Boolean(currentCard?.id)
  });

  const reviewMutation = useMutation({
    mutationFn: ({ cardId, payload }: { cardId: number; payload: ReviewCardPayload }) => submitCardReview(cardId, payload),
    onSuccess: async () => {
      message.success("Review submitted.");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["todayCards", search.planId, search.date] }),
        queryClient.invalidateQueries({ queryKey: ["cardReviews", currentCard?.id] })
      ]);
      reviewForm.resetFields();
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const latestReview = reviewsQuery.data?.[0];
  const activePlans = (studyPlansQuery.data?.items ?? []).filter((plan) => plan.status === "ACTIVE");
  const planOptions = activePlans.length > 0 ? activePlans : studyPlansQuery.data?.items ?? [];

  function handleReviewSubmit(rating: ReviewRating) {
    if (!currentCard) {
      message.warning("No current card in this session.");
      return;
    }

    const values = reviewForm.getFieldsValue();
    reviewMutation.mutate({
      cardId: currentCard.id,
      payload: {
        rating,
        responseTimeMs: values.responseTimeMs,
        note: values.note?.trim() || undefined
      }
    });
  }

  function moveCurrentCard(offset: number) {
    const nextCard = orderedCards[resolvedCurrentIndex + offset];
    if (!nextCard) {
      return;
    }
    setCurrentCardId(nextCard.id);
    reviewForm.resetFields();
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Word Review Session"
        description="Choose a plan once, enter the current card directly, then keep reviewing forward without table-driven context switching."
        extra={<Tag color="purple">card_instance / review_log</Tag>}
      />

      <PageSection title="Session Setup">
        <Form<SearchFormValues>
          layout="inline"
          initialValues={{
            planId: search.planId,
            date: dayjs(search.date)
          }}
          onFinish={(values) => {
            const nextPlanId = values.planId;
            setCurrentPlanId(nextPlanId);
            reviewForm.resetFields();
            setSearch({
              planId: nextPlanId,
              date: values.date?.format("YYYY-MM-DD") ?? dayjs().format("YYYY-MM-DD")
            });
            setCurrentCardId(undefined);
          }}
        >
          <Form.Item label="Plan" name="planId" rules={[{ required: true, message: "Select a study plan." }]}>
            <Select
              style={{ minWidth: 280 }}
              loading={studyPlansQuery.isLoading}
              options={planOptions.map((plan) => ({
                label: buildPlanLabel(plan),
                value: plan.id
              }))}
              placeholder="Select a study plan"
            />
          </Form.Item>
          <Form.Item label="Date" name="date" rules={[{ required: true, message: "Select a date." }]}>
            <DatePicker />
          </Form.Item>
          <Button type="primary" htmlType="submit">
            Start Session
          </Button>
        </Form>
      </PageSection>

      {!enabled ? (
        <StatusState mode="empty" description="Select a plan and date to start a word-review session." />
      ) : cardsQuery.isLoading ? (
        <StatusState mode="loading" />
      ) : cardsQuery.isError ? (
        <StatusState mode="error" description={(cardsQuery.error as Error).message} />
      ) : (
        <>
          <PageSection title="Session Progress">
            <div className="dashboard-overview-grid">
              <CardStat title="Total Due" value={sessionSummary.totalCount} />
              <CardStat title="Pending" value={sessionSummary.pendingCount} />
              <CardStat title="Completed" value={sessionSummary.completedCount} />
              <CardStat title="Current Position" value={currentQueuePosition} suffix={sessionSummary.totalCount ? `/ ${sessionSummary.totalCount}` : ""} />
            </div>
            {sessionSummary.totalCount === 0 ? (
              <Alert
                style={{ marginTop: 16 }}
                type="info"
                showIcon
                message="No cards found for this session."
                description="Try another date or plan."
              />
            ) : sessionSummary.pendingCount === 0 ? (
              <Alert
                style={{ marginTop: 16 }}
                type="success"
                showIcon
                message="This session is complete."
                description="All due cards for the selected plan and date have been reviewed."
              />
            ) : (
              <Alert
                style={{ marginTop: 16 }}
                type="info"
                showIcon
                message="Current flow"
                description={`Stay on the current card, submit a rating, and the session will advance to the next pending item automatically. Pending position: ${currentPendingPosition} / ${sessionSummary.pendingCount}`}
              />
            )}
          </PageSection>

          <PageSection title="Current Card">
            {!currentCard ? (
              <StatusState mode="empty" description="No current card is available for this session." />
            ) : (
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <Space wrap>
                  <Tag color={currentCard.status === "DONE" ? "green" : "gold"}>{currentCard.status}</Tag>
                  <Tag>{currentCard.cardType}</Tag>
                  <Tag>Stage {currentCard.stageNo}</Tag>
                  <Tag>Due {currentCard.dueDate}</Tag>
                </Space>

                <Typography.Title level={3} style={{ margin: 0 }}>
                  {currentCard.expression || "-"}
                </Typography.Title>
                <Typography.Text type="secondary">
                  {currentCard.reading || "No reading"}
                </Typography.Text>
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

                <Space wrap>
                  <Button onClick={() => moveCurrentCard(-1)} disabled={resolvedCurrentIndex <= 0}>
                    Previous
                  </Button>
                  <Button onClick={() => moveCurrentCard(1)} disabled={resolvedCurrentIndex >= orderedCards.length - 1}>
                    Next
                  </Button>
                </Space>

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
                  <Typography.Text type="secondary">No review has been submitted for this card yet.</Typography.Text>
                )}
              </Space>
            )}
          </PageSection>

          <PageSection title="Queue">
            <Table<TodayCard>
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={orderedCards}
              onRow={(record) => ({
                onClick: () => {
                  setCurrentCardId(record.id);
                  reviewForm.resetFields();
                }
              })}
              rowClassName={(record) => (record.id === currentCard?.id ? "review-session-row-active" : "")}
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
                  title: "Status",
                  dataIndex: "status",
                  width: 110,
                  render: (value: string) => <Tag color={value === "DONE" ? "green" : "default"}>{value}</Tag>
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
