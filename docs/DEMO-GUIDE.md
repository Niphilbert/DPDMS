# DPDMS demo guide

Digital Project Delivery Management System for ITZETech Group. About 15 minutes.

## 1. Before you start

Start the two parts in two terminals (Java 21 and Node 22 or newer are needed; no database install is needed).

- Backend: in the folder backend, run: ./mvnw spring-boot:run -Dspring-boot.run.profiles=embedded
- Frontend: in the folder frontend, run: npm start

Wait until the backend says it has started (about 30 seconds), then open http://localhost:4200 in the browser. Use a second browser window in private mode for the client, so that two people can be signed in at the same time.

Demo accounts (the password is in the file .dev-credentials, which stays on your computer):

- Administrator: admin@itzetech.local
- Project manager: jane.pm@itzetech.local
- Developer: eric.dev@itzetech.local
- Client: jean.client@acme.example

Tip: the administrator signs in with a password and then a 6-digit code. In development the code is shown on the screen, because no mail server is set up. Say this out loud, so nobody thinks it is a mistake.

## 2. The story to tell

One client (Acme Hotels) asks ITZETech for a hotel booking website. The system follows that client from the first request to the care of the website after it goes live. Say this in one sentence before you click anything.

## 3. The demo, step by step

### Part A. The client asks for a project (Client window)

1. Sign in as the client and open Requests and quotes.
2. Click New request. Fill in a title, a description and a deadline, then send it.
3. Point out: the status is "Waiting for a quote" and the managers were notified (the bell).

### Part B. The quote (Project manager window)

1. Sign in as the project manager. Open Requests and quotes and open the new request.
2. Write a quote with two or three lines, the number of weeks and a valid-until date. Send it.
3. Back in the client window, ask for changes with a short note. The manager revises the quote. The client accepts.
4. Point out: accepting creates the project, with four starter tasks and a budget equal to the quote.

### Part C. Running the project (Project manager, then Developer)

1. As the manager, open Projects and the new project. Assign the Design and Development tasks to the developer (the list of people comes from the system). Click Start project.
2. As the developer, open My tasks. Move Design to Doing, log some hours (try 30 hours to show the validation message), then Submit work with a link.
3. Point out: the developer sees no budget and no money anywhere.
4. As the manager, open the task in review. Send it back with a comment once, then mark it done after it is submitted again. Show the progress bar change.

### Part D. Approvals and changes (Manager, then Client)

1. As the manager, open the Approvals tab and send "Home page design" for approval.
2. As the client, approve it. Point out that progress and the updates feed change at once.
3. As the manager, open Change requests and propose an extra module with a price. The client accepts, and the project budget goes up.

### Part E. Money (Manager, then Client)

1. As the manager, open Invoices and click New invoice. Use the suggested hours line, add a line, set a tax rate and a due date. Show the live total.
2. Send it. Record a partial payment (mobile money). Try an amount that is too big to show that it is refused. Record the rest and watch the status become Paid.
3. As the client, open Invoices: only sent invoices are visible, with the balance and the payment history. There are no edit buttons.
4. Point out the red "Overdue" invoice and the CSV export.

### Part F. Care after launch (Administrator, then Client)

1. Renewals: show domain, hosting, SSL and maintenance items with their status. As the administrator, enter a pretend date and click Run reminders now. Show that the client received notifications.
2. Support tickets: as the client, raise an urgent ticket. As the manager, assign it to the developer, write an internal note and a public reply.
3. Show that the client cannot see the internal note. Show the countdown to the answer deadline (4 hours for urgent, 8 for high, 24 for medium, 72 for low).
4. As the administrator, enter a pretend time 10 hours ahead and click Run SLA check now to show the breach alert.

### Part G. The owner's view (Administrator)

1. Reports: money, projects, tickets, renewals and the revenue chart. Show the date filter and the CSV export.
2. Users and access: add a staff member, change a role, deactivate someone. Explain that a deactivated person is locked out immediately.
3. Audit trail: every important action, who did it and when.

## 4. Rules worth mentioning (they show the design)

- Four roles: administrator, project manager, developer and client. Each sees only what belongs to them.
- Developers never see money. Clients see only their own company, and never see internal notes.
- Passwords are stored as hashes. Administrators need a code by e-mail. Five wrong passwords lock the account for 15 minutes.
- Everything important is written to the audit trail.
- The money, deadlines and reminders follow fixed rules, not guesses: SLA times by priority, reminders at 60, 30, 14, 7 days and on expiry, overdue detected automatically.

## 5. Questions the examiners may ask

- Why these technologies? Java and Spring Boot for a secure, well-tested backend; Angular for a structured, typed frontend; PostgreSQL for reliable money and history records.
- How do you know it works? The backend has 91 automated tests on a real PostgreSQL database and the frontend has 115 tests. All four roles were also run through the whole flow in a browser.
- What is not done? Real e-mail sending is not tested, there is no online payment gateway (payments are recorded by hand), and it has not been tested on a normal PostgreSQL server or on very small phones in every screen.
- What would you do next? Online mobile-money payments, real e-mail, a file upload store, and hosting.

## 6. If something goes wrong

- The page does not open: check that both terminals are still running. The backend must say "Started".
- Port already in use: close the other program that uses port 8080, 5433 or 4200, or start the frontend on another port with: npx ng serve --port 4300
- Forgot the administrator password: stop the backend, delete the folder backend/.pgdata, and start again. This also removes the demo data. The administrator is then admin@itzetech.local with the starting password ChangeMe#2026, and the system asks for a new one.
- A code does not arrive: in development it is shown on the screen and written in the backend terminal.
- You need fresh demo data: after deleting backend/.pgdata, create the people with Users and access, and register a client on the Register page.
