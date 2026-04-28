import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, Card, Form, Input, Segmented, Space, Typography } from "antd";
import {
  changeMyPassword,
  getMe,
  updateMyProfile,
  updateMySettings,
  type ChangePasswordPayload,
  type PreferredLearningOrder,
  type UpdateProfilePayload
} from "@/features/auth/api";
import { ApiClientError } from "@/shared/api/errors";
import { StatusState } from "@/shared/components/StatusState";

interface ProfileFormValues extends UpdateProfilePayload {
  preferredLearningOrder: PreferredLearningOrder;
}

export function AccountPage() {
  const { message } = App.useApp();
  const [profileForm] = Form.useForm<ProfileFormValues>();
  const [passwordForm] = Form.useForm<ChangePasswordPayload>();
  const queryClient = useQueryClient();

  const currentUserQuery = useQuery({
    queryKey: ["me"],
    queryFn: getMe
  });

  const profileMutation = useMutation({
    mutationFn: async (values: ProfileFormValues) => {
      const currentUser = currentUserQuery.data!;
      const nextProfile = values.displayName.trim() !== currentUser.displayName
        ? await updateMyProfile({ displayName: values.displayName.trim() })
        : currentUser;

      if (values.preferredLearningOrder !== nextProfile.preferredLearningOrder) {
        return updateMySettings({ preferredLearningOrder: values.preferredLearningOrder });
      }

      return nextProfile;
    },
    onSuccess: (currentUser) => {
      queryClient.setQueryData(["me"], currentUser);
      message.success("账户信息已更新");
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const passwordMutation = useMutation({
    mutationFn: changeMyPassword,
    onSuccess: () => {
      passwordForm.resetFields();
      message.success("密码已更新");
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  if (currentUserQuery.isLoading) {
    return <StatusState mode="loading" />;
  }

  if (currentUserQuery.isError) {
    return <StatusState mode="error" description={(currentUserQuery.error as Error).message} />;
  }

  const currentUser = currentUserQuery.data!;

  return (
    <div className="page-stack">
      <Card bordered={false}>
        <Space direction="vertical" size={8} style={{ display: "flex" }}>
          <Typography.Title level={3} style={{ margin: 0 }}>
            账户设置
          </Typography.Title>
          <Typography.Text type="secondary">
            当前只支持本地账号。微信登录保留到后续更低优先级任务，不在这轮接入。
          </Typography.Text>
        </Space>
      </Card>

      <div className="account-grid">
        <Card bordered={false}>
          <Space direction="vertical" size={16} style={{ display: "flex" }}>
            <div>
              <Typography.Title level={4} style={{ marginBottom: 4 }}>
                基本资料
              </Typography.Title>
              <Typography.Text type="secondary">@{currentUser.username}</Typography.Text>
            </div>

            <Form
              form={profileForm}
              layout="vertical"
              initialValues={{
                displayName: currentUser.displayName,
                preferredLearningOrder: currentUser.preferredLearningOrder
              }}
              onFinish={(values) => profileMutation.mutate(values)}
            >
              <Form.Item label="显示名" name="displayName" rules={[{ required: true, message: "请输入显示名" }]}>
                <Input maxLength={128} />
              </Form.Item>
              <Form.Item label="默认学习顺序" name="preferredLearningOrder">
                <Segmented
                  block
                  options={[
                    { label: "单词优先", value: "WORD_FIRST" },
                    { label: "知识点优先", value: "NOTE_FIRST" }
                  ]}
                />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={profileMutation.isPending}>
                保存资料
              </Button>
            </Form>
          </Space>
        </Card>

        <Card bordered={false}>
          <Space direction="vertical" size={16} style={{ display: "flex" }}>
            <div>
              <Typography.Title level={4} style={{ marginBottom: 4 }}>
                修改密码
              </Typography.Title>
              <Typography.Text type="secondary">密码长度保持在 8 到 72 位。</Typography.Text>
            </div>

            <Form form={passwordForm} layout="vertical" onFinish={(values) => passwordMutation.mutate(values)}>
              <Form.Item label="当前密码" name="currentPassword" rules={[{ required: true, message: "请输入当前密码" }]}>
                <Input.Password autoComplete="current-password" />
              </Form.Item>
              <Form.Item label="新密码" name="newPassword" rules={[{ required: true, message: "请输入新密码" }, { min: 8, message: "新密码至少 8 位" }]}>
                <Input.Password autoComplete="new-password" />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={passwordMutation.isPending}>
                更新密码
              </Button>
            </Form>
          </Space>
        </Card>
      </div>
    </div>
  );
}
