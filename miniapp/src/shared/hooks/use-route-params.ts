import Taro from "@tarojs/taro";

export function useRouteParams() {
  return Taro.getCurrentInstance().router?.params ?? {};
}
