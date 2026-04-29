import { downloadFile, postDownloadFile, postFormData, postJson } from "@/shared/api/http";

export interface BackupRestorePrepareResponse {
  restoreToken: string;
  safetySnapshotFileName: string;
  safetySnapshotDownloadPath: string;
}

export interface BackupRestoreConfirmResponse {
  restored: boolean;
}

function normalizeApiPath(path: string) {
  return path.startsWith("/api/") ? path.slice(4) : path;
}

export function exportAccountBackup() {
  return postDownloadFile("/backups/export");
}

export function prepareBackupRestore(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return postFormData<BackupRestorePrepareResponse>("/backups/restore/prepare", formData);
}

export function downloadSafetySnapshot(downloadPath: string) {
  return downloadFile(normalizeApiPath(downloadPath));
}

export function confirmBackupRestore(restoreToken: string) {
  return postJson<BackupRestoreConfirmResponse, Record<string, never>>(`/backups/restore/${restoreToken}/confirm`, {});
}
