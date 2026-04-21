import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  App,
  Button,
  Descriptions,
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
  previewWordEntriesImport,
  updateWordEntry,
  type CreateWordSetPayload,
  type WordEntry,
  type WordEntryFilters,
  type WordEntryImportPreviewResult,
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

const PREVIEW_STATUS_COLORS: Record<string, string> = {
  READY: "green",
  DUPLICATE: "orange",
  ERROR: "red"
};

export function WordSetPage() {
  const [wordSetForm] = Form.useForm<CreateWordSetPayload>();
  const [filterForm] = Form.useForm<WordEntryFilterFormValues>();
  const [wordEntryForm] = Form.useForm<WordEntryFormValues>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [selectedWordSet, setSelectedWordSet] = useState<WordSet | null>(null);
  const [lastImportResult, setLastImportResult] = useState<WordEntryImportResult | null>(null);
  const [importPreview, setImportPreview] = useState<WordEntryImportPreviewResult | null>(null);
  const [pendingImportFile, setPendingImportFile] = useState<File | null>(null);
  const [previewModalOpen, setPreviewModalOpen] = useState(false);
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
      message.success("Word set created.");
      wordSetForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ["wordSets"] });
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const previewMutation = useMutation({
    mutationFn: ({ wordSetId, file }: { wordSetId: number; file: File }) => previewWordEntriesImport(wordSetId, file),
    onSuccess: (result, variables) => {
      setImportPreview(result);
      setPendingImportFile(variables.file);
      setPreviewModalOpen(true);
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const importMutation = useMutation({
    mutationFn: ({ wordSetId, file }: { wordSetId: number; file: File }) => importWordEntries(wordSetId, file),
    onSuccess: async (result) => {
      setLastImportResult(result);
      setPreviewModalOpen(false);
      setImportPreview(null);
      setPendingImportFile(null);
      message.success(`Import completed: ${result.importedCount} imported, ${result.skippedCount} skipped.`);
      await refreshWordEntries();
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const createWordEntryMutation = useMutation({
    mutationFn: ({ wordSetId, payload }: { wordSetId: number; payload: WordEntryPayload }) => createWordEntry(wordSetId, payload),
    onSuccess: async () => {
      message.success("Word entry created.");
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
      message.success("Word entry updated.");
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
      message.success("Word entry deleted.");
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
        message.warning("Select a word set before importing.");
        return Upload.LIST_IGNORE;
      }

      if (!/\.(csv|apkg)$/i.test(file.name)) {
        message.error("Only .csv and .apkg files are supported.");
        return Upload.LIST_IGNORE;
      }

      previewMutation.mutate({ wordSetId: selectedWordSet.id, file });
      return false;
    }
  };

  const handleSelectWordSet = (wordSet: WordSet | null) => {
    setSelectedWordSet(wordSet);
    setLastImportResult(null);
    setImportPreview(null);
    setPendingImportFile(null);
    setPreviewModalOpen(false);
    setEditingWordEntry(null);
    setWordEntryModalOpen(false);
    wordEntryForm.resetFields();
    filterForm.resetFields();
    setFilters({ page: 1, pageSize: 20 });
  };

  const handleOpenCreateWordEntry = () => {
    if (!selectedWordSet) {
      message.warning("Select a word set first.");
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
      message.warning("Select a word set first.");
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

  const handleConfirmImport = () => {
    if (!selectedWordSet || !pendingImportFile) {
      return;
    }

    importMutation.mutate({
      wordSetId: selectedWordSet.id,
      file: pendingImportFile
    });
  };

  const wordEntryMutationPending = createWordEntryMutation.isPending || updateWordEntryMutation.isPending;

  return (
    <div className="page-stack">
      <PageHeader
        title="Word Sets"
        description="Manage source data, preview CSV / APKG imports before saving, and maintain individual word entries."
        extra={<Tag color="gold">word_set / word_entry</Tag>}
      />

      <PageSection title="Create Word Set">
        <Form form={wordSetForm} layout="vertical" onFinish={(values) => createWordSetMutation.mutate(values)}>
          <Form.Item label="Name" name="name" rules={[{ required: true, message: "Enter a word set name." }]}>
            <Input placeholder="N4 Core Vocabulary" />
          </Form.Item>
          <Form.Item label="Description" name="description">
            <Input.TextArea rows={3} placeholder="Short note about this word set" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={createWordSetMutation.isPending}>
            Create
          </Button>
        </Form>
      </PageSection>

      <PageSection
        title="Word Set List"
        extra={<Typography.Text type="secondary">GET /api/word-sets</Typography.Text>}
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
              { title: "Name", dataIndex: "name" },
              { title: "Description", dataIndex: "description", render: (value?: string) => value || "-" },
              { title: "Created At", dataIndex: "createdAt" }
            ]}
            locale={{
              emptyText: <Typography.Text type="secondary">No word sets yet.</Typography.Text>
            }}
          />
        )}
      </PageSection>

      <PageSection
        title={selectedWordSet ? `Word Entries: ${selectedWordSet.name}` : "Word Entries"}
        extra={
          <Space wrap>
            <Button onClick={handleOpenCreateWordEntry} disabled={!selectedWordSet}>
              New Entry
            </Button>
            <Upload {...uploadProps}>
              <Button loading={previewMutation.isPending} disabled={!selectedWordSet}>
                Preview Import
              </Button>
            </Upload>
          </Space>
        }
      >
        {lastImportResult ? (
          <Space direction="vertical" style={{ width: "100%", marginBottom: 16 }}>
            <Typography.Text>
              Last import result: {lastImportResult.importedCount} imported, {lastImportResult.skippedCount} skipped.
            </Typography.Text>
            {lastImportResult.errors.length > 0 ? (
              <Table
                rowKey={(record) => `${record.lineNumber}-${record.field}`}
                size="small"
                pagination={false}
                dataSource={lastImportResult.errors}
                columns={[
                  { title: "Line", dataIndex: "lineNumber", width: 80 },
                  { title: "Field", dataIndex: "field", width: 120 },
                  { title: "Message", dataIndex: "message" }
                ]}
              />
            ) : null}
          </Space>
        ) : null}

        {!selectedWordSet ? (
          <StatusState mode="empty" description="Select a word set to browse or import entries." />
        ) : (
          <Space direction="vertical" style={{ width: "100%" }} size={16}>
            <Form form={filterForm} layout="inline" onFinish={handleFilterSubmit}>
              <Form.Item label="Keyword" name="keyword">
                <Input allowClear placeholder="expression / reading / meaning" />
              </Form.Item>
              <Form.Item label="Level" name="level">
                <Input allowClear placeholder="N4" />
              </Form.Item>
              <Form.Item label="Tag" name="tag">
                <Input allowClear placeholder="verb" />
              </Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">
                  Filter
                </Button>
                <Button onClick={handleResetFilters}>Reset</Button>
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
                  emptyText: <Typography.Text type="secondary">No entries match the current filter.</Typography.Text>
                }}
                columns={[
                  { title: "Order", dataIndex: "sourceOrder", width: 80 },
                  { title: "Expression", dataIndex: "expression", width: 160 },
                  { title: "Reading", dataIndex: "reading", render: (value?: string) => value || "-" },
                  { title: "Meaning", dataIndex: "meaning" },
                  { title: "Level", dataIndex: "level", width: 100, render: (value?: string) => value || "-" },
                  {
                    title: "Tags",
                    dataIndex: "tags",
                    render: (tags: string[]) => (tags.length ? tags.join(", ") : "-")
                  },
                  {
                    title: "Actions",
                    key: "actions",
                    width: 180,
                    render: (_, record: WordEntry) => (
                      <Space>
                        <Button type="link" onClick={() => handleOpenEditWordEntry(record)}>
                          Edit
                        </Button>
                        <Popconfirm
                          title="Delete word entry"
                          description="This deletes the current word entry. Related cards or review data are handled by backend rules."
                          okText="Delete"
                          cancelText="Cancel"
                          onConfirm={() => deleteWordEntryMutation.mutate(record.id)}
                        >
                          <Button type="link" danger loading={deleteWordEntryMutation.isPending}>
                            Delete
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
        title="Import Preview"
        open={previewModalOpen}
        onCancel={() => {
          setPreviewModalOpen(false);
          setImportPreview(null);
          setPendingImportFile(null);
        }}
        onOk={handleConfirmImport}
        okText="Confirm Import"
        cancelText="Cancel"
        okButtonProps={{
          disabled: !importPreview || importPreview.readyCount === 0
        }}
        confirmLoading={importMutation.isPending}
        width={1080}
        destroyOnHidden
      >
        {!importPreview ? (
          <StatusState mode="empty" description="No preview data loaded." />
        ) : (
          <Space direction="vertical" style={{ width: "100%" }} size={16}>
            <Descriptions
              bordered
              size="small"
              column={3}
              items={[
                { key: "sourceType", label: "Source Type", children: importPreview.sourceType },
                { key: "totalRows", label: "Total Rows", children: importPreview.totalRows },
                { key: "readyCount", label: "Ready", children: importPreview.readyCount },
                { key: "duplicateCount", label: "Duplicates", children: importPreview.duplicateCount },
                { key: "errorCount", label: "Errors", children: importPreview.errorCount },
                {
                  key: "note",
                  label: "Import Rule",
                  children: "Only READY rows will be imported."
                }
              ]}
            />

            <Table
              rowKey="targetField"
              size="small"
              pagination={false}
              dataSource={importPreview.fieldMappings}
              columns={[
                { title: "Target Field", dataIndex: "targetField" },
                {
                  title: "Required",
                  dataIndex: "required",
                  width: 90,
                  render: (value: boolean) => (value ? "Yes" : "No")
                },
                {
                  title: "Mapped",
                  dataIndex: "mapped",
                  width: 90,
                  render: (value: boolean) => <Tag color={value ? "green" : "default"}>{value ? "Yes" : "No"}</Tag>
                },
                { title: "Source Field", dataIndex: "sourceField", render: (value?: string) => value || "-" },
                { title: "Note", dataIndex: "note" }
              ]}
            />

            <Table
              rowKey={(record) => `${record.lineNumber}-${record.expression}-${record.status}`}
              size="small"
              pagination={{ pageSize: 10 }}
              dataSource={importPreview.previewRows}
              columns={[
                { title: "Line", dataIndex: "lineNumber", width: 80 },
                { title: "Expression", dataIndex: "expression", render: (value?: string) => value || "-" },
                { title: "Reading", dataIndex: "reading", render: (value?: string) => value || "-" },
                { title: "Meaning", dataIndex: "meaning", render: (value?: string) => value || "-" },
                {
                  title: "Status",
                  dataIndex: "status",
                  width: 120,
                  render: (value: string) => <Tag color={PREVIEW_STATUS_COLORS[value] ?? "default"}>{value}</Tag>
                },
                { title: "Field", dataIndex: "field", width: 120, render: (value?: string) => value || "-" },
                { title: "Message", dataIndex: "message" }
              ]}
            />
          </Space>
        )}
      </Modal>

      <Modal
        title={editingWordEntry ? "Edit Word Entry" : "New Word Entry"}
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
          <Form.Item label="Expression" name="expression" rules={[{ required: true, message: "Enter expression." }]}>
            <Input />
          </Form.Item>
          <Form.Item label="Reading" name="reading">
            <Input />
          </Form.Item>
          <Form.Item label="Meaning" name="meaning" rules={[{ required: true, message: "Enter meaning." }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item label="Part Of Speech" name="partOfSpeech">
            <Input />
          </Form.Item>
          <Form.Item label="Example JP" name="exampleJp">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="Example ZH" name="exampleZh">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="Level" name="level">
            <Input placeholder="N4" />
          </Form.Item>
          <Form.Item label="Tags" name="tagsText">
            <Input placeholder="comma separated tags" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
