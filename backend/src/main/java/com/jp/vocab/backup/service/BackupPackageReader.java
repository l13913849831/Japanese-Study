package com.jp.vocab.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class BackupPackageReader {

    private final ObjectMapper objectMapper;

    public BackupPackageReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedBackupPackage read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "backup file must not be empty");
        }

        try (InputStream inputStream = file.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            Map<String, byte[]> entries = unzipEntries(zipInputStream);
            BackupPackageManifest manifest = readManifest(entries);
            validateManifest(manifest, entries);
            return new ParsedBackupPackage(manifest, buildSnapshot(entries));
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Failed to read backup package");
        }
    }

    private Map<String, byte[]> unzipEntries(ZipInputStream zipInputStream) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                zipInputStream.closeEntry();
                continue;
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            zipInputStream.transferTo(outputStream);
            entries.put(entry.getName(), outputStream.toByteArray());
            zipInputStream.closeEntry();
        }
        return entries;
    }

    private BackupPackageManifest readManifest(Map<String, byte[]> entries) throws IOException {
        byte[] manifestBytes = entries.get(BackupPackageLayout.MANIFEST_PATH);
        if (manifestBytes == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "manifest.json is required");
        }
        return objectMapper.readValue(manifestBytes, BackupPackageManifest.class);
    }

    private void validateManifest(BackupPackageManifest manifest, Map<String, byte[]> entries) {
        if (!BackupPackageManifest.FORMAT_VERSION.equals(manifest.formatVersion())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "backup formatVersion is unsupported");
        }
        if (!BackupPackageManifest.PACKAGE_TYPE.equals(manifest.packageType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "backup packageType is invalid");
        }
        if (!BackupPackageManifest.SCOPE.equals(manifest.scope())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "backup scope is invalid");
        }

        for (BackupManifestFile expected : BackupPackageLayout.defaultManifestFiles()) {
            if (expected.required() && !entries.containsKey(expected.path())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "backup file is missing: " + expected.path());
            }
        }
    }

    private BackupPayloadSnapshot buildSnapshot(Map<String, byte[]> entries) throws IOException {
        return new BackupPayloadSnapshot(
                readObject(entries, BackupPayloadType.USER_PROFILE, BackupUserProfilePayload.class),
                readObject(entries, BackupPayloadType.USER_SETTINGS, BackupUserSettingsPayload.class),
                readList(entries, BackupPayloadType.WORD_SETS, BackupWordSetPayload.class),
                readList(entries, BackupPayloadType.WORD_ENTRIES, BackupWordEntryPayload.class),
                readList(entries, BackupPayloadType.ANKI_TEMPLATES, BackupAnkiTemplatePayload.class),
                readList(entries, BackupPayloadType.MARKDOWN_TEMPLATES, BackupMarkdownTemplatePayload.class),
                readList(entries, BackupPayloadType.NOTE_SOURCES, BackupNoteSourcePayload.class),
                readList(entries, BackupPayloadType.STUDY_PLANS, BackupStudyPlanPayload.class),
                readList(entries, BackupPayloadType.CARD_INSTANCES, BackupCardInstancePayload.class),
                readList(entries, BackupPayloadType.REVIEW_LOGS, BackupReviewLogPayload.class),
                readList(entries, BackupPayloadType.NOTES, BackupNotePayload.class),
                readList(entries, BackupPayloadType.NOTE_REVIEW_LOGS, BackupNoteReviewLogPayload.class)
        );
    }

    private <T> T readObject(Map<String, byte[]> entries, BackupPayloadType payloadType, Class<T> type) throws IOException {
        return objectMapper.readValue(requireBytes(entries, payloadType), type);
    }

    private <T> List<T> readList(Map<String, byte[]> entries, BackupPayloadType payloadType, Class<T> elementType) throws IOException {
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return objectMapper.readValue(requireBytes(entries, payloadType), collectionType);
    }

    private byte[] requireBytes(Map<String, byte[]> entries, BackupPayloadType payloadType) {
        byte[] payload = entries.get(payloadType.getPath());
        if (payload == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "backup file is missing: " + payloadType.getPath());
        }
        return payload;
    }
}
