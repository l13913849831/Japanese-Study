import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Radio,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  type TablePaginationConfig,
  type UploadProps
} from "antd";
import dayjs from "dayjs";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import {
  createNote,
  deleteNote,
  importNotes,
  listNotes,
  previewNoteImport,
  updateNote,
  type Note,
  type NoteFilters,
  type NoteImportPreviewItem,
  type NoteImportPreviewResult,
  type NoteImportResult,
  type NoteImportSplitMode,
  type NoteMasteryStatus,
  type SaveNotePayload
} from "@/features/notes/api";

const { TextArea } = Input;

interface NoteFormValues {
  title?: string;
  content?: string;
  tagsText?: string;
}

interface NoteFilterFormValues {
  keyword?: string;
  tag?: string;
  masteryStatus?: NoteMasteryStatus;
}

interface ImportFormValues {
  splitMode?: NoteImportSplitMode;
  commonTagsText?: string;
}

interface EditablePreviewItem {
  itemId: string;
  title: string;
  content: string;
  tagsText: string;
  tagSyncEnabled: boolean;
}

interface ImportDraft {
  splitMode: NoteImportSplitMode;
  commonTagsText: string;
  items: EditablePreviewItem[];
}

const NOTE_MASTERY_LABELS: Record<NoteMasteryStatus, string> = {
  UNSTARTED: "未开始",
  LEARNING: "学习中",
  CONSOLIDATING: "巩固中",
  MASTERED: "已掌握"
};

const NOTE_MASTERY_COLORS: Record<NoteMasteryStatus, string> = {
  UNSTARTED: "default",
  LEARNING: "orange",
  CONSOLIDATING: "blue",
  MASTERED: "green"
};

const SPLIT_MODE_OPTIONS: Array<{ value: NoteImportSplitMode; label: string }> = [
  { value: "H1", label: "只按 #" },
  { value: "H1_H2", label: "按 # + ##" },
  { value: "ALL", label: "按全部标题" }
];

