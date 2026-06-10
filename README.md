# Japanese Vocabulary Study Workspace

This repository is now a local-account Japanese study MVP rather than an early bootstrap skeleton.

Current implemented surface:

- word-study flow: word sets, study plans, `/cards`, review history, dashboard
- note-study flow: notes CRUD, Markdown preview import, `/notes/review`, note dashboard
- account flow: register, login, logout, `/account`, session-cookie auth
- support flows: weak items, template preview/edit, export jobs, account backup and restore

## Workspace layout

- `backend/`: Spring Boot 3 + Java 21 + PostgreSQL + Flyway
- `frontend/`: React 18 + TypeScript + Vite + Ant Design
- `docs/`: usage guide, API contracts, backlog and modeling notes
- `.trellis/`: workflow, task tracking and project specs

## Main modules

Backend:

- `user`
- `wordset`
- `studyplan`
- `card`
- `dashboard`
- `note`
- `weakitem`
- `template`
- `exportjob`
- `backup`
- `shared`

Frontend:

- `features/auth`
- `features/dashboard`
- `features/word-sets`
- `features/study-plans`
- `features/cards`
- `features/notes`
- `features/weak-items`
- `features/templates`
- `features/backups`
- `features/export-jobs`
- `shared`
- `app`

## Local development

### 1. Start PostgreSQL

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
- Maven 3.9+

Commands:

```bash
cd backend
mvn spring-boot:run
```

Important environment variables:

- `JP_DB_URL`
- `JP_DB_USERNAME`
- `JP_DB_PASSWORD`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_AUTH_BOOTSTRAP_ENABLED`
- `APP_SESSION_COOKIE_SECURE`
- `APP_SESSION_COOKIE_SAME_SITE`
- `APP_EXPORT_BASE_DIR`
- `APP_BACKUP_BASE_DIR`

Security baseline:

- login state uses session cookie
- write requests require CSRF token
- bootstrap demo account is disabled by default and must be explicitly enabled

Backend currently runs Flyway `V1` to `V11`.

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

The frontend proxies `/api` to `http://localhost:8080`.

## Verification checklist

- backend boots and applies Flyway migrations
- frontend builds and renders the route shell
- `/api/health` responds successfully
- login/register works with session cookie + CSRF flow
- `/dashboard`, `/cards`, `/notes/review`, `/backups` can be reached after login

## Source-of-truth docs

- `docs/system-usage-guide.md`
- `docs/api-specification.md`
- `docs/open-items.md`
- `.trellis/spec/backend/current-feature-contracts.md`
- `.trellis/spec/frontend/current-feature-workflows.md`
