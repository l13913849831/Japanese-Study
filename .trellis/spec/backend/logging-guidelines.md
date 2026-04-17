# Logging Guidelines

> Current logging reality and minimum expectations for new backend changes.

---

## Overview

Logging is currently minimal. The application relies mostly on Spring Boot's default logging setup plus configured package log levels in `application.yml`.

Observed today:

- no application classes currently define explicit logger fields
- `logging.level.org.flywaydb=info`
- `logging.level.org.springframework.web=info`

This means new logging should be added deliberately and consistently rather than scattered ad hoc.

---

## Log Levels

- `INFO`: lifecycle events worth tracking in production, such as export generation start/end or major import completion
- `WARN`: recoverable anomalies or suspicious inputs that did not fail the whole request
- `ERROR`: unexpected failures or infrastructure exceptions that escape the normal happy path
- `DEBUG`: local investigation only; avoid depending on debug logs for core observability

Because the codebase currently has no explicit application logging, prefer adding logs only around operationally meaningful boundaries.

---

## Structured Logging

If you introduce logging, include stable identifiers in the message text:

- domain object ID (`planId`, `wordSetId`, `exportJobId`)
- operation name (`create export`, `import entries`, `preview template`)
- outcome (`success`, `failed`, `skipped`)

There is no custom structured logging wrapper yet. Use the standard Spring/SLF4J stack when logging becomes necessary and keep message templates consistent.

---

## What to Log

- file-system side effects such as export file generation
- long-running or batch-style operations such as CSV/APKG import
- unexpected infrastructure failures before they are converted into `BusinessException`
- startup/config issues that affect connectivity or environment setup

---

## What NOT to Log

- database passwords, connection strings with credentials, or env secrets
- raw uploaded file contents
- full payloads containing user-generated text unless needed for debugging and sanitized first
- stack traces in both logs and HTTP responses for expected validation or business-rule failures

---

## Examples

- `backend/src/main/resources/application.yml`: current log level configuration
- `backend/src/main/java/com/jp/vocab/exportjob/service/ExportJobService.java`: good candidate for future operational logs
- `backend/src/main/java/com/jp/vocab/wordset/service/WordEntryService.java`: good candidate for import progress/error logs when logging is added
