import { useQuery } from "@tanstack/react-query";
import { Descriptions, List, Tabs, Tag, Typography } from "antd";
import { listAnkiTemplates, listMarkdownTemplates } from "@/features/templates/api";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";

export function TemplatePage() {
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
