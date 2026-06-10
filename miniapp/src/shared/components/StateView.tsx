import { Button, Text, View } from "@tarojs/components";

interface StateViewProps {
  title: string;
  body?: string;
  actionText?: string;
  onAction?: () => void;
}

export function StateView({ title, body, actionText, onAction }: StateViewProps) {
  return (
    <View className="state-card">
      <Text className="state-card__title">{title}</Text>
      {body ? <Text className="state-card__body">{body}</Text> : null}
      {actionText && onAction ? (
        <View className="action-row">
          <Button className="primary-button" onClick={onAction}>
            {actionText}
          </Button>
        </View>
      ) : null}
    </View>
  );
}
