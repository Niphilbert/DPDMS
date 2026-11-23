# DPDMS REST API, stage 3: money and care

Stage 3 adds **invoices and payments**, **renewals with reminders**, **support tickets with deadlines** and **reports**.
Everything in [api.md](api.md) (conventions, error body, roles, sign-in, notifications) applies here unchanged; this file only
lists the new endpoints. Base path `/api`, JSON in and out, `Authorization: Bearer <token>` on every call.

## Changes

(none yet: this contract is the first version. Any later change to it is listed here with the reason.)

- [Conventions for stage 3](#conventions-for-stage-3)
- [1. Invoices and payments](#1-invoices-and-payments) · [2. Renewals](#2-renewals) · [3. Support tickets](#3-support-tickets)
- [4. Reports and dashboard](#4-reports-and-dashboard) · [5. Companies](#5-companies)
- [Who sees what](#who-sees-what-stage-3) · [Error codes](#new-error-codes) · [Notifications and audit](#notifications-and-audit) · [Settings](#settings)

---

## Conventions for stage 3

| Topic | Rule |
|---|---|
| Money | Whole RWF as JSON integers (`long`). No decimals anywhere. Quantities on invoice lines may have 2 decimals (`11.75` hours); a line total is `round(quantity × unitPrice)` (half up). |
| Dates and moments | As in api.md: dates `YYYY-MM-DD`, moments ISO-8601 UTC. "Today" is the server's calendar day. |
| Not visible = 404, role not allowed = 403 | Same rule as api.md. A **developer has no access to money**: every invoice, payment, money and report endpoint answers **403 `FORBIDDEN`** for a developer (or **404** when a project id is in the path and the developer may not see that project). |
| Nulls | The DTOs below always send every field (`null` when empty), unless the text says "omitted". |
| Lists | Plain JSON arrays, no paging. |
| Links in notifications | `/invoices/<id>`, `/renewals/<id>`, `/tickets/<id>` (paths of the Angular app). |
| Files | `GET .../export.csv` style endpoints return `text/csv; charset=UTF-8` with `Content-Disposition: attachment; filename=...`, start with a UTF-8 byte order mark and use `,` as separator. The browser must fetch them with the Authorization header (fetch, then save the blob); a plain link will not work. Cells that start with `=`, `+`, `-` or `@` are prefixed with `'` so spreadsheets do not run them. |

---

## 1. Invoices and payments

### Status

```
DRAFT ──send──▶ SENT ──payment──▶ PARTIALLY_PAID ──payment──▶ PAID
  │               │  ▲                  │  ▲                    │
  │               │  └── payment reversed ──┘ (status is recomputed from the payments)
  └──void──▶ VOID ◀──void (only while no active payment exists)
```

- `status` is stored: `DRAFT`, `SENT`, `PARTIALLY_PAID`, `PAID`, `VOID`.
- `OVERDUE` is **never stored**. `overdue` is `true` when `status` is `SENT` or `PARTIALLY_PAID` and `dueDate` is before today. `displayStatus` equals `status`, except it is `"OVERDUE"` when `overdue` is true. Show `displayStatus` in lists and pills.
- The status follows the active (not reversed) payments automatically: none = `SENT`, some but not all = `PARTIALLY_PAID`, the whole total = `PAID`. A reversal can take an invoice back from `PAID` to `PARTIALLY_PAID` or `SENT`.
- A `DRAFT` can be edited and is invisible to clients. Anything else cannot be edited.
- Numbers look like `INV-2026-0001`: `INV-<year of the issue date>-<4 digit running number for that year>`; unique and gap-free per year (a draft you no longer need is voided, never deleted).
- A client sees an invoice only when it belongs to their company and has been sent (`sentAt` is set): statuses `SENT`, `PARTIALLY_PAID`, `PAID`, and `VOID` for an invoice that had been sent. Everything else is **404** for them.

### Invoice objects

`InvoiceSummary` (list rows):

```json
{
  "id": "uuid", "number": "INV-2026-0001",
  "projectId": "uuid | null", "projectName": "Booking website | null",
  "companyId": "uuid", "companyName": "Acme Hotels",
  "status": "PARTIALLY_PAID", "displayStatus": "OVERDUE", "overdue": true, "daysOverdue": 12,
  "issueDate": "2026-10-01", "dueDate": "2026-10-20",
  "taxRate": 18,
  "subtotal": 1000000, "taxAmount": 180000, "total": 1180000,
  "paidAmount": 400000, "balance": 780000,
  "note": "Milestone 2 | null",
  "createdByName": "Jane Manager", "createdAt": "instant",
  "sentAt": "instant | null", "voidedAt": "instant | null", "voidReason": "text | null (always null for clients)"
}
```

`daysOverdue` is `null` unless `overdue`. `taxRate` is a percentage (`18` or `7.5`, up to 2 decimals). `taxAmount = round(subtotal × taxRate ÷ 100)`, `total = subtotal + taxAmount`, `balance = total − paidAmount` (`paidAmount` counts only active payments; for a `VOID` invoice `balance` is `0`).

`Invoice` (detail) = every `InvoiceSummary` field plus:

```json
{
  "lines": [ { "id": "uuid", "description": "Design", "quantity": 1, "unitPrice": 500000, "lineTotal": 500000 } ],
  "payments": [ "...Payment, oldest first..." ]
}
```

`Payment`:

```json
{
  "id": "uuid", "amount": 400000, "paidOn": "2026-10-15",
  "method": "MOBILE_MONEY", "reference": "MoMo 123456 | null",
  "recordedByName": "Jane Manager", "recordedAt": "instant",
  "reversed": false, "reversedAt": "instant | null",
  "reversedByName": "name | null (null for clients)", "reverseReason": "text | null (null for clients)"
}
```

`method`: `MOBILE_MONEY`, `BANK_TRANSFER`, `CASH`. Reversed payments stay in the list (with `reversed: true`) and do not count towards `paidAmount`. Clients see their invoice's payments too, including reversed ones.

### GET /api/invoices
Management: all invoices. Client: their company's visible invoices. Developer: 403.
Query (all optional): `status` (`DRAFT`, `SENT`, `PARTIALLY_PAID`, `PAID`, `VOID`, or the derived `OVERDUE`; anything else: 400 `BAD_REQUEST`), `projectId`, `companyId` (management only; ignored for clients), `from` and `to` (issue date, inclusive; `from` after `to`: 400 `INVALID_RANGE`).
Newest issue date first, then by number. Returns a list of `InvoiceSummary`. `status=SENT` matches the stored status, so overdue invoices that are still `SENT` are included; `status=OVERDUE` matches only the overdue ones (`SENT` or `PARTIALLY_PAID` past their due date). An id that is not a UUID or a date that is not `YYYY-MM-DD`: 400 `BAD_REQUEST`.

### GET /api/invoices/{id}
Returns `Invoice`. 404 if it does not exist or the client may not see it; 403 for a developer.

### POST /api/invoices
Management only. Creates a **DRAFT**. Body:

```json
{
  "projectId": "uuid (optional)", "companyId": "uuid (needed when there is no projectId)",
  "issueDate": "2026-11-04", "dueDate": "2026-11-18",
  "taxRate": 18, "note": "optional, up to 1000 characters",
  "lines": [ { "description": "Design", "quantity": 1, "unitPrice": 500000 }, { "description": "Hours to date", "quantity": 11.75, "unitPrice": 25000 } ]
}
```

- Either `projectId` or `companyId`. With a project the invoice is for that project's company (a `companyId` that differs from the project's company is a 400 `VALIDATION_FAILED`). Without a project it is a company invoice (for example a renewal fee).
- `issueDate`, `dueDate` required; `dueDate` must not be before `issueDate` (400 `DUE_BEFORE_ISSUE`). The issue date can be in the past or future.
- `taxRate` optional, 0 to 100, default 0.
- 1 to 50 `lines`; `description` required, up to 200 characters; `quantity` above 0 and at most 100000 (2 decimals); `unitPrice` a whole number from 1 to 1,000,000,000.
- The number is assigned now (`INV-<issue year>-nnnn`).

Returns **201** `Invoice` (status `DRAFT`).
Errors: 400 `VALIDATION_FAILED` (with `fields` such as `lines[0].description`), `DUE_BEFORE_ISSUE`; 403; 404 (project or company not found).

### PUT /api/invoices/{id}
Management only. Replaces everything the create body contains (same body, same rules; the project cannot be changed to a different company's project). Only while `DRAFT`; the number does not change. Returns `Invoice`.
Errors: 400 `VALIDATION_FAILED`, `DUE_BEFORE_ISSUE`; 403; 404; 409 `INVOICE_NOT_DRAFT`.

### POST /api/invoices/{id}/send
Management only. Body `{}`. `DRAFT` to `SENT`; sets `sentAt`; the client company's users are notified (and e-mailed). Returns `Invoice`.
Errors: 403; 404; 409 `INVOICE_NOT_DRAFT`.

### POST /api/invoices/{id}/payments
Management only. Records a payment by hand (there is no payment gateway). Body:

```json
{ "amount": 400000, "paidOn": "2026-11-04", "method": "MOBILE_MONEY", "reference": "optional, up to 100 characters" }
```

`amount` a whole number above 0; `paidOn` not in the future; the invoice must be `SENT` or `PARTIALLY_PAID` (an overdue invoice is still payable). The amount cannot exceed the current `balance`. The status is updated (`PARTIALLY_PAID` or `PAID`), the client company is notified. Returns **201** `Invoice` (with the new payment in `payments`).
Errors: 400 `VALIDATION_FAILED`, `DATE_IN_FUTURE`; 403; 404; 409 `INVOICE_NOT_PAYABLE` (draft, already paid or void), `OVERPAYMENT` (more than the balance; nothing is recorded).

### POST /api/invoices/{id}/payments/{paymentId}/reverse
Management only. Body `{ "reason": "Cheque bounced" }` (required, up to 300 characters). The payment row is kept and marked reversed; `paidAmount`, `balance` and `status` are recomputed. Returns `Invoice`.
Errors: 400 `VALIDATION_FAILED`; 403; 404 (invoice, or the payment is not on this invoice); 409 `PAYMENT_ALREADY_REVERSED`.

### POST /api/invoices/{id}/void
Management only. Body `{}` or `{ "reason": "Wrong client" }` (optional, up to 300 characters). Status becomes `VOID`. Not allowed while the invoice has an active payment (reverse it first); reversed payments do not block. If the invoice had been sent, the client company is notified. Returns `Invoice`.
Errors: 400 `VALIDATION_FAILED`; 403; 404; 409 `INVOICE_HAS_PAYMENTS`, `INVALID_STATE` (already void).

### POST /api/invoices/{id}/remind
Management only. Body `{}`. Sends the client company a payment reminder (notification and e-mail). Allowed for `SENT` and `PARTIALLY_PAID`. Returns `Invoice`.
Errors: 403; 404; 409 `INVALID_STATE`.

### GET /api/invoices/summary
Management and clients. A client gets totals for their company's visible invoices. Management gets all invoices, optionally narrowed with `companyId` and `projectId`. Developer: 403. `DRAFT` and `VOID` invoices are never counted. Returns `InvoiceTotals`:

```json
{ "invoiceCount": 5, "invoiced": 5900000, "paid": 2000000, "balanceDue": 3900000,
  "openCount": 3, "overdueCount": 1, "overdueAmount": 1180000 }
```

`openCount` = invoices still `SENT` or `PARTIALLY_PAID`. `overdueAmount` = the balance of the overdue ones.

### GET /api/invoices/export.csv
Management only. Same query as `GET /api/invoices` (`status`, `projectId`, `companyId`, `from`, `to`). CSV file `invoices.csv`, one row per invoice, header:
`Number,Status,Client,Project,Issue date,Due date,Subtotal,Tax,Total,Paid,Balance` (`Status` is the `displayStatus`; amounts are plain integers).

### GET /api/projects/{projectId}/invoices
The invoices of one project, newest first, list of `InvoiceSummary`. Management: all (drafts included). Client of the project: the visible ones. Developer: 403 (404 when the project is not visible to them). 404 for another company's client.

### GET /api/projects/{projectId}/money
Management and the client of the project (developer: 403/404). Returns `ProjectMoney`:

```json
{ "projectId": "uuid", "budget": 1900000, "invoiced": 1180000, "paid": 400000, "balanceDue": 780000,
  "unbilled": 720000, "overdueAmount": 0 }
```

`budget` is the project's budget (quote plus accepted change requests). `invoiced` = total of its `SENT`, `PARTIALLY_PAID` and `PAID` invoices; `paid` = their active payments; `balanceDue = invoiced − paid`; `unbilled = max(0, budget − invoiced)`. The existing `ProjectDto` is unchanged.

### GET /api/projects/{projectId}/invoice-suggestion
Management only. Helps fill the new-invoice form (the prototype bills time at **25,000 RWF per hour**, the configured `hourlyRate`, and bills the rest of the budget). Returns:

```json
{
  "projectId": "uuid", "projectName": "Booking website", "companyId": "uuid",
  "hourlyRate": 25000, "loggedHours": 11.75,
  "budget": 1900000, "invoiced": 1180000, "unbilled": 720000,
  "timeLine": { "description": "Hours logged to date (11.75 h)", "quantity": 11.75, "unitPrice": 25000, "lineTotal": 293750 },
  "balanceLine": { "description": "Remaining budget balance", "quantity": 1, "unitPrice": 720000, "lineTotal": 720000 }
}
```

`timeLine` is `null` when no hours are logged; `balanceLine` is `null` when `unbilled` is 0. A line has the same shape as an invoice line input (`description`, `quantity`, `unitPrice`) plus `lineTotal`, so the frontend can drop it straight into the form. Nothing is saved.

---

## 2. Renewals

A **renewal** is one thing that expires for a client's website and project: a domain, hosting plan, SSL certificate or maintenance plan.

`type`: `DOMAIN`, `HOSTING`, `SSL`, `MAINTENANCE`.

**Status is derived** from `expiresOn` and today, never stored: `EXPIRED` (`expiresOn` before today), `EXPIRING` (0 to 30 days left, today included), `ACTIVE` (more than 30 days left). `daysLeft` is `expiresOn − today` in days (negative when expired).

`Renewal`:

```json
{
  "id": "uuid", "projectId": "uuid", "projectName": "Booking website",
  "companyId": "uuid", "companyName": "Acme Hotels",
  "type": "DOMAIN", "label": "acmehotels.example", "provider": "Rwanda Host Pro | null",
  "expiresOn": "2027-02-01", "daysLeft": 89, "status": "ACTIVE",
  "price": 25000, "note": "text | null",
  "lastRenewedOn": "2026-02-01 | null", "renewalCount": 1,
  "history": [ { "id": "uuid", "renewedOn": "2026-02-01", "previousExpiresOn": "2026-01-30", "newExpiresOn": "2027-01-30", "price": 25000, "note": "text | null", "renewedByName": "Jane Manager" } ],
  "createdAt": "instant", "updatedAt": "instant"
}
```

- `price` is the price of the next renewal (whole RWF). It is **omitted for developers**. Clients see it (read only).
- `note` is internal: **omitted for clients** (and omitted when empty). `price` is never null for management and clients.
- `history` (newest first) is only on the detail call (`GET /api/renewals/{id}` and the responses of create, update and renew); list rows leave it out ("omitted").
- `lastRenewedOn` is `null` until the first renewal.

### GET /api/renewals
Management: every renewal. Client: their company's. Developer: renewals of projects they can see (read only, no `price`). Query, all optional: `status` (`ACTIVE`, `EXPIRING`, `EXPIRED`), `type`, `companyId` (management only), `projectId`, `expiringWithinDays` (a number from 0 to 3650: renewals with `0 ≤ daysLeft ≤ N`, expired ones not included). Unknown enum value: 400 `BAD_REQUEST`. Soonest `expiresOn` first. Returns a list of `Renewal` without `history`.

### GET /api/renewals/{id}
Returns `Renewal` with `history`. 404 if the person may not see it.

### GET /api/projects/{projectId}/renewals
The renewals of one project, soonest first, same visibility as the list. 404 when the project is not visible.

### POST /api/renewals
Management only. Body:

```json
{ "projectId": "uuid", "type": "DOMAIN", "label": "acmehotels.example", "provider": "optional, up to 150 chars",
  "expiresOn": "2027-02-01", "price": 25000, "note": "optional, up to 500 chars" }
```

`label` required, up to 200 characters (the domain name, or "Business hosting plan"). `price` a whole number, 0 or more. `expiresOn` any date (a date in the past registers an already expired item). Any project may get renewals. Returns **201** `Renewal`.
Errors: 400 `VALIDATION_FAILED`; 403; 404 (project).

### PUT /api/renewals/{id}
Management only. Same body as create without `projectId` (the project cannot be changed). Replaces `type`, `label`, `provider`, `expiresOn`, `price`, `note`. Returns `Renewal`. Changing `expiresOn` starts a fresh set of reminders for the new date.
Errors: 400 `VALIDATION_FAILED`; 403; 404.

### POST /api/renewals/{id}/renew
Management only. Marks the renewal as done for another period. Body:

```json
{ "newExpiresOn": "2028-02-01", "price": 27000, "note": "optional, up to 500 chars" }
```

`newExpiresOn` must be after the current `expiresOn` (400 `EXPIRY_NOT_LATER`). `price` is optional; when given it also becomes the renewal's price for the next period. A history row is written (renewal date = today, old and new expiry, the price paid = `price` if given, otherwise the previous price). The client company is notified. Reminders restart for the new date. Returns `Renewal` with `history`.
Errors: 400 `VALIDATION_FAILED`, `EXPIRY_NOT_LATER`; 403; 404.

### DELETE /api/renewals/{id}
Management only. Allowed only when the renewal has **no history** (it was never renewed): **204** no body. Otherwise 409 `HAS_HISTORY`. 404; 403.

### POST /api/renewals/{id}/request-renewal
Clients of the owning company only. Body `{}`. Tells management the client wants this renewed (notification to the project manager and administrators). **202** no body. Errors: 403 `CLIENT_ONLY`; 404.

### Reminders

Reminders go out **60, 30, 14 and 7 days before** `expiresOn` and **once on expiry** (threshold `0`).

- Recipients: every active user of the client company, plus every active administrator and project manager. Each gets an in-app notification and an e-mail (the log when no mail server is configured).
- **Exactly once** per renewal, threshold and expiry date: if the daily job missed some days or the renewal was registered late, only the nearest threshold that has been reached is sent (for example 20 days left: the "30 days" reminder, once). After a renewal moves to a new expiry date the thresholds start over.
- A daily job (`app.jobs.*` settings below) runs the logic; an administrator can also run it by hand:

### POST /api/admin/renewals/run-reminders
**Administrator only** (everyone else: 403 with no body, because it is under `/api/admin`). Body is optional: `{}` or `{ "today": "2026-12-01" }` pretends it is that day (to demonstrate or test). Returns:

```json
{
  "today": "2026-12-01", "checked": 12, "sent": 2,
  "reminders": [ { "renewalId": "uuid", "type": "DOMAIN", "label": "acmehotels.example", "projectName": "Booking website",
                   "expiresOn": "2026-12-15", "daysLeft": 14, "threshold": 14 } ]
}
```

`checked` = renewals looked at (those already expired or expiring within 60 days of `today`), `sent` = reminders sent in this run (running it again for the same day returns `sent: 0`). Errors: 400 `BAD_REQUEST` (bad date).

---

## 3. Support tickets

A client raises a ticket; management assigns it to a developer or project manager; the **SLA deadline** is fixed when the ticket is created.

| `priority` | Answer deadline (SLA) |
|---|---|
| `URGENT` | 4 hours |
| `HIGH` | 8 hours |
| `MEDIUM` | 24 hours |
| `LOW` | 72 hours |

Calendar hours (nights and weekends count), as in the prototype. The deadline is `dueAt = clock start + slaHours`. The clock start is the creation time, restarted when a ticket is reopened. The clock does not pause while waiting for the client. Changing the priority recalculates `dueAt` from the clock start.

### Status

`OPEN`, `IN_PROGRESS`, `WAITING_CLIENT`, `RESOLVED`, `CLOSED`.

| From | Staff (management or the assigned developer) may move it to | Client may |
|---|---|---|
| `OPEN` | `IN_PROGRESS`, `WAITING_CLIENT`, `RESOLVED` | close |
| `IN_PROGRESS` | `WAITING_CLIENT`, `RESOLVED` | close |
| `WAITING_CLIENT` | `IN_PROGRESS`, `RESOLVED` | close; a comment moves it to `IN_PROGRESS` |
| `RESOLVED` | `IN_PROGRESS` (reopen), `CLOSED` (management only) | **reopen once** (to `IN_PROGRESS`), close |
| `CLOSED` | nothing | nothing |

- A public reply by staff on an `OPEN` ticket moves it to `IN_PROGRESS` by itself.
- Reopening (by the client, or by staff moving `RESOLVED` to `IN_PROGRESS`) clears the resolution and restarts the SLA clock (`dueAt = now + slaHours`). The client can reopen a given ticket only once (`reopenCount`); a second attempt is 409 `REOPEN_LIMIT`.
- Resolving records `resolvedAt` and `resolvedWithinSla` (`resolvedAt ≤ dueAt`). Closing a ticket that was never resolved leaves both `null`.
- `breached` is `true` while the ticket is not `RESOLVED`/`CLOSED` and `now` is after `dueAt`.

### Ticket objects

`TicketSummary`:

```json
{
  "id": "uuid", "number": "TCK-0007", "subject": "Website is down", "priority": "URGENT", "status": "IN_PROGRESS",
  "companyId": "uuid", "companyName": "Acme Hotels",
  "projectId": "uuid | null", "projectName": "Booking website | null",
  "createdById": "uuid", "createdByName": "Jean Bosco",
  "assigneeId": "uuid | null", "assigneeName": "Dev Name | null",
  "slaHours": 4, "createdAt": "instant", "dueAt": "instant",
  "breached": false, "minutesLeft": 95, "slaState": "ON_TRACK",
  "resolvedAt": "instant | null", "resolvedWithinSla": "true | false | null",
  "closedAt": "instant | null", "reopenCount": 0, "commentCount": 3, "updatedAt": "instant"
}
```

- `minutesLeft`: whole minutes from now to `dueAt` (rounded down, **negative** when the deadline has passed); `null` once the ticket is `RESOLVED` or `CLOSED`.
- `slaState`: `ON_TRACK`; `AT_RISK` (not resolved, 25% or less of the time window left); `BREACHED` (deadline passed, not resolved); `MET` (resolved within SLA); `MISSED` (resolved late); `null` for a ticket closed without being resolved.
- `commentCount` counts the comments the caller may see (clients do not count internal notes).

`Ticket` (detail) = every `TicketSummary` field plus `description` (string) and `comments`, oldest first:

```json
{ "id": "uuid", "authorId": "uuid", "authorName": "Dev Name", "authorRole": "DEVELOPER",
  "body": "We found oversized images.", "internal": false, "createdAt": "instant" }
```

**Internal notes** (`internal: true`) exist only for staff. They are never sent to a client (not in `comments`, not counted, no notification).

### GET /api/tickets
Management: all tickets. Client: their company's. Developer: tickets assigned to them. Query, all optional: `status`, `priority`, `projectId`, `companyId` (management only), `assigneeId` (management only), `unassigned=true` (management only), `breached=true`, `mine=true` (assigned to me). Unknown enum value: 400 `BAD_REQUEST`.
Order: tickets still being worked on (`OPEN`, `IN_PROGRESS`, `WAITING_CLIENT`) first, soonest `dueAt` first; then `RESOLVED` and `CLOSED`, newest first. Returns a list of `TicketSummary`.

### GET /api/tickets/{id}
Returns `Ticket`. 404 if the client's company does not own it or the developer is not the assignee.

### POST /api/tickets
Client only (management and developers: 403 `CLIENT_ONLY`). Body:

```json
{ "subject": "Website is down", "description": "Patients cannot open our website.", "priority": "URGENT", "projectId": "uuid (optional)" }
```

`subject` up to 200 characters, `description` up to 4000, both required; `priority` required. `projectId` must be a project of the client's own company (otherwise 404). Status `OPEN`, number `TCK-nnnn` (running, unique), `dueAt` computed. Administrators and project managers are notified. Returns **201** `Ticket`.
Errors: 400 `VALIDATION_FAILED`; 403 `CLIENT_ONLY`; 404 (project).

### POST /api/tickets/{id}/comments
Client of the owning company, management, or the assigned developer. Body:

```json
{ "body": "Please try again now.", "internal": false }
```

`body` required, up to 4000 characters; `internal` optional (default false) and only for staff (a client sending `true`: 403 `FORBIDDEN`). Not on a `CLOSED` ticket (409 `TICKET_CLOSED`). A client's comment on a `WAITING_CLIENT` ticket moves it to `IN_PROGRESS`; a staff public reply on an `OPEN` ticket moves it to `IN_PROGRESS`. Notifications: a staff reply tells the client company (an internal note does not); a client comment tells the assignee, or all management when nobody is assigned; an internal note tells the assignee when somebody else wrote it. Returns **201** the whole `Ticket`.
Errors: 400 `VALIDATION_FAILED`; 403 `FORBIDDEN`; 404; 409 `TICKET_CLOSED`.

### POST /api/tickets/{id}/status
Staff only (management, or the developer the ticket is assigned to): see the table above. Body `{ "status": "RESOLVED" }`. Clients: 403 `FORBIDDEN` (they use `reopen`, `close` and comments). A developer asking for `CLOSED`: 403 `MANAGER_ONLY`. The client company is notified of every change, and the assignee when someone else changed it. Returns `Ticket`.
Errors: 400 `BAD_REQUEST` (unknown status); 403; 404; 409 `INVALID_TRANSITION`, `TICKET_CLOSED`.

### POST /api/tickets/{id}/assign
Management only. Body `{ "assigneeId": "uuid" }`. The assignee must be an active developer or project manager (400 `NOT_ASSIGNABLE`). The assignee is notified; the client company is told who is looking at their ticket. Not on a `CLOSED` ticket. Returns `Ticket`.
Errors: 400 `VALIDATION_FAILED`, `NOT_ASSIGNABLE`; 403; 404; 409 `TICKET_CLOSED`.

### POST /api/tickets/{id}/priority
Management only. Body `{ "priority": "HIGH" }`. Recalculates `slaHours` and `dueAt`. Only while the ticket is `OPEN`, `IN_PROGRESS` or `WAITING_CLIENT` (otherwise 409 `INVALID_TRANSITION`). The assignee and the client company are notified. Returns `Ticket`.

### POST /api/tickets/{id}/reopen
Client of the owning company only. Body `{}`. The ticket must be `RESOLVED`; becomes `IN_PROGRESS`; the SLA clock restarts; the assignee (or all management) is notified. Returns `Ticket`.
Errors: 403 `CLIENT_ONLY`; 404; 409 `INVALID_TRANSITION` (not resolved), `REOPEN_LIMIT` (already reopened once).

### POST /api/tickets/{id}/close
The client of the owning company (from any status but `CLOSED`) or management (from `RESOLVED` only). Body `{}`. Sets `CLOSED` and `closedAt`. Returns `Ticket`.
Errors: 403 `FORBIDDEN` (developer); 404; 409 `TICKET_CLOSED` (already closed), `INVALID_TRANSITION` (management on an unresolved ticket).

### Deadline alerts

A job (and the endpoint below) looks at every ticket that is not resolved or closed:

- **At risk**: 25% or less of the time between the clock start and `dueAt` is left → notification (and e-mail) to the assignee and all management, **once per ticket**.
- **Breached**: `now` is after `dueAt` → notification to the assignee and all management, **once per ticket**. If a ticket jumps straight to breached, only the breach is sent.
- A reopened ticket gets a new clock, so both alerts can fire again for the new deadline.

### POST /api/admin/tickets/run-sla-check
**Administrator only.** Body optional: `{}`, `{ "now": "2026-11-05T10:00:00Z" }` or `{ "today": "2026-11-05" }` (that day at 00:00 UTC; `now` wins when both are given). Returns:

```json
{ "now": "2026-11-05T10:00:00Z", "checked": 8, "atRisk": 1, "breached": 2,
  "alerts": [ { "ticketId": "uuid", "number": "TCK-0007", "kind": "BREACHED", "assigneeName": "Dev Name | null" } ] }
```

`kind` is `AT_RISK` or `BREACHED`. `atRisk` and `breached` count the alerts sent in this run; a second run with the same time sends nothing new. Errors: 400 `BAD_REQUEST`.

---

## 4. Reports and dashboard

### GET /api/reports/summary
**Management only** (developer and client: 403 `FORBIDDEN`). Optional `from` and `to` (dates, inclusive). `from` after `to`: 400 `INVALID_RANGE`. What the range limits:

| Block | Range applies to |
|---|---|
| `revenue` | invoices by **issue date** (drafts and void invoices never count) |
| `projects` | projects by **start date** |
| `tickets` | tickets by **creation date** |
| `hours` | time entries by **work date** |
| `renewals`, `projectProfit` | not limited: always the current state |

Response:

```json
{
  "from": "2026-01-01 | null", "to": "2026-12-31 | null", "generatedAt": "instant", "hourlyRate": 25000,
  "revenue": {
    "invoiced": 5900000, "invoiceCount": 5, "paid": 2000000, "outstanding": 3900000,
    "overdue": 1180000, "overdueCount": 1,
    "byClient": [ { "companyId": "uuid", "companyName": "Acme Hotels", "invoiced": 3000000, "paid": 2000000 } ]
  },
  "projects": { "total": 4, "byStatus": { "PLANNING": 1, "IN_PROGRESS": 2, "IN_REVIEW": 0, "LAUNCHED": 1 }, "averageProgress": 54.5 },
  "tickets": {
    "total": 9, "open": 4, "breached": 1, "resolved": 5, "resolvedWithinSlaPercent": 80, "averageResolutionHours": 5.25,
    "byPriority": [ { "priority": "URGENT", "total": 2, "open": 1, "breached": 1 }, { "priority": "HIGH", "total": 3, "open": 1, "breached": 0 },
                    { "priority": "MEDIUM", "total": 3, "open": 2, "breached": 0 }, { "priority": "LOW", "total": 1, "open": 0, "breached": 0 } ]
  },
  "renewals": {
    "total": 12, "expiring": 3, "expired": 1,
    "byType": [ { "type": "DOMAIN", "active": 2, "expiring": 1, "expired": 0 }, { "type": "HOSTING", "active": 3, "expiring": 0, "expired": 0 },
                { "type": "SSL", "active": 1, "expiring": 1, "expired": 1 }, { "type": "MAINTENANCE", "active": 2, "expiring": 1, "expired": 0 } ]
  },
  "hours": { "total": 120.5, "byDeveloper": [ { "userId": "uuid", "fullName": "Dev Name", "hours": 80.25 } ] },
  "projectProfit": [ { "projectId": "uuid", "projectName": "Booking website", "companyName": "Acme Hotels", "status": "IN_PROGRESS",
                      "budget": 1900000, "loggedHours": 11.75, "costSoFar": 293750, "plannedHours": 104,
                      "forecastCost": 2600000, "forecastMarginPercent": -37 } ]
}
```

Definitions:
- `revenue` covers invoices with status `SENT`, `PARTIALLY_PAID` or `PAID`. `paid` = their active payments; `outstanding = invoiced − paid`; `overdue` = balance of those that are overdue today.
- `projects.averageProgress`: mean of the projects' progress percentages, one decimal; `null` with no projects. `byStatus` always has all four keys.
- `tickets.open` = `OPEN`, `IN_PROGRESS`, `WAITING_CLIENT`; `breached` = open and past `dueAt` right now; `resolved` = tickets with a `resolvedAt`; `resolvedWithinSlaPercent` = whole percent of the resolved tickets with `resolvedWithinSla`, `null` when none; `averageResolutionHours` = mean of `resolvedAt − createdAt`, 2 decimals, `null` when none. `byPriority` always has the four priorities in this order.
- `renewals.expiring` = status `EXPIRING` (within 30 days), `expired` = `EXPIRED`; `byType` always has the four types.
- `hours.byDeveloper`: most hours first, only people with hours. Hours are numbers with 2 decimals.
- `projectProfit`: one row per project, `costSoFar = loggedHours × hourlyRate`, `forecastCost = Σ over tasks of max(planned, logged) hours × hourlyRate`, `forecastMarginPercent = round(100 × (budget − forecastCost) ÷ budget)` (`null` when the budget is 0), as in the prototype's "Project profit" table.

### GET /api/reports/revenue-by-month
Management only. Optional `months` (1 to 36, default 12). The last N calendar months **including the current one**, oldest first, months without activity are present with zeros:

```json
[ { "month": "2026-11", "invoiced": 1180000, "paid": 400000 } ]
```

`invoiced` = totals of `SENT`/`PARTIALLY_PAID`/`PAID` invoices by the month of their issue date; `paid` = active payments by the month of `paidOn`.

### GET /api/reports/summary.csv
Management only. Same query as the summary. A flat CSV `reports-summary.csv` with the header `Section,Item,Value`, for example `Revenue,Invoiced,5900000`, `Tickets,Urgent open,1`, `Hours,Dev Name,80.25`, `Renewals,SSL expired,1`.

### GET /api/dashboard/stage3
Everyone signed in. Counts for the home page, **only the blocks the caller may see** (a block that is not allowed is omitted from the JSON):

```json
{
  "invoices": { "unpaidCount": 3, "unpaidAmount": 3900000, "overdueCount": 1, "overdueAmount": 1180000, "draftCount": 2 },
  "renewals": { "expiringCount": 3, "expiredCount": 1 },
  "tickets": { "openCount": 4, "breachedCount": 1, "atRiskCount": 1, "unassignedCount": 2, "waitingOnClientCount": 0 }
}
```

| Block / field | Management | Developer | Client |
|---|---|---|---|
| `invoices` | all invoices | **omitted** | own company, sent invoices only |
| `invoices.draftCount` | yes | n/a | omitted |
| `renewals` | all | projects they can see | own company |
| `tickets` | all | assigned to them | own company |
| `tickets.unassignedCount` | yes | omitted | omitted |
| `tickets.waitingOnClientCount` | yes | omitted | yes (tickets waiting for **you**) |
| `tickets.breachedCount`, `atRiskCount` | yes | yes | omitted |

`unpaidCount`/`unpaidAmount` = invoices still `SENT` or `PARTIALLY_PAID` and their balance; `overdue*` is the overdue part of that; `expiringCount` = `EXPIRING` renewals, `expiredCount` = `EXPIRED`; `openCount` = tickets not `RESOLVED`/`CLOSED`.

---

## 5. Companies

### GET /api/companies
Management only. The client companies, for pick lists and filters (company invoices, filters). Sorted by name:

```json
[ { "id": "uuid", "name": "Acme Hotels" } ]
```

---

## Decisions where the prototype was silent

- Invoice numbers are assigned when the draft is created (not when sent), so a draft that is not needed is voided, never deleted: numbers have no gaps.
- A voided invoice that had never been sent stays invisible to clients; one that had been sent stays visible as `VOID`.
- "Void not allowed once a payment exists" counts active payments only: after every payment is reversed the invoice can be voided.
- Overdue invoices have no automatic reminder job; management sends reminders with `POST /api/invoices/{id}/remind`.
- Renewals can be registered for any project (not only launched ones). The price shown to a client is the price of the next renewal; creating the renewal invoice is a normal company invoice (the renewal is not linked to it).
- The SLA clock uses calendar hours and does not pause in `WAITING_CLIENT`. A client may close a ticket in any open state; management may close only a resolved one. Tickets are raised by clients only.
- Priority change recalculates the deadline from the clock start (creation or last reopen).
- Hours are costed at the configured `hourlyRate` (25,000 RWF); the profit report copies the prototype ("forecast counts the larger of planned and logged hours for each task").

## Who sees what (stage 3)

| | Admin | Project manager | Developer | Client |
|---|---|---|---|---|
| Invoices, payments, money, suggestion, invoice CSV | everything | everything | **no access** (403; 404 on a project they cannot see) | own company: sent invoices and their payments, summary, project money |
| Create/edit/send/void invoice, record/reverse payment | yes | yes | no | no |
| Renewals | everything; create, edit, renew, delete | same | read only, projects they can see, **no price** | own company, read only, price visible, note hidden, can ask for a renewal |
| Run reminder job (`/api/admin/renewals/run-reminders`) | yes | no (403, no body) | no | no |
| Tickets | all; assign, priority, status, close | same | assigned ones: status, comments, internal notes | own company: raise, comment, reopen once, close; **internal notes hidden** |
| Run SLA check (`/api/admin/tickets/run-sla-check`) | yes | no | no | no |
| Reports and CSV | yes | yes | no (403) | no (403) |
| Dashboard `GET /api/dashboard/stage3` | yes | yes | tickets and renewals only | own company's invoices, renewals, tickets |
| `GET /api/companies` | yes | yes | no | no |

## New error codes

| Code | Status | Meaning |
|---|---|---|
| `DUE_BEFORE_ISSUE` | 400 | Invoice due date is before its issue date. |
| `INVALID_RANGE` | 400 | `from` is after `to`. |
| `EXPIRY_NOT_LATER` | 400 | The new expiry date of a renewal is not after the current one. |
| `INVOICE_NOT_DRAFT` | 409 | Only a draft invoice can be edited or sent. |
| `INVOICE_NOT_PAYABLE` | 409 | Payments can only be recorded on a sent or partially paid invoice. |
| `OVERPAYMENT` | 409 | The payment is more than the balance of the invoice. |
| `INVOICE_HAS_PAYMENTS` | 409 | An invoice with an active payment cannot be voided; reverse the payment first. |
| `PAYMENT_ALREADY_REVERSED` | 409 | This payment was already reversed. |
| `HAS_HISTORY` | 409 | A renewal that has been renewed before cannot be deleted. |
| `TICKET_CLOSED` | 409 | The ticket is closed and cannot change any more. |
| `REOPEN_LIMIT` | 409 | A ticket can be reopened by the client only once. |
| `INVALID_TRANSITION` | 409 | (existing code) the ticket cannot move that way; also priority changes on finished tickets and closing an unresolved ticket as management. |
| `INVALID_STATE` | 409 | (existing code) for example voiding a void invoice, reminding for a paid one. |
| `NOT_ASSIGNABLE`, `DATE_IN_FUTURE`, `MANAGER_ONLY`, `CLIENT_ONLY`, `FORBIDDEN`, `NOT_FOUND`, `VALIDATION_FAILED`, `BAD_REQUEST` | | Same meaning as in [api.md](api.md#error-codes). |

## Notifications and audit

Notifications (all in-app; those marked ✉ also go out by e-mail through the mail service, which only logs when no mail server is configured):

| Event | Who is notified |
|---|---|
| Invoice sent ✉, payment recorded, payment reversed, invoice voided (if it had been sent), reminder ✉ | client company's users |
| Renewal renewed | client company's users |
| Renewal reminder (60/30/14/7/0 days) ✉ | client company's users + all active administrators and project managers |
| Client asks for a renewal | administrators and project managers |
| New ticket | administrators and project managers |
| Ticket assigned | the assignee; the client company |
| Ticket comment, status change, priority change, reopen | see each endpoint above |
| Ticket at risk / breached ✉ | the assignee + administrators and project managers |

Audit trail entries (`GET /api/admin/audit`) added in stage 3: Invoice created, Invoice updated, Invoice sent, Payment recorded, Payment reversed, Invoice voided, Invoice reminder sent, Renewal added, Renewal updated, Renewal renewed, Renewal deleted, Renewal reminder sent, Renewal requested, Ticket opened, Ticket assigned, Ticket status changed, Ticket priority changed, Ticket reopened, Ticket closed, Ticket SLA alert. Jobs record the actor as "System".

## Settings

| Property (environment variable) | Default | Meaning |
|---|---|---|
| `app.hourly-rate` (`HOURLY_RATE`) | `25000` | RWF per hour for `invoice-suggestion` and the profit report. |
| `app.jobs.enabled` (`JOBS_ENABLED`) | `true` | Turns both scheduled jobs on or off. The admin endpoints always work. Tests set it to `false`. |
| `app.jobs.renewal-reminders-cron` (`RENEWAL_REMINDERS_CRON`) | `0 0 7 * * *` | Daily at 07:00 server time. |
| `app.jobs.ticket-sla-cron` (`TICKET_SLA_CRON`) | `0 */15 * * * *` | Every 15 minutes. |
