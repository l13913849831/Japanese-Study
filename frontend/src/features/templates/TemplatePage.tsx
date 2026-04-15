import { useMutation, useQuery } from "@tanstack/react-query";
import { App, Button, Descriptions, List, Modal, Tabs, Tag, Typography } from "antd";
import { ApiClientError } from "@/shared/api/errors";
import {
  listAnkiTemplates,
  listMarkdownTemplates,
  previewAnkiTemplate,
  previewMarkdownTemplate,
  type AnkiTemplate,
  type MarkdownTemplate,
  type TemplateCardSample
} from "@/features/templates/api";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";

export function TemplatePage() {
  const { message } = App.useApp();
  const ankiQuery = useQuery({
    queryKey: ["ankiTemplates"],
    queryFn: listAnkiTemplates
  });
  const markdownQuery = useQuery({
    queryKey: ["markdownTemplates"],
    queryFn: listMarkdownTemplates
  });

  const loading = ankiQuery.isLoading || markdownQuery.isLoading;
  const error = (ankiQuery.error as Error | undefined)?.message ?? (markdownQuery.error as Error | undefined)?.message;
  const sampleCard: TemplateCardSample = {
    expression: "準備",
    reading: "じゅんび",
    meaning: "准备",
    partOfSpeech: "名词/サ变",
    exampleJp: "明日の会議を準備する。",
    exampleZh: "为明天的会议做准备。",
    tags: ["N4", "工作"],
    dueDate: "2026-04-20",
    planName: "N4 每日 20 词"
  };

  const ankiPreviewMutation = useMutation({
    mutationFn: previewAnkiTemplate,
    onSuccess: (result) => {
      Modal.info({
        width: 720,
        title: "Anki 模板预览",
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
        width: 720,
        title: "Markdown 模板预览",
        content: <Typography.Paragraph code>{result.renderedContent}</Typography.Paragraph>
      });
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const handleAnkiPreview = (item: AnkiTemplate) => {
    ankiPreviewMutation.mutate({
      frontTemplate: item.frontTemplate,
      backTemplate: item.backTemplate,
      cssTemplate: item.cssTemplate,
      sample: sampleCard
    });
  };

  const handleMarkdownPreview = (item: MarkdownTemplate) => {
    markdownPreviewMutation.mutate({
      templateContent: item.templateContent,
      date: "2026-04-20",
      planName: "N4 每日 20 词",
      newCards: [sampleCard],
      reviewCards: [{ ...sampleCard, expression: "整理", reading: "せいり", meaning: "整理" }]
    });
  };

  return (
    <div className="page-stack">
      <PageHeader
        title="模板"
        description="读取 Flyway 初始化的模板种子，并预留模板预览与编辑扩展点。"
        extra={<Tag color="cyan">anki_template / md_template</Tag>}
      />

      <PageSection title="模板列表">
        {loading ? (
          <StatusState mode="loading" />
        ) : error ? (
          <StatusState mode="error" description={error} />
        ) : (
          <Tabs
            items={[
              {
                key: "anki",
                label: "Anki 模板",
                children: (
                  <List
                    dataSource={ankiQuery.data ?? []}
                    renderItem={(item) => (
                      <List.Item>
                        <Descriptions title={item.name} column={1} size="small">
                          <Descriptions.Item label="描述">{item.description || "-"}</Descriptions.Item>
                          <Descriptions.Item label="Field Mapping">
                            <Typography.Text code>{JSON.stringify(item.fieldMapping)}</Typography.Text>
                          </Descriptions.Item>
                          <Descriptions.Item label="Front Template">
                            <Typography.Paragraph code>{item.frontTemplate}</Typography.Paragraph>
                          </Descriptions.Item>
                          <Descriptions.Item label="操作">
                            <Button onClick={() => handleAnkiPreview(item)} loading={ankiPreviewMutation.isPending}>
                              预览
                            </Button>
                          </Descriptions.Item>
                        </Descriptions>
                      </List.Item>
                    )}
                  />
                )
              },
              {
                key: "markdown",
                label: "Markdown 模板",
                children: (
                  <List
                    dataSource={markdownQuery.data ?? []}
                    renderItem={(item) => (
                      <List.Item>
                        <Descriptions title={item.name} column={1} size="small">
                          <Descriptions.Item label="描述">{item.description || "-"}</Descriptions.Item>
                          <Descriptions.Item label="内容">
                            <Typography.Paragraph code>{item.templateContent}</Typography.Paragraph>
                          </Descriptions.Item>
                          <Descriptions.Item label="操作">
                            <Button onClick={() => handleMarkdownPreview(item)} loading={markdownPreviewMutation.isPending}>
                              预览
                            </Button>
                          </Descriptions.Item>
                        </Descriptions>
                      </List.Item>
                    )}
                  />
                )
              }
            ]}
          />
        )}
      </PageSection>
    </div>
  );
}
