import Taro from "@tarojs/taro";
import { Button, Text, View } from "@tarojs/components";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppPage } from "@/shared/components/AppPage";
import { MetricCard } from "@/shared/components/MetricCard";
import { StateView } from "@/shared/components/StateView";
import { getErrorMessage } from "@/shared/errors";
import { useAuthGuard } from "@/shared/hooks/use-auth-guard";
import { submitCardReview, type ReviewRating } from "@/features/cards/api";
import { submitNoteReview } from "@/features/notes/api";
import {
  dismissWeakNote,
  dismissWeakWord,
  getWeakItemSummary,
  listWeakNotes,
  listWeakWords,
  type WeakNoteItem,
  type WeakWordItem
} from "@/features/weak-items/api";

definePageConfig({
  navigationBarTitleText: "弱项"
});

type FocusedReviewSession =
  | {
      mode: "words";
      items: WeakWordItem[];
      currentIndex: number;
      completedCount: number;
      startedAtMs: number;
    }
  | {
      mode: "notes";
      items: WeakNoteItem[];
      currentIndex: number;
      completedCount: number;
      startedAtMs: number;
    };

const ratingLabels: Array<{ rating: ReviewRating; label: string; danger?: boolean }> = [
  { rating: "AGAIN", label: "再来", danger: true },
  { rating: "HARD", label: "困难" },
  { rating: "GOOD", label: "记得" },
  { rating: "EASY", label: "轻松" }
];

