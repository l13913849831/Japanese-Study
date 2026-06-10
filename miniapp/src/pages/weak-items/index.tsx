import Taro from "@tarojs/taro";
import { Button, Text, View } from "@tarojs/components";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppPage } from "@/shared/components/AppPage";
import { MetricCard } from "@/shared/components/MetricCard";
import { StateView } from "@/shared/components/StateView";
import { getErrorMessage } from "@/shared/errors";
import { useAuthGuard } from "@/shared/hooks/use-auth-guard";
import {
  dismissWeakNote,
  dismissWeakWord,
  getWeakItemSummary,
  listWeakNotes,
  listWeakWords
} from "@/features/weak-items/api";

definePageConfig({
  navigationBarTitleText: "弱项"
});

export default function WeakItemsPage() {
  const authenticated = useAuthGuard();
  const queryClient = useQueryClient();

  const summaryQuery = useQuery({
    queryKey: ["weakItemSummary"],
    queryFn: getWeakItemSummary,
    enabled: authenticated
  });
  const wordsQuery = useQuery({
    queryKey: ["weakWords", 1, 20],
    queryFn: () => listWeakWords(1, 20),
    enabled: authenticated
  });
  const notesQuery = useQuery({
    queryKey: ["weakNotes", 1, 20],
    queryFn: () => listWeakNotes(1, 20),
    enabled: authenticated
  });

  const dismissWordMutation = useMutation({
    mutationFn: dismissWeakWord,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["weakItemSummary"] });
      void queryClient.invalidateQueries({ queryKey: ["weakWords"] });
    },
    onError: (error) => {
      void Taro.showToast({ title: getErrorMessage(error), icon: "none" });
    }
  });

  const dismissNoteMutation = useMutation({
    mutationFn: dismissWeakNote,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["weakItemSummary"] });
      void queryClient.invalidateQueries({ queryKey: ["weakNotes"] });
    },
    onError: (error) => {
      void Taro.showToast({ title: getErrorMessage(error), icon: "none" });
    }
  });

  if (!authenticated) {
    return null;
  }

  const summary = summaryQuery.data;
  const weakWords = wordsQuery.data?.items ?? [];
  const weakNotes = notesQuery.data?.items ?? [];
  const error = summaryQuery.error ?? wordsQuery.error ?? notesQuery.error;

  return (
    <AppPage title="弱项" subtitle="先把容易错的单词和知识卡集中处理掉。">
      {summaryQuery.isLoading || wordsQuery.isLoading || notesQuery.isLoading ? (
        <StateView title="加载中" body="正在读取弱项数据。" />
      ) : null}
      {error ? <StateView title="加载失败" body={getErrorMessage(error)} /> : null}

      {summary ? (
        <View className="metric-grid">
          <MetricCard label="单词弱项" value={summary.weakWordCount} />
          <MetricCard label="知识卡弱项" value={summary.weakNoteCount} />
        </View>
      ) : null}

      <View className="app-card">
        <Text className="app-card__title">单词弱项</Text>
        {weakWords.length === 0 ? <Text className="app-card__body">暂无单词弱项。</Text> : null}
        {weakWords.map((item) => (
          <View className="list-item" key={item.cardId}>
            <Text className="list-item__title">{item.expression}</Text>
            <Text className="list-item__meta">
              {item.meaning ?? "暂无释义"} / {item.planName}
            </Text>
            <View className="action-row">
              <Button
                className="secondary-button"
                loading={dismissWordMutation.isPending}
                disabled={dismissWordMutation.isPending}
                onClick={() => dismissWordMutation.mutate(item.cardId)}
              >
                移出弱项
              </Button>
            </View>
          </View>
        ))}
      </View>

      <View className="app-card">
        <Text className="app-card__title">知识卡弱项</Text>
        {weakNotes.length === 0 ? <Text className="app-card__body">暂无知识卡弱项。</Text> : null}
        {weakNotes.map((item) => (
          <View className="list-item" key={item.noteId}>
            <Text className="list-item__title">{item.title}</Text>
            <Text className="list-item__meta">{item.tags.length > 0 ? item.tags.join(" / ") : item.masteryStatus}</Text>
            <View className="action-row">
              <Button
                className="secondary-button"
                loading={dismissNoteMutation.isPending}
                disabled={dismissNoteMutation.isPending}
                onClick={() => dismissNoteMutation.mutate(item.noteId)}
              >
                移出弱项
              </Button>
            </View>
          </View>
        ))}
      </View>
    </AppPage>
  );
}
