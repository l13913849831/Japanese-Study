import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, Descriptions, Form, Input, List, Modal, Space, Tabs, Tag, Typography, type FormInstance } from "antd";
import { useState } from "react";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import {
  createAnkiTemplate,
  createMarkdownTemplate,
  listAnkiTemplates,
  listMarkdownTemplates,
  previewAnkiTemplate,
  previewMarkdownTemplate,
  updateAnkiTemplate,
  updateMarkdownTemplate,
  type AnkiTemplate,
  type MarkdownTemplate,
  type SaveAnkiTemplatePayload,
  type SaveMarkdownTemplatePayload,
  type TemplateCardSample
} from "@/features/templates/api";

const { TextArea } = Input;

interface AnkiTemplateFormValues {
  name?: string;
  description?: string;
  fieldMappingText?: string;
  frontTemplate?: string;
  backTemplate?: string;
  cssTemplate?: string;
}

interface MarkdownTemplateFormValues {
  name?: string;
  description?: string;
  templateContent?: string;
}

const SAMPLE_CARD: TemplateCardSample = {
  expression: "study",
  reading: "study",
  meaning: "learning",
  partOfSpeech: "noun",
  exampleJp: "I study Japanese every day.",
  exampleZh: "I study Japanese every day.",
  tags: ["N4", "common"],
  dueDate: "2026-04-20",
  planName: "N4 Daily 20"
};

const SAMPLE_REVIEW_CARD: TemplateCardSample = {
  ...SAMPLE_CARD,
  expression: "teacher",
  reading: "teacher",
  meaning: "teacher"
};

const DEFAULT_FIELD_MAPPING_TEXT = JSON.stringify(
  {
    front: ["expression"],
    back: ["reading", "meaning", "exampleJp", "exampleZh"]
  },
  null,
  2
);

function formatFieldMapping(fieldMapping: Record<string, string[]>) {
  return JSON.stringify(fieldMapping, null, 2);
}

function parseFieldMapping(fieldMappingText: string | undefined) {
  const raw = String(fieldMappingText ?? "").trim();
  if (!raw) {
    throw new Error("Field mapping is required");
  }

  const parsed = JSON.parse(raw) as unknown;
  if (!parsed || Array.isArray(parsed) || typeof parsed !== "object") {
    throw new Error("Field mapping must be a JSON object");
  }

  const result: Record<string, string[]> = {};
  for (const [key, value] of Object.entries(parsed as Record<string, unknown>)) {
    if (!key.trim()) {
      throw new Error("Field mapping keys must not be blank");
    }
    if (!Array.isArray(value) || value.some((item) => typeof item !== "string")) {
      throw new Error(`Field mapping '${key}' must be an array of strings`);
    }
    result[key.trim()] = value.map((item) => item.trim());
  }

  return result;
}

function normalizeOptionalText(value: string | undefined) {
  const normalized = String(value ?? "").trim();
  return normalized ? normalized : undefined;
}

function fillAnkiForm(form: FormInstance<AnkiTemplateFormValues>, template: AnkiTemplate) {
  form.setFieldsValue({
    name: template.name,
    description: template.description,
    fieldMappingText: formatFieldMapping(template.fieldMapping),
    frontTemplate: template.frontTemplate,
    backTemplate: template.backTemplate,
    cssTemplate: template.cssTemplate
  });
}

function fillMarkdownForm(form: FormInstance<MarkdownTemplateFormValues>, template: MarkdownTemplate) {
  form.setFieldsValue({
    name: template.name,
    description: template.description,
    templateContent: template.templateContent
  });
}

function renderScopeTag(scope: "SYSTEM" | "USER") {
  return <Tag color={scope === "SYSTEM" ? "blue" : "green"}>{scope === "SYSTEM" ? "SYSTEM" : "MY"}</Tag>;
}

