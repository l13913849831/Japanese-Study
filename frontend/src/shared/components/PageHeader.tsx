import type { ReactNode } from "react";
import { Space, Typography } from "antd";

interface PageHeaderProps {
  title: string;
  description: string;
  extra?: ReactNode;
}

export function PageHeader({ title, description, extra }: PageHeaderProps) {
  return (
    <div
      style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "flex-start",
        gap: 16,
        flexWrap: "wrap"
      }}
    >
      <Space direction="vertical" size={4}>
        <Typography.Title level={2} style={{ margin: 0 }}>
          {title}
        </Typography.Title>
        <Typography.Text type="secondary">{description}</Typography.Text>
      </Space>
      {extra}
    </div>
  );
}
