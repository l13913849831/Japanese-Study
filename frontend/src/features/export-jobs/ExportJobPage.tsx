import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, App, Button, DatePicker, Form, Select, Space, Table, Tag } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ApiClientError } from "@/shared/api/errors";
import {
  createExportJob,
  downloadExportJob,
  listExportJobs,
  preflightExportJob,
  type CreateExportJobPayload,
  type ExportJobPreflight
} from "@/features/export-jobs/api";
import { listStudyPlans } from "@/features/study-plans/api";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";

interface ExportJobFormValues {
  planId?: number;
  exportType?: string;
  targetDate?: Dayjs;
}

export function ExportJobPage() {
  const [form] = Form.useForm<ExportJobFormValues>();
  const [searchParams] = useSearchParams();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [preflightResult, setPreflightResult] = useState<ExportJobPreflight | null>(null);
  const exportJobsQuery = useQuery({
    queryKey: ["exportJobs"],
    queryFn: listExportJobs
  });
  const studyPlansQuery = useQuery({
    queryKey: ["studyPlans"],
    queryFn: listStudyPlans
  });
  const fromClosure = searchParams.get("source") === "closure";
  const hasStudyPlans = (studyPlansQuery.data?.items ?? []).length > 0;
  const hasExportJobs = (exportJobsQuery.data?.items ?? []).length > 0;

  const createMutation = useMutation({
    mutationFn: async (payload: CreateExportJobPayload) => {
      const preflight = await preflightExportJob(payload);
      setPreflightResult(preflight);
      if (!preflight.creatable) {
        throw new Error(preflight.message);
      }
      return createExportJob(payload);
    },
    onSuccess: async () => {
      message.success("复盘材料导出任务已创建");
      await queryClient.invalidateQueries({ queryKey: ["exportJobs"] });
    },
    onError: (error) => message.error(error instanceof ApiClientError ? error.message : (error as Error).message)
  });

  const preflightMutation = useMutation({
    mutationFn: preflightExportJob,
    onSuccess: (result) => {
      setPreflightResult(result);
      if (result.creatable) {
        message.success("预检查通过，可以创建复盘材料");
      } else {
        message.warning(result.message);
      }
    },
    onError: (error) => {
      setPreflightResult(null);
      message.error((error as ApiClientError).message);
    }
  });

  const downloadMutation = useMutation({
    mutationFn: async ({ id, fileName }: { id: number; fileName?: string }) => {
      const blob = await downloadExportJob(id);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = fileName ?? `export-${id}`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  useEffect(() => {
    const nextValues: ExportJobFormValues = {};
    const planId = Number(searchParams.get("planId"));
    const targetDate = searchParams.get("targetDate");

    if (Number.isInteger(planId) && planId > 0) {
      nextValues.planId = planId;
    }
    if (targetDate && dayjs(targetDate).isValid()) {
      nextValues.targetDate = dayjs(targetDate);
    }
    if (Object.keys(nextValues).length > 0) {
      form.setFieldsValue(nextValues);
    }
  }, [form, searchParams]);

  const buildExportPayload = (values: ExportJobFormValues): CreateExportJobPayload | null => {
    if (!values.planId || !values.exportType || !values.targetDate) {
      return null;
    }
    return {
      planId: values.planId,
      exportType: values.exportType,
      targetDate: values.targetDate.format("YYYY-MM-DD")
    };
  };

  const handlePreflight = async () => {
    const values = await form.validateFields();
    const payload = buildExportPayload(values);
    if (payload) {
      preflightMutation.mutate(payload);
    }
  };

  const handleCreate = (values: ExportJobFormValues) => {
    const payload = buildExportPayload(values);
    if (payload) {
      createMutation.mutate(payload);
    }
  };

  return (
    <div className="page-stack">
      <PageHeader
        title="导出复盘材料"
        description="把某个计划在指定日期的学习结果整理成复盘材料；历史任务仍保留在下方。"
        extra={<Tag color="magenta">closure</Tag>}
      />

      <PageSection title="当前入口">
        <Alert
          type={fromClosure ? "success" : "info"}
          showIcon
          message={fromClosure ? "你刚完成一段学习。" : "这里是学习收尾和历史管理共用的导出入口。"}
          description={
            fromClosure
              ? "可以直接把今天的学习结果整理成复盘材料，导出后再去看历史记录。"
              : "学习模式里把它当成复盘材料导出；历史任务和下载记录继续留在这里统一管理。"
          }
        />
      </PageSection>

      <PageSection title="创建复盘材料">
        {!hasStudyPlans && !studyPlansQuery.isLoading ? (
          <Alert
            type="info"
            showIcon
            message="还没有可导出的学习计划。"
            description="先建立并激活至少一个学习计划，再回来创建复盘材料。"
          />
        ) : (
          <Form
            form={form}
            layout="inline"
            onValuesChange={() => setPreflightResult(null)}
            onFinish={handleCreate}
          >
            <Form.Item label="计划" name="planId" rules={[{ required: true, message: "请选择计划" }]}>
              <Select
                style={{ minWidth: 220 }}
                options={(studyPlansQuery.data?.items ?? []).map((item) => ({ label: item.name, value: item.id }))}
              />
            </Form.Item>
            <Form.Item label="导出类型" name="exportType" rules={[{ required: true, message: "请选择导出类型" }]}>
              <Select
                style={{ minWidth: 180 }}
                options={[
                  { label: "ANKI_CSV", value: "ANKI_CSV" },
                  { label: "ANKI_TSV", value: "ANKI_TSV" },
                  { label: "MARKDOWN", value: "MARKDOWN" }
                ]}
              />
            </Form.Item>
            <Form.Item label="目标日期" name="targetDate" rules={[{ required: true, message: "请选择日期" }]}>
              <DatePicker />
            </Form.Item>
            <Space wrap>
              <Button loading={preflightMutation.isPending} onClick={handlePreflight}>
                预检查
              </Button>
              <Button type="primary" htmlType="submit" loading={createMutation.isPending}>
                创建复盘材料
              </Button>
            </Space>
          </Form>
        )}
        {preflightResult ? (
          <Alert
            type={preflightResult.creatable ? "success" : "warning"}
            showIcon
            style={{ marginTop: 16 }}
            message={preflightResult.creatable ? "导出内容已确认" : "当前日期没有可导出内容"}
            description={[
              `计划：${preflightResult.planName}`,
              `日期：${preflightResult.targetDate}`,
              `类型：${preflightResult.exportType}`,
              `总计：${preflightResult.totalCards}`,
              `新卡：${preflightResult.newCards}`,
              `复习：${preflightResult.reviewCards}`,
              preflightResult.markdownTemplateName ? `模板：${preflightResult.markdownTemplateName}` : undefined
            ]
              .filter(Boolean)
              .join("；")}
          />
        ) : null}
      </PageSection>

      <PageSection title="历史导出记录">
        {exportJobsQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : exportJobsQuery.isError ? (
          <StatusState mode="error" description={(exportJobsQuery.error as Error).message} />
        ) : !hasExportJobs ? (
          <Alert
            type="info"
            showIcon
            message="还没有历史导出记录。"
            description="完成一段学习后，从会话页或这里创建第一份复盘材料。"
          />
        ) : (
          <Table
            rowKey="id"
            pagination={false}
            dataSource={exportJobsQuery.data?.items ?? []}
            columns={[
              { title: "ID", dataIndex: "id" },
              { title: "计划 ID", dataIndex: "planId" },
              { title: "导出类型", dataIndex: "exportType" },
              { title: "目标日期", dataIndex: "targetDate", render: (value?: string) => value || "-" },
              { title: "文件名", dataIndex: "fileName", render: (value?: string) => value || "-" },
              {
                title: "状态",
                dataIndex: "status",
                render: (value: string) => <Tag color={value === "SUCCESS" ? "green" : value === "FAILED" ? "red" : "blue"}>{value}</Tag>
              },
              {
                title: "操作",
                render: (_, record) => (
                  <Space>
                    <Button
                      disabled={record.status !== "SUCCESS"}
                      loading={downloadMutation.isPending}
                      onClick={() => downloadMutation.mutate({ id: record.id, fileName: record.fileName })}
                    >
                      下载
                    </Button>
                  </Space>
                )
              }
            ]}
          />
        )}
      </PageSection>
    </div>
  );
}
