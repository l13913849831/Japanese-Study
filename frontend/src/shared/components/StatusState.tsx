import type { ReactNode } from "react";
import { Empty, Result, Spin } from "antd";

interface StatusStateProps {
  mode: "loading" | "empty" | "error";
  title?: string;
  description?: string;
  extra?: ReactNode;
}

export function StatusState({ mode, title, description, extra }: StatusStateProps) {
  if (mode === "loading") {
    return (
      <div style={{ display: "grid", placeItems: "center", minHeight: 180 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (mode === "error") {
    return <Result status="error" title={title ?? "请求失败"} subTitle={description} extra={extra} />;
  }

  return <Empty description={description ?? title ?? "暂无数据"} />;
}