function parseTags(value: string | undefined) {
  return String(value ?? "")
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function formatTags(tags: string[]) {
  return tags.join(", ");
}

function buildNotePayload(values: NoteFormValues): SaveNotePayload {
  return {
    title: values.title?.trim() ?? "",
    content: values.content?.trim() ?? "",
    tags: parseTags(values.tagsText)
  };
}

function buildImportDraft(result: NoteImportPreviewResult, commonTagsText: string): ImportDraft {
  return {
    splitMode: result.splitMode,
    commonTagsText,
    items: result.previewItems.map((item) => ({
      itemId: item.itemId,
      title: item.title,
      content: item.content,
      tagsText: formatTags(item.tags),
      tagSyncEnabled: true
    }))
  };
}

function validatePreviewItem(item: EditablePreviewItem) {
  const title = item.title.trim();
  const content = item.content.trim();
  if (!title) {
    return "标题不能为空";
  }
  if (!content) {
    return "内容不能为空";
  }
  return null;
}

export function NotesPage() {
  const [noteForm] = Form.useForm<NoteFormValues>();
  const [filterForm] = Form.useForm<NoteFilterFormValues>();
  const [importForm] = Form.useForm<ImportFormValues>();
  const { message } = App.useApp();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<NoteFilters>({ page: 1, pageSize: 20 });
  const [editingNote, setEditingNote] = useState<Note | null>(null);
  const [noteModalOpen, setNoteModalOpen] = useState(false);
  const [importPreviewOpen, setImportPreviewOpen] = useState(false);
  const [importDraft, setImportDraft] = useState<ImportDraft | null>(null);
  const [lastImportResult, setLastImportResult] = useState<NoteImportResult | null>(null);

  const notesQuery = useQuery({
    queryKey: [
      "notes",
      filters.page ?? 1,
      filters.pageSize ?? 20,
      filters.keyword ?? "",
      filters.tag ?? "",
      filters.masteryStatus ?? ""
    ],
    queryFn: () => listNotes(filters)
  });

  const refreshNotes = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["notes"] }),
      queryClient.invalidateQueries({ queryKey: ["noteDashboard"] }),
      queryClient.invalidateQueries({ queryKey: ["todayNoteReviews"] })
    ]);
  };

  const createMutation = useMutation({
    mutationFn: createNote,
    onSuccess: async () => {
      message.success("Knowledge point created.");
      setNoteModalOpen(false);
      setEditingNote(null);
      noteForm.resetFields();
      await refreshNotes();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const updateMutation = useMutation({
    mutationFn: ({ noteId, payload }: { noteId: number; payload: SaveNotePayload }) => updateNote(noteId, payload),
    onSuccess: async () => {
      message.success("Knowledge point updated.");
      setNoteModalOpen(false);
      setEditingNote(null);
      noteForm.resetFields();
      await refreshNotes();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const deleteMutation = useMutation({
    mutationFn: deleteNote,
    onSuccess: async () => {
      message.success("Knowledge point deleted.");
      await refreshNotes();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const previewMutation = useMutation({
    mutationFn: ({ file, splitMode, commonTagsText }: { file: File; splitMode: NoteImportSplitMode; commonTagsText?: string }) =>
      previewNoteImport(file, splitMode, commonTagsText),
    onSuccess: (result, variables) => {
      setImportDraft(buildImportDraft(result, variables.commonTagsText?.trim() ?? ""));
      setImportPreviewOpen(true);
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const importMutation = useMutation({
    mutationFn: importNotes,
    onSuccess: async (result) => {
      setLastImportResult(result);
      setImportDraft(null);
      setImportPreviewOpen(false);
      message.success(`Import completed: ${result.importedCount} imported, ${result.skippedCount} skipped.`);
      await refreshNotes();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const uploadProps: UploadProps = {
    accept: ".md",
    showUploadList: false,
    beforeUpload: (file) => {
      if (!/\.md$/i.test(file.name)) {
        message.error("Only .md files are supported.");
        return Upload.LIST_IGNORE;
      }
      const values = importForm.getFieldsValue();
      previewMutation.mutate({
        file,
        splitMode: values.splitMode ?? "H1",
        commonTagsText: values.commonTagsText?.trim() || undefined
      });
      return false;
    }
  };

  const handleOpenCreate = () => {
    setEditingNote(null);
    noteForm.resetFields();
    setNoteModalOpen(true);
  };

  const handleOpenEdit = (note: Note) => {
    setEditingNote(note);
    noteForm.setFieldsValue({
      title: note.title,
      content: note.content,
      tagsText: formatTags(note.tags)
    });
    setNoteModalOpen(true);
  };

  const handleSubmitNote = (values: NoteFormValues) => {
    const payload = buildNotePayload(values);
    if (editingNote) {
      updateMutation.mutate({ noteId: editingNote.id, payload });
      return;
    }
    createMutation.mutate(payload);
  };

  const handleChangeImportCommonTags = (value: string) => {
    setImportDraft((current) => {
      if (!current) {
        return current;
      }
      const nextTagsText = formatTags(parseTags(value));
      return {
        ...current,
        commonTagsText: value,
        items: current.items.map((item) =>
          item.tagSyncEnabled
            ? {
                ...item,
                tagsText: nextTagsText
              }
            : item
        )
      };
    });
  };

  const handleChangePreviewItem = (itemId: string, patch: Partial<EditablePreviewItem>) => {
    setImportDraft((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        items: current.items.map((item) => (item.itemId === itemId ? { ...item, ...patch } : item))
      };
    });
  };

  const handleRemovePreviewItem = (itemId: string) => {
    setImportDraft((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        items: current.items.filter((item) => item.itemId !== itemId)
      };
    });
  };

  const draftItems = importDraft?.items ?? [];
  const draftErrors = draftItems
    .map((item, index) => ({ index, message: validatePreviewItem(item) }))
    .filter((item): item is { index: number; message: string } => Boolean(item.message));

  const handleConfirmImport = () => {
    if (!importDraft) {
      return;
    }
    importMutation.mutate(
      importDraft.items.map((item) => ({
        title: item.title.trim(),
        content: item.content.trim(),
        tags: parseTags(item.tagsText)
      }))
    );
  };

  const noteMutationPending = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="page-stack">
      <PageHeader
        title="Notes"
        description="Create knowledge points, preview Markdown imports before saving, and maintain an independent note review source."
        extra={
          <Space wrap>
            <Button onClick={() => navigate("/notes/review")}>Start Review</Button>
            <Tag color="cyan">note / note-review</Tag>
          </Space>
        }
      />

      <PageSection title="Create Knowledge Point" extra={<Button type="primary" onClick={handleOpenCreate}>New Note</Button>}>
        <Typography.Text type="secondary">
          Direct creation keeps the model minimal: title, content, and tags.
        </Typography.Text>
      </PageSection>

      <PageSection title="Markdown Import">
        <Form<ImportFormValues>
          form={importForm}
          layout="vertical"
          initialValues={{ splitMode: "H1" }}
        >
          <Form.Item label="Split Mode" name="splitMode">
            <Radio.Group optionType="button" buttonStyle="solid" options={SPLIT_MODE_OPTIONS} />
          </Form.Item>
          <Form.Item label="Common Tags" name="commonTagsText">
            <Input placeholder="grammar, jlpt-n2" />
          </Form.Item>
          <Upload {...uploadProps}>
            <Button loading={previewMutation.isPending}>Upload Markdown For Preview</Button>
          </Upload>
        </Form>
        {lastImportResult ? (
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            Last import: {lastImportResult.importedCount} imported, {lastImportResult.skippedCount} skipped.
          </Typography.Paragraph>
        ) : null}
      </PageSection>

      <PageSection title="Filters">
        <Form<NoteFilterFormValues>
          form={filterForm}
          layout="inline"
          onFinish={(values) =>
            setFilters({
              page: 1,
              pageSize: filters.pageSize ?? 20,
              keyword: values.keyword?.trim() || undefined,
              tag: values.tag?.trim() || undefined,
              masteryStatus: values.masteryStatus
            })
          }
        >
          <Form.Item label="Keyword" name="keyword">
            <Input placeholder="title / content" />
          </Form.Item>
          <Form.Item label="Tag" name="tag">
            <Input placeholder="grammar" />
          </Form.Item>
          <Form.Item label="Mastery" name="masteryStatus">
            <Radio.Group
              options={[
                { label: "未开始", value: "UNSTARTED" },
                { label: "学习中", value: "LEARNING" },
                { label: "巩固中", value: "CONSOLIDATING" },
                { label: "已掌握", value: "MASTERED" }
              ]}
            />
          </Form.Item>
          <Button type="primary" htmlType="submit">
            Apply
          </Button>
          <Button
            onClick={() => {
              filterForm.resetFields();
              setFilters({ page: 1, pageSize: filters.pageSize ?? 20 });
            }}
          >
            Reset
          </Button>
        </Form>
      </PageSection>

      <PageSection title="Knowledge Point List">
        {notesQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : notesQuery.isError ? (
          <StatusState mode="error" description={(notesQuery.error as Error).message} />
        ) : (
          <Table<Note>
            rowKey="id"
            dataSource={notesQuery.data?.items ?? []}
            pagination={{
              current: notesQuery.data?.page,
              pageSize: notesQuery.data?.pageSize,
              total: notesQuery.data?.total,
              showSizeChanger: true
            }}
            onChange={(pagination: TablePaginationConfig) =>
              setFilters((current) => ({
                ...current,
                page: pagination.current ?? 1,
                pageSize: pagination.pageSize ?? 20
              }))
            }
            columns={[
              {
                title: "Title",
                dataIndex: "title"
              },
              {
                title: "Tags",
                dataIndex: "tags",
                render: (tags: string[]) => (tags.length ? tags.join(", ") : "-")
              },
              {
                title: "Mastery",
                dataIndex: "masteryStatus",
                render: (value: NoteMasteryStatus) => (
                  <Tag color={NOTE_MASTERY_COLORS[value]}>{NOTE_MASTERY_LABELS[value]}</Tag>
                )
              },
              {
                title: "Due",
                dataIndex: "dueAt",
                render: (value: string) => dayjs(value).format("YYYY-MM-DD HH:mm")
              },
              {
                title: "Reviews",
                dataIndex: "reviewCount",
                width: 100
              },
              {
                title: "Updated",
                dataIndex: "updatedAt",
                render: (value: string) => dayjs(value).format("YYYY-MM-DD HH:mm")
              },
              {
                title: "Action",
                render: (_, record) => (
                  <Space wrap>
                    <Button type="link" onClick={() => handleOpenEdit(record)}>
                      Edit
                    </Button>
                    <Button type="link" onClick={() => navigate("/notes/review")}>
                      Review
                    </Button>
                    <Popconfirm
                      title="Delete this knowledge point?"
                      onConfirm={() => deleteMutation.mutate(record.id)}
                    >
                      <Button type="link" danger loading={deleteMutation.isPending}>
                        Delete
                      </Button>
                    </Popconfirm>
                  </Space>
                )
              }
            ]}
            locale={{
              emptyText: "No knowledge points found."
            }}
          />
        )}
      </PageSection>

      <Modal
        title={editingNote ? `Edit Note #${editingNote.id}` : "Create Knowledge Point"}
        open={noteModalOpen}
        confirmLoading={noteMutationPending}
        onCancel={() => {
          setNoteModalOpen(false);
          setEditingNote(null);
          noteForm.resetFields();
        }}
        onOk={() => noteForm.submit()}
        width={720}
      >
        <Form<NoteFormValues> form={noteForm} layout="vertical" onFinish={handleSubmitNote}>
          <Form.Item label="Title" name="title" rules={[{ required: true, message: "Enter a title." }]}>
            <Input />
          </Form.Item>
          <Form.Item label="Content" name="content" rules={[{ required: true, message: "Enter content." }]}>
            <TextArea rows={8} />
          </Form.Item>
          <Form.Item label="Tags" name="tagsText">
            <Input placeholder="grammar, reading" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Import Preview"
        open={importPreviewOpen}
        width={1100}
        destroyOnClose
        confirmLoading={importMutation.isPending}
        okButtonProps={{
          disabled: draftItems.length === 0 || draftErrors.length > 0
        }}
        onCancel={() => {
          setImportPreviewOpen(false);
          setImportDraft(null);
        }}
        onOk={handleConfirmImport}
      >
        {!importDraft ? (
          <StatusState mode="empty" description="Upload a Markdown file first." />
        ) : (
          <Space direction="vertical" style={{ width: "100%" }} size={16}>
            <Typography.Text type="secondary">
              Common tags still sync into untouched rows. Once you edit one row’s tags, that row stops auto-syncing.
            </Typography.Text>
            <Input
              value={importDraft.commonTagsText}
              onChange={(event) => handleChangeImportCommonTags(event.target.value)}
              placeholder="Common tags"
            />
            {draftErrors.length ? (
              <Typography.Text type="danger">
                {draftErrors.length} row(s) still invalid. Fill title/content or remove them before import.
              </Typography.Text>
            ) : null}
            <Table<EditablePreviewItem>
              rowKey="itemId"
              pagination={false}
              size="small"
              dataSource={draftItems}
              scroll={{ x: 960 }}
              columns={[
                {
                  title: "Title",
                  width: 220,
                  render: (_, record) => (
                    <Input
                      value={record.title}
                      onChange={(event) => handleChangePreviewItem(record.itemId, { title: event.target.value })}
                    />
                  )
                },
                {
                  title: "Content",
                  width: 380,
                  render: (_, record) => (
                    <TextArea
                      rows={4}
                      value={record.content}
                      onChange={(event) => handleChangePreviewItem(record.itemId, { content: event.target.value })}
                    />
                  )
                },
                {
                  title: "Tags",
                  width: 220,
                  render: (_, record) => (
                    <Input
                      value={record.tagsText}
                      onChange={(event) =>
                        handleChangePreviewItem(record.itemId, {
                          tagsText: event.target.value,
                          tagSyncEnabled: false
                        })
                      }
                    />
                  )
                },
                {
                  title: "State",
                  width: 160,
                  render: (_, record) => {
                    const error = validatePreviewItem(record);
                    return (
                      <Space direction="vertical" size={4}>
                        <Tag color={error ? "red" : "green"}>{error ? "ERROR" : "READY"}</Tag>
                        <Typography.Text type={error ? "danger" : "secondary"}>{error ?? "Ready to import"}</Typography.Text>
                      </Space>
                    );
                  }
                },
                {
                  title: "Action",
                  width: 100,
                  render: (_, record) => (
                    <Button danger type="link" onClick={() => handleRemovePreviewItem(record.itemId)}>
                      Remove
                    </Button>
                  )
                }
              ]}
            />
          </Space>
        )}
      </Modal>
    </div>
  );
}
