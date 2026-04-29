package com.jp.vocab.backup.controller;

import com.jp.vocab.backup.dto.BackupRestoreConfirmResponse;
import com.jp.vocab.backup.dto.BackupRestorePrepareResponse;
import com.jp.vocab.backup.service.BackupDownload;
import com.jp.vocab.backup.service.BackupRestoreService;
import com.jp.vocab.shared.api.ApiResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/backups/restore")
public class BackupRestoreController {

    private final BackupRestoreService backupRestoreService;

    public BackupRestoreController(BackupRestoreService backupRestoreService) {
        this.backupRestoreService = backupRestoreService;
    }

    @PostMapping(path = "/prepare", consumes = "multipart/form-data")
    public ApiResponse<BackupRestorePrepareResponse> prepare(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(backupRestoreService.prepareRestore(file));
    }

    @GetMapping("/{token}/safety-snapshot")
    public ResponseEntity<ByteArrayResource> downloadSafetySnapshot(@PathVariable String token) {
        BackupDownload backup = backupRestoreService.downloadSafetySnapshot(token);
        ByteArrayResource resource = new ByteArrayResource(backup.content());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + backup.fileName() + "\"")
                .contentType(MediaType.valueOf("application/zip"))
                .contentLength(backup.content().length)
                .body(resource);
    }

    @PostMapping("/{token}/confirm")
    public ApiResponse<BackupRestoreConfirmResponse> confirm(@PathVariable String token) {
        return ApiResponse.success(backupRestoreService.confirmRestore(token));
    }
}
