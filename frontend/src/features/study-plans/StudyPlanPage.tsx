import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  App,
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type FormInstance
} from "antd";
import dayjs from "dayjs";
import type { Dayjs } from "dayjs";
import { useEffect } from "react";
import { listAnkiTemplates, listMarkdownTemplates } from "@/features/templates/api";
import {
  activateStudyPlan,
  archiveStudyPlan,
  createStudyPlan,
  listStudyPlans,
  pauseStudyPlan,
  updateStudyPlan,
  type StudyPlan,
  type StudyPlanPayload,
  type StudyPlanStatus
} from "@/features/study-plans/api";
import { listWordSets } from "@/features/word-sets/api";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";

type StudyPlanAction = "activate" | "pause" | "archive";

interface StudyPlanFormValues {
  selectedPlanId?: number;
  name?: string;
  wordSetId?: number;
  startDate?: Dayjs;
  dailyNewCount?: number;
  reviewOffsetsText?: string;
  ankiTemplateId?: number;
  mdTemplateId?: number;
}

const STATUS_COLORS: Record<StudyPlanStatus, string> = {
  DRAFT: "default",
  ACTIVE: "green",
  PAUSED: "orange",
  ARCHIVED: "red"
};

const ACTION_LABELS: Record<StudyPlanAction, string> = {
  activate: "激活计划",
  pause: "暂停计划",
  archive: "归档计划"
};

const ACTION_MESSAGES: Record<StudyPlanAction, string> = {
  activate: "学习计划已激活",
  pause: "学习计划已暂停",
  archive: "学习计划已归档"
};

const ACTION_HANDLERS: Record<StudyPlanAction, (id: number) => Promise<StudyPlan>> = {
  activate: activateStudyPlan,
  pause: pauseStudyPlan,
  archive: archiveStudyPlan
};

function fillForm(form: FormInstance<StudyPlanFormValues>, plan: StudyPlan) {
  form.setFieldsValue({
    selectedPlanId: plan.id,
    name: plan.name,
    wordSetId: plan.wordSetId,
    startDate: dayjs(plan.startDate),
    dailyNewCount: plan.dailyNewCount,
    reviewOffsetsText: plan.reviewOffsets.join(","),
    ankiTemplateId: plan.ankiTemplateId,
    mdTemplateId: plan.mdTemplateId
  });
}

function parseReviewOffsets(value: string | undefined) {
  return String(value ?? "")
    .split(",")
    .map((item) => Number(item.trim()))
    .filter((item) => !Number.isNaN(item));
}

function isEditableStatus(status: StudyPlanStatus) {
  return status === "DRAFT" || status === "PAUSED";
}

function getAvailableActions(status: StudyPlanStatus): StudyPlanAction[] {
  if (status === "DRAFT" || status === "PAUSED") {
    return ["activate", "archive"];
  }
  if (status === "ACTIVE") {
    return ["pause", "archive"];
  }
  return [];
}

