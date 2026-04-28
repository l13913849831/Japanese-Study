import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, App, Button, Card, Space, Statistic, Table, Tabs, Tag, Typography } from "antd";
import dayjs from "dayjs";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import {
  dismissWeakNote,
  dismissWeakWord,
  getWeakItemSummary,
  listWeakNotes,
  listWeakWords,
  type WeakNoteItem,
  type WeakWordItem
} from "@/features/weak-items/api";

type ActiveTab = "words" | "notes";

export function WeakItemsPage() {
  const [activeTab, setActiveTab] = useState<ActiveTab>("words");
  const navigate = useNavigate();
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  const summaryQuery = useQuery({
    queryKey: ["weakItemSummary"],
    queryFn: getWeakItemSummary
  });

  const weakWordsQuery = useQuery({
    queryKey: ["weakWords"],
    queryFn: () => listWeakWords()
  });

  const weakNotesQuery = useQuery({
    queryKey: ["weakNotes"],
    queryFn: () => listWeakNotes()
  });

  const dismissWordMutation = useMutation({
    mutationFn: dismissWeakWord,
    onSuccess: async () => {
      message.success("已从易错词移出。");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["weakItemSummary"] }),
        queryClient.invalidateQueries({ queryKey: ["weakWords"] })
      ]);
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const dismissNoteMutation = useMutation({
    mutationFn: dismissWeakNote,
    onSuccess: async () => {
      message.success("已从易错知识点移出。");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["weakItemSummary"] }),
        queryClient.invalidateQueries({ queryKey: ["weakNotes"] })
      ]);
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const words = weakWordsQuery.data?.items ?? [];
  const notes = weakNotesQuery.data?.items ?? [];

  return (
    <div className="page-stack">
      <PageHeader
        title="Weak Items"
        description="集中查看需要额外留意的易错词和易错知识点，支持手动移出列表。"
        extra={<Tag color="purple">weak</Tag>}
      />

      <PageSection title="Overview">
        {summaryQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : summaryQuery.isError ? (
          <StatusState mode="error" description={(summaryQuery.error as Error).message} />
        ) : (
          <div className="dashboard-overview-grid">
            <Statistic title="易错词" value={summaryQuery.data?.weakWordCount ?? 0} />
            <Statistic title="易错知识点" value={summaryQuery.data?.weakNoteCount ?? 0} />
          </div>
        )}
      </PageSection>

      {(summaryQuery.data?.weakWordCount ?? 0) + (summaryQuery.data?.weakNoteCount ?? 0) === 0 && !summaryQuery.isLoading ? (
        <PageSection title="当前状态">
          <Alert
            type="success"
            showIcon
            message="现在还没有薄弱项。"
            description="这通常说明你还没开始复习，或者当前会话里还没有内容进入“多次答错”的补强层。"
          />
        </PageSection>
      ) : null}

      <PageSection title="闭环说明">
        <div className="dashboard-plan-grid">
          <Card size="small" title="为什么会进来">
            <Space direction="vertical" size={8}>
              <Typography.Text>单词或知识点在当天会话里多次答错后，会被标到这里。</Typography.Text>
              <Typography.Text type="secondary">这不是新任务类型，而是对今天反复卡住内容的补强入口。</Typography.Text>
            </Space>
          </Card>
          <Card size="small" title="现在该做什么">
            <Space direction="vertical" size={8}>
              <Typography.Text>先把今日主队列做完，再回来集中补弱项。</Typography.Text>
              <Space wrap>
                <Button type="primary" onClick={() => navigate("/dashboard")}>
                  回工作台
                </Button>
                <Button onClick={() => navigate("/notes/review")}>知识点复习</Button>
              </Space>
            </Space>
          </Card>
          <Card size="small" title="做完后怎么收尾">
            <Space direction="vertical" size={8}>
              <Typography.Text>处理完薄弱项后，可以把今天结果导出成复盘材料，或者手动移出已恢复项。</Typography.Text>
              <Space wrap>
                <Button onClick={() => navigate("/export-jobs?source=closure")}>导出复盘材料</Button>
              </Space>
            </Space>
          </Card>
        </div>
      </PageSection>

      <PageSection title="Items">
        <Tabs
          activeKey={activeTab}
          onChange={(key) => setActiveTab(key as ActiveTab)}
          items={[
            {
              key: "words",
              label: "易错词",
              children: renderWords(words, weakWordsQuery.isLoading, weakWordsQuery.isError, weakWordsQuery.error as Error | null, dismissWordMutation.isPending, (cardId) => dismissWordMutation.mutate(cardId))
            },
            {
              key: "notes",
              label: "易错知识点",
              children: renderNotes(notes, weakNotesQuery.isLoading, weakNotesQuery.isError, weakNotesQuery.error as Error | null, dismissNoteMutation.isPending, (noteId) => dismissNoteMutation.mutate(noteId))
            }
          ]}
        />
      </PageSection>
    </div>
  );
}

