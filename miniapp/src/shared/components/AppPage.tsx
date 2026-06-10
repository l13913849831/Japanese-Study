import { View, Text } from "@tarojs/components";
import type { PropsWithChildren, ReactNode } from "react";

interface AppPageProps extends PropsWithChildren {
  eyebrow?: string;
  title: string;
  subtitle?: string;
  children: ReactNode;
}

export function AppPage({ eyebrow = "Japanese Study", title, subtitle, children }: AppPageProps) {
  return (
    <View className="app-page">
      <View className="app-hero">
        <Text className="app-hero__eyebrow">{eyebrow}</Text>
        <Text className="app-hero__title">{title}</Text>
        {subtitle ? <Text className="app-hero__subtitle">{subtitle}</Text> : null}
      </View>
      <View className="app-stack">{children}</View>
    </View>
  );
}
