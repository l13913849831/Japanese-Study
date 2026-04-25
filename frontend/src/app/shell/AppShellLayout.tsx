import { AlertOutlined, BookOutlined, CalendarOutlined, DashboardOutlined, ExportOutlined, FileTextOutlined, ProfileOutlined, ReadOutlined, TableOutlined } from "@ant-design/icons";
import { Layout, Menu, Typography } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";

const { Header, Content, Sider } = Layout;

const menuItems = [
  { key: "/dashboard", icon: <DashboardOutlined />, label: "Workbench" },
  { key: "/word-sets", icon: <BookOutlined />, label: "Word Sets" },
  { key: "/study-plans", icon: <ProfileOutlined />, label: "Study Plans" },
  { key: "/cards", icon: <CalendarOutlined />, label: "Today Cards" },
  { key: "/notes/dashboard", icon: <DashboardOutlined />, label: "Note Dashboard" },
  { key: "/notes", icon: <FileTextOutlined />, label: "Notes" },
  { key: "/notes/review", icon: <ReadOutlined />, label: "Note Review" },
  { key: "/weak-items", icon: <AlertOutlined />, label: "Weak Items" },
  { key: "/templates", icon: <TableOutlined />, label: "Templates" },
  { key: "/export-jobs", icon: <ExportOutlined />, label: "Exports" }
];

export function AppShellLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const selectedKey =
    menuItems
      .filter((item) => location.pathname.startsWith(item.key))
      .sort((left, right) => right.key.length - left.key.length)[0]?.key ?? location.pathname;

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
