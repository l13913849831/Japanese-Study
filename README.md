# Japanese Vocabulary Phase-One Workspace

This repository now contains the bootstrap skeleton for the first-phase Japanese vocabulary system described in `docs/`.

## Workspace layout

- `backend/`: Spring Boot 3 + Java 21 service foundation
- `frontend/`: React 18 + TypeScript + Vite application shell
- `docs/`: product, API, database, and MVP planning references
- `.trellis/`: active workflow, spec index, and task tracking

## Phase-one module boundaries

### Backend modules

- `wordset`: word sets and word entries
- `studyplan`: study plan management
- `card`: today cards and calendar queries
- `template`: Anki/Markdown template access
- `exportjob`: export job queries
- `shared`: API envelope, validation, exceptions, config, mapping conventions

### Frontend modules

- `features/word-sets`
- `features/study-plans`
- `features/cards`
- `features/templates`
- `features/export-jobs`
- `shared`
- `app`

## Local development

### 1. Start PostgreSQL

Use the bundled Docker Compose file:

```bash
docker compose up -d db
```

Default connection:

- host: `localhost`
- port: `5432`
- database: `jp_vocab`
- username: `jp`
- password: `jp`

### 2. Start backend

Requirements:

- Java 21
- Maven 3.9+ or Maven Wrapper

Commands:

```bash
cd backend
mvn spring-boot:run
```

Environment variables:

- `JP_DB_URL` default: `jdbc:postgresql://localhost:5432/jp_vocab`
- `JP_DB_USERNAME` default: `jp`
- `JP_DB_PASSWORD` default: `jp`
- `APP_CORS_ALLOWED_ORIGINS` default: `http://localhost:5173`

Backend responsibilities:

- run Flyway `V1`-`V5`
- expose `/api` endpoints
- return unified `success/data/error/timestamp` envelopes

### 3. Start frontend

Requirements:

- Node.js 22+
- npm 10+

Commands:

```bash
cd frontend
npm install
npm run dev
```

Default frontend URL: `http://localhost:5173`

Frontend dev proxy forwards `/api` requests to `http://localhost:8080`.

## Verification checklist

- backend can boot with PostgreSQL and execute Flyway migrations
- frontend can start with Vite and render the phase-one navigation shell
- `/api/health` responds successfully
- frontend `/api` requests resolve through the configured Vite proxy

## DTO / VO naming conventions

- database layer stays aligned with Flyway snake_case table/column names
- JPA entities use camelCase fields with explicit `@Table` / `@Column(name = "...")` mapping
- API request/response models use camelCase names aligned with `docs/api-specification.md`
- JSON-like database columns (`tags`, `reviewOffsets`, `fieldMapping`) are represented as structured Java collections

## Suggested next implementation steps

1. Add automated tests for import preview/apply, study-plan runtime, review, dashboard, template preview, and export download
2. Expand dashboard metrics such as streaks, stage distribution, and longer-range trends
3. Add clearer export creation guidance, including template-selection expectations and preview linkage
4. Keep Trellis specs in sync whenever the current feature surface changes
