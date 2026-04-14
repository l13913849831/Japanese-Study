import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, Form, Input, Space, Table, Tag, Typography } from "antd";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import { createWordSet, listWordSets, type CreateWordSetPayload } from "@/features/word-sets/api";

export function WordSetPage() {
  const [form] = Form.useForm<CreateWordSetPayload>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  const wordSetsQuery = useQuery({
    queryKey: ["wordSets"],
    queryFn: listWordSets
  });

  const createMutation = useMutation({
    mutationFn: createWordSet,
    onSuccess: async () => {
      message.success("词库已创建");
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ["wordSets"] });
    },
    onError: (error) => {
      const apiError = error as ApiClientError;
      message.error(apiError.message);
    }
  });

  return (
    <div className="page-stack">
      <PageHeader
        title="词库"
        description="提供词库列表、创建入口与后续 CSV 导入扩展点。"
        extra={<Tag color="gold">word_set / word_entry</Tag>}
      />

      <PageSection title="创建词库">
        <Form
          form={form}
          layout="vertical"
          onFinish={(values) => createMutation.mutate(values)}
        >
          <Form.Item label="名称" name="name" rules={[{ required: true, message: "请输入词库名称" }]}>
            <Input placeholder="例如：N4 核心词汇" />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={3} placeholder="例如：N4 高频日语词汇" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={createMutation.isPending}>
            创建
          </Button>
        </Form>
      </PageSection>

      <PageSection
        title="词库列表"
        extra={<Typography.Text type="secondary">对接 GET /api/word-sets</Typography.Text>}
      >
        {wordSetsQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : wordSetsQuery.isError ? (
          <StatusState mode="error" description={(wordSetsQuery.error as Error).message} />
        ) : (
          <Table
            rowKey="id"
            pagination={false}
            dataSource={wordSetsQuery.data?.items ?? []}
            columns={[
              { title: "ID", dataIndex: "id", width: 80 },
              { title: "名称", dataIndex: "name" },
              { title: "描述", dataIndex: "description", render: (value?: string) => value || "-" },
              { title: "创建时间", dataIndex: "createdAt" }
            ]}
            locale={{
              emptyText: (
                <Space direction="vertical">
                  <Typography.Text type="secondary">暂无词库</Typography.Text>
                </Space>
              )
            }}
          />
        )}
      </PageSection>
    </div>
  );
}
