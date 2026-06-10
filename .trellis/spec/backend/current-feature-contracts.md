# Current Feature Contracts

> Migrated from retired OpenSpec changes and synced to the backend that is currently implemented.

---

## Source of Truth

Use this file when the change touches any implemented learner-facing backend flow.

This file absorbs the retired OpenSpec feature contracts for bootstrap, core workflows, and `.apkg` import.
These contracts are consumed by both the Web app and the miniapp, so response shapes must stay compatible for both clients.

When Trellis, `docs/api-specification.md`, and code disagree, prefer current code first, then update both docs.

---

## Scenario: Admin role foundation and access control

### 1. Scope / Trigger

- Trigger: change touches user roles, authorities, `/api/admin/**`, or admin bootstrap behavior.
- Packages: `com.jp.vocab.user`, `com.jp.vocab.shared.auth`.

### 2. Signatures

- `GET /api/me`
- `GET /api/admin/me`

### 3. Contracts

- User role is stored on `user_account.role`.
- Supported roles are:
  - `USER`
  - `ADMIN`
- Authenticated principals expose exactly one authority derived from role:
  - `ROLE_USER`
  - `ROLE_ADMIN`
- `/api/admin/**` requires `ROLE_ADMIN`.
- `GET /api/me` includes `roles[]` so the SPA can guard admin routes.
- Bootstrap account role defaults to `USER` and can be explicitly set by configuration.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| anonymous request to `/api/admin/**` | reject with standard `UNAUTHORIZED` envelope |
| authenticated `USER` request to `/api/admin/**` | reject with standard `FORBIDDEN` envelope |
| authenticated `ADMIN` request to `/api/admin/**` | allow request |
| unsupported bootstrap role | normalize to `USER` |

### 5. Good / Base / Bad Cases

- Good: admin account sees `/admin` and can call `/api/admin/me`.
- Base: ordinary user keeps all learner routes but cannot access admin routes.
- Bad: frontend-only admin hiding without backend enforcement.

### 6. Tests Required

- admin API unauthenticated rejection coverage
- admin API non-admin rejection coverage
- admin API admin success coverage
- principal authority and bootstrap role coverage

### 7. Wrong vs Correct

#### Wrong

- rely only on route hiding in the frontend
- leave `AppUserPrincipal#getAuthorities()` empty
- introduce complex RBAC before any admin endpoint exists

#### Correct

- use one minimal `USER` / `ADMIN` role field first
- protect `/api/admin/**` at Spring Security boundary
- expose `roles[]` from `/api/me` for frontend route guards

---

## Scenario: Admin user governance and security alerts

### 1. Scope / Trigger

- Trigger: change touches `/api/admin/users`, `/api/admin/audit-events`, `/api/admin/security-alerts`, user status governance, admin password reset, or security audit aggregation.
- Packages: `com.jp.vocab.user`, `com.jp.vocab.shared.auth`.

### 2. Signatures

- `GET /api/admin/users?page=&pageSize=&keyword=&status=&role=`
- `GET /api/admin/users/{userId}`
- `POST /api/admin/users/{userId}/disable`
- `POST /api/admin/users/{userId}/enable`
- `POST /api/admin/users/{userId}/reset-password`
- `GET /api/admin/audit-events?page=&pageSize=&eventType=&outcome=&username=`
- `GET /api/admin/security-alerts?lookbackHours=&limit=`

### 3. Contracts

- All endpoints are under `/api/admin/**` and require `ROLE_ADMIN`.
- User list returns `PageResponse<AdminUserListItemResponse>`.
- User detail returns account metadata plus `assetSummary` counts only:
  - `wordSetCount`
  - `studyPlanCount`
  - `noteCount`
- Admin password reset accepts `newPassword` and never returns password hashes.
- Security audit event types include login events and admin user-governance events.
- Security alerts are derived from `security_audit_event`; there is no dedicated alert table.
- Security alert response fields are:
  - `id`
  - `alertType`
  - `severity`
  - `title`
  - `description`
  - `username`
  - `ipAddress`
  - `eventCount`
  - `lastSeenAt`
