import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  type TablePaginationConfig,
  type UploadProps
} from "antd";
import { useState } from "react";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import {
  createWordEntry,
  createWordSet,
  deleteWordEntry,
  importWordEntries,
  listWordEntries,
  listWordSets,
  updateWordEntry,
  type CreateWordSetPayload,
  type WordEntry,
  type WordEntryFilters,
  type WordEntryImportResult,
  type WordEntryPayload,
  type WordSet
} from "@/features/word-sets/api";

interface WordEntryFilterFormValues {
  keyword?: string;
  level?: string;
  tag?: string;
}

interface WordEntryFormValues {
  expression?: string;
  reading?: string;
  meaning?: string;
  partOfSpeech?: string;
  exampleJp?: string;
  exampleZh?: string;
  level?: string;
  tagsText?: string;
}

function parseTags(value: string | undefined) {
  return String(value ?? "")
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function buildWordEntryPayload(values: WordEntryFormValues): WordEntryPayload {
  return {
    expression: values.expression ?? "",
    reading: values.reading?.trim() || undefined,
    meaning: values.meaning ?? "",
    partOfSpeech: values.partOfSpeech?.trim() || undefined,
    exampleJp: values.exampleJp?.trim() || undefined,
    exampleZh: values.exampleZh?.trim() || undefined,
    level: values.level?.trim() || undefined,
    tags: parseTags(values.tagsText)
  };
}

export function WordSetPage() {
  const [wordSetForm] = Form.useForm<CreateWordSetPayload>();
  const [filterForm] = Form.useForm<WordEntryFilterFormValues>();
  const [wordEntryForm] = Form.useForm<WordEntryFormValues>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [selectedWordSet, setSelectedWordSet] = useState<WordSet | null>(null);
  const [lastImportResult, setLastImportResult] = useState<WordEntryImportResult | null>(null);
  const [wordEntryModalOpen, setWordEntryModalOpen] = useState(false);
  const [editingWordEntry, setEditingWordEntry] = useState<WordEntry | null>(null);
  const [filters, setFilters] = useState<WordEntryFilters>({ page: 1, pageSize: 20 });

  const wordSetsQuery = useQuery({
    queryKey: ["wordSets"],
    queryFn: listWordSets
  });

  const wordEntriesQuery = useQuery({
    queryKey: [
      "wordEntries",
      selectedWordSet?.id,
      filters.page ?? 1,
      filters.pageSize ?? 20,
      filters.keyword ?? "",
      filters.level ?? "",
      filters.tag ?? ""
    ],
    queryFn: () => listWordEntries(selectedWordSet!.id, filters),
    enabled: Boolean(selectedWordSet?.id)
  });

  const refreshWordEntries = async () => {
    await queryClient.invalidateQueries({ queryKey: ["wordEntries", selectedWordSet?.id] });
  };

  const createWordSetMutation = useMutation({
    mutationFn: createWordSet,
    onSuccess: async () => {
      message.success("词库已创建");
      wordSetForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ["wordSets"] });
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const importMutation = useMutation({
    mutationFn: ({ wordSetId, file }: { wordSetId: number; file: File }) => importWordEntries(wordSetId, file),
    onSuccess: async (result) => {
      setLastImportResult(result);
      message.success(`导入完成：新增 ${result.importedCount} 条，跳过 ${result.skippedCount} 条`);
      await refreshWordEntries();
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const createWordEntryMutation = useMutation({
    mutationFn: ({ wordSetId, payload }: { wordSetId: number; payload: WordEntryPayload }) => createWordEntry(wordSetId, payload),
    onSuccess: async () => {
      message.success("词条已创建");
      setWordEntryModalOpen(false);
      setEditingWordEntry(null);
      wordEntryForm.resetFields();
      await refreshWordEntries();
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const updateWordEntryMutation = useMutation({
    mutationFn: ({ wordId, payload }: { wordId: number; payload: WordEntryPayload }) => updateWordEntry(wordId, payload),
    onSuccess: async () => {
      message.success("词条已更新");
      setWordEntryModalOpen(false);
      setEditingWordEntry(null);
      wordEntryForm.resetFields();
      await refreshWordEntries();
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const deleteWordEntryMutation = useMutation({
    mutationFn: deleteWordEntry,
    onSuccess: async () => {
      message.success("词条已删除");
      await refreshWordEntries();
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const uploadProps: UploadProps = {
    accept: ".csv,.apkg",
    showUploadList: false,
    beforeUpload: (file) => {
      if (!selectedWordSet) {
        message.warning("请先选择一个词库");
        return Upload.LIST_IGNORE;
      }

      if (!/\.(csv|apkg)$/i.test(file.name)) {
        message.error("仅支持 .csv 或 .apkg 文件");
        return Upload.LIST_IGNORE;
      }

      importMutation.mutate({ wordSetId: selectedWordSet.id, file });
      return false;
    }
  };

  const handleSelectWordSet = (wordSet: WordSet | null) => {
    setSelectedWordSet(wordSet);
    setLastImportResult(null);
    setEditingWordEntry(null);
    setWordEntryModalOpen(false);
    wordEntryForm.resetFields();
    filterForm.resetFields();
    setFilters({ page: 1, pageSize: 20 });
  };

  const handleOpenCreateWordEntry = () => {
    if (!selectedWordSet) {
      message.warning("请先选择一个词库");
      return;
    }

    setEditingWordEntry(null);
    wordEntryForm.resetFields();
    setWordEntryModalOpen(true);
  };

  const handleOpenEditWordEntry = (wordEntry: WordEntry) => {
    setEditingWordEntry(wordEntry);
    wordEntryForm.setFieldsValue({
      expression: wordEntry.expression,
      reading: wordEntry.reading,
      meaning: wordEntry.meaning,
      partOfSpeech: wordEntry.partOfSpeech,
      exampleJp: wordEntry.exampleJp,
      exampleZh: wordEntry.exampleZh,
      level: wordEntry.level,
      tagsText: wordEntry.tags.join(", ")
    });
    setWordEntryModalOpen(true);
  };

  const handleWordEntrySubmit = (values: WordEntryFormValues) => {
    if (!selectedWordSet) {
      message.warning("请先选择一个词库");
      return;
    }

    const payload = buildWordEntryPayload(values);
    if (editingWordEntry) {
      updateWordEntryMutation.mutate({ wordId: editingWordEntry.id, payload });
      return;
    }

    createWordEntryMutation.mutate({ wordSetId: selectedWordSet.id, payload });
  };

  const handleFilterSubmit = (values: WordEntryFilterFormValues) => {
    setFilters({
      page: 1,
      pageSize: filters.pageSize ?? 20,
      keyword: values.keyword?.trim() || undefined,
      level: values.level?.trim() || undefined,
      tag: values.tag?.trim() || undefined
    });
  };

  const handleResetFilters = () => {
    filterForm.resetFields();
    setFilters({
      page: 1,
      pageSize: filters.pageSize ?? 20
    });
  };

  const handleWordEntryTableChange = (pagination: TablePaginationConfig) => {
    setFilters((current) => ({
      ...current,
      page: pagination.current ?? 1,
      pageSize: pagination.pageSize ?? 20
    }));
  };

  const wordEntryMutationPending = createWordEntryMutation.isPending || updateWordEntryMutation.isPending;

  return (
    <div className="page-stack">
      <PageHeader
        title="词库"
        description="提供词库列表、单词条维护、过滤检索与 CSV / APKG 导入能力。"
        extra={<Tag color="gold">word_set / word_entry</Tag>}
      />

      <PageSection title="创建词库">
        <Form
          form={wordSetForm}
          layout="vertical"
          onFinish={(values) => createWordSetMutation.mutate(values)}
        >
          <Form.Item label="名称" name="name" rules={[{ required: true, message: "请输入词库名称" }]}>
            <Input placeholder="例如：N4 高频词汇" />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={3} placeholder="例如：日语能力考 N4 词库" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={createWordSetMutation.isPending}>
            创建
          </Button>
        </Form>
      </PageSection>

      <PageSection
        title="词库列表"
        extra={<Typography.Text type="secondary">来源：GET /api/word-sets</Typography.Text>}
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
              onChange: (_, rows) => handleSelectWordSet(rows[0] ?? null)
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
          <Space wrap>
            <Button onClick={handleOpenCreateWordEntry} disabled={!selectedWordSet}>
              新增词条
            </Button>
            <Upload {...uploadProps}>
              <Button loading={importMutation.isPending} disabled={!selectedWordSet}>
                导入 CSV / APKG
              </Button>
            </Upload>
          </Space>
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
          <StatusState mode="empty" description="请选择一个词库后再查看和维护词条" />
        ) : (
          <Space direction="vertical" style={{ width: "100%" }} size={16}>
            <Form form={filterForm} layout="inline" onFinish={handleFilterSubmit}>
              <Form.Item label="关键词" name="keyword">
                <Input allowClear placeholder="匹配词条 / 读音 / 释义" />
              </Form.Item>
              <Form.Item label="等级" name="level">
                <Input allowClear placeholder="例如：N4" />
              </Form.Item>
              <Form.Item label="标签" name="tag">
                <Input allowClear placeholder="例如：工作" />
              </Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">
                  筛选
                </Button>
                <Button onClick={handleResetFilters}>重置</Button>
              </Space>
            </Form>

            {wordEntriesQuery.isLoading ? (
              <StatusState mode="loading" />
            ) : wordEntriesQuery.isError ? (
              <StatusState mode="error" description={(wordEntriesQuery.error as Error).message} />
            ) : (
              <Table
                rowKey="id"
                dataSource={wordEntriesQuery.data?.items ?? []}
                pagination={{
                  current: wordEntriesQuery.data?.page ?? filters.page ?? 1,
                  pageSize: wordEntriesQuery.data?.pageSize ?? filters.pageSize ?? 20,
                  total: wordEntriesQuery.data?.total ?? 0,
                  showSizeChanger: true
                }}
                onChange={handleWordEntryTableChange}
                locale={{
                  emptyText: <Typography.Text type="secondary">当前筛选条件下暂无词条</Typography.Text>
                }}
                columns={[
                  { title: "顺序", dataIndex: "sourceOrder", width: 80 },
                  { title: "词条", dataIndex: "expression", width: 140 },
                  { title: "读音", dataIndex: "reading", render: (value?: string) => value || "-" },
                  { title: "释义", dataIndex: "meaning" },
                  { title: "等级", dataIndex: "level", width: 100, render: (value?: string) => value || "-" },
                  {
                    title: "标签",
                    dataIndex: "tags",
                    render: (tags: string[]) => tags.length ? tags.join(", ") : "-"
                  },
                  {
                    title: "操作",
                    key: "actions",
                    width: 180,
                    render: (_, record: WordEntry) => (
                      <Space>
                        <Button type="link" onClick={() => handleOpenEditWordEntry(record)}>
                          编辑
                        </Button>
                        <Popconfirm
                          title="删除词条"
                          description="删除后会级联删除相关卡片与复习记录，确认继续吗？"
                          okText="删除"
                          cancelText="取消"
                          onConfirm={() => deleteWordEntryMutation.mutate(record.id)}
                        >
                          <Button type="link" danger loading={deleteWordEntryMutation.isPending}>
                            删除
                          </Button>
                        </Popconfirm>
                      </Space>
                    )
                  }
                ]}
              />
            )}
          </Space>
        )}
      </PageSection>

      <Modal
        title={editingWordEntry ? "编辑词条" : "新增词条"}
        open={wordEntryModalOpen}
        onCancel={() => {
          setWordEntryModalOpen(false);
          setEditingWordEntry(null);
          wordEntryForm.resetFields();
        }}
        onOk={() => wordEntryForm.submit()}
        confirmLoading={wordEntryMutationPending}
        destroyOnHidden
      >
        <Form form={wordEntryForm} layout="vertical" onFinish={handleWordEntrySubmit}>
          <Form.Item label="词条" name="expression" rules={[{ required: true, message: "请输入词条" }]}>
            <Input />
          </Form.Item>
          <Form.Item label="读音" name="reading">
            <Input />
          </Form.Item>
          <Form.Item label="释义" name="meaning" rules={[{ required: true, message: "请输入释义" }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item label="词性" name="partOfSpeech">
            <Input />
          </Form.Item>
          <Form.Item label="日语例句" name="exampleJp">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="中文例句" name="exampleZh">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="等级" name="level">
            <Input placeholder="例如：N4" />
          </Form.Item>
          <Form.Item label="标签" name="tagsText">
            <Input placeholder="多个标签请用逗号分隔" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
