# Digital Project Delivery Management System (DPDMS)

A web system for Itzetech that manages a client project from the first request to ongoing care after launch:
requests and quotes, task assignment, design review and approval, invoices and payments, and the renewals
(domain, hosting, SSL certificate, maintenance) and support tickets that follow every launch.

| Part | Technology | Folder |
|---|---|---|
| Backend (REST API) | Java 21, Spring Boot 4, Spring Security (JWT), Spring Data JPA, Flyway | `backend/` |
| Frontend | Angular (TypeScript), standalone components | `frontend/` |
| Database | PostgreSQL 16 | |

The clickable prototype that this system is built from is `../dpdms-prototype.html`. The written description
is `../DPDMS-system-description.docx`. The API reference is `docs/api.md` (stages 1 and 2);
the stage 3 contract (invoices and payments, renewals, support tickets, reports) is `docs/api-stage3.md`.

## Run it

You need Java 21 and Node 22 or newer. You do **not** need to install PostgreSQL to try it.

**1. Start the backend** (port 8080). The `embedded` profile starts a real PostgreSQL inside the application and keeps
its data in `backend/.pgdata`:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=embedded
```

On a brand-new database the backend creates one administrator: `admin@itzetech.local` with password
`ChangeMe#2026`. Sign in, choose a new password, then enter the 6-digit code. In development the code is shown on
screen and written to the backend console, because no mail server is configured.

To run the embedded database from a packaged jar instead of `spring-boot:run`, build it with the `embedded` Maven
profile (the normal jar leaves the embedded PostgreSQL out; this one is about 180 MB because it carries the database
binaries, so use it for development only):

```bash
cd backend
./mvnw package -Pembedded -DskipTests
java -jar target/dpdms-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=embedded
```

**2. Start the frontend** (port 4200):

```bash
cd frontend
npm install
npm start
```

Open http://localhost:4200.

To start a second, separate backend (for example a test copy) next to a running one, give it its own port, database
port and data folder: `SERVER_PORT=8081 EMBEDDED_PG_PORT=5434 EMBEDDED_PG_DATA=.pgdata-test ./mvnw spring-boot:run -Dspring-boot.run.profiles=embedded`.

### Using a normal PostgreSQL instead

```bash
docker compose up -d            # or install PostgreSQL and create a database "dpdms" with user "dpdms"
cd backend
./mvnw spring-boot:run          # defaults: jdbc:postgresql://localhost:5432/dpdms, user dpdms, password dpdms
```

### Settings (environment variables)

| Variable | Meaning | Default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | database connection | local `dpdms` database |
| `JWT_SECRET` | signing key, at least 32 characters. **Always set in production** | development value |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | first administrator on an empty database | `admin@itzetech.local` / `ChangeMe#2026` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP server. Empty = e-mails are written to the log | empty |
| `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` | where the Angular app is served | `http://localhost:4200` |
| `EXPOSE_SECRETS` | development only: return one-time codes and temporary passwords in API responses | `false` |
| `EMBEDDED_PG_PORT`, `EMBEDDED_PG_DATA` | `embedded` profile only: port and data folder of the built-in PostgreSQL. Change both to run a second copy of the backend without sharing the first one's database | `5433` / `.pgdata` |
| `JOBS_ENABLED` | run the daily scheduled jobs (renewal reminders, ticket deadline check). They can always be run by hand from `/api/admin/...` | `true` |
| `RENEWAL_REMINDERS_CRON`, `TICKET_SLA_CRON` | when the jobs run (Spring cron, server time) | `0 0 7 * * *` / `0 */15 * * * *` |
| `HOURLY_RATE` | RWF per hour, used to cost logged hours in invoice suggestions and the profit report | `25000` |

## Tests

```bash
cd backend
./mvnw test      # runs against a real, throw-away PostgreSQL started by the tests
```

## How the work is organised

Built in stages, each one working end to end before the next starts:

1. **Foundation**: sign-in, client self-registration with e-mail code, staff accounts, roles, notifications, audit trail.
2. **Delivery**: requests, quotes, projects, task assignment, work submission and review, approvals, time logs, updates feed.
3. **Money and care**: invoices and payments, renewals with reminders, support tickets with deadlines, reports.

## Security notes

- Passwords are stored as BCrypt hashes. Administrators need an e-mailed code as a second step.
- Five wrong passwords lock an account for 15 minutes.
- Deactivating an account, or changing a role, takes effect on the next request (accounts are checked on every call).
- Never enable `EXPOSE_SECRETS` outside development, and always set your own `JWT_SECRET`.
