# DPDMS frontend

Angular 22 (TypeScript) app for the Digital Project Delivery Management System. It talks to the Spring Boot backend in `../backend` through a JSON API under `/api`.

Phase 1: sign-in (with the emailed code for administrators and the first-sign-in password change), client self-registration with email confirmation, the signed-in shell with a role-based menu, notifications, and the administrator pages **Users and access** and **Audit trail**.

Phase 2 (against `../docs/api.md`): requests and quotes, projects with overall and per-phase progress, a task board, assignment, submit and review, approvals, change requests, time logs, a per-project updates feed, administrator grant or revoke of a developer's project access, and a dashboard with counts. 

Stage 3 (against `../docs/api-stage3.md`): **invoices and payments** (list with filters and totals, draft editor with live totals and a time-based suggestion, send, record and reverse payments, void, reminders, CSV export, a Money tab on each project, and a client view with balance due and payment history), **renewals** (status chips, filters, history, add, edit, renew, delete, client request, and an administrator button that runs the reminder job with a pretend date), **support tickets** (client raises tickets, answer-deadline countdown and badge, comment thread with staff-only internal notes, status actions by role, assignment and priority, administrator SLA check) and **reports** (management only: summary cards, revenue-by-month chart with a table alternative, tables, CSV). Developers never see money: no Invoices or Reports in their menu, and the routes are guarded.

## Run it

You need Node 20 or newer (built with Node 24) and the backend running on `http://localhost:8080`.

```bash
npm install
npm start
```

Open <http://localhost:4200>. `ng serve` forwards every `/api` call to `http://localhost:8080` (see `proxy.conf.json`), so the browser never needs CORS.

To try the app without installing PostgreSQL, start the backend with its embedded database: `java -jar dpdms-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=embedded` (see the backend README).

First administrator (created by the backend on first start): `admin@itzetech.local` / `ChangeMe#2026`. You must choose a new password, and then enter the code the backend emails. With no mail server configured the backend logs the code and, in development, returns it as `devCode`; the app shows it in a box labelled **Development only**.

## Commands

| Command | What it does |
| --- | --- |
| `npm start` | Dev server on port 4200 with the `/api` proxy |
| `npm run build` | Production build into `dist/frontend` |
| `npm test` | Unit tests (Vitest through the Angular CLI). Add `-- --no-watch` for a single run |

## Pointing at another API

- Development: edit the `target` in `proxy.conf.json`.
- Production: `src/environments/environment.prod.ts` sets `apiUrl` (default `/api`, meaning the API is served from the same host through a reverse proxy). Use a full URL such as `https://api.example.com/api` if the API lives elsewhere; then the backend must allow your site's origin in its CORS settings.

## Structure

```
src/
  environments/            apiUrl for development and production
  styles.scss              all shared styles (tokens, cards, tables, drawer, forms), ported from the prototype
  app/
    app.config.ts          providers: router, HttpClient with the auth interceptor
    app.routes.ts          lazy routes and guards
    core/
      models.ts            types that mirror the backend DTOs
      session.service.ts   token + user (signals), kept in memory and in sessionStorage
      auth.service.ts      sign-in steps, register, confirm email, me, logout
      auth.interceptor.ts  adds the Bearer token; a 401 clears the session and goes to /login
      guards.ts            authGuard, guestGuard, roleGuard (roles come from route data)
      nav.ts               menu per role
      requests.service.ts, projects.service.ts, dashboard.service.ts, staff.service.ts
      notifications.service.ts, admin.service.ts, toast.service.ts
      api-error.ts         turns errors into {code, message, fields}; FRIENDLY holds the wording for each error code
      format.ts            money, dates, status labels, invoice arithmetic and deadline wording
      permissions.ts       who may do what (money, reports, invoice buttons, ticket actions); used by screens and guards
      invoices.service.ts, renewals.service.ts, tickets.service.ts, reports.service.ts, download.ts (CSV save)
    layout/shell.component.ts   left menu, bell with unread badge and panel, user chip
    pages/auth/            login (password, code and new-password steps), register, verify-email
    pages/dashboard, pages/notifications, pages/admin (users, audit)
    pages/requests         list (+ new request for clients) and detail (quote form for managers, answers for clients)
    pages/projects         list, detail with tabs (tasks, approvals, change requests, time, updates, developer access)
    pages/tasks            my tasks (developers and project managers)
    pages/invoices         list, detail (lines, payments, history, dialogs) and the draft editor
    pages/renewals         list (+ reminder job), detail with history, add/edit form
    pages/tickets          list (+ new ticket, SLA check), detail with thread and actions, SLA badge
    pages/reports          summary, revenue chart, tables
```

## Conventions

- Standalone components, signals for state, functional guards and interceptors, lazy-loaded routes, typed `HttpClient` calls. No UI library.
- Every screen reads backend errors through `toApiError()`: field problems come as `{ code: 'VALIDATION_FAILED', fields: {...} }` and everything else as `{ code, message }`. Codes listed in `FRIENDLY` (`PROJECT_NOT_STARTED`, `CODE_TOO_SOON`, `QUOTE_EXPIRED` ...) get wording that says what to do next; to change a message, edit that table. Show `message` as is.
- Role differences are decided by the API (developers never receive budget fields, clients receive a simplified task) and mirrored in the screens, which only show what the data contains.
- Route ids (`/projects/:id`, `/requests/:id`) arrive as component inputs (`withComponentInputBinding`).
- Roles: `ADMIN`, `PROJECT_MANAGER`, `DEVELOPER`, `CLIENT`. The menu for each role is in `core/nav.ts`; restrict a route by adding `canActivate: [roleGuard], data: { roles: ['ADMIN'] }`. The backend enforces the same rules, so the guards are only for a clean experience.
- Light and dark follow the device setting; the sign-in pages use one flat dark colour like the prototype.
- UI text is plain English.

## Adding a screen for a new module

1. Add the types to `core/models.ts` and a small service next to `admin.service.ts`.
2. Add the page under `pages/<module>/` and a lazy route in `app.routes.ts` (inside the shell's `children`).
3. Add the menu item to `core/nav.ts` for the roles that may see it.
4. Add a unit test next to anything with logic.
