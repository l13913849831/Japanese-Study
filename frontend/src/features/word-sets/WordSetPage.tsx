import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, Form, Input, Space, Table, Tag, Typography, Upload } from "antd";
import type { UploadProps } from "antd";
import { useState } from "react";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import {
  createWordSet,
  importWordEntries,
  listWordEntries,
  listWordSets,
  type CreateWordSetPayload,
  type WordEntryImportResult,
  type WordSet
} from "@/features/word-sets/api";

export function WordSetPage() {
  const [form] = Form.useForm<CreateWordSetPayload>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [selectedWordSet, setSelectedWordSet] = useState<WordSet | null>(null);
  const [lastImportResult, setLastImportResult] = useState<WordEntryImportResult | null>(null);

  const wordSetsQuery = useQuery({
    queryKey: ["wordSets"],
    queryFn: listWordSets
  });

  const wordEntriesQuery = useQuery({
    queryKey: ["wordEntries", selectedWordSet?.id],
    queryFn: () => listWordEntries(selectedWordSet!.id),
    enabled: Boolean(selectedWordSet?.id)
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

  const importMutation = useMutation({
    mutationFn: ({ wordSetId, file }: { wordSetId: number; file: File }) => importWordEntries(wordSetId, file),
    onSuccess: async (result) => {
      setLastImportResult(result);
      message.success(`导入完成：新增 ${result.importedCount} 条，跳过 ${result.skippedCount} 条`);
      await queryClient.invalidateQueries({ queryKey: ["wordEntries", selectedWordSet?.id] });
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const uploadProps: UploadProps = {
    showUploadList: false,
    beforeUpload: (file) => {
      if (!selectedWordSet) {
        message.warning("请先选择一个词库");
        return Upload.LIST_IGNORE;
      }
      importMutation.mutate({ wordSetId: selectedWordSet.id, file });
      return false;
    }
  };

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
            rowSelection={{
              type: "radio",
              selectedRowKeys: selectedWordSet ? [selectedWordSet.id] : [],
              onChange: (_, rows) => {
                setSelectedWordSet(rows[0] ?? null);
                setLastImportResult(null);
              }
            }}
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

      <PageSection
        title={selectedWordSet ? `词条列表：${selectedWordSet.name}` : "词条列表"}
        extra={
          <Upload {...uploadProps}>
            <Button loading={importMutation.isPending} disabled={!selectedWordSet}>
              导入 CSV
            </Button>
          </Upload>
        }
      >
        {lastImportResult ? (
          <Space direction="vertical" style={{ width: "100%", marginBottom: 16 }}>
            <Typography.Text>
              最近一次导入结果：新增 {lastImportResult.importedCount} 条，跳过 {lastImportResult.skippedCount} 条
            </Typography.Text>
            {lastImportResult.errors.length > 0 ? (
              <Table
                rowKey={(record) => `${record.lineNumber}-${record.field}`}
                size="small"
                pagination={false}
                dataSource={lastImportResult.errors}
                columns={[
                  { title: "行号", dataIndex: "lineNumber", width: 80 },
                  { title: "字段", dataIndex: "field", width: 120 },
                  { title: "错误", dataIndex: "message" }
                ]}
              />
            ) : null}
          </Space>
        ) : null}

        {!selectedWordSet ? (
          <StatusState mode="empty" description="先在上方选择一个词库" />
        ) : wordEntriesQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : wordEntriesQuery.isError ? (
          <StatusState mode="error" description={(wordEntriesQuery.error as Error).message} />
        ) : (
          <Table
            rowKey="id"
            pagination={false}
            dataSource={wordEntriesQuery.data?.items ?? []}
            columns={[
              { title: "顺序", dataIndex: "sourceOrder", width: 80 },
              { title: "表达", dataIndex: "expression", width: 140 },
              { title: "读音", dataIndex: "reading", render: (value?: string) => value || "-" },
              { title: "含义", dataIndex: "meaning" },
              {
                title: "标签",
                dataIndex: "tags",
                render: (tags: string[]) => tags?.length ? tags.join(", ") : "-"
              }
            ]}
          />
        )}
      </PageSection>
    </div>
  );
}
