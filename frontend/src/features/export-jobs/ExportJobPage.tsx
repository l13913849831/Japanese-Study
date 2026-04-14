import { useQuery } from "@tanstack/react-query";
import { Alert, Table, Tag } from "antd";
import { listExportJobs } from "@/features/export-jobs/api";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";

export function ExportJobPage() {
  const exportJobsQuery = useQuery({
    queryKey: ["exportJobs"],
    queryFn: listExportJobs
  });

  return (
    <div className="page-stack">
      <PageHeader
        title="导出任务"
        description="展示导出任务读取入口，为后续 CSV/TSV/Markdown 导出创建流程预留界面。"
        extra={<Tag color="magenta">export_job</Tag>}
      />

      <Alert
        type="info"
        showIcon
        message="导出能力尚未完整实现"
        description="当前页面重点验证统一 API 调用、分页包装解析和模块导航入口。"
      />

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
              { title: "状态", dataIndex: "status" }
            ]}
          />
        )}
      </PageSection>
    </div>
  );
}
