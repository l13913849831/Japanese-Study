package com.jp.vocab.backup.service;

public enum BackupPayloadType {
    USER_PROFILE("data/user-profile.json", true),
    USER_SETTINGS("data/user-settings.json", true),
    WORD_SETS("data/word-sets.json", true),
    WORD_ENTRIES("data/word-entries.json", true),
    ANKI_TEMPLATES("data/anki-templates.json", true),
    MARKDOWN_TEMPLATES("data/markdown-templates.json", true),
    NOTE_SOURCES("data/note-sources.json", true),
    STUDY_PLANS("data/study-plans.json", true),
    CARD_INSTANCES("data/card-instances.json", true),
    REVIEW_LOGS("data/review-logs.json", true),
    NOTES("data/notes.json", true),
    NOTE_REVIEW_LOGS("data/note-review-logs.json", true);

    private final String path;
    private final boolean required;

    BackupPayloadType(String path, boolean required) {
        this.path = path;
        this.required = required;
    }

    public String getPath() {
        return path;
    }

    public boolean isRequired() {
        return required;
    }
}