export function TemplatePage() {
  const [ankiForm] = Form.useForm<AnkiTemplateFormValues>();
  const [markdownForm] = Form.useForm<MarkdownTemplateFormValues>();
  const [editingAnkiId, setEditingAnkiId] = useState<number>();
  const [editingMarkdownId, setEditingMarkdownId] = useState<number>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  const ankiQuery = useQuery({
    queryKey: ["ankiTemplates"],
    queryFn: listAnkiTemplates
  });
  const markdownQuery = useQuery({
    queryKey: ["markdownTemplates"],
    queryFn: listMarkdownTemplates
  });

  const refreshAnkiTemplates = async () => {
    await queryClient.invalidateQueries({ queryKey: ["ankiTemplates"] });
  };

  const refreshMarkdownTemplates = async () => {
    await queryClient.invalidateQueries({ queryKey: ["markdownTemplates"] });
  };

  const resetAnkiForm = () => {
    setEditingAnkiId(undefined);
    ankiForm.setFieldsValue({
      name: undefined,
      description: undefined,
      fieldMappingText: DEFAULT_FIELD_MAPPING_TEXT,
      frontTemplate: undefined,
      backTemplate: undefined,
      cssTemplate: undefined
    });
  };

  const resetMarkdownForm = () => {
    setEditingMarkdownId(undefined);
    markdownForm.resetFields();
  };

  const saveAnkiMutation = useMutation({
    mutationFn: ({ id, payload }: { id?: number; payload: SaveAnkiTemplatePayload }) =>
      id ? updateAnkiTemplate(id, payload) : createAnkiTemplate(payload),
    onSuccess: async (_, variables) => {
      message.success(variables.id ? "Anki template updated" : "Anki template created");
      await refreshAnkiTemplates();
      resetAnkiForm();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const saveMarkdownMutation = useMutation({
    mutationFn: ({ id, payload }: { id?: number; payload: SaveMarkdownTemplatePayload }) =>
      id ? updateMarkdownTemplate(id, payload) : createMarkdownTemplate(payload),
    onSuccess: async (_, variables) => {
      message.success(variables.id ? "Markdown template updated" : "Markdown template created");
      await refreshMarkdownTemplates();
      resetMarkdownForm();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const ankiPreviewMutation = useMutation({
    mutationFn: previewAnkiTemplate,
    onSuccess: (result) => {
      Modal.info({
        width: 760,
        title: "Anki Preview",
        content: (
          <Descriptions column={1} size="small">
            <Descriptions.Item label="Front">
              <Typography.Paragraph code>{result.frontRendered}</Typography.Paragraph>
            </Descriptions.Item>
            <Descriptions.Item label="Back">
              <Typography.Paragraph code>{result.backRendered}</Typography.Paragraph>
            </Descriptions.Item>
            <Descriptions.Item label="CSS">
              <Typography.Paragraph code>{result.cssRendered || "-"}</Typography.Paragraph>
            </Descriptions.Item>
          </Descriptions>
        )
      });
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const markdownPreviewMutation = useMutation({
    mutationFn: previewMarkdownTemplate,
    onSuccess: (result) => {
      Modal.info({
        width: 760,
        title: "Markdown Preview",
        content: <Typography.Paragraph code>{result.renderedContent}</Typography.Paragraph>
      });
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const handleSubmitAnki = async () => {
    try {
      const values = await ankiForm.validateFields();
      const payload: SaveAnkiTemplatePayload = {
        name: String(values.name ?? "").trim(),
        description: normalizeOptionalText(values.description),
        fieldMapping: parseFieldMapping(values.fieldMappingText),
        frontTemplate: String(values.frontTemplate ?? "").trim(),
        backTemplate: String(values.backTemplate ?? "").trim(),
        cssTemplate: normalizeOptionalText(values.cssTemplate)
      };
      saveAnkiMutation.mutate({ id: editingAnkiId, payload });
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message);
      }
    }
  };

  const handlePreviewAnki = async () => {
    try {
      const values = await ankiForm.validateFields(["frontTemplate", "backTemplate", "cssTemplate"]);
      ankiPreviewMutation.mutate({
        frontTemplate: String(values.frontTemplate ?? "").trim(),
        backTemplate: String(values.backTemplate ?? "").trim(),
        cssTemplate: normalizeOptionalText(values.cssTemplate),
        sample: SAMPLE_CARD
      });
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message);
      }
    }
  };

  const handleSubmitMarkdown = async () => {
    try {
      const values = await markdownForm.validateFields();
      const payload: SaveMarkdownTemplatePayload = {
        name: String(values.name ?? "").trim(),
        description: normalizeOptionalText(values.description),
        templateContent: String(values.templateContent ?? "").trim()
      };
      saveMarkdownMutation.mutate({ id: editingMarkdownId, payload });
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message);
      }
    }
  };

  const handlePreviewMarkdown = async () => {
    try {
      const values = await markdownForm.validateFields(["templateContent"]);
      markdownPreviewMutation.mutate({
        templateContent: String(values.templateContent ?? "").trim(),
        date: "2026-04-20",
        planName: SAMPLE_CARD.planName,
        newCards: [SAMPLE_CARD],
        reviewCards: [SAMPLE_REVIEW_CARD]
      });
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message);
      }
    }
  };

  const handleEditAnki = (template: AnkiTemplate) => {
    setEditingAnkiId(template.id);
    fillAnkiForm(ankiForm, template);
  };

  const handleEditMarkdown = (template: MarkdownTemplate) => {
    setEditingMarkdownId(template.id);
    fillMarkdownForm(markdownForm, template);
  };

  const loading = ankiQuery.isLoading || markdownQuery.isLoading;
  const errorMessage =
    (ankiQuery.error as Error | undefined)?.message ?? (markdownQuery.error as Error | undefined)?.message;

  return (
    <div className="page-stack">
      <PageHeader
        title="Template Management"
        description="Create, update, and preview Anki or Markdown templates. Study plans reuse the same template query keys, so newly saved templates become selectable after cache refresh."
        extra={<Tag color="cyan">anki_template / md_template</Tag>}
      />

      <PageSection title="Template Workspace">
        {loading ? (
          <StatusState mode="loading" />
        ) : errorMessage ? (
          <StatusState mode="error" description={errorMessage} />
        ) : (
          <Tabs
            items={[
              {
                key: "anki",
                label: "Anki Templates",
                children: (
                  <Space direction="vertical" size={16} style={{ display: "flex" }}>
                    <Form
                      form={ankiForm}
                      layout="vertical"
                      initialValues={{ fieldMappingText: DEFAULT_FIELD_MAPPING_TEXT }}
                    >
                      <Typography.Title level={5} style={{ margin: 0 }}>
                        {editingAnkiId ? `Edit template #${editingAnkiId}` : "Create Anki Template"}
                      </Typography.Title>
                      <Form.Item label="Name" name="name" rules={[{ required: true, message: "Name is required" }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item label="Description" name="description">
                        <Input />
                      </Form.Item>
                      <Form.Item
                        label="Field Mapping (JSON)"
                        name="fieldMappingText"
                        rules={[{ required: true, message: "Field mapping is required" }]}
                      >
                        <TextArea
                          autoSize={{ minRows: 6, maxRows: 12 }}
                          placeholder='{"front":["expression"],"back":["reading","meaning"]}'
                        />
                      </Form.Item>
                      <Form.Item
                        label="Front Template"
                        name="frontTemplate"
                        rules={[{ required: true, message: "Front template is required" }]}
                      >
                        <TextArea autoSize={{ minRows: 4, maxRows: 10 }} />
                      </Form.Item>
                      <Form.Item
                        label="Back Template"
                        name="backTemplate"
                        rules={[{ required: true, message: "Back template is required" }]}
                      >
                        <TextArea autoSize={{ minRows: 6, maxRows: 12 }} />
                      </Form.Item>
                      <Form.Item label="CSS Template" name="cssTemplate">
                        <TextArea autoSize={{ minRows: 4, maxRows: 10 }} />
                      </Form.Item>
                      <Space wrap>
                        <Button type="primary" onClick={handleSubmitAnki} loading={saveAnkiMutation.isPending}>
                          {editingAnkiId ? "Save Changes" : "Create Template"}
                        </Button>
                        <Button onClick={handlePreviewAnki} loading={ankiPreviewMutation.isPending}>
                          Preview Draft
                        </Button>
                        <Button onClick={resetAnkiForm}>Reset</Button>
                      </Space>
                    </Form>

                    <List
                      bordered
                      dataSource={ankiQuery.data ?? []}
                      locale={{ emptyText: "No Anki templates" }}
                      renderItem={(item) => (
                        <List.Item
                          actions={[
                            <Button
                              key="edit"
                              type="link"
                              disabled={item.scope !== "USER"}
                              onClick={() => handleEditAnki(item)}
                            >
                              Edit
                            </Button>
                          ]}
                        >
                          <Descriptions
                            title={
                              <Space wrap>
                                <span>{item.name}</span>
                                {renderScopeTag(item.scope)}
                                {editingAnkiId === item.id ? <Tag color="blue">Editing</Tag> : null}
                              </Space>
                            }
                            column={1}
                            size="small"
                          >
                            <Descriptions.Item label="Description">{item.description || "-"}</Descriptions.Item>
                            <Descriptions.Item label="Field Mapping">
                              <Typography.Paragraph code>{formatFieldMapping(item.fieldMapping)}</Typography.Paragraph>
                            </Descriptions.Item>
                            <Descriptions.Item label="Front">
                              <Typography.Paragraph code>{item.frontTemplate}</Typography.Paragraph>
                            </Descriptions.Item>
                            <Descriptions.Item label="Back">
                              <Typography.Paragraph code>{item.backTemplate}</Typography.Paragraph>
                            </Descriptions.Item>
                          </Descriptions>
                        </List.Item>
                      )}
                    />
                  </Space>
                )
              },
              {
                key: "markdown",
                label: "Markdown Templates",
                children: (
                  <Space direction="vertical" size={16} style={{ display: "flex" }}>
                    <Form form={markdownForm} layout="vertical">
                      <Typography.Title level={5} style={{ margin: 0 }}>
                        {editingMarkdownId ? `Edit template #${editingMarkdownId}` : "Create Markdown Template"}
                      </Typography.Title>
                      <Form.Item label="Name" name="name" rules={[{ required: true, message: "Name is required" }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item label="Description" name="description">
                        <Input />
                      </Form.Item>
                      <Form.Item
                        label="Template Content"
                        name="templateContent"
                        rules={[{ required: true, message: "Template content is required" }]}
                      >
                        <TextArea autoSize={{ minRows: 10, maxRows: 18 }} />
                      </Form.Item>
                      <Space wrap>
                        <Button
                          type="primary"
                          onClick={handleSubmitMarkdown}
                          loading={saveMarkdownMutation.isPending}
                        >
                          {editingMarkdownId ? "Save Changes" : "Create Template"}
                        </Button>
                        <Button onClick={handlePreviewMarkdown} loading={markdownPreviewMutation.isPending}>
                          Preview Draft
                        </Button>
                        <Button onClick={resetMarkdownForm}>Reset</Button>
                      </Space>
                    </Form>

                    <List
                      bordered
                      dataSource={markdownQuery.data ?? []}
                      locale={{ emptyText: "No Markdown templates" }}
                      renderItem={(item) => (
                        <List.Item
                          actions={[
                            <Button
                              key="edit"
                              type="link"
                              disabled={item.scope !== "USER"}
                              onClick={() => handleEditMarkdown(item)}
                            >
                              Edit
                            </Button>
                          ]}
                        >
                          <Descriptions
                            title={
                              <Space wrap>
                                <span>{item.name}</span>
                                {renderScopeTag(item.scope)}
                                {editingMarkdownId === item.id ? <Tag color="blue">Editing</Tag> : null}
                              </Space>
                            }
                            column={1}
                            size="small"
                          >
                            <Descriptions.Item label="Description">{item.description || "-"}</Descriptions.Item>
                            <Descriptions.Item label="Template Content">
                              <Typography.Paragraph code>{item.templateContent}</Typography.Paragraph>
                            </Descriptions.Item>
                          </Descriptions>
                        </List.Item>
                      )}
                    />
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
