import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, App, Button, DatePicker, Descriptions, Form, Input, InputNumber, Space, Table, Tag, Typography } from "antd";
import type { Dayjs } from "dayjs";
import dayjs from "dayjs";
import { useState } from "react";
import {
  getCardReviews,
  getTodayCards,
  submitCardReview,
  type ReviewCardPayload,
  type ReviewLogItem,
  type ReviewRating,
  type TodayCard
} from "@/features/cards/api";
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

const reviewRatings: ReviewRating[] = ["AGAIN", "HARD", "GOOD", "EASY"];

export function TodayCardsPage() {
  const currentPlanId = useUiStore((state) => state.currentPlanId);
  const setCurrentPlanId = useUiStore((state) => state.setCurrentPlanId);
  const [search, setSearch] = useState<{ planId?: number; date?: string }>({
    planId: currentPlanId,
    date: dayjs().format("YYYY-MM-DD")
  });
  const [selectedCardId, setSelectedCardId] = useState<number>();
  const [reviewForm] = Form.useForm<ReviewFormValues>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  const enabled = Boolean(search.planId && search.date);
  const cardsQuery = useQuery({
    queryKey: ["todayCards", search.planId, search.date],
    queryFn: () => getTodayCards(search.planId!, search.date!),
    enabled
  });

  const selectedCard = cardsQuery.data?.find((item) => item.id === selectedCardId);

  const reviewsQuery = useQuery({
    queryKey: ["cardReviews", selectedCardId],
    queryFn: () => getCardReviews(selectedCardId!),
    enabled: Boolean(selectedCardId)
  });

  const reviewMutation = useMutation({
    mutationFn: ({ cardId, payload }: { cardId: number; payload: ReviewCardPayload }) => submitCardReview(cardId, payload),
    onSuccess: async () => {
      message.success("Review submitted.");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["todayCards", search.planId, search.date] }),
        queryClient.invalidateQueries({ queryKey: ["cardReviews", selectedCardId] })
      ]);
      reviewForm.resetFields();
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const latestReview = reviewsQuery.data?.[0];

  function handleReviewSubmit(rating: ReviewRating) {
    if (!selectedCard) {
      message.warning("Select a card first.");
      return;
    }

    const values = reviewForm.getFieldsValue();
    reviewMutation.mutate({
      cardId: selectedCard.id,
      payload: {
        rating,
        responseTimeMs: values.responseTimeMs,
        note: values.note?.trim() || undefined
      }
    });
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Today Cards"
        description="Query one study plan by date, select a card, submit a review, and inspect review history."
        extra={<Tag color="purple">card_instance / review_log</Tag>}
      />

      <PageSection title="Search">
        <Form<SearchFormValues>
          layout="inline"
          initialValues={{
            planId: currentPlanId,
            date: dayjs(search.date)
          }}
          onFinish={(values) => {
            const nextPlanId = values.planId;
            setCurrentPlanId(nextPlanId);
            setSelectedCardId(undefined);
            reviewForm.resetFields();
            setSearch({
              planId: nextPlanId,
              date: values.date?.format("YYYY-MM-DD")
            });
          }}
        >
          <Form.Item label="Plan ID" name="planId" rules={[{ required: true, message: "Enter a plan ID." }]}>
            <InputNumber min={1} placeholder="1" />
          </Form.Item>
          <Form.Item label="Date" name="date" rules={[{ required: true, message: "Select a date." }]}>
            <DatePicker />
          </Form.Item>
          <Button type="primary" htmlType="submit">
            Load Cards
          </Button>
        </Form>
      </PageSection>

      <Alert
        type="info"
        showIcon
        message="Review loop is now handled from this page."
        description="Pick one card from the table, optionally enter response time and a note, then submit a rating."
      />

      <PageSection title="Today Card Results">
        {!enabled ? (
          <StatusState mode="empty" description="Select a plan and date to load cards." />
        ) : cardsQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : cardsQuery.isError ? (
          <StatusState mode="error" description={(cardsQuery.error as Error).message} />
        ) : (
          <Table<TodayCard>
            rowKey="id"
            pagination={false}
            dataSource={cardsQuery.data ?? []}
            rowSelection={{
              type: "radio",
              selectedRowKeys: selectedCardId ? [selectedCardId] : [],
              onChange: (selectedRowKeys) => {
                setSelectedCardId(selectedRowKeys[0] as number | undefined);
                reviewForm.resetFields();
              }
            }}
            columns={[
              { title: "Card ID", dataIndex: "id", width: 90 },
              { title: "Word ID", dataIndex: "wordEntryId", width: 100 },
              { title: "Expression", dataIndex: "expression", render: (value?: string) => value || "-" },
              { title: "Reading", dataIndex: "reading", render: (value?: string) => value || "-" },
              { title: "Meaning", dataIndex: "meaning", render: (value?: string) => value || "-" },
              { title: "Type", dataIndex: "cardType", width: 100 },
              { title: "Stage", dataIndex: "stageNo", width: 80 },
              {
                title: "Status",
                dataIndex: "status",
                width: 110,
                render: (value: string) => <Tag color={value === "DONE" ? "green" : "default"}>{value}</Tag>
              }
            ]}
            locale={{
              emptyText: "No cards found for the selected plan and date."
            }}
          />
        )}
      </PageSection>

      <PageSection title="Review Action">
        {!selectedCard ? (
          <StatusState mode="empty" description="Select a card from the table before submitting a review." />
        ) : (
          <Space direction="vertical" style={{ width: "100%" }} size={16}>
            <Descriptions
              bordered
              size="small"
              column={2}
              items={[
                { key: "cardId", label: "Card ID", children: selectedCard.id },
                { key: "status", label: "Current Status", children: <Tag>{selectedCard.status}</Tag> },
                { key: "expression", label: "Expression", children: selectedCard.expression || "-" },
                { key: "reading", label: "Reading", children: selectedCard.reading || "-" },
                { key: "meaning", label: "Meaning", children: selectedCard.meaning || "-" },
                { key: "dueDate", label: "Due Date", children: selectedCard.dueDate }
              ]}
            />

            <Form<ReviewFormValues> form={reviewForm} layout="vertical">
              <Form.Item label="Response Time (ms)" name="responseTimeMs">
                <InputNumber min={0} style={{ width: "100%" }} placeholder="3200" />
              </Form.Item>
              <Form.Item label="Note" name="note">
                <Input.TextArea rows={3} placeholder="Optional note for this review result" />
              </Form.Item>
            </Form>

            <Space wrap>
              {reviewRatings.map((rating) => (
                <Button
                  key={rating}
                  type={rating === "GOOD" ? "primary" : "default"}
                  loading={reviewMutation.isPending}
                  onClick={() => handleReviewSubmit(rating)}
                >
                  {rating}
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

      <PageSection title="Review History">
        {!selectedCard ? (
          <StatusState mode="empty" description="Select a card to inspect review history." />
        ) : reviewsQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : reviewsQuery.isError ? (
          <StatusState mode="error" description={(reviewsQuery.error as Error).message} />
        ) : (
          <Table<ReviewLogItem>
            rowKey="id"
            pagination={false}
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
              emptyText: "No review history for the selected card."
            }}
          />
        )}
      </PageSection>
    </div>
  );
}