function renderWords(
  items: WeakWordItem[],
  isLoading: boolean,
  isError: boolean,
  error: Error | null,
  dismissing: boolean,
  onDismiss: (cardId: number) => void
) {
  if (isLoading) {
    return <StatusState mode="loading" />;
  }
  if (isError) {
    return <StatusState mode="error" description={error?.message} />;
  }
  if (!items.length) {
    return (
      <Alert
        type="success"
        showIcon
        message="当前没有易错词。"
        description="当单词在会话里多次 Again 后，会进入这里。"
      />
    );
  }

  return (
    <Table<WeakWordItem>
      rowKey="cardId"
      pagination={false}
      dataSource={items}
      columns={[
        {
          title: "单词",
          render: (_, item) => (
            <Space direction="vertical" size={2}>
              <Typography.Text strong>{item.expression}</Typography.Text>
              <Typography.Text type="secondary">{item.reading || "无 reading"}</Typography.Text>
            </Space>
          )
        },
        {
          title: "释义",
          dataIndex: "meaning"
        },
        {
          title: "计划",
          dataIndex: "planName"
        },
        {
          title: "最近评分",
          dataIndex: "lastReviewRating",
          render: (value?: string) => value ?? "-"
        },
        {
          title: "标记时间",
          dataIndex: "weakMarkedAt",
          render: (value?: string) => (value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-")
        },
        {
          title: "操作",
          render: (_, item) => (
            <Button size="small" loading={dismissing} onClick={() => onDismiss(item.cardId)}>
              移出
            </Button>
          )
        }
      ]}
    />
  );
}

function renderNotes(
  items: WeakNoteItem[],
  isLoading: boolean,
  isError: boolean,
  error: Error | null,
  dismissing: boolean,
  onDismiss: (noteId: number) => void
) {
  if (isLoading) {
    return <StatusState mode="loading" />;
  }
  if (isError) {
    return <StatusState mode="error" description={error?.message} />;
  }
  if (!items.length) {
    return (
      <Alert
        type="success"
        showIcon
        message="当前没有易错知识点。"
        description="当知识点在会话里多次 Again 后，会进入这里。"
      />
    );
  }

  return (
    <Table<WeakNoteItem>
      rowKey="noteId"
      pagination={false}
      dataSource={items}
      columns={[
        {
          title: "标题",
          dataIndex: "title",
          render: (value: string) => <Typography.Text strong>{value}</Typography.Text>
        },
        {
          title: "标签",
          dataIndex: "tags",
          render: (tags: string[]) => (
            <Space wrap>
              {tags.length ? tags.map((tag) => <Tag key={tag}>{tag}</Tag>) : <Typography.Text type="secondary">无标签</Typography.Text>}
            </Space>
          )
        },
        {
          title: "掌握状态",
          dataIndex: "masteryStatus"
        },
        {
          title: "最近评分",
          dataIndex: "lastReviewRating",
          render: (value?: string) => value ?? "-"
        },
        {
          title: "标记时间",
          dataIndex: "weakMarkedAt",
          render: (value?: string) => (value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-")
        },
        {
          title: "操作",
          render: (_, item) => (
            <Button size="small" loading={dismissing} onClick={() => onDismiss(item.noteId)}>
              移出
            </Button>
          )
        }
      ]}
    />
  );
}
