import { useQuery } from "@tanstack/react-query";
import { Alert, Table, Tag } from "antd";
import { listStudyPlans } from "@/features/study-plans/api";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";

export function StudyPlanPage() {
  const studyPlanQuery = useQuery({
    queryKey: ["studyPlans"],
    queryFn: listStudyPlans
  });

  return (
    <div className="page-stack">
      <PageHeader
        title="学习计划"
        description="展示第一阶段学习计划列表和后续创建/编辑扩展位。"
        extra={<Tag color="blue">study_plan</Tag>}
      />

      <Alert
        type="info"
        showIcon
        message="当前已完成读取骨架"
        description="后续可在此页继续补齐创建计划、修改起始日期、模板绑定和状态切换。"
      />

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
          />
        )}
      </PageSection>
    </div>
  );
}
