# DPDMS REST API (phase 1 + phase 2)

Backend: Spring Boot, base path `/api`. This file lists **every** endpoint of stages 1 and 2. Stage 3 (invoices and payments, renewals, support tickets, reports) is in [api-stage3.md](api-stage3.md).

- [Conventions](#conventions)
- [Sign-in and accounts](#1-sign-in-and-accounts) · [Notifications](#2-notifications) · [Administrator](#3-administrator)
- [Requests and quotes](#4-requests-and-quotes)
- [Projects](#5-projects) · [Tasks](#6-tasks) · [Time logs](#7-time-logs) · [Updates feed](#8-updates-feed)
- [Approvals](#9-approvals) · [Change requests](#10-change-requests)
- [Who sees what](#who-sees-what)
- [Error codes](#error-codes)

---

## Conventions

| Topic | Rule |
|---|---|
| Format | JSON in and out (`Content-Type: application/json`). |
| Sign-in | `Authorization: Bearer <token>` on every call except the public `/api/auth/*` ones. The token comes from the sign-in flow ([section 1](#1-sign-in-and-accounts)). |
| Ids | UUID strings. |
| Dates | `YYYY-MM-DD` (for example `2026-12-31`). Moments (`createdAt` and so on) are ISO-8601 UTC, for example `2026-11-04T09:30:00.123456Z`. |
| Money | Whole Rwandan francs (RWF) as JSON integers. Only the amounts a person may see are sent (see [Who sees what](#who-sees-what)). |
| Hours | Numbers with up to 2 decimals (`2.5`). Planned hours are whole numbers. |
| Missing fields | Some responses (`ProjectDto`, `TaskDto`) **leave out** a field the caller may not see, or that has no value, instead of sending `null`. The tables say "omitted". Other responses always send every field (`null` when empty). |
| Not allowed vs. not found | If you are not allowed to know a project, request or approval exists (another company's data, a developer's project with no task for them), the answer is **404 NOT_FOUND**. If you can see it but may not do the action, the answer is **403**. |
| Statuses | Words in `UPPER_CASE` below are exact values. |

### Error body

Every error from the application has this shape:

```json
{ "code": "INVALID_TRANSITION", "message": "A readable sentence to show the person.", "fields": { "title": "Enter the task name." } }
```

- `code`: stable text the frontend can react to (full list [below](#error-codes)).
- `message`: safe to show on screen.
- `fields` (only with `VALIDATION_FAILED`): field name to message, for showing next to form fields.

Answers produced by the security layer itself have **no body**: **401** (missing, expired or wrong token, or the account was deactivated) and **403** on `/api/admin/**` for a person who is not an administrator. A 401 means "sign in again".

HTTP statuses: 200 OK, 201 created, 202 accepted (no body), 400 bad input, 401 not signed in, 403 not allowed, 404 not found, 405 wrong method, 409 not possible in the current state, 423 account locked, 429 too many requests.

### Roles

`ADMIN`, `PROJECT_MANAGER`, `DEVELOPER`, `CLIENT`. "Management" below means `ADMIN` and `PROJECT_MANAGER`. "Staff" means every role except `CLIENT`.

---

## 1. Sign-in and accounts

Public (no token): `login`, `verify-code`, `change-password`, `register`, `verify-email`, `resend-code`. `me` needs a token.

`AuthResult`, the answer to the sign-in steps:

```json
{ "status": "AUTHENTICATED", "token": "eyJ...", "user": { "...UserDto..." } }
```

| `status` | Extra fields | What the frontend does next |
|---|---|---|
| `AUTHENTICATED` | `token`, `user` | Store the token, go to the app. |
| `CODE_REQUIRED` | `challenge`, `devCode?` | Administrators: ask for the 6-digit code that was e-mailed, then call `verify-code` with the `challenge`. |
| `PASSWORD_CHANGE_REQUIRED` | `challenge` | Invited staff on first sign-in: ask for a new password, then call `change-password`. |
| `VERIFICATION_REQUIRED` | `email`, `devCode?` | New client: ask for the e-mailed code, then call `verify-email`. |

`devCode` is only present in development (`app.expose-secrets=true`); never in production. Fields that do not apply are omitted. The `challenge` is a short-lived token that only works for the next step; it is **not** an access token.

`UserDto`:

```json
{
  "id": "uuid", "email": "jean@acme.example", "fullName": "Jean Bosco",
  "role": "CLIENT", "roleLabel": "Client", "status": "ACTIVE",
  "companyId": "uuid | null", "companyName": "Acme Hotels | null",
  "selfRegistered": true, "lastLoginAt": "instant | null", "createdAt": "instant"
}
```

`status` is `PENDING_VERIFICATION`, `INVITED`, `ACTIVE` or `DEACTIVATED`.

### POST /api/auth/login
Public. Body `{ "email": "...", "password": "..." }`. Returns `AuthResult`.
Staff with a temporary password get `PASSWORD_CHANGE_REQUIRED`; administrators get `CODE_REQUIRED` (password first, then an e-mailed code); everyone else gets `AUTHENTICATED`.
Errors: 400 `VALIDATION_FAILED`; 401 `BAD_CREDENTIALS` (the same answer for an unknown email); 423 `ACCOUNT_LOCKED` (5 wrong passwords lock the account for a few minutes); 403 `ACCOUNT_DEACTIVATED`; 403 `EMAIL_NOT_VERIFIED`; 429 `CODE_TOO_SOON` (an administrator signing in again within 60 seconds of the last e-mailed code; use the code already sent).

### POST /api/auth/verify-code
Public. Body `{ "challenge": "...", "code": "123456" }`. Returns `AuthResult` (`AUTHENTICATED`).
Errors: 400 `CODE_INVALID` / `CODE_EXPIRED` / `CODE_LOCKED`; 401 `CHALLENGE_INVALID` (expired step, sign in again).

### POST /api/auth/change-password
Public. Body `{ "challenge": "...", "newPassword": "..." }`. The password needs at least 8 characters with letters and a number. Returns `AuthResult` (`AUTHENTICATED`, or `CODE_REQUIRED` for an administrator).
Errors: 400 `WEAK_PASSWORD`, `SAME_PASSWORD`, `NOT_INVITED`, `VALIDATION_FAILED`; 401 `CHALLENGE_INVALID`.

### POST /api/auth/register
Public. A client creates their own account. Body:

```json
{ "companyName": "Acme Hotels", "fullName": "Jean Bosco", "email": "jean@acme.example", "phone": "0788000000", "password": "Sunrise2026", "acceptTerms": true }
```

`phone` is optional. Returns **201** `AuthResult` with `status: "VERIFICATION_REQUIRED"` and the e-mail address. Administrators are notified.
Errors: 400 `VALIDATION_FAILED` (with `fields`), `WEAK_PASSWORD`; 409 `EMAIL_TAKEN`.

### POST /api/auth/verify-email
Public. Body `{ "email": "...", "code": "123456" }`. Returns `AuthResult` (`AUTHENTICATED`, role `CLIENT`).
Errors: 400 `CODE_INVALID`, `CODE_EXPIRED`, `CODE_LOCKED`, `ALREADY_VERIFIED`.

### POST /api/auth/resend-code
Public. Body `{ "email": "..." }`. Sends a new confirmation code to a client who has not confirmed yet (the old code stops working). Always **202**, so it never reveals whether an address is known: an unknown or already confirmed address gets the same answer.
- Normally the body is empty.
- In development (`app.expose-secrets=true`) and when a code was actually sent, the body is `{ "devCode": "123456" }` so the dev UI can show it.

Errors: 400 `VALIDATION_FAILED` (not an e-mail address); 429 `CODE_TOO_SOON` (a code was sent less than 60 seconds ago; the earlier code still works).

### GET /api/auth/me
Any signed-in person. Returns `UserDto`. Use it to restore the session from a stored token (401 means sign in again).

---

## 2. Notifications

Every signed-in person has their own list. `link` is a path inside the Angular app, for example `/projects/<id>` or `/requests/<id>`; it may be `null`.

`NotificationList`: `{ "unread": 3, "items": [ { "id": "uuid", "message": "text", "link": "/projects/uuid", "createdAt": "instant", "read": false } ] }` (newest first, at most 50).

| Method and path | Who | What |
|---|---|---|
| `GET /api/notifications` | everyone | Returns `NotificationList`. |
| `POST /api/notifications/{id}/read` | everyone (own only) | Marks one as read. Returns the notification. 404 `NOT_FOUND` for someone else's. |
| `POST /api/notifications/read-all` | everyone | Marks all of your notifications as read. Returns `NotificationList`. |

---

## 3. Administrator

All of `/api/admin/**` is for `ADMIN` only (anyone else: 403 with no body, no token: 401). The one exception in this section is `GET /api/staff`, which project managers may also call.

### GET /api/admin/users
Query `role` (optional, one of the four roles) and `q` (optional search in name, email, company). Returns a list of `UserDto`, newest first.

### POST /api/admin/users
Creates a staff account. Body `{ "fullName": "...", "email": "...", "role": "DEVELOPER" }` (`role` is `ADMIN`, `PROJECT_MANAGER` or `DEVELOPER`). Returns **201**:

```json
{ "user": { "...UserDto..." , "status": "INVITED" }, "temporaryPassword": "Tmp-abCd23" }
```

The temporary password is also e-mailed; the person must choose a new one at first sign-in.
Errors: 400 `VALIDATION_FAILED`, `ROLE_NOT_STAFF` (clients create their own accounts); 409 `EMAIL_TAKEN`.

### PATCH /api/admin/users/{id}/role
Body `{ "role": "PROJECT_MANAGER" }`. Staff roles only. Returns `UserDto`.
Errors: 400 `ROLE_NOT_STAFF`, `SELF_NOT_ALLOWED`, `LAST_ADMIN`; 404.

### POST /api/admin/users/{id}/deactivate and POST /api/admin/users/{id}/reactivate
Body `{}`. Returns `UserDto`. A deactivated person's token stops working at once.
Errors: 400 `SELF_NOT_ALLOWED`, `LAST_ADMIN`; 404.

### POST /api/admin/users/{id}/reset-password
Body `{}`. Returns `{ "user": UserDto (status INVITED), "temporaryPassword": "..." }`.
Errors: 400 `CANNOT_RESET` (account not active), `SELF_NOT_ALLOWED`; 404.

### GET /api/staff
Not under `/api/admin`: readable by `ADMIN` and `PROJECT_MANAGER` only (developers and clients: 403 `FORBIDDEN`). A short list for choosing who to give a task to. Query `role` is optional (`ADMIN`, `PROJECT_MANAGER` or `DEVELOPER`). Only active staff are returned: clients, deactivated people and invited people who have not signed in yet are left out. Sorted by name. Returns:

```json
[ { "id": "uuid", "fullName": "Dev Name", "email": "dev@itzetech.example", "role": "DEVELOPER" } ]
```

Errors: 400 `ROLE_NOT_STAFF` (`role=CLIENT`), 400 `BAD_REQUEST` (unknown role); 401; 403.

### GET /api/admin/audit
Query `q` (optional text) and `limit` (1 to 500, default 100). Newest first. Returns:

```json
[ { "id": "uuid", "occurredAt": "instant", "actorName": "Jane Manager", "action": "Quote sent", "subject": "Booking website · 1,500,000 RWF", "ref": "9f2c01ab" } ]
```

Actions recorded in phase 2: Request sent, Quote sent, Quote revised, Quote accepted, Quote changes requested, Quote declined, Project started, Project launched, Task added, Task assigned, Task reassigned, Task moved, Task submitted for review, Task sent back, Task marked done, Time logged, Approval requested, Approval given, Changes requested on approval, Change request sent, Change request accepted, Change request declined, Project access granted, Project access removed.

### GET /api/admin/projects/{projectId}/access
Developers the administrator has given extra access to this project (people who only have a task on it are not listed). Returns a list of `AccessDto`:

```json
[ { "userId": "uuid", "fullName": "Dev Name", "email": "dev@itzetech.example", "grantedAt": "instant" } ]
```

404 if the project does not exist.

### POST /api/admin/projects/{projectId}/access
Body `{ "userId": "uuid" }`. Lets an active developer see the project (read it, its updates feed and time summary) without having a task on it. Granting twice changes nothing. The developer is notified. Returns the full `AccessDto` list.
Errors: 400 `VALIDATION_FAILED`, `NOT_A_DEVELOPER`; 404 (project or person).

### DELETE /api/admin/projects/{projectId}/access/{userId}
Removes the extra access (a developer who still has a task on the project keeps seeing it). Returns the remaining `AccessDto` list. 404 if that developer had no such access.

---

## 4. Requests and quotes

A **request** belongs to a client company. Status flow:

```
NEW ──quote──▶ QUOTED ──client accepts──▶ ACCEPTED (project created)
                 │  ▲
   client asks   │  └── manager revises the quote
   for changes   ▼
        CHANGES_REQUESTED          QUOTED / CHANGES_REQUESTED ──client declines──▶ DECLINED
```

`RequestDto` (every field always present):

```json
{
  "id": "uuid", "companyId": "uuid", "companyName": "Acme Hotels",
  "title": "Booking website", "description": "We need ...", "budget": "RWF 3,000,000",
  "deadline": "2027-01-15",
  "status": "QUOTED",
  "changeNote": "text the client wrote when asking for changes, or the decline reason | null",
  "quote": {
    "items": [ { "description": "Design", "amount": 500000 }, { "description": "Build", "amount": 1000000 } ],
    "total": 1500000, "weeks": 8, "validUntil": "2026-11-18",
    "sentAt": "instant", "quotedByName": "Jane Manager"
  },
  "projectId": "uuid | null (set once accepted)",
  "createdByName": "Jean Bosco", "createdAt": "instant", "updatedAt": "instant"
}
```

`quote` is `null` until the first quote is sent. `budget` is the client's own free-text idea of the budget (optional).

### GET /api/requests
Management: all requests. Client: their own company's. Developer: 403 `FORBIDDEN`. Query `status` (optional) filters (invalid value: 400 `BAD_REQUEST`). Newest first. Returns a list of `RequestDto`.

### POST /api/requests
Client only. Body:

```json
{ "title": "Booking website", "description": "We need ...", "budget": "RWF 3,000,000", "deadline": "2027-01-15" }
```

`budget` is optional; `deadline` must be after today. Returns **201** `RequestDto` (status `NEW`). Project managers and administrators are notified.
Errors: 400 `VALIDATION_FAILED`, `DEADLINE_PAST`; 403 `CLIENT_ONLY`.

### GET /api/requests/{id}
Management, or a client of the owning company. Another company's client: 404. Developer: 403.

### POST /api/requests/{id}/quote
Management only. Writes the quote, or revises it. Allowed while the request is `NEW`, `QUOTED` or `CHANGES_REQUESTED`. Body:

```json
{ "items": [ { "description": "Design", "amount": 500000 }, { "description": "Build", "amount": 1000000 } ], "weeks": 8, "validUntil": "2026-11-18" }
```

1 to 30 lines, each amount above zero; `weeks` 1 to 104; `validUntil` today or later. The status becomes `QUOTED`, the old change note is cleared and the client's company is notified. Returns `RequestDto`.
Errors: 400 `VALIDATION_FAILED`, `VALID_UNTIL_PAST`; 403 `FORBIDDEN`; 404; 409 `INVALID_STATE` (already `ACCEPTED` or `DECLINED`).

### POST /api/requests/{id}/accept
Client of the owning company only. Body `{}`. The quote must be `QUOTED` and still valid. Creates the project (see [Projects](#5-projects)): status `PLANNING`, budget = quote total, four starter tasks, manager = the person who wrote the quote (if an active project manager or administrator, otherwise the oldest active project manager). Project managers and administrators are notified. Returns `RequestDto` with `status: "ACCEPTED"` and `projectId`.
Errors: 403 `CLIENT_ONLY`; 404; 409 `INVALID_STATE` (no quote yet, or already answered), `QUOTE_EXPIRED` (past `validUntil`; the manager must send a new quote), `NO_MANAGER` (no active project manager or administrator exists).

### POST /api/requests/{id}/request-changes
Client only. Body `{ "note": "Please add a mobile app" }` (required, up to 1000 characters). Status becomes `CHANGES_REQUESTED`; the quoter and project managers are notified. Returns `RequestDto`.
Errors: 400 `VALIDATION_FAILED`; 403 `CLIENT_ONLY`; 404; 409 `INVALID_STATE` (only a `QUOTED` request can be changed).

### POST /api/requests/{id}/decline
Client only. Body `{}` or `{ "reason": "Over our budget" }` (optional). Allowed only while `QUOTED`. Status becomes `DECLINED`. Returns `RequestDto`.
Errors: 403 `CLIENT_ONLY`; 404; 409 `INVALID_STATE`.

---

## 5. Projects

`Project.status` is `PLANNING` (just created, not started), `IN_PROGRESS`, `IN_REVIEW` (every task is handed in or done, at least one waiting for review) or `LAUNCHED` (every task done). After the project has started the status follows the tasks automatically; only `start` is a manual step.

**Progress** = `round(100 × planned hours of DONE tasks ÷ total planned hours)`, 0 when there are no tasks. Each of the four **phases** (`0 Discovery`, `1 Design`, `2 Build`, `3 Test and launch`) has the same percentage over its own tasks, or `null` when it has no tasks. A task is **late** when it is not `DONE` and its due date is before today.

`ProjectDto`. Fields marked * are omitted for some people (see [Who sees what](#who-sees-what)):

```json
{
  "id": "uuid", "requestId": "uuid *", "name": "Booking website", "status": "IN_PROGRESS",
  "companyId": "uuid", "companyName": "Acme Hotels",
  "managerId": "uuid", "managerName": "Jane Manager",
  "startDate": "2026-11-04", "dueDate": "2027-01-15",
  "budget": 1900000,
  "progress": 19,
  "phases": [ { "index": 0, "name": "Discovery", "progress": 0, "taskCount": 1 }, { "index": 1, "name": "Design", "progress": 100, "taskCount": 1 }, { "index": 2, "name": "Build", "progress": 0, "taskCount": 1 }, { "index": 3, "name": "Test and launch", "progress": null, "taskCount": 0 } ],
  "plannedHours": 104, "loggedHours": 11.75,
  "lateTasks": 1, "openApprovals": 2,
  "tasks": [ "...TaskDto, only on GET /api/projects/{id}..." ],
  "createdAt": "instant"
}
```

| Field | Client | Developer | Management |
|---|---|---|---|
| `budget` (quote total plus accepted change requests) | yes | **omitted** | yes |
| `requestId`, `plannedHours`, `loggedHours` | omitted | yes | yes |
| `openApprovals` (answers still waiting) | yes | **omitted** | yes |
| `tasks` | safe list (below) | full | full |

`tasks` only appears on `GET /api/projects/{id}`, not in the list.

### GET /api/projects
The projects you may see, newest first (no `tasks`).
- Management: all projects.
- Client: their own company's.
- Developer: projects where a task is assigned to them, plus those the administrator gave them access to.

### GET /api/projects/{id}
One project with its `tasks`. 404 if you may not see it.

### POST /api/projects/{id}/start
Management only. Body `{}`. `PLANNING` to `IN_PROGRESS`. Task moves, submissions and time logs are only possible after this. The client and everybody with an assigned task are notified, and the updates feed gets "Work has started on your project." Returns `ProjectDto` with `tasks`.
Errors: 403; 404; 409 `ALREADY_STARTED`.

---

## 6. Tasks

`TaskDto` (a field with no value is omitted):

```json
{
  "id": "uuid", "phase": 1, "title": "Design", "status": "REVIEW",
  "dueDate": "2026-11-14", "late": false,
  "plannedHours": 20, "assigneeId": "uuid", "assigneeName": "Dev Name", "loggedHours": 3.75,
  "submissionFile": "https://files.example/work.zip", "submissionNote": "Ready for review",
  "submittedAt": "instant", "submittedByName": "Dev Name",
  "sentBackNote": "Mobile layout is missing", "sentBackAt": "instant"
}
```

`status` is `TODO`, `DOING`, `REVIEW` or `DONE`.

**A client only gets** `id`, `phase`, `title`, `status`, `dueDate` and `late`: no people, hours, files or comments.

A new project gets four tasks: **Kick-off** (phase 0, 8 h, assigned to the project manager), **Design** (1, 20 h), **Development** (2, 60 h), **Testing and launch** (3, 16 h), the last three unassigned. Due dates are spread from today: `today + 7 + i × max(7, round(days until the deadline ÷ 4))` days.

Status moves:

| Who | Allowed through `status` |
|---|---|
| Developer (own tasks only) | `TODO → DOING`, `DOING → TODO`, `REVIEW → DOING` (take back a submission) |
| Management (any task) | any step except to `REVIEW` (that needs `submit`) |
| Nobody | to `REVIEW` without a submission; a developer cannot set `DONE` |

`DOING → REVIEW` happens only through `submit`. `REVIEW → DONE` is `complete` (or the client approving a task-linked item). `REVIEW → DOING` by the manager is `send-back`.

Every call below needs the project to be visible to you (else 404). Clients get 403 on every task action.

### POST /api/projects/{id}/tasks
Management only. Adds a task. Body:

```json
{ "title": "Payment integration", "phase": 2, "plannedHours": 30, "dueDate": "2026-12-01", "assigneeId": "uuid (optional)" }
```

`phase` 0 to 3; `plannedHours` 1 to 2000; `dueDate` today or later; the assignee must be an active developer or project manager and is notified. Returns **201** `TaskDto`.
Errors: 400 `VALIDATION_FAILED`, `DUE_DATE_PAST`, `NOT_ASSIGNABLE`; 403; 404; 409 `PROJECT_LAUNCHED`.

### POST /api/projects/{id}/tasks/{taskId}/assign
Management only. Assigns or reassigns. Body `{ "assigneeId": "uuid", "dueDate": "2026-11-20", "note": "optional, up to 500 characters" }`. The new person is notified (with the note); a previous assignee is told the task was reassigned. The status is not changed. Works while the project is still in `PLANNING`. Returns `TaskDto`.
Errors: 400 `VALIDATION_FAILED`, `DUE_DATE_PAST`, `NOT_ASSIGNABLE` (not an active developer or project manager); 403; 404; 409 `TASK_DONE`.

### POST /api/projects/{id}/tasks/{taskId}/status
Body `{ "status": "DOING" }`. Developers: only their own task (403 `NOT_YOUR_TASK` otherwise), and only the steps in the table above. Returns `TaskDto`.
Errors: 400 `BAD_REQUEST` (unknown status); 403 `FORBIDDEN` (client), `NOT_YOUR_TASK`, `MANAGER_ONLY` (a developer asked for `DONE`); 404; 409 `PROJECT_NOT_STARTED`, `SUBMISSION_REQUIRED` (asked for `REVIEW`), `INVALID_TRANSITION`.

### POST /api/projects/{id}/tasks/{taskId}/submit
The task's developer (management may too). The task must be `DOING`. Body `{ "file": "https://... or file name", "note": "optional, up to 1000 characters" }`. Status becomes `REVIEW`; any earlier "sent back" comments are cleared; the project manager is notified. Returns `TaskDto`.
Errors: 400 `VALIDATION_FAILED` (file is required); 403 `FORBIDDEN`, `NOT_YOUR_TASK`; 404; 409 `PROJECT_NOT_STARTED`, `INVALID_TRANSITION` (not `DOING`).

### POST /api/projects/{id}/tasks/{taskId}/send-back
Management only. The task must be `REVIEW`. Body `{ "note": "what must change" }` (required). Status becomes `DOING`; the developer is notified with the comments. Returns `TaskDto` (with `sentBackNote`).
Errors: 400 `VALIDATION_FAILED`; 403; 404; 409 `INVALID_TRANSITION`.

### POST /api/projects/{id}/tasks/{taskId}/complete
Management only. Body `{}`. The task must be `REVIEW`. Status becomes `DONE`; the developer is notified; the feed gets "Finished: <task>"; project progress and status are recalculated (the last task done launches the project). Returns `TaskDto`.
Errors: 403; 404; 409 `INVALID_TRANSITION`.

### GET /api/tasks/mine
Staff. Tasks assigned to you, soonest due date first, all statuses. Clients: 403. Returns:

```json
[ { "task": { "...TaskDto..." }, "projectId": "uuid", "projectName": "Booking website", "projectStatus": "IN_PROGRESS" } ]
```

---

## 7. Time logs

A developer logs hours against **their own** task (the person it is assigned to; nobody else, managers included, can log on someone else's task). Needs a started project and a task that is not `DONE`. Clients: 403 on all of these.

### POST /api/projects/{id}/tasks/{taskId}/time
Body `{ "date": "2026-11-04", "hours": 2.5, "note": "optional, up to 500 characters" }`. `hours` above 0 and at most 24; `date` not in the future. Returns **201**:

```json
{ "entry": { "id": "uuid", "taskId": "uuid", "taskTitle": "Design", "userId": "uuid", "userName": "Dev Name", "date": "2026-11-04", "hours": 2.5, "note": "Wireframes", "createdAt": "instant" },
  "taskLoggedHours": 3.75, "taskPlannedHours": 20 }
```

Errors: 400 `VALIDATION_FAILED` (hours missing, zero, negative or above 24; date missing), `DATE_IN_FUTURE`; 403 `FORBIDDEN`, `NOT_YOUR_TASK`; 404; 409 `PROJECT_NOT_STARTED`, `TASK_DONE`.

### GET /api/projects/{id}/tasks/{taskId}/time
The entries on one task, newest first (list of the entry object above). Management: any task. Developer: own task only (403 `NOT_YOUR_TASK`).

### GET /api/projects/{id}/time
Planned against logged hours:

```json
{ "plannedHours": 104, "loggedHours": 11.75,
  "tasks": [ { "taskId": "uuid", "title": "Design", "assigneeId": "uuid", "assigneeName": "Dev Name", "plannedHours": 20, "loggedHours": 3.75 } ],
  "entries": [ { "...entry..." } ] }
```

Management: every task. Developer: only tasks assigned to them (the totals cover only those).

---

## 8. Updates feed

### GET /api/projects/{id}/updates
Plain-language lines everybody on the project can read, newest first: `[ { "id": "uuid", "message": "Finished: Design", "createdAt": "instant" } ]`.
Visible to: management, the client company, and developers who can see the project. Lines are added automatically:

| Event | Line |
|---|---|
| Quote accepted | Your request was accepted and your project has been set up. |
| Project started | Work has started on your project. |
| Task done | Finished: *task title* |
| Item sent for approval | Waiting for your approval: *item* (version *n*) |
| Item approved / changes asked | Approved: *item (version n)* / Changes requested: *item (version n)* |
| Change request sent / answered | Proposed change: *title* (*amount* RWF) / Change accepted: *title* / Change declined: *title* |
| All tasks done | All work is finished: your project has launched. |

---

## 9. Approvals

The project manager sends the client an item (design, page content, document); the client approves it or asks for changes. A new version of an item is a new approval with the same name (matched without regard to capital letters); `version` counts up per project and name.

Developers have **no access** to approvals (403, or 404 on a project they cannot see).

`ApprovalDto`:

```json
{ "id": "uuid", "projectId": "uuid", "taskId": "uuid | null", "item": "Home page design", "type": "DESIGN",
  "file": "https://figma.example/home", "version": 2, "message": "Please review | null",
  "status": "PENDING", "answerNote": "text | null", "sentBy": "uuid", "sentByName": "Jane Manager",
  "sentAt": "instant", "answeredByName": "Client name | null", "answeredAt": "instant | null" }
```

`type`: `DESIGN`, `PAGE_CONTENT`, `DOCUMENT`. `status`: `PENDING`, `APPROVED`, `CHANGES_REQUESTED`.

### GET /api/projects/{projectId}/approvals
Management and the client of the project. Newest first.

### POST /api/projects/{projectId}/approvals
Management only. Body:

```json
{ "item": "Home page design", "type": "DESIGN", "file": "https://figma.example/home", "message": "optional, up to 1000 characters", "taskId": "uuid (optional)" }
```

`file` (a file name or link) is required. `taskId`, if given, must be a task of this project; approving the item then finishes that task. The client company is notified. Returns **201** `ApprovalDto`.
Errors: 400 `VALIDATION_FAILED`; 403; 404 (project, or the task is not in this project).

### POST /api/projects/{projectId}/approvals/{approvalId}/approve
The client of the project only. Body `{}` or `{ "note": "Looks great" }`, or no body at all. Status `APPROVED`. If the item is linked to a task, that task becomes `DONE` (progress and project status update). The person who sent the item and the project manager are notified. Returns `ApprovalDto`.
Errors: 403 `CLIENT_ONLY`; 404; 409 `ALREADY_ANSWERED`.

### POST /api/projects/{projectId}/approvals/{approvalId}/request-changes
The client only. Body `{ "note": "Make the logo bigger" }` (required). Status `CHANGES_REQUESTED`; the sender and project manager are notified; a linked task is not changed. The manager then sends a new version. Returns `ApprovalDto`.
Errors: 400 `VALIDATION_FAILED`; 403 `CLIENT_ONLY`; 404; 409 `ALREADY_ANSWERED`.

---

## 10. Change requests

Extra work with its own price after the quote was accepted. The team proposes it, the client accepts or declines. Accepting adds the cost to the project `budget`. Developers have no access (403).

`ChangeRequestDto`:

```json
{ "id": "uuid", "projectId": "uuid", "title": "Add loyalty module", "cost": 400000, "status": "PROPOSED",
  "proposedByName": "Jane Manager", "createdAt": "instant", "answeredByName": "name | null", "answeredAt": "instant | null" }
```

`status`: `PROPOSED`, `ACCEPTED`, `DECLINED`.

### GET /api/projects/{projectId}/change-requests
Management and the client of the project. Newest first.

### POST /api/projects/{projectId}/change-requests
Management only. Body `{ "title": "Add loyalty module", "cost": 400000 }` (`cost` above zero). The client company is notified. Returns **201** `ChangeRequestDto`.
Errors: 400 `VALIDATION_FAILED`; 403; 404.

### POST /api/projects/{projectId}/change-requests/{changeId}/accept
### POST /api/projects/{projectId}/change-requests/{changeId}/decline
The client of the project only. Body `{}`. The project manager and the proposer are notified. Returns `ChangeRequestDto`.
Errors: 403 `CLIENT_ONLY`; 404; 409 `ALREADY_ANSWERED`.

---

## Who sees what

| | Admin | Project manager | Developer | Client |
|---|---|---|---|---|
| Requests and quotes | all | all, writes quotes | **no access** (403) | own company only; sends requests, answers quotes |
| Projects | all | all; start, add tasks, assign | only projects with one of their tasks, or granted by the admin | own company only |
| Budget and change-request amounts | yes | yes | **never** (field omitted, endpoints 403) | yes |
| Task detail (people, hours, files, comments) | yes | yes | yes | **no**: title, phase, status, due date, late flag only |
| Approvals and change requests | yes | yes | **no access** | own company; answer them |
| Time logs | read all | read all | log and read own tasks | **no access** |
| Updates feed | yes | yes | on projects they can see | own company |
| Grant developer access (`/api/admin/**`) | yes | no | no | no |

## Error codes

| Code | Status | Meaning |
|---|---|---|
| `VALIDATION_FAILED` | 400 | A field is missing or wrong; see `fields`. |
| `BAD_REQUEST` | 400 | The request could not be read, or a path/query value is invalid (unknown enum value, bad id). |
| (no body) | 401 | Not signed in, or the token is expired, wrong or belongs to a deactivated account. Sign in again. |
| `FORBIDDEN` | 403 | You may not do this (wrong role). |
| `CLIENT_ONLY` | 403 | Only the client can answer this. |
| `MANAGER_ONLY` | 403 | A developer tried to mark a task done. |
| `NOT_YOUR_TASK` | 403 | The task is assigned to someone else. |
| `NOT_FOUND` | 404 | It does not exist, or you may not know about it. |
| `METHOD_NOT_ALLOWED` | 405 | Wrong HTTP method for this path. |
| `INVALID_STATE` | 409 | The request is in a state where this answer is not possible (quote). |
| `INVALID_TRANSITION` | 409 | The task cannot move that way. |
| `SUBMISSION_REQUIRED` | 409 | Moving to review needs a submission (use `submit`). |
| `PROJECT_NOT_STARTED` | 409 | The project is still in planning. |
| `ALREADY_STARTED` | 409 | The project has already started. |
| `PROJECT_LAUNCHED` | 409 | The project is finished; no new tasks. |
| `TASK_DONE` | 409 | The task is finished; it cannot be reassigned or get more time. |
| `ALREADY_ANSWERED` | 409 | Approval or change request already answered. |
| `QUOTE_EXPIRED` | 409 | The quote's `validUntil` has passed. |
| `NO_MANAGER` | 409 | No active project manager or administrator can run the project. |
| `DEADLINE_PAST` | 400 | Request deadline is not after today. |
| `VALID_UNTIL_PAST` | 400 | Quote validity date is in the past. |
| `DUE_DATE_PAST` | 400 | Task due date is in the past. |
| `NOT_ASSIGNABLE` | 400 | Only active developers and project managers can be given tasks. |
| `NOT_A_DEVELOPER` | 400 | Project access can only be given to an active developer. |
| `DATE_IN_FUTURE` | 400 | Time cannot be logged for a future day. |
| `BAD_CREDENTIALS`, `ACCOUNT_LOCKED` (423), `ACCOUNT_DEACTIVATED`, `EMAIL_NOT_VERIFIED`, `CHALLENGE_INVALID` (401), `CODE_INVALID`, `CODE_EXPIRED`, `CODE_LOCKED`, `CODE_TOO_SOON` (429), `WEAK_PASSWORD`, `SAME_PASSWORD`, `NOT_INVITED`, `EMAIL_TAKEN` (409), `ALREADY_VERIFIED` | | Sign-in and registration, see [section 1](#1-sign-in-and-accounts). |
| `ROLE_NOT_STAFF`, `SELF_NOT_ALLOWED`, `LAST_ADMIN`, `CANNOT_RESET` | 400 | Account management, see [section 3](#3-administrator). |
| `SERVER_ERROR` | 500 | Something unexpected; try again. |