- Current alert types are:
  - `REPEATED_LOGIN_FAILURE`
  - `ACCOUNT_LOCKED`
  - `DISABLED_ACCOUNT_LOGIN`

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| anonymous admin request | reject with standard `UNAUTHORIZED` envelope |
| non-admin admin request | reject with standard `FORBIDDEN` envelope |
| invalid user status or role filter | reject with `VALIDATION_ERROR` |
| target user not found | reject with standard `NOT_FOUND` envelope |
| admin tries to disable self | reject with `CONFLICT` |
| reset password length outside 8-72 chars | reject with validation error |
| invalid audit event type or outcome | reject with `VALIDATION_ERROR` |
| missing alert query parameters | use 24-hour window and default limit |

### 5. Good / Base / Bad Cases

- Good: admin searches users, views detail, disables an account, sees the audit event, and sees alert signals derived from recent audit events.
- Base: no security alert rows are returned when recent audit events do not cross the current strategy.
- Bad: expose raw learning content or password hashes through admin detail, or persist a separate alert table before acknowledgement/notification semantics exist.

### 6. Tests Required

- admin API access-control smoke coverage
- admin user service coverage for disable self guard, detail asset summary, and password reset audit
- security audit service coverage for event recording, filter normalization, and truncation
- security alert service coverage for repeated-failure aggregation, lock/disabled-login alert mapping, and query limit clamping
- frontend build/type-check for admin API response contracts

### 7. Wrong vs Correct

#### Wrong

- let admin pages inspect raw user learning content during the first governance MVP
- derive admin access from frontend route state only
- create an alert persistence model without acknowledgement, notification, or owner semantics

#### Correct

- keep the first admin detail response to account metadata and asset counts
- enforce all admin APIs through `/api/admin/**`
- derive first-stage security alerts from immutable audit events

---

## Scenario: Local account auth and session security

### 1. Scope / Trigger

- Trigger: change touches local account login/register/logout, session-cookie auth, CSRF flow, or bootstrap account behavior.
- Packages: `com.jp.vocab.user`, `com.jp.vocab.shared.auth`, frontend `/login`, shared HTTP client.

### 2. Signatures

