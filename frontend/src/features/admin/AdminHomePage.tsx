import { Alert, Card, Space, Tag, Typography } from "antd";
import { PageSection } from "@/shared/components/PageSection";

export function AdminHomePage() {
  return (
    <Space direction="vertical" size={24} style={{ width: "100%" }}>
      <div>
        <Typography.Title level={2} style={{ marginBottom: 4 }}>
          Admin Console
        </Typography.Title>
        <Typography.Text type="secondary">管理员后台基础入口。当前阶段先验证角色和路由隔离。</Typography.Text>
      </div>

      <Alert
        type="info"
        showIcon
        message="权限基础已启用"
        description="只有拥有 ADMIN 角色的账号能进入该路由。用户治理列表、详情、启用/禁用和审计查询会在下一步接入。"
      />

      <PageSection title="当前边界">
        <Space wrap>
          <Card size="small">
            <Tag color="blue">USER</Tag>
            <Typography.Text>普通学习账号</Typography.Text>
          </Card>
          <Card size="small">
            <Tag color="red">ADMIN</Tag>
            <Typography.Text>管理员账号</Typography.Text>
          </Card>
        </Space>
      </PageSection>
    </Space>
  );
}
