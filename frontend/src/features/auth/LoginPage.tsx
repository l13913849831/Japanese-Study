import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, Card, Form, Input, Space, Tag, Typography } from "antd";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { getMe, login, type LoginPayload } from "@/features/auth/api";
import { ApiClientError } from "@/shared/api/errors";
import { StatusState } from "@/shared/components/StatusState";

interface LocationState {
  from?: string;
}

export function LoginPage() {
  const [form] = Form.useForm<LoginPayload>();
  const { message } = App.useApp();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const locationState = location.state as LocationState | undefined;
  const redirectTarget = locationState?.from || "/dashboard";

  const currentUserQuery = useQuery({
    queryKey: ["me"],
    queryFn: getMe,
    retry: false
  });

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: async (currentUser) => {
      queryClient.setQueryData(["me"], currentUser);
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      navigate(redirectTarget, { replace: true });
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  if (currentUserQuery.isLoading) {
    return <StatusState mode="loading" />;
  }

  if (currentUserQuery.isSuccess) {
    return <Navigate to="/dashboard" replace />;
  }

  if (currentUserQuery.isError && (currentUserQuery.error as ApiClientError).status !== 401) {
    return <StatusState mode="error" description={(currentUserQuery.error as Error).message} />;
  }

  return (
    <div className="auth-page-shell">
      <div className="auth-page-copy">
        <Tag color="gold">P0-a 最小用户归属底座</Tag>
        <Typography.Title level={1} className="auth-page-title">
          登录后再进入学习工作台
        </Typography.Title>
        <Typography.Paragraph type="secondary" className="auth-page-description">
          当前版本先使用本地账号 + Session Cookie，后端数据归属已经开始按内部用户模型收口，后续接 Keycloak 或微信小程序时不需要重做业务主键。
        </Typography.Paragraph>
        <Space wrap>
          <Tag color="blue">本地登录</Tag>
          <Tag color="cyan">HttpOnly Session</Tag>
          <Tag color="green">可扩到多身份</Tag>
        </Space>
      </div>

      <Card className="auth-card" bordered={false}>
        <Space direction="vertical" size={16} style={{ display: "flex" }}>
          <div>
            <Typography.Title level={3} style={{ marginBottom: 4 }}>
              账号登录
            </Typography.Title>
            <Typography.Text type="secondary">默认开发账号可由后端环境变量覆盖。</Typography.Text>
          </div>

          <Form form={form} layout="vertical" onFinish={(values) => loginMutation.mutate(values)}>
            <Form.Item label="用户名" name="username" rules={[{ required: true, message: "请输入用户名" }]}>
              <Input autoComplete="username" placeholder="demo" />
            </Form.Item>
            <Form.Item label="密码" name="password" rules={[{ required: true, message: "请输入密码" }]}>
              <Input.Password autoComplete="current-password" placeholder="demo123456" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={loginMutation.isPending}>
              进入工作台
            </Button>
          </Form>
        </Space>
      </Card>
    </div>
  );
}
