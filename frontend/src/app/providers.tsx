import type { PropsWithChildren } from "react";
import { ConfigProvider, App as AntdApp } from "antd";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "@/app/query-client";

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: "#8c5d20",
          borderRadius: 12,
          colorBgLayout: "#f6f1e8"
        }
      }}
    >
      <AntdApp>
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  );
}
