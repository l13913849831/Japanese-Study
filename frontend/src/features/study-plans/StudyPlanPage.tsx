import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, DatePicker, Form, Input, InputNumber, Select, Space, Table, Tag } from "antd";
import dayjs from "dayjs";
import { useEffect } from "react";
import { listAnkiTemplates, listMarkdownTemplates } from "@/features/templates/api";
import { listWordSets } from "@/features/word-sets/api";
import { ApiClientError } from "@/shared/api/errors";
import {
  createStudyPlan,
  listStudyPlans,
  updateStudyPlan,
  type StudyPlan,
  type StudyPlanPayload
} from "@/features/study-plans/api";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";

export function StudyPlanPage() {
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const studyPlanQuery = useQuery({
    queryKey: ["studyPlans"],
    queryFn: listStudyPlans
  });
  const wordSetsQuery = useQuery({
    queryKey: ["wordSets"],
    queryFn: listWordSets
  });
  const ankiTemplatesQuery = useQuery({
    queryKey: ["ankiTemplates"],
    queryFn: listAnkiTemplates
  });
  const markdownTemplatesQuery = useQuery({
    queryKey: ["markdownTemplates"],
    queryFn: listMarkdownTemplates
  });

  const selectedPlanId = Form.useWatch("selectedPlanId", form);
  const selectedPlan = studyPlanQuery.data?.items.find((item) => item.id === selectedPlanId);

  useEffect(() => {
    if (!selectedPlan) {
      return;
    }

    form.setFieldsValue({
      ...selectedPlan,
      startDate: dayjs(selectedPlan.startDate),
      reviewOffsetsText: selectedPlan.reviewOffsets.join(",")
    });
  }, [form, selectedPlan]);

  const createMutation = useMutation({
    mutationFn: createStudyPlan,
    onSuccess: async () => {
      message.success("学习计划已创建");
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ["studyPlans"] });
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: StudyPlanPayload }) => updateStudyPlan(id, payload),
    onSuccess: async () => {
      message.success("学习计划已更新");
      await queryClient.invalidateQueries({ queryKey: ["studyPlans"] });
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const handleSubmit = (values: any) => {
    const payload: StudyPlanPayload = {
      name: values.name,
      wordSetId: values.wordSetId,
      startDate: values.startDate.format("YYYY-MM-DD"),
      dailyNewCount: values.dailyNewCount,
      reviewOffsets: String(values.reviewOffsetsText)
        .split(",")
        .map((item) => Number(item.trim()))
        .filter((item) => !Number.isNaN(item)),
      ankiTemplateId: values.ankiTemplateId ?? undefined,
      mdTemplateId: values.mdTemplateId ?? undefined,
      status: values.status
    };

    if (selectedPlan) {
      updateMutation.mutate({ id: selectedPlan.id, payload });
      return;
    }

    createMutation.mutate(payload);
  };

  return (
    <div className="page-stack">
      <PageHeader
        title="学习计划"
        description="展示第一阶段学习计划列表和后续创建/编辑扩展位。"
        extra={<Tag color="blue">study_plan</Tag>}
      />

      <PageSection title="创建 / 编辑学习计划">
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="选择已有计划以编辑" name="selectedPlanId">
            <Select
              allowClear
              placeholder="不选则创建新计划"
              options={(studyPlanQuery.data?.items ?? []).map((item) => ({ label: item.name, value: item.id }))}
            />
          </Form.Item>
          <Form.Item label="名称" name="name" rules={[{ required: true, message: "请输入计划名称" }]}>
            <Input />
          </Form.Item>
          <Form.Item label="词库" name="wordSetId" rules={[{ required: true, message: "请选择词库" }]}>
            <Select options={(wordSetsQuery.data?.items ?? []).map((item) => ({ label: item.name, value: item.id }))} />
          </Form.Item>
          <Form.Item label="开始日期" name="startDate" rules={[{ required: true, message: "请选择开始日期" }]}>
            <DatePicker />
          </Form.Item>
          <Form.Item label="每日新词数" name="dailyNewCount" rules={[{ required: true, message: "请输入数量" }]}>
            <InputNumber min={1} />
          </Form.Item>
          <Form.Item label="复习间隔" name="reviewOffsetsText" rules={[{ required: true, message: "请输入复习间隔" }]}>
            <Input placeholder="例如：0,1,3,7,14,30" />
          </Form.Item>
          <Form.Item label="Anki 模板" name="ankiTemplateId">
            <Select
              allowClear
              options={(ankiTemplatesQuery.data ?? []).map((item) => ({ label: item.name, value: item.id }))}
            />
          </Form.Item>
          <Form.Item label="Markdown 模板" name="mdTemplateId">
            <Select
              allowClear
              options={(markdownTemplatesQuery.data ?? []).map((item) => ({ label: item.name, value: item.id }))}
            />
          </Form.Item>
          <Form.Item label="状态" name="status" rules={[{ required: true, message: "请选择状态" }]}>
            <Select
              options={[
                { label: "DRAFT", value: "DRAFT" },
                { label: "ACTIVE", value: "ACTIVE" },
                { label: "PAUSED", value: "PAUSED" },
                { label: "ARCHIVED", value: "ARCHIVED" }
              ]}
            />
          </Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={createMutation.isPending || updateMutation.isPending}>
              {selectedPlan ? "更新计划" : "创建计划"}
            </Button>
            <Button onClick={() => form.resetFields()}>清空</Button>
          </Space>
        </Form>
      </PageSection>

      <PageSection title="学习计划列表">
        {studyPlanQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : studyPlanQuery.isError ? (
          <StatusState mode="error" description={(studyPlanQuery.error as Error).message} />
        ) : (
          <Table
            rowKey="id"
            pagination={false}
            dataSource={studyPlanQuery.data?.items ?? []}
            columns={[
              { title: "名称", dataIndex: "name" },
              { title: "词库 ID", dataIndex: "wordSetId" },
              { title: "开始日期", dataIndex: "startDate" },
              { title: "每日新词", dataIndex: "dailyNewCount" },
              {
                title: "复习间隔",
                dataIndex: "reviewOffsets",
                render: (value: number[]) => value?.join(", ") || "-"
              },
              { title: "状态", dataIndex: "status" }
            ]}
            onRow={(record: StudyPlan) => ({
              onClick: () => form.setFieldValue("selectedPlanId", record.id)
            })}
          />
        )}
      </PageSection>
    </div>
  );
}
