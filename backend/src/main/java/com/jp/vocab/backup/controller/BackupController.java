package com.jp.vocab.backup.controller;

import com.jp.vocab.backup.service.BackupDownload;
import com.jp.vocab.backup.service.BackupExportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/backups")
public class BackupController {

    private final BackupExportService backupExportService;

    public BackupController(BackupExportService backupExportService) {
        this.backupExportService = backupExportService;
    }

    @PostMapping("/export")
    public ResponseEntity<ByteArrayResource> export() {
        BackupDownload backup = backupExportService.exportCurrentAccountBackup();
        ByteArrayResource resource = new ByteArrayResource(backup.content());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + backup.fileName() + "\"")
                .contentType(MediaType.valueOf("application/zip"))
                .contentLength(backup.content().length)
                .body(resource);
    }
}
