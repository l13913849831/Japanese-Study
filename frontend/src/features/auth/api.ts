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

export interface RegisterPayload {
  username: string;
  displayName: string;
  password: string;
}

export interface LogoutResult {
  loggedOut: boolean;
}

export interface UpdateProfilePayload {
  displayName: string;
}

export interface UpdateSettingsPayload {
  preferredLearningOrder: PreferredLearningOrder;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export interface PasswordChangeResult {
  changed: boolean;
}

export function getMe() {
  return getJson<CurrentUser>("/me");
}

export function login(payload: LoginPayload) {
  return postJson<CurrentUser, LoginPayload>("/auth/login", payload);
}

export function register(payload: RegisterPayload) {
  return postJson<CurrentUser, RegisterPayload>("/auth/register", payload);
}

export function logout() {
  return postJson<LogoutResult, Record<string, never>>("/auth/logout", {});
}

export function updateMyProfile(payload: UpdateProfilePayload) {
  return putJson<CurrentUser, UpdateProfilePayload>("/me/profile", payload);
}

export function updateMySettings(payload: UpdateSettingsPayload) {
  return putJson<CurrentUser, UpdateSettingsPayload>("/me/settings", payload);
}

export function changeMyPassword(payload: ChangePasswordPayload) {
  return putJson<PasswordChangeResult, ChangePasswordPayload>("/me/password", payload);
}
