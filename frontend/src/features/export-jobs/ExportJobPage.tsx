import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, DatePicker, Form, Select, Space, Table, Tag } from "antd";
import { ApiClientError } from "@/shared/api/errors";
import { createExportJob, downloadExportJob, listExportJobs } from "@/features/export-jobs/api";
import { listStudyPlans } from "@/features/study-plans/api";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";

export function ExportJobPage() {
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const exportJobsQuery = useQuery({
    queryKey: ["exportJobs"],
    queryFn: listExportJobs
  });
  const studyPlansQuery = useQuery({
    queryKey: ["studyPlans"],
    queryFn: listStudyPlans
  });

  const createMutation = useMutation({
    mutationFn: createExportJob,
    onSuccess: async () => {
      message.success("导出任务已创建");
      await queryClient.invalidateQueries({ queryKey: ["exportJobs"] });
    },
    onError: (error) => message.error((error as ApiClientError).message)
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

  return (
    <div className="page-stack">
      <PageHeader
        title="导出任务"
        description="展示导出任务读取入口，为后续 CSV/TSV/Markdown 导出创建流程预留界面。"
        extra={<Tag color="magenta">export_job</Tag>}
      />

      <PageSection title="创建导出任务">
        <Form
          form={form}
          layout="inline"
          onFinish={(values) =>
            createMutation.mutate({
              planId: values.planId,
              exportType: values.exportType,
              targetDate: values.targetDate.format("YYYY-MM-DD")
            })
          }
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
          <Button type="primary" htmlType="submit" loading={createMutation.isPending}>
            创建
          </Button>
        </Form>
      </PageSection>

      <PageSection title="导出任务列表">
        {exportJobsQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : exportJobsQuery.isError ? (
          <StatusState mode="error" description={(exportJobsQuery.error as Error).message} />
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
              { title: "状态", dataIndex: "status" },
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
