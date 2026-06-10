import Taro from "@tarojs/taro";

export const routes = {
  login: "/pages/login/index",
  dashboard: "/pages/dashboard/index",
  cards: "/pages/cards/index",
  notesReview: "/pages/notes-review/index",
  weakItems: "/pages/weak-items/index",
  account: "/pages/account/index"
};

export function relaunchLogin() {
  return Taro.reLaunch({ url: routes.login });
}

export function relaunchDashboard() {
  return Taro.reLaunch({ url: routes.dashboard });
}

export function openDashboard() {
  return Taro.navigateTo({ url: routes.dashboard });
}

export function openCards(date: string, planId?: number) {
  const query = planId ? `?date=${date}&planId=${planId}` : `?date=${date}`;
  return Taro.navigateTo({ url: `${routes.cards}${query}` });
}

export function openNotesReview(date: string) {
  return Taro.navigateTo({ url: `${routes.notesReview}?date=${date}` });
}

export function openWeakItems() {
  return Taro.navigateTo({ url: routes.weakItems });
}

export function openAccount() {
  return Taro.navigateTo({ url: routes.account });
}
