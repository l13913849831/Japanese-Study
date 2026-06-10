import Taro from "@tarojs/taro";
import { Button, Text, View } from "@tarojs/components";
import { useMutation, useQuery } from "@tanstack/react-query";
import { AppPage } from "@/shared/components/AppPage";
import { StateView } from "@/shared/components/StateView";
import { clearMobileToken } from "@/shared/auth/token-store";
import { getErrorMessage } from "@/shared/errors";
import { useAuthGuard } from "@/shared/hooks/use-auth-guard";
import { relaunchLogin } from "@/shared/routes";
import { getMe, logoutMobile } from "@/features/auth/api";

definePageConfig({
  navigationBarTitleText: "账号"
});

export default function AccountPage() {
  const authenticated = useAuthGuard();

  const meQuery = useQuery({
    queryKey: ["me"],
    queryFn: getMe,
    enabled: authenticated
  });

  const logoutMutation = useMutation({
    mutationFn: logoutMobile,
    onSettled: () => {
      clearMobileToken();
      void relaunchLogin();
    },
    onError: (error) => {
      void Taro.showToast({ title: getErrorMessage(error), icon: "none" });
    }
  });

  if (!authenticated) {
    return null;
  }

  const user = meQuery.data;

  return (
    <AppPage title="账号" subtitle="小程序端只展示学习账号信息，不展示管理端能力。">
      {meQuery.isLoading ? <StateView title="加载中" body="正在读取账号信息。" /> : null}
      {meQuery.error ? <StateView title="加载失败" body={getErrorMessage(meQuery.error)} /> : null}

      {user ? (
        <View className="app-card app-card--accent">
          <Text className="app-card__title">{user.displayName}</Text>
          <Text className="app-card__body">
            用户名：{user.username}
            {"\n"}默认学习顺序：{user.preferredLearningOrder === "WORD_FIRST" ? "单词优先" : "知识卡优先"}
            {"\n"}角色：{user.roles.filter((role) => role === "USER").join(" / ") || "USER"}
          </Text>
          <View className="action-row">
            <Button
              className="danger-button"
              loading={logoutMutation.isPending}
              disabled={logoutMutation.isPending}
              onClick={() => logoutMutation.mutate()}
            >
              退出登录
            </Button>
          </View>
        </View>
      ) : null}
    </AppPage>
  );
}
