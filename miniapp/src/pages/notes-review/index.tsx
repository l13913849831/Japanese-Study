import { useRef, useState } from "react";
import Taro from "@tarojs/taro";
import { Button, Text, View } from "@tarojs/components";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppPage } from "@/shared/components/AppPage";
import { StateView } from "@/shared/components/StateView";
import { formatLocalDate } from "@/shared/date";
import { getErrorMessage } from "@/shared/errors";
import { useAuthGuard } from "@/shared/hooks/use-auth-guard";
import { useRouteParams } from "@/shared/hooks/use-route-params";
import { relaunchDashboard } from "@/shared/routes";
import { getTodayNoteReviews, submitNoteReview, type NoteReviewRating } from "@/features/notes/api";

definePageConfig({
  navigationBarTitleText: "知识卡复习"
});

const ratingLabels: Array<{ rating: NoteReviewRating; label: string }> = [
  { rating: "AGAIN", label: "再来" },
  { rating: "HARD", label: "困难" },
  { rating: "GOOD", label: "记得" },
  { rating: "EASY", label: "轻松" }
];

export default function NotesReviewPage() {
  const authenticated = useAuthGuard();
  const routeParams = useRouteParams();
  const date = typeof routeParams.date === "string" ? routeParams.date : formatLocalDate();
  const [currentIndex, setCurrentIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const startedAtRef = useRef(Date.now());
  const queryClient = useQueryClient();

  const notesQuery = useQuery({
    queryKey: ["todayNoteReviews", date],
    queryFn: () => getTodayNoteReviews(date),
    enabled: authenticated
  });

  const notes = notesQuery.data ?? [];
  const currentNote = notes[currentIndex];

  const reviewMutation = useMutation({
    mutationFn: (rating: NoteReviewRating) => {
      if (!currentNote) {
        throw new Error("当前没有可提交的知识卡");
      }

      return submitNoteReview(currentNote.id, {
        rating,
        responseTimeMs: Date.now() - startedAtRef.current,
        sessionAgainCount: 0
      });
    },
    onSuccess: () => {
      setRevealed(false);
      startedAtRef.current = Date.now();
      setCurrentIndex((value) => value + 1);
      void queryClient.invalidateQueries({ queryKey: ["todayNoteReviews", date] });
      void queryClient.invalidateQueries({ queryKey: ["noteDashboard", date] });
    },
    onError: (error) => {
      void Taro.showToast({ title: getErrorMessage(error), icon: "none" });
    }
  });

  if (!authenticated) {
    return null;
  }

  return (
    <AppPage title="知识卡复习" subtitle={`${date} 的知识卡复习。先回忆标题，再揭示内容并评分。`}>
      {notesQuery.isLoading ? <StateView title="加载中" body="正在读取知识卡队列。" /> : null}
      {notesQuery.error ? <StateView title="加载失败" body={getErrorMessage(notesQuery.error)} /> : null}

      {notes.length === 0 && !notesQuery.isLoading ? (
        <StateView title="今日知识卡已完成" body="当前没有到期知识卡。" actionText="回到工作台" onAction={relaunchDashboard} />
      ) : null}

      {currentNote ? (
        <View className="app-card app-card--accent">
          <Text className="app-card__title">{currentNote.title}</Text>
          <Text className="app-card__body">
            {currentNote.tags.length > 0 ? `标签：${currentNote.tags.join(" / ")}` : "暂无标签"}
          </Text>
          {revealed ? (
            <Text className="app-card__body">{currentNote.content}</Text>
          ) : (
            <View className="action-row">
              <Button className="primary-button" onClick={() => setRevealed(true)}>
                显示内容
              </Button>
            </View>
          )}
          <Text className="app-card__body">
            {currentIndex + 1} / {notes.length}
          </Text>
          {revealed ? (
            <View className="action-row">
              {ratingLabels.map((item) => (
                <Button
                  key={item.rating}
                  className={item.rating === "AGAIN" ? "danger-button" : "secondary-button"}
                  loading={reviewMutation.isPending}
                  disabled={reviewMutation.isPending}
                  onClick={() => reviewMutation.mutate(item.rating)}
                >
                  {item.label}
                </Button>
              ))}
            </View>
          ) : null}
        </View>
      ) : null}
    </AppPage>
  );
}