- `GET /api/auth/csrf`
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/logout`
- `GET /api/me`
- `PUT /api/me/profile`
- `PUT /api/me/settings`
- `PUT /api/me/password`

### 3. Contracts

- Web auth uses session cookie rather than JWT.
- Default session cookie name is `JP_SESSION`.
- Write requests must pass CSRF validation.
- `GET /api/auth/csrf` returns:
  - `headerName`
  - `parameterName`
  - `token`
- Frontend obtains a fresh CSRF token before write requests and sends it through the returned header name.
- Bootstrap local account is disabled by default and must be explicitly enabled by configuration.
- Local login tracks failed attempts per local identity.
- Default lock policy is 5 failed attempts followed by a 15-minute temporary lock.
- Successful login clears the local identity's failed-login state.
- Authentication security audit persists events for login success, login failure, temporary lock, disabled-account block, and logout.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| missing or invalid CSRF token on write request | reject with standard `FORBIDDEN` envelope |
| login with bad credentials | reject with standard `UNAUTHORIZED` envelope |
| login during temporary lock | reject with standard `FORBIDDEN` envelope |
| login for disabled account | reject with standard `FORBIDDEN` envelope |
| bootstrap enabled with blank password | fail startup rather than creating an unsafe account |

### 5. Good / Base / Bad Cases

- Good: client fetches `/api/auth/csrf`, submits login, receives session cookie, then continues authenticated writes with fresh CSRF tokens.
- Base: bootstrap account stays off by default and ordinary local registration remains available.
- Bad: frontend writes without CSRF header, or backend silently keeps a default demo account enabled in non-local environments.

### 6. Tests Required

- CSRF endpoint coverage
- auth write request rejection when CSRF token is missing
- login success path with CSRF token present
- bootstrap default-off and blank-password guard coverage
- login failure counting, lockout rejection, and success reset coverage
- auth security audit event coverage

### 7. Wrong vs Correct

#### Wrong

- leave CSRF disabled while using cookie auth
- rely on an always-on demo bootstrap account
- make the frontend guess the CSRF header name

#### Correct

- keep cookie auth and enforce CSRF on writes
- expose one explicit CSRF bootstrap endpoint for the SPA
- require bootstrap account enablement via configuration

---

## Scenario: WeChat miniapp mobile auth

### 1. Scope / Trigger

- Trigger: change touches `/api/mobile/**`, WeChat code exchange, mobile bearer tokens, `user_identity` external providers, or mobile access to learner APIs.
- Packages: `com.jp.vocab.user`, `com.jp.vocab.shared.auth`, `com.jp.vocab.shared.config`.

### 2. Signatures

- `POST /api/mobile/auth/wechat-login`
- `POST /api/mobile/auth/logout`
- `GET /api/mobile/me`
- DB migration:
  - `V16__init_mobile_auth.sql`
- DB tables:
  - `user_identity.provider = WECHAT_MINIAPP`
  - `mobile_session`
- Environment keys:
  - `APP_AUTH_WECHAT_MINIAPP_APP_ID`
  - `APP_AUTH_WECHAT_MINIAPP_APP_SECRET`
  - `APP_AUTH_WECHAT_MINIAPP_CODE_TO_SESSION_URL`
  - `APP_AUTH_MOBILE_SESSION_TOKEN_TTL`

### 3. Contracts

- Miniapp login accepts:
  - `code`: non-blank `wx.login` code.
- Backend exchanges `code` through WeChat `jscode2session` and treats `openid` only as an external identity.
- Internal ownership remains anchored on `user_account.id`.
- New WeChat users create:
  - active `user_account` with role `USER`
  - `user_identity` with provider `WECHAT_MINIAPP`
  - default `user_setting`
  - one `mobile_session`
- Mobile token response returns:
  - `token`
  - `expiresAt`
  - `user`
- Mobile bearer tokens are stored as SHA-256 hashes in `mobile_session.token_hash`.
- Mobile principal username uses `wechat-miniapp:<userId>`.
- Mobile principal always exposes `ROLE_USER`, even if the backing account later has `ADMIN`.
- Existing learner APIs may accept `Authorization: Bearer <token>` without CSRF.
- `/api/admin/**` still requires `ROLE_ADMIN`, so mobile tokens cannot access admin APIs.
- Web auth remains session cookie + CSRF and does not reuse mobile tokens.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| blank `code` | reject with validation error |
| WeChat appId or secret missing | reject with `INTERNAL_ERROR` |
| WeChat code exchange fails or returns no `openid` | reject with `UNAUTHORIZED` |
| existing account is disabled | reject with `FORBIDDEN` and record disabled-login audit |
| `/api/mobile/**` without bearer token except login | reject with `UNAUTHORIZED` |
| bearer token missing, expired, revoked, or unknown | reject with `UNAUTHORIZED` |
| mobile bearer token calls `/api/admin/**` | reject with `FORBIDDEN` |
| logout with a valid token | revoke matching `mobile_session` row |

### 5. Good / Base / Bad Cases

- Good: miniapp sends `wx.login` code, backend exchanges `openid`, creates or reuses the learner account, returns mobile token, and learner APIs accept the bearer token.
- Base: existing WeChat identity logs in again and receives a fresh mobile token without duplicating the internal account.
- Bad: treat `openid` as the system user id, reuse Web cookie auth in miniapp, or allow mobile token to inherit `ADMIN`.

### 6. Tests Required

- service coverage for new `openid` account creation, identity binding, setting creation, session issuance, disabled-account rejection, and mobile `USER` role response.
- controller/security coverage for login without CSRF, `/api/mobile/me` without token rejection, bearer-token success, and logout without CSRF.
- existing Web auth/admin controller security tests must include the mobile session service mock when importing `SecurityConfig`.

### 7. Wrong vs Correct

#### Wrong

- store raw mobile tokens in the database
- make `openid` the internal user primary key
- let mobile token principal expose the backing account's admin role
- disable CSRF globally to support miniapp writes

#### Correct

- store only token hashes and expiry/revoke metadata
- bind `WECHAT_MINIAPP + openid` to `user_account.id`
- force mobile principals to `ROLE_USER`
- ignore CSRF only for `/api/mobile/**` and bearer-token requests

---

## Scenario: Word-set data preparation and import

### 1. Scope / Trigger

- Trigger: change touches `wordset` controllers, import parsing, word-entry CRUD, or import preview/apply behavior.
- Packages: `com.jp.vocab.wordset`, shared API envelope, frontend `/word-sets`.

### 2. Signatures

- `POST /api/word-sets`
- `GET /api/word-sets`
- `GET /api/word-sets/{wordSetId}/words?page=&pageSize=&keyword=&level=&tag=`
- `POST /api/word-sets/{wordSetId}/words`
- `PUT /api/words/{wordId}`
- `DELETE /api/words/{wordId}`
- `POST /api/word-sets/{wordSetId}/import/preview` with `multipart/form-data`
- `POST /api/word-sets/{wordSetId}/import` with `multipart/form-data`

### 3. Contracts

- Import supports `CSV` and `APKG`.
- Preview returns:
  - `sourceType`
  - `totalRows`
  - `readyCount`
  - `duplicateCount`
  - `errorCount`
  - `fieldMappings[]`
  - `previewRows[]`
- Preview row status is one of:
  - `READY`
  - `DUPLICATE`
  - `ERROR`
- Preview does not persist data.
- Apply persists only rows that were effectively valid for import and returns:
  - `importedCount`
  - `skippedCount`
  - `errors[]`
- Duplicate detection is scoped to the target word set and keyed by `expression + reading`.
- `.apkg` import reuses the same save pipeline as CSV after field mapping and cleanup.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| unsupported file extension | reject with standard error envelope |
| missing required CSV/APKG fields | preview still returns diagnostics and row-level `ERROR` |
| duplicate row | preview marks `DUPLICATE`; apply increments `skippedCount` |
| unreadable `.apkg` package | reject with import error, do not partially persist |
| alias-based CSV match | surface it through `fieldMappings[].note` |

### 5. Good / Base / Bad Cases

- Good: valid CSV or `.apkg`, required fields mapped, preview shows `READY`, apply imports rows in source order.
- Base: preview includes a mix of `READY`, `DUPLICATE`, and `ERROR`; apply still imports only valid rows.
- Bad: package cannot expose readable note data or required fields never map to `expression` and `meaning`.

### 6. Tests Required

- controller test for preview/apply multipart endpoints
- CSV preview and apply regression coverage
- `.apkg` parsing and field-mapping coverage
- duplicate handling and source-order preservation coverage
- word-entry list filter and CRUD coverage

### 7. Wrong vs Correct

#### Wrong

- create a separate `.apkg`-only response contract
- persist rows during preview
- duplicate CSV and `.apkg` validation logic in different save paths

#### Correct

- keep one preview/apply result model for both file types
- treat preview as pure analysis
- convert both sources into the same analyzed-row/save pipeline

---

## Scenario: Study-plan runtime, cards, review, and dashboard

### 1. Scope / Trigger

- Trigger: change touches `studyplan`, `card`, `dashboard`, or the state transition between plan, generated cards, and review logs.
- Packages: `com.jp.vocab.studyplan`, `com.jp.vocab.card`, `com.jp.vocab.dashboard`.

### 2. Signatures

- `GET /api/study-plans`
- `GET /api/study-plans/{id}`
- `POST /api/study-plans`
- `PUT /api/study-plans/{id}`
- `POST /api/study-plans/{id}/activate`
- `POST /api/study-plans/{id}/pause`
- `POST /api/study-plans/{id}/archive`
- `GET /api/study-plans/{planId}/cards/today?date=YYYY-MM-DD`
- `GET /api/study-plans/{planId}/cards/calendar?start=YYYY-MM-DD&end=YYYY-MM-DD`
- `POST /api/cards/{cardId}/review`
- `GET /api/cards/{cardId}/reviews`
- `GET /api/dashboard?date=YYYY-MM-DD`

### 3. Contracts

- Study plan status is one of `DRAFT`, `ACTIVE`, `PAUSED`, `ARCHIVED`.
- `reviewOffsets` must be non-empty, sorted ascending, and start with `0`.
- Plan create/update returns the full persisted plan object.
- Plan create and rebuild paths work against pre-generated `card_instance` records.
- Today cards return real card instances plus word-entry fields needed by the UI.
- Review submission appends a review log and returns:
  - `reviewId`
  - `cardId`
  - `rating`
  - `cardStatus`
  - `reviewedAt`
- Dashboard is aggregate-only:
  - `overview`
  - `activePlans[]`
  - `recentTrend[]`
- Dashboard adds no new persistence model; it reuses `study_plan`, `card_instance`, and `review_log`.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| invalid `reviewOffsets` | reject with field-level validation error |
| missing word set or template reference | reject before plan persistence |
| lifecycle action not allowed for current status | reject with business error |
| review request for unknown card | reject with standard error envelope |
| dashboard date omitted or invalid | validate at controller boundary before aggregation |

### 5. Good / Base / Bad Cases

- Good: create a valid plan, generate cards, activate it, query today cards, submit reviews, and see dashboard aggregates move.
- Base: pause or archive a plan without losing its persisted history.
- Bad: mutate key plan inputs without rebuilding dependent cards, or compute today cards ad hoc outside `card_instance`.

### 6. Tests Required

- plan create/update/lifecycle controller coverage
- card generation service coverage for offsets, sequence, and due dates
- today/calendar query coverage
- review submit and review-history coverage
- dashboard aggregation coverage for mixed active-plan states

### 7. Wrong vs Correct

#### Wrong

- recalculate cards only at query time
- let review-offset rules drift between frontend and backend
- add dashboard-only tables for data that already exists

#### Correct

- keep generated cards as the runtime source for today/calendar flows
- validate lifecycle and offset rules at the backend boundary
- treat dashboard as an aggregate read model over existing tables

---

## Scenario: Word-review FSRS runtime

### 1. Scope / Trigger

- Trigger: change touches word-review scheduling, `card_instance` runtime semantics, card generation, card review, or dashboard/card queries.
- Packages: `com.jp.vocab.studyplan`, `com.jp.vocab.card`, `com.jp.vocab.dashboard`, frontend `/study-plans`, `/cards`.

### 2. Signatures

- DB migration:
  - `V9__migrate_word_cards_to_fsrs.sql`
- `POST /api/study-plans`
- `PUT /api/study-plans/{id}`
- `GET /api/study-plans/{planId}/cards/today?date=YYYY-MM-DD`
- `GET /api/study-plans/{planId}/cards/calendar?start=YYYY-MM-DD&end=YYYY-MM-DD`
- `POST /api/cards/{cardId}/review`
- `GET /api/dashboard?date=YYYY-MM-DD`

### 3. Contracts

- `study_plan.dailyNewCount` still controls new-word introduction cadence.
- `study_plan.reviewOffsets` is now a compatibility field; it no longer drives long-term review scheduling.
- New plans generate one initial pending `card_instance` per word instead of pre-generating all review stages.
- `card_instance` runtime fields now include:
  - `due_at`
  - `fsrs_card_json`
  - `review_count`
  - `last_reviewed_at`
- A reviewed word keeps its current row as historical `DONE` state and appends one next pending row with `stage_no + 1`.
- `GET /api/study-plans/{planId}/cards/today` returns pending rows whose `due_at` is before the selected date's day-end, so overdue cards are included.
- `POST /api/cards/{cardId}/review` now:
  - writes one `review_log`
  - marks the current row `DONE`
  - appends one next pending FSRS row
  - still returns weak-item fields and `todayAction`
- Dashboard word-study aggregates read pending rows from `due_at` instead of pre-generated offset rows.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| invalid rating | reject with validation error |
| unknown card id | reject with standard `NOT_FOUND` envelope |
| plan create/update with invalid compatibility `reviewOffsets` | reject with field-level validation error |
| reviewing a card with missing legacy FSRS state | initialize from default FSRS card state and continue |
| legacy plan has multiple future pending rows for one word | migration keeps the earliest pending row and drops extra unreviewed future rows |

### 5. Good / Base / Bad Cases

- Good: create a new plan, get one initial pending row per word, review a word, and observe exactly one next pending FSRS row.
- Base: legacy plan keeps its earliest pending row and adopts FSRS scheduling from the next review onward.
- Bad: continue pre-generating all future review rows or let today queries include both historical `DONE` rows and new pending rows for the same review state.

### 6. Tests Required

- card FSRS scheduler coverage for initial state and review advancement
- card review service coverage for next pending-row creation and weak-state compatibility
- card query/dashboard coverage for `due_at`-based pending selection
- regression verification that weak-item recovery still passes after FSRS migration

### 7. Wrong vs Correct

#### Wrong

- keep `reviewOffsets` as the active long-term schedule while also claiming FSRS is enabled
- mutate the current pending row into the next pending row and lose review history
- keep multiple future pending rows for the same word after migration

#### Correct

- treat `reviewOffsets` as compatibility only and let FSRS own next due time
- append one next pending row after each review so history remains queryable
- keep exactly one pending runtime row per word

---

## Scenario: Note knowledge capture, Markdown import, note review, and dashboard

### 1. Scope / Trigger

- Trigger: change touches `note` controllers, Markdown parsing/import, note review scheduling, or note dashboard aggregation.
- Packages: `com.jp.vocab.note`, shared API envelope, frontend `/notes*`.

### 2. Signatures

- `GET /api/notes?page=&pageSize=&keyword=&tag=&masteryStatus=`
- `POST /api/notes`
- `PUT /api/notes/{noteId}`
- `DELETE /api/notes/{noteId}`
- `POST /api/notes/import/preview` with `multipart/form-data`
- `POST /api/notes/import`
- `GET /api/notes/reviews/today?date=YYYY-MM-DD`
- `POST /api/notes/{noteId}/reviews`
- `GET /api/notes/{noteId}/reviews`
- `GET /api/notes/dashboard?date=YYYY-MM-DD`
- DB tables:
  - `note`
  - `note_review_log`

### 3. Contracts

- Note is an independent learner-facing entity and stores:
  - `title`
  - `content`
  - `tags[]`
  - `reviewCount`
  - `masteryStatus`
  - `dueAt`
  - `lastReviewedAt`
  - `fsrsCardJson`
- Markdown import preview accepts:
  - `file`
  - `splitMode` in `H1 | H1_H2 | ALL`
  - `commonTagsText`
- Preview returns:
  - `splitMode`
  - `totalItems`
  - `readyCount`
  - `errorCount`
  - `previewItems[]`
- Untitled Markdown content is preserved as editable preview item titled `未命名知识点`.
- Empty parent headings created only as structure must not surface as preview rows.
- Review ratings are fixed to:
  - `AGAIN`
  - `HARD`
  - `GOOD`
  - `EASY`
- Product wording maps to FSRS as:
  - `不会 -> AGAIN`
  - `吃力 -> HARD`
  - `还行 -> GOOD`
  - `熟悉 -> EASY`
- Review submission appends one `note_review_log` row and updates the `note` scheduling fields from FSRS.
- Dashboard is aggregate-only and returns:
  - `overview`
  - `masteryDistribution[]`
  - `recentTrend[]`
  - `recentNotes[]`

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| empty or non-`.md` upload | reject with standard import error envelope |
| blank note title/content on create or import | reject with validation error |
| note not found | reject with standard `NOT_FOUND` envelope |
| invalid review rating | reject with validation error |
| blank section generated only from parent heading | skip it from preview instead of emitting an error row |

### 5. Good / Base / Bad Cases

- Good: create a note, import Markdown with preview edits, submit reviews, and see queue/dashboard values move.
- Base: preview contains both untitled content and titled sections; user edits tags and imports only the kept rows.
- Bad: persist Markdown preview rows before confirmation, or couple notes to the legacy `study_plan/card_instance/review_log` tables.

### 6. Tests Required

- Markdown parser coverage for untitled content preservation and breadcrumb titles

---

## Scenario: Weak-item lifecycle and same-day recovery

### 1. Scope / Trigger

- Trigger: change touches weak-item state, card/note review responses, weak-item listing APIs, or same-day recovery semantics.
- Packages: `com.jp.vocab.card`, `com.jp.vocab.note`, `com.jp.vocab.weakitem`, frontend `/cards`, `/notes/review`, `/weak-items`.

### 2. Signatures

- DB migration:
  - `V8__add_weak_state.sql`
- `POST /api/cards/{cardId}/review`
- `POST /api/notes/{noteId}/reviews`
- `GET /api/weak-items/summary`
- `GET /api/weak-items/words?page=&pageSize=`
- `GET /api/weak-items/notes?page=&pageSize=`
- `POST /api/weak-items/words/{cardId}/dismiss`
- `POST /api/weak-items/notes/{noteId}/dismiss`

### 3. Contracts

- `card_instance` adds:
  - `weak_flag`
  - `weak_marked_at`
  - `weak_review_count`
  - `last_review_rating`
- `note` adds:
  - `weak_flag`
  - `weak_marked_at`
  - `last_review_rating`
- Card review request accepts optional `sessionAgainCount`.
- Note review request accepts optional `sessionAgainCount`.
- Card review response returns:
  - `reviewId`
  - `cardId`
  - `rating`
  - `cardStatus`
  - `reviewedAt`
  - `weak`
  - `weakMarkedAt`
  - `todayAction`
- Note review response returns:
  - `reviewId`
  - `noteId`
  - `rating`
  - `masteryStatus`
  - `reviewedAt`
  - `dueAt`
  - `weak`
  - `weakMarkedAt`
  - `todayAction`
- `todayAction` values:
  - card: `DONE | REQUEUE_TODAY | MOVE_TO_WEAK_ROUND`
  - note: `DONE | MOVE_TO_RECOVERY_QUEUE | MOVE_TO_WEAK_ROUND`
- `GET /api/weak-items/summary` returns:
  - `weakWordCount`
  - `weakNoteCount`
- Weak-item list endpoints are paged and sorted by `weak_marked_at desc, id desc`.
- Dismiss endpoints clear weak state only; they do not mutate review logs.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| `sessionAgainCount < 0` | reject with validation error |
| second-or-later `AGAIN` in same session | mark item weak and return `MOVE_TO_WEAK_ROUND` |
| first `AGAIN` for card | keep long-term schedule path, return `REQUEUE_TODAY` |
| first `AGAIN` for note | keep long-term schedule path, return `MOVE_TO_RECOVERY_QUEUE` |
| later `GOOD` or `EASY` on weak item | clear weak state automatically |
| dismiss unknown card/note | reject with standard `NOT_FOUND` envelope |

### 5. Good / Base / Bad Cases

- Good: card or note hits `AGAIN` twice in the same session, enters weak list, appears in `/weak-items`, then exits on later `GOOD`.
- Base: item enters weak state and the learner manually dismisses it from `/weak-items`.
- Bad: persist today's temporary recovery queue in the database or require a dedicated temp table just to support same-day requeue.

### 6. Tests Required

- card review service coverage for `sessionAgainCount`, weak marking, and `GOOD/EASY` weak clearing
- note review service coverage for `sessionAgainCount`, weak marking, and `GOOD/EASY` weak clearing
- weak-item controller/service coverage for summary, paged list, and dismiss behavior
- migration verification for new weak-state columns and constraints

### 7. Wrong vs Correct

#### Wrong

- treat same-day recovery queue as persisted backend state
- mark items weak on first `AGAIN` without session context
- overload review logs to act as the weak-item read model

#### Correct

- keep same-day recovery as frontend session state and persist only long-term weak state
- use `sessionAgainCount` to distinguish first and second `AGAIN`
- expose weak-item summary/list/dismiss through dedicated read endpoints
- FSRS scheduler coverage for initial state and mastery bucketing
- CRUD, import preview/apply, review, and dashboard controller/service coverage
- migration assertions for note status/rating constraints and note-review foreign key behavior

### 7. Wrong vs Correct

#### Wrong

- store imported Markdown as a required parent document entity
- surface empty structural headings as editable note rows
- reuse the word-study runtime tables for note review

#### Correct

- treat imported output as first-class notes
- keep preview focused on real editable knowledge points
- keep note scheduling and review logs in dedicated note tables

---

## Scenario: Template preview and export delivery

### 1. Scope / Trigger

- Trigger: change touches template CRUD/preview, export generation/download, or shared rendering context.
- Packages: `com.jp.vocab.template`, `com.jp.vocab.exportjob`.

### 2. Signatures

- `GET /api/templates/anki`
- `POST /api/templates/anki`
- `PUT /api/templates/anki/{id}`
- `POST /api/templates/anki/preview`
- `GET /api/templates/md`
- `POST /api/templates/md`
- `PUT /api/templates/md/{id}`
- `POST /api/templates/md/preview`
- `GET /api/export-jobs`
- `POST /api/export-jobs/preflight`
- `POST /api/export-jobs`
- `GET /api/export-jobs/{id}/download`

### 3. Contracts

- Preview routes are request-body driven and do not require template IDs.
- Anki preview request carries template strings plus one `sample` card context.
- Anki preview response returns:
  - `frontRendered`
  - `backRendered`
  - `cssRendered`
- Markdown preview request carries:
  - `templateContent`
  - `date`
  - `planName`
  - `newCards[]`
  - `reviewCards[]`
- Markdown preview response returns `renderedContent`.
- Export create supports:
  - `ANKI_CSV`
  - `ANKI_TSV`
  - `MARKDOWN`
- Export preflight validates plan ownership, export type, target-date card availability, and Markdown template syntax.
- Export preflight returns:
  - `planId`
  - `planName`
  - `exportType`
  - `targetDate`
  - `totalCards`
  - `newCards`
  - `reviewCards`
  - `markdownTemplateName`
  - `creatable`
  - `message`
- Export create reuses the preflight checks and must reject empty target-date exports before writing a file.
- Download serves the generated file for a completed export job.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| unsupported template variable or syntax error | map to standardized preview error response |
| missing export target plan or date | reject before job creation |
| unsupported export type | reject with validation or business error |
| target date has no exportable cards | reject with `CONFLICT`, do not create a file or job |
| Markdown template is missing or invalid | reject before job creation |
| download before file generation is ready | reject with standard error envelope |

### 5. Good / Base / Bad Cases

- Good: save template, preview with sample data, run export preflight, create export job, list it, then download the generated file.
- Base: refresh template lists and reuse them immediately in study-plan selectors.
- Bad: create an empty export file for a date with no due cards, couple preview to stored template IDs only, or bypass export-job persistence when generating files.

### 6. Tests Required

- template create/update/preview controller coverage
- preview rendering error-path coverage
- export preflight coverage for card counts and Markdown template validation
- export create/list/download coverage, including empty-date rejection before save
- file-name and content-type assertions for each export type

### 7. Wrong vs Correct

#### Wrong

- require database persistence before every preview
- invent a preview-only variable shape that diverges from export rendering
- generate files before checking whether the target date has exportable cards
- stream ad hoc files without recording export-job state

#### Correct

- keep preview request-body driven
- share rendering expectations between preview and export
- run preflight before file generation and reuse the same checks in create
- keep export generation and download anchored to `export_job`

---

## Scenario: Long-term learning metrics

### 1. Scope / Trigger

- Trigger: change touches `/api/dashboard/long-term`, long-range review analytics, dashboard aggregation, or future workload forecast.
- Packages: `com.jp.vocab.dashboard`, `com.jp.vocab.note`, `com.jp.vocab.card`.

### 2. Signatures

- `GET /api/dashboard/long-term?date=YYYY-MM-DD&rangeDays=90`

### 3. Contracts

- The endpoint is read-only and returns `ApiResponse<LongTermDashboardResponse>`.
- `date` defaults to `LocalDate.now()` when omitted.
- `rangeDays` defaults to `90` and is constrained to `30..180`.
- Response contains:
  - `summary`
  - `trend`
  - `loadForecast`
- `summary` includes:
  - current streak days
  - longest streak days
  - last 7-day total, word, and note review counts
  - last 30-day total, word, and note review counts
- `trend` is daily and contains word, note, and total review counts.
- `loadForecast` contains next 7 / 14 / 30 day buckets with word, note, and total due counts.
- Learning day means either word review logs or note review logs exist for that date.
- Word trend reads `review_log` through `card_instance -> study_plan`.
- Note trend reads `note_review_log` through `note`.
- Word future load only counts `ACTIVE` plans and `PENDING` card instances.
- Note future load counts current user's due notes.
- The first version must not create a materialized statistics table.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| `rangeDays < 30` or `rangeDays > 180` | reject with validation error |
| omitted date | use current local date |
| no review history | return zero summary and zero-filled trend |
| no future due items | return zero load buckets |

### 5. Good / Base / Bad Cases

- Good: a learner with word and note reviews sees streaks, 90-day split trend, and future workload buckets.
- Base: a new learner sees zero values instead of API errors.
- Bad: derive long-term metrics from frontend-only recent trend, or count another user's review logs.

### 6. Tests Required

- service coverage for streak calculation, last 7 / 30 day totals, and future load buckets
- controller coverage for daily and long-term route delegation
- frontend build/type-check for the long-term dashboard DTO contract

### 7. Wrong vs Correct

#### Wrong

- add a persistent analytics table before query performance requires it
- fold long-term fields into the existing daily `/api/dashboard` response
- count archived/deleted user data outside the current user's ownership boundary

#### Correct

- keep the long-term endpoint separate from the daily dashboard endpoint
- derive MVP metrics from existing review logs and due runtime rows
- keep failure isolated so long-term query issues do not block today's workbench data

---

## Migration Note

The retired OpenSpec work is now treated as Trellis-owned knowledge.

The main remaining debt from those original task lists is test coverage and regression verification, not missing feature scope.
