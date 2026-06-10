import { getJson, postJson } from "@/shared/api/http";
import type { PageResponse } from "@/shared/api/types";

export type AdminUserStatus = "ACTIVE" | "DISABLED";
export type AdminUserRole = "USER" | "ADMIN";

export interface AdminUserFilters {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: AdminUserStatus;
  role?: AdminUserRole;
}

export interface AdminUserListItem {
  id: number;
  username: string | null;
  displayName: string;
  status: AdminUserStatus;
  role: AdminUserRole;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserAssetSummary {
  wordSetCount: number;
  studyPlanCount: number;
  noteCount: number;
}

export interface AdminUserDetail extends AdminUserListItem {
  assetSummary: AdminUserAssetSummary;
}

export interface AdminUserStatusResult {
  id: number;
  status: AdminUserStatus;
}

export interface AdminResetPasswordPayload {
  newPassword: string;
}

export interface AdminUserPasswordResetResult {
  id: number;
  reset: boolean;
}

export type SecurityAuditOutcome = "SUCCESS" | "FAILURE" | "BLOCKED";
export type SecurityAuditEventType =
  | "LOGIN_SUCCESS"
  | "LOGIN_FAILURE"
  | "LOGIN_LOCKED"
  | "LOGIN_DISABLED"
  | "LOGOUT"
  | "ADMIN_USER_DETAIL_VIEW"
  | "ADMIN_USER_STATUS_CHANGE"
  | "ADMIN_USER_PASSWORD_RESET";

export interface SecurityAuditFilters {
  page?: number;
  pageSize?: number;
  eventType?: SecurityAuditEventType;
  outcome?: SecurityAuditOutcome;
  username?: string;
}

export interface SecurityAuditEvent {
  id: number;
  eventType: SecurityAuditEventType;
  outcome: SecurityAuditOutcome;
  userId: number | null;
  username: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  message: string | null;
  createdAt: string;
}

function buildAdminUserQuery(filters: AdminUserFilters = {}) {
  const params = new URLSearchParams();
  params.set("page", String(filters.page ?? 1));
  params.set("pageSize", String(filters.pageSize ?? 20));
  if (filters.keyword?.trim()) {
    params.set("keyword", filters.keyword.trim());
  }
  if (filters.status) {
    params.set("status", filters.status);
  }
  if (filters.role) {
    params.set("role", filters.role);
  }
  return params.toString();
}

function buildSecurityAuditQuery(filters: SecurityAuditFilters = {}) {
  const params = new URLSearchParams();
  params.set("page", String(filters.page ?? 1));
  params.set("pageSize", String(filters.pageSize ?? 20));
  if (filters.eventType) {
    params.set("eventType", filters.eventType);
  }
  if (filters.outcome) {
    params.set("outcome", filters.outcome);
  }
  if (filters.username?.trim()) {
    params.set("username", filters.username.trim());
  }
  return params.toString();
}

export function listAdminUsers(filters: AdminUserFilters = {}) {
  return getJson<PageResponse<AdminUserListItem>>(`/admin/users?${buildAdminUserQuery(filters)}`);
}

export function getAdminUserDetail(userId: number) {
  return getJson<AdminUserDetail>(`/admin/users/${userId}`);
}

export function disableAdminUser(userId: number) {
  return postJson<AdminUserStatusResult, Record<string, never>>(`/admin/users/${userId}/disable`, {});
}

export function enableAdminUser(userId: number) {
  return postJson<AdminUserStatusResult, Record<string, never>>(`/admin/users/${userId}/enable`, {});
}

export function resetAdminUserPassword(userId: number, payload: AdminResetPasswordPayload) {
  return postJson<AdminUserPasswordResetResult, AdminResetPasswordPayload>(`/admin/users/${userId}/reset-password`, payload);
}

export function listSecurityAuditEvents(filters: SecurityAuditFilters = {}) {
  return getJson<PageResponse<SecurityAuditEvent>>(`/admin/audit-events?${buildSecurityAuditQuery(filters)}`);
}
