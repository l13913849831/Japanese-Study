import { AlertOutlined, BookOutlined, CalendarOutlined, DashboardOutlined, ExportOutlined, FileTextOutlined, ProfileOutlined, ReadOutlined, TableOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Layout, Menu, Segmented, Space, Tag, Typography, App } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { getMe, logout, updateMySettings, type PreferredLearningOrder } from "@/features/auth/api";
import { ApiClientError } from "@/shared/api/errors";

const { Header, Content, Sider } = Layout;

const learningMenuItems = [
  { key: "/dashboard", icon: <DashboardOutlined />, label: "Workbench" },
  { key: "/cards", icon: <CalendarOutlined />, label: "Today Cards" },
  { key: "/notes/review", icon: <ReadOutlined />, label: "Note Review" },
  { key: "/weak-items", icon: <AlertOutlined />, label: "Weak Items" }
];

const managementMenuItems = [
  { key: "/study-plans", icon: <ProfileOutlined />, label: "Study Plans" },
  { key: "/word-sets", icon: <BookOutlined />, label: "Word Sets" },
  { key: "/notes", icon: <FileTextOutlined />, label: "Notes" },
  { key: "/notes/dashboard", icon: <DashboardOutlined />, label: "Note Dashboard" },
  { key: "/templates", icon: <TableOutlined />, label: "Templates" },
  { key: "/export-jobs", icon: <ExportOutlined />, label: "Exports" }
];

const menuItems = [...learningMenuItems, ...managementMenuItems];
const groupedMenuItems = [
  { type: "group" as const, label: "学习", children: learningMenuItems },
  { type: "group" as const, label: "管理", children: managementMenuItems }
];

export function AppShellLayout() {
  const { message } = App.useApp();
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const selectedKey =
    menuItems
      .filter((item) => location.pathname.startsWith(item.key))
      .sort((left, right) => right.key.length - left.key.length)[0]?.key ?? location.pathname;

  const currentUserQuery = useQuery({
    queryKey: ["me"],
    queryFn: getMe,
    retry: false
  });

  const updateSettingsMutation = useMutation({
    mutationFn: updateMySettings,
    onSuccess: (currentUser) => {
      queryClient.setQueryData(["me"], currentUser);
      message.success("默认学习顺序已更新");
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: async () => {
      queryClient.clear();
      navigate("/login", { replace: true });
    },
    onError: (error) => {
      message.error((error as ApiClientError).message);
    }
  });

  const currentUser = currentUserQuery.data;
  const preferredLearningOrder = currentUser?.preferredLearningOrder ?? "WORD_FIRST";

  const handleLearningOrderChange = (value: string | number) => {
    if (!currentUser) {
      return;
    }

    const nextValue = String(value) as PreferredLearningOrder;
    if (nextValue === currentUser.preferredLearningOrder) {
      return;
    }

    updateSettingsMutation.mutate({ preferredLearningOrder: nextValue });
  };

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider breakpoint="lg" collapsedWidth="0" theme="light">
        <div style={{ padding: 20 }}>
          <Typography.Title level={4} style={{ margin: 0 }}>
            JP Phase One
          </Typography.Title>
          <Typography.Text type="secondary">Japanese study workflow</Typography.Text>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={groupedMenuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: "rgba(255,255,255,0.75)",
            backdropFilter: "blur(12px)",
            borderBottom: "1px solid rgba(24,34,47,0.08)",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: 16,
            flexWrap: "wrap",
            paddingInline: 24
          }}
        >
          <Typography.Text strong>Japanese Vocabulary System</Typography.Text>
          <Space wrap size={12}>
            <Segmented
              size="middle"
              value={preferredLearningOrder}
              disabled={!currentUser || updateSettingsMutation.isPending}
              onChange={handleLearningOrderChange}
              options={[
                { label: "单词优先", value: "WORD_FIRST" },
                { label: "知识点优先", value: "NOTE_FIRST" }
              ]}
            />
            {currentUser ? (
              <Space size={8}>
                <Tag color="blue">{currentUser.displayName}</Tag>
                <Typography.Text type="secondary">@{currentUser.username}</Typography.Text>
              </Space>
            ) : null}
            <Button onClick={() => navigate("/account")}>
              账户
            </Button>
            <Button onClick={() => logoutMutation.mutate()} loading={logoutMutation.isPending}>
              退出登录
            </Button>
          </Space>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
