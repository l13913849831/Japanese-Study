import { useQuery } from "@tanstack/react-query";
import { Alert, Button, DatePicker, Form, InputNumber, Table, Tag } from "antd";
import dayjs, { Dayjs } from "dayjs";
import { useMemo, useState } from "react";
import { getTodayCards } from "@/features/cards/api";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import { useUiStore } from "@/shared/store/useUiStore";

interface SearchFormValues {
  planId?: number;
  date?: Dayjs;
}

export function TodayCardsPage() {
  const [search, setSearch] = useState<{ planId?: number; date?: string }>({
    date: dayjs().format("YYYY-MM-DD")
  });
  const currentPlanId = useUiStore((state) => state.currentPlanId);
  const setCurrentPlanId = useUiStore((state) => state.setCurrentPlanId);

  const enabled = Boolean(search.planId && search.date);
  const cardsQuery = useQuery({
    queryKey: ["todayCards", search.planId, search.date],
    queryFn: () => getTodayCards(search.planId!, search.date!),
    enabled
  });

  const initialValues = useMemo<SearchFormValues>(
    () => ({
      planId: currentPlanId,
      date: dayjs(search.date)
    }),
    [currentPlanId, search.date]
  );

  return (
    <div className="page-stack">
      <PageHeader
        title="今日卡片"
        description="提供计划 + 日期查询入口，对接 today/calendar 类接口。"
        extra={<Tag color="purple">card_instance</Tag>}
      />

      <PageSection title="查询条件">
        <Form<SearchFormValues>
          layout="inline"
          initialValues={initialValues}
          onFinish={(values) => {
            const nextPlanId = values.planId;
            setCurrentPlanId(nextPlanId);
            setSearch({
              planId: nextPlanId,
              date: values.date?.format("YYYY-MM-DD")
            });
          }}
        >
          <Form.Item label="计划 ID" name="planId" rules={[{ required: true, message: "请输入计划 ID" }]}>
            <InputNumber min={1} placeholder="1" />
          </Form.Item>
          <Form.Item label="日期" name="date" rules={[{ required: true, message: "请选择日期" }]}>
            <DatePicker />
          </Form.Item>
          <Button type="primary" htmlType="submit">
            查询
          </Button>
        </Form>
      </PageSection>

      <Alert
        type="info"
        showIcon
        message="当前接口为骨架版本"
        description="卡片查询 API 已预留完成，后续可继续接入真实预生成逻辑和日历视图。"
      />

      <PageSection title="查询结果">
        {!enabled ? (
          <StatusState mode="empty" description="先选择计划 ID 和日期" />
        ) : cardsQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : cardsQuery.isError ? (
          <StatusState mode="error" description={(cardsQuery.error as Error).message} />
        ) : (
          <Table
            rowKey={(record) => `${record.id}-${record.stageNo}`}
            pagination={false}
            dataSource={cardsQuery.data ?? []}
            columns={[
              { title: "词条 ID", dataIndex: "wordEntryId" },
              { title: "表达", dataIndex: "expression", render: (value?: string) => value || "-" },
              { title: "读音", dataIndex: "reading", render: (value?: string) => value || "-" },
              { title: "含义", dataIndex: "meaning", render: (value?: string) => value || "-" },
              { title: "类型", dataIndex: "cardType" },
              { title: "阶段", dataIndex: "stageNo" },
              { title: "状态", dataIndex: "status" }
            ]}
          />
        )}
      </PageSection>
    </div>
  );
}
