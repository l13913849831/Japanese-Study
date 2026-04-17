# Error Handling

> Error propagation and HTTP failure contracts for backend APIs.

---

## Overview

The backend uses a unified API envelope for both success and failure responses. Validation and business failures are translated centrally in `GlobalExceptionHandler`.

The current approach is:

- validate request shape with Jakarta Validation
- enforce domain rules in services
- throw `BusinessException` for expected business failures
- map everything to `ApiResponse.failure(...)` in `@RestControllerAdvice`

---

## Error Types

- `BusinessException`: expected domain/application exception carrying an `ErrorCode`
- `ErrorCode`: stable error categories used in API responses
- framework validation exceptions:
  - `MethodArgumentNotValidException`
  - `BindException`
  - `ConstraintViolationException`
  - `MissingServletRequestParameterException`
- persistence conflict fallback:
  - `DataIntegrityViolationException`

---

## Error Handling Patterns

- controllers should not catch and translate business exceptions manually
- services should throw `BusinessException` when a caller-facing rule fails
- use specific `ErrorCode` values such as `NOT_FOUND`, `VALIDATION_ERROR`, and `CONFLICT`
- catch checked infrastructure exceptions only when they need to be converted into a domain-facing `BusinessException`, as in `ExportJobService`
- keep generic `Exception` handling centralized in `GlobalExceptionHandler`

Examples:

- `StudyPlanService.getEntity(...)` throws `BusinessException(ErrorCode.NOT_FOUND, ...)`
- `StudyPlanService.validateRequest(...)` throws `BusinessException(ErrorCode.VALIDATION_ERROR, ...)`
- `ExportJobService.create(...)` converts `IOException` into `BusinessException(ErrorCode.EXPORT_ERROR, ...)`

---

## API Error Responses

All API endpoints should return the shared envelope:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      { "field": "name", "message": "must not be blank" }
    ]
  },
  "timestamp": "2026-04-17T00:00:00Z"
}
```

Current HTTP status mapping:

- `NOT_FOUND` -> `404`
- `CONFLICT` -> `409`
- `BAD_REQUEST`, `IMPORT_ERROR`, `TEMPLATE_RENDER_ERROR`, `EXPORT_ERROR`, `VALIDATION_ERROR` -> `400`
- uncaught exceptions -> `500`

---

## Common Mistakes

- Do not return raw exceptions or stack traces from controllers.
- Do not invent one-off response bodies; use `ApiResponse.failure(...)`.
- Do not use `RuntimeException` directly for business rules when `BusinessException` is the established contract.
- Do not swallow checked exceptions without converting them into a meaningful `ErrorCode`.

---

## Examples

- `backend/src/main/java/com/jp/vocab/shared/web/GlobalExceptionHandler.java`
- `backend/src/main/java/com/jp/vocab/shared/exception/BusinessException.java`
- `backend/src/main/java/com/jp/vocab/shared/exception/ErrorCode.java`
- `backend/src/main/java/com/jp/vocab/exportjob/service/ExportJobService.java`