export function StudyPlanPage() {
  const [form] = Form.useForm<StudyPlanFormValues>();
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
  const isEditLocked = Boolean(selectedPlan && !isEditableStatus(selectedPlan.status));
  const availableActions = selectedPlan ? getAvailableActions(selectedPlan.status) : [];

  useEffect(() => {
    if (!selectedPlan) {
      return;
    }

    fillForm(form, selectedPlan);
  }, [form, selectedPlan]);

  const refreshStudyPlans = async () => {
    await queryClient.invalidateQueries({ queryKey: ["studyPlans"] });
  };

  const createMutation = useMutation({
    mutationFn: createStudyPlan,
    onSuccess: async () => {
      message.success("学习计划已创建");
      form.resetFields();
      await refreshStudyPlans();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: StudyPlanPayload }) => updateStudyPlan(id, payload),
    onSuccess: async (plan) => {
      message.success("学习计划已更新");
      fillForm(form, plan);
      await refreshStudyPlans();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const lifecycleMutation = useMutation({
    mutationFn: ({ id, action }: { id: number; action: StudyPlanAction }) => ACTION_HANDLERS[action](id),
    onSuccess: async (plan, variables) => {
      message.success(ACTION_MESSAGES[variables.action]);
      fillForm(form, plan);
      await refreshStudyPlans();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const handleSubmit = (values: StudyPlanFormValues) => {
    const payload: StudyPlanPayload = {
      name: values.name ?? "",
      wordSetId: values.wordSetId ?? 0,
      startDate: values.startDate?.format("YYYY-MM-DD") ?? "",
      dailyNewCount: values.dailyNewCount ?? 0,
      reviewOffsets: parseReviewOffsets(values.reviewOffsetsText),
      ankiTemplateId: values.ankiTemplateId ?? undefined,
      mdTemplateId: values.mdTemplateId ?? undefined
    };

    if (selectedPlan) {
      updateMutation.mutate({ id: selectedPlan.id, payload });
      return;
    }

    createMutation.mutate(payload);
  };

  const handleLifecycleAction = (action: StudyPlanAction) => {
    if (!selectedPlan) {
      return;
    }

    lifecycleMutation.mutate({ id: selectedPlan.id, action });
  };

  return (
    <div className="page-stack">
      <PageHeader
        title="学习计划"
        description="配置每日新词、复习偏移和模板，并通过显式生命周期动作管理计划状态。"
        extra={<Tag color="blue">study_plan</Tag>}
      />

      <PageSection title="创建 / 编辑学习计划">
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="选择已有计划以编辑" name="selectedPlanId">
            <Select
              allowClear
              placeholder="不选择则创建新计划"
              options={(studyPlanQuery.data?.items ?? []).map((item) => ({ label: item.name, value: item.id }))}
            />
          </Form.Item>

          {selectedPlan ? (
            <Space direction="vertical" size={8} style={{ display: "flex", marginBottom: 16 }}>
              <Space wrap>
                <Typography.Text strong>当前状态</Typography.Text>
                <Tag color={STATUS_COLORS[selectedPlan.status]}>{selectedPlan.status}</Tag>
                <Typography.Text type="secondary">
                  {isEditLocked ? "当前状态不允许直接编辑计划内容，请先执行对应生命周期动作。" : "当前状态允许编辑计划内容。"}
                </Typography.Text>
              </Space>
              {availableActions.length > 0 ? (
                <Space wrap>
                  {availableActions.map((action) => (
                    <Button
                      key={action}
                      onClick={() => handleLifecycleAction(action)}
                      loading={lifecycleMutation.isPending}
                    >
                      {ACTION_LABELS[action]}
                    </Button>
                  ))}
                </Space>
              ) : (
                <Typography.Text type="secondary">已归档计划不可再执行生命周期动作。</Typography.Text>
              )}
            </Space>
          ) : (
            <Typography.Text type="secondary" style={{ display: "block", marginBottom: 16 }}>
              新建计划默认以 DRAFT 状态创建，后续通过“激活计划”进入学习流程。
            </Typography.Text>
          )}

          <Form.Item label="名称" name="name" rules={[{ required: true, message: "请输入学习计划名称" }]}>
            <Input disabled={isEditLocked} />
          </Form.Item>
          <Form.Item label="词库" name="wordSetId" rules={[{ required: true, message: "请选择词库" }]}>
            <Select
              disabled={isEditLocked}
              options={(wordSetsQuery.data?.items ?? []).map((item) => ({ label: item.name, value: item.id }))}
            />
          </Form.Item>
          <Form.Item label="开始日期" name="startDate" rules={[{ required: true, message: "请选择开始日期" }]}>
            <DatePicker disabled={isEditLocked} />
          </Form.Item>
          <Form.Item label="每日新词数" name="dailyNewCount" rules={[{ required: true, message: "请输入每日新词数" }]}>
            <InputNumber min={1} disabled={isEditLocked} />
          </Form.Item>
          <Form.Item
            label="复习偏移"
            name="reviewOffsetsText"
            rules={[{ required: true, message: "请输入复习偏移" }]}
          >
            <Input disabled={isEditLocked} placeholder="例如：0,1,3,7,14,30" />
          </Form.Item>
          <Form.Item label="Anki 模板" name="ankiTemplateId">
            <Select
              allowClear
              disabled={isEditLocked}
              options={(ankiTemplatesQuery.data ?? []).map((item) => ({ label: item.name, value: item.id }))}
            />
          </Form.Item>
          <Form.Item label="Markdown 模板" name="mdTemplateId">
            <Select
              allowClear
              disabled={isEditLocked}
              options={(markdownTemplatesQuery.data ?? []).map((item) => ({ label: item.name, value: item.id }))}
            />
          </Form.Item>
          <Space>
            <Button
              type="primary"
              htmlType="submit"
              disabled={isEditLocked}
              loading={createMutation.isPending || updateMutation.isPending}
            >
              {selectedPlan ? "更新计划" : "创建计划"}
            </Button>
            <Button onClick={() => form.resetFields()}>重置</Button>
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
            rowSelection={{
              type: "radio",
              selectedRowKeys: selectedPlanId ? [selectedPlanId] : [],
              onChange: (selectedRowKeys) => form.setFieldValue("selectedPlanId", selectedRowKeys[0])
            }}
            columns={[
              { title: "名称", dataIndex: "name" },
              { title: "词库 ID", dataIndex: "wordSetId" },
              { title: "开始日期", dataIndex: "startDate" },
              { title: "每日新词数", dataIndex: "dailyNewCount" },
              {
                title: "复习偏移",
                dataIndex: "reviewOffsets",
                render: (value: number[]) => value?.join(", ") || "-"
              },
              {
                title: "状态",
                dataIndex: "status",
                render: (status: StudyPlanStatus) => <Tag color={STATUS_COLORS[status]}>{status}</Tag>
              }
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
