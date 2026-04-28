import { getJson, postJson, putJson } from "@/shared/api/http";

export type PreferredLearningOrder = "WORD_FIRST" | "NOTE_FIRST";

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  preferredLearningOrder: PreferredLearningOrder;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface LogoutResult {
  loggedOut: boolean;
}

export interface UpdateSettingsPayload {
  preferredLearningOrder: PreferredLearningOrder;
}

export function getMe() {
  return getJson<CurrentUser>("/me");
}

export function login(payload: LoginPayload) {
  return postJson<CurrentUser, LoginPayload>("/auth/login", payload);
}

export function logout() {
  return postJson<LogoutResult, Record<string, never>>("/auth/logout", {});
}

export function updateMySettings(payload: UpdateSettingsPayload) {
  return putJson<CurrentUser, UpdateSettingsPayload>("/me/settings", payload);
}
