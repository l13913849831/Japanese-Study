import type { PropsWithChildren, ReactNode } from "react";
import { Card } from "antd";

interface PageSectionProps extends PropsWithChildren {
  title: string;
  extra?: ReactNode;
}

export function PageSection({ title, extra, children }: PageSectionProps) {
  return (
    <Card title={title} extra={extra}>
      {children}
    </Card>
  );
}
