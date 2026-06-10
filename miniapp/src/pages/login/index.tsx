import Taro from "@tarojs/taro";
import { Button, Text, View } from "@tarojs/components";
import { useMutation } from "@tanstack/react-query";
import { AppPage } from "@/shared/components/AppPage";
import { StateView } from "@/shared/components/StateView";
import { getErrorMessage } from "@/shared/errors";
import { relaunchDashboard } from "@/shared/routes";
import { saveMobileToken } from "@/shared/auth/token-store";
import { loginWithWechatCode } from "@/features/auth/api";

definePageConfig({
  navigationBarTitleText: "微信登录"
});

export default function LoginPage() {
  const loginMutation = useMutation({
    mutationFn: async () => {
      const loginResult = await Taro.login();

      if (!loginResult.code) {
        throw new Error("微信登录未返回 code");
      }

      return loginWithWechatCode(loginResult.code);
    },
    onSuccess: (session) => {
      saveMobileToken(session.token, session.expiresAt);
      void Taro.showToast({ title: "登录成功", icon: "success" });
      void relaunchDashboard();
    },
    onError: (error) => {
      void Taro.showToast({ title: getErrorMessage(error), icon: "none" });
    }
  });

  return (
    <AppPage title="微信登录" subtitle="小程序端使用微信身份换取学习账号，不复用 Web Cookie 会话。">
      <View className="app-card app-card--accent">
        <Text className="app-card__title">学习端账号边界</Text>
        <Text className="app-card__body">
          微信 openid 只作为外部身份。学习数据仍归属于内部 user_account，后端返回 mobile token 后才访问学习 API。
        </Text>
        <View className="action-row">
          <Button
            className="primary-button"
            loading={loginMutation.isPending}
            disabled={loginMutation.isPending}
            onClick={() => loginMutation.mutate()}
          >
            微信登录
          </Button>
        </View>
      </View>

      {loginMutation.isError ? (
        <StateView
          title="登录失败"
          body={`${getErrorMessage(loginMutation.error)}。如果后端还没实现 /api/mobile/auth/wechat-login，这里会正常失败。`}
        />
      ) : null}
    </AppPage>
  );
}
