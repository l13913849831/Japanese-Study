import { getJson, postJson } from "@/shared/api/http";

export type PreferredLearningOrder = "WORD_FIRST" | "NOTE_FIRST";

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  preferredLearningOrder: PreferredLearningOrder;
  roles: Array<"USER" | "ADMIN">;
}

export interface WechatLoginPayload {
  code: string;
}

export interface MobileAuthSession {
  token: string;
  expiresAt: string;
  user: CurrentUser;
}

export interface LogoutResult {
  loggedOut: boolean;
}

export function loginWithWechatCode(code: string) {
  return postJson<MobileAuthSession, WechatLoginPayload>("/mobile/auth/wechat-login", { code }, false);
}

export function getMe() {
  return getJson<CurrentUser>("/mobile/me");
}

export function logoutMobile() {
  return postJson<LogoutResult, Record<string, never>>("/mobile/auth/logout", {});
}