export default function WeakItemsPage() {
  const authenticated = useAuthGuard();
  const queryClient = useQueryClient();
  const [focusedSession, setFocusedSession] = useState<FocusedReviewSession | null>(null);
  const [noteAnswerRevealed, setNoteAnswerRevealed] = useState(false);

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

  const focusedWordReviewMutation = useMutation({
    mutationFn: ({ item, rating, startedAtMs }: { item: WeakWordItem; rating: ReviewRating; startedAtMs: number }) =>
      submitCardReview(item.cardId, {
        rating,
        responseTimeMs: Date.now() - startedAtMs,
        sessionAgainCount: rating === "AGAIN" ? 1 : 0,
        note: "Focused weak-item review"
      }),
    onSuccess: async () => {
      advanceFocusedSession();
      void Taro.showToast({ title: "已记录", icon: "success" });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["weakItemSummary"] }),
        queryClient.invalidateQueries({ queryKey: ["weakWords"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] })
      ]);
    },
    onError: (error) => {
      void Taro.showToast({ title: getErrorMessage(error), icon: "none" });
    }
  });

  const focusedNoteReviewMutation = useMutation({
    mutationFn: ({ item, rating, startedAtMs }: { item: WeakNoteItem; rating: ReviewRating; startedAtMs: number }) =>
      submitNoteReview(item.noteId, {
        rating,
        responseTimeMs: Date.now() - startedAtMs,
        sessionAgainCount: rating === "AGAIN" ? 1 : 0,
        note: "Focused weak-item review"
      }),
    onSuccess: async () => {
      advanceFocusedSession();
      void Taro.showToast({ title: "已记录", icon: "success" });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["weakItemSummary"] }),
        queryClient.invalidateQueries({ queryKey: ["weakNotes"] }),
        queryClient.invalidateQueries({ queryKey: ["noteDashboard"] })
      ]);
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
  const focusedSessionDone = focusedSession ? focusedSession.currentIndex >= focusedSession.items.length : false;
  const currentFocusedWord =
    focusedSession && focusedSession.mode === "words" && !focusedSessionDone
      ? focusedSession.items[focusedSession.currentIndex]
      : undefined;
  const currentFocusedNote =
    focusedSession && focusedSession.mode === "notes" && !focusedSessionDone
      ? focusedSession.items[focusedSession.currentIndex]
      : undefined;

  function startFocusedSession(mode: "words" | "notes") {
    const items = mode === "words" ? weakWords : weakNotes;
    if (!items.length) {
      void Taro.showToast({ title: mode === "words" ? "暂无单词弱项" : "暂无知识卡弱项", icon: "none" });
      return;
    }

    setFocusedSession({
      mode,
      items,
      currentIndex: 0,
      completedCount: 0,
      startedAtMs: Date.now()
    } as FocusedReviewSession);
    setNoteAnswerRevealed(false);
  }

  function advanceFocusedSession() {
    setFocusedSession((previous) =>
      previous
        ? {
            ...previous,
            currentIndex: previous.currentIndex + 1,
            completedCount: previous.completedCount + 1,
            startedAtMs: Date.now()
          }
        : previous
    );
    setNoteAnswerRevealed(false);
  }

  function stopFocusedSession() {
    setFocusedSession(null);
    setNoteAnswerRevealed(false);
  }

  function submitFocusedReview(rating: ReviewRating) {
    if (currentFocusedWord && focusedSession?.mode === "words") {
      focusedWordReviewMutation.mutate({
        item: currentFocusedWord,
        rating,
        startedAtMs: focusedSession.startedAtMs
      });
      return;
    }

    if (currentFocusedNote && focusedSession?.mode === "notes") {
      if (!noteAnswerRevealed) {
        void Taro.showToast({ title: "先显示内容", icon: "none" });
        return;
      }
      focusedNoteReviewMutation.mutate({
        item: currentFocusedNote,
        rating,
        startedAtMs: focusedSession.startedAtMs
      });
    }
  }

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

      <View className="app-card app-card--accent">
        <Text className="app-card__title">专题补强</Text>
        <Text className="app-card__body">从当前弱项列表直接开始一轮补强。记得或轻松会从弱项毕业。</Text>
        <View className="action-row">
          <Button className="primary-button" disabled={!weakWords.length} onClick={() => startFocusedSession("words")}>
            练单词弱项
          </Button>
          <Button className="secondary-button" disabled={!weakNotes.length} onClick={() => startFocusedSession("notes")}>
            练知识卡弱项
          </Button>
        </View>
      </View>

      {focusedSession ? (
        <View className="app-card">
          {focusedSessionDone ? (
            <>
              <Text className="app-card__title">本轮补强已完成</Text>
              <Text className="app-card__body">本轮共处理 {focusedSession.completedCount} 个弱项。</Text>
              <View className="action-row">
                <Button className="secondary-button" onClick={() => startFocusedSession("words")}>
                  再练单词
                </Button>
                <Button className="secondary-button" onClick={() => startFocusedSession("notes")}>
                  再练知识卡
                </Button>
                <Button className="primary-button" onClick={stopFocusedSession}>
                  结束
                </Button>
              </View>
            </>
          ) : currentFocusedWord ? (
            <>
              <Text className="app-card__title">
                易错词 {focusedSession.currentIndex + 1} / {focusedSession.items.length}
              </Text>
              <Text className="app-card__body">{currentFocusedWord.expression}</Text>
              <Text className="app-card__body">{currentFocusedWord.reading ?? "暂无读音"}</Text>
              <Text className="app-card__body">{currentFocusedWord.meaning ?? "暂无释义"}</Text>
              {currentFocusedWord.exampleJp ? <Text className="app-card__body">{currentFocusedWord.exampleJp}</Text> : null}
              {currentFocusedWord.exampleZh ? <Text className="app-card__body">{currentFocusedWord.exampleZh}</Text> : null}
              <View className="action-row">
                {ratingLabels.map((item) => (
                  <Button
                    key={item.rating}
                    className={item.danger ? "danger-button" : "secondary-button"}
                    loading={focusedWordReviewMutation.isPending}
                    disabled={focusedWordReviewMutation.isPending}
                    onClick={() => submitFocusedReview(item.rating)}
                  >
                    {item.label}
                  </Button>
                ))}
                <Button className="secondary-button" onClick={stopFocusedSession}>
                  退出
                </Button>
              </View>
            </>
          ) : currentFocusedNote ? (
            <>
              <Text className="app-card__title">
                易错知识卡 {focusedSession.currentIndex + 1} / {focusedSession.items.length}
              </Text>
              <Text className="app-card__body">{currentFocusedNote.title}</Text>
              <Text className="app-card__body">
                {currentFocusedNote.tags.length > 0 ? `标签：${currentFocusedNote.tags.join(" / ")}` : "暂无标签"}
              </Text>
              {noteAnswerRevealed ? (
                <Text className="app-card__body">{currentFocusedNote.content}</Text>
              ) : (
                <View className="action-row">
                  <Button className="primary-button" onClick={() => setNoteAnswerRevealed(true)}>
                    显示内容
                  </Button>
                </View>
              )}
              <View className="action-row">
                {ratingLabels.map((item) => (
                  <Button
                    key={item.rating}
                    className={item.danger ? "danger-button" : "secondary-button"}
                    loading={focusedNoteReviewMutation.isPending}
                    disabled={focusedNoteReviewMutation.isPending}
                    onClick={() => submitFocusedReview(item.rating)}
                  >
                    {item.label}
                  </Button>
                ))}
                <Button className="secondary-button" onClick={stopFocusedSession}>
                  退出
                </Button>
              </View>
            </>
          ) : null}
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
