import { View, Text } from "@tarojs/components";

interface MetricCardProps {
  label: string;
  value: number | string;
}

export function MetricCard({ label, value }: MetricCardProps) {
  return (
    <View className="metric-card">
      <Text className="metric-card__value">{value}</Text>
      <Text className="metric-card__label">{label}</Text>
    </View>
  );
}
