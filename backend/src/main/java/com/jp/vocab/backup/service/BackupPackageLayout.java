package com.jp.vocab.backup.service;

import java.util.Arrays;
import java.util.List;

public final class BackupPackageLayout {

    public static final String MANIFEST_PATH = "manifest.json";

    private BackupPackageLayout() {
    }

    public static List<BackupManifestFile> defaultManifestFiles() {
        return Arrays.stream(BackupPayloadType.values())
                .map(payloadType -> BackupManifestFile.forPayload(payloadType, null))
                .toList();
    }
}
