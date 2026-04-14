import { BookOutlined, CalendarOutlined, ExportOutlined, ProfileOutlined, TableOutlined } from "@ant-design/icons";
import { Layout, Menu, Typography } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";

const { Header, Content, Sider } = Layout;

const menuItems = [
  { key: "/word-sets", icon: <BookOutlined />, label: "词库" },
  { key: "/study-plans", icon: <ProfileOutlined />, label: "学习计划" },
  { key: "/cards", icon: <CalendarOutlined />, label: "今日卡片" },
  { key: "/templates", icon: <TableOutlined />, label: "模板" },
  { key: "/export-jobs", icon: <ExportOutlined />, label: "导出" }
];

export function AppShellLayout() {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider breakpoint="lg" collapsedWidth="0" theme="light">
        <div style={{ padding: 20 }}>
          <Typography.Title level={4} style={{ margin: 0 }}>
            JP Phase One
          </Typography.Title>
          <Typography.Text type="secondary">第一阶段框架骨架</Typography.Text>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
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
            paddingInline: 24
          }}
        >
          <Typography.Text strong>Japanese Vocabulary System</Typography.Text>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
