package com.jp.vocab.backup.service;

record ParsedBackupPackage(
        BackupPackageManifest manifest,
        BackupPayloadSnapshot snapshot
) {
}
