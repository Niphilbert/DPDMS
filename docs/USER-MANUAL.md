# DPDMS User Manual - Digital Project Delivery Management System of ITZETech Group

## 1. About the system

### 1.1 What DPDMS is

DPDMS is the one place where ITZETech Group and its clients work together on a project. It follows a project from the first request to the care of the finished website.

You use it in a web browser, on a computer or a phone. You do not install anything.

Clients use it to ask for work, answer quotes, follow progress, approve designs, see invoices and ask for help. Project managers run the projects and the money. Developers do the tasks and log their hours. The administrator looks after the people and sees everything.

### 1.2 The journey of one client project

Every project follows the same steps. Each step is a screen in the system.

1. **Request.** The client sends a request that says what is needed and by when.
2. **Quote.** A project manager writes a quote with prices, the number of weeks and a valid-until date. The client accepts it, asks for changes or declines it.
3. **Project.** When the client accepts the quote, the project is created at once. It starts in **Planning** with four starter tasks.
4. **Tasks.** The project manager gives the tasks to developers and clicks **Start the project**. Developers do the work, log their hours and hand the work in for review.
5. **Approvals.** The project manager sends designs, page content or documents to the client. The client approves them or asks for changes.
6. **Change requests.** If extra work is needed, the project manager proposes it with a price. The client accepts or declines it.
7. **Invoices.** The project manager makes an invoice and sends it to the client.
8. **Payment.** The client pays outside the system. The project manager records the payment in the system.
9. **Launch.** When every task is done, the project becomes **Launched**.
10. **Renewals.** Domains, hosting, SSL certificates and maintenance plans are listed with their expiry dates. The system sends reminders before they expire.
11. **Support.** After launch the client can open a support ticket. Every ticket has an answer deadline.

### 1.3 The four roles

- **Client.** A person from a company that orders work from ITZETech.
- **Project manager.** Runs requests, quotes, projects, invoices, renewals and support tickets.
- **Developer.** Does the tasks that are given to them and works on tickets assigned to them.
- **Administrator.** Does everything a project manager does, and also looks after users, developer access and the audit trail.

> Note: Clients create their own account. Project managers, developers and administrators are added by an administrator.

### 1.4 Who can do what

The table shows the main areas of the system. "Yes" means the person can use the area. "Own" means only the company's or the person's own items.

| Area | Client | Project manager | Developer | Administrator |
|---|---|---|---|---|
| Send a request | Yes | No | No | No |
| See requests and quotes | Own company | All | No | All |
| Write and revise quotes | No | Yes | No | Yes |
| Accept, change or decline a quote | Yes | No | No | No |
| See projects | Own company | All | Own tasks only | All |
| Start a project, add and assign tasks | No | Yes | No | Yes |
| Move tasks, hand in work, log hours | No | Own tasks | Own tasks | Own tasks |
| Review work (Mark done, Send back) | No | Yes | No | Yes |
| Approvals | Answer them | Send them | No | Send them |
| Change requests | Answer them | Propose them | No | Propose them |
| See budget and money of a project | Yes | Yes | Never | Yes |
| See invoices | Sent ones, own company | All | Never | All |
| Make, send and void invoices, record payments | No | Yes | No | Yes |
| See renewals | Own company | All | Read only, no prices | All |
| Add, renew, edit, delete renewals | No | Yes | No | Yes |
| Ask for a renewal | Yes | No | No | No |
| Open a support ticket | Yes | No | No | No |
| Work on support tickets | No | All tickets | Assigned tickets | All tickets |
| Assign tickets, change priority | No | Yes | No | Yes |
| Write internal notes on tickets | Never | Yes | Yes | Yes |
| Reports | No | Yes | No | Yes |
| Users and access | No | No | No | Yes |
| Developer access to a project | No | No | No | Yes |
| Audit trail | No | No | No | Yes |
| Run reminders now, Run SLA check now | No | No | No | Yes |

### 1.5 What each role never sees

- **Developers never see money.** They have no Invoices and no Reports in their menu. Project budgets, invoice amounts, payments and renewal prices are hidden from them.
- **Developers never see requests, quotes, approvals or change requests.** They see only the projects where a task is given to them, or where the administrator gave them access. They see only the support tickets assigned to them.
- **Clients see only their own company.** They never see another company's requests, projects, invoices, renewals or tickets.
- **Clients never see internal notes.** Internal notes on tickets and the internal note on a renewal are for staff only.
- **Clients never see invoice drafts.** An invoice becomes visible to the client only after it is sent.
- **Clients see a simple task list.** They see the task name, phase, status and due date. They do not see who does the task, the hours or the files that were handed in.
- **Clients never see Reports, Time, Users and access or Audit trail.**
- **Project managers never see Users and access, Audit trail or the Developer access tab.** They cannot run the reminder and deadline tools.

## 2. Getting started

### 2.1 Opening the system

ITZETech gives you the address of the system. Type it in the address bar of your browser, or open the link from the e-mail you received. The **Sign in** page opens.

> Tip: Save the address as a bookmark so you can find it again.

### 2.2 Creating a client account

#### Create your client account

**Who:** A new client. Staff do not use this page; an administrator creates staff accounts.

1. Open the **Sign in** page.
2. Below the form, next to **New client?**, click **Create your account**.
3. Fill in **Company or organisation**, **Your full name** and **Email**.
4. Fill in **Phone (optional)** if you want to.
5. Type a **Password**. Use at least 8 characters, with letters and a number.
6. Type the same password again in **Repeat password**.
7. Tick **I accept the terms of use**.
8. Click **Create account**.
9. The page **Confirm your email** opens. Open your e-mail and find the 6-digit code. Type it in **6-digit code**. Check that **Email** shows your address.
10. Click **Confirm and continue**.

**What you will see:** Your dashboard opens and you are signed in. If a field is wrong, the message **Please correct the fields marked below.** appears and the wrong fields show a short message, for example **Enter a valid email address.**

> Note: The code works for 10 minutes. After 5 wrong tries the code stops working and you must ask for a new one.

> Warning: Each new account creates its own company space. Two colleagues who each create an account will not see each other's requests and projects. Agree with your colleagues who creates the account for your company.

#### Ask for a new confirmation code

**Who:** A client who did not receive the code, or whose code is wrong or old.

1. On the **Confirm your email** page, check that **Email** is correct.
2. Click **Send a new code**.
3. Wait for the new e-mail and type the new code in **6-digit code**.
4. Click **Confirm and continue**.

**What you will see:** The message **A new code is on its way. Check your email.** The old code stops working.

> Tip: If you ask again too soon you see **A code was sent a moment ago. Please wait a minute before asking for another one; the code we already sent still works.** Wait one minute, or look again for the first e-mail (also in the spam folder).

#### Confirm your e-mail later

**Who:** A client who created an account but did not finish the code step.

1. Open the **Sign in** page and sign in with your e-mail and password.
2. The message **Please confirm your email address first. We can send you a new code.** appears. Click **Confirm your email**.
3. Type the 6-digit code, or click **Send a new code** first.
4. Click **Confirm and continue**.

**What you will see:** Your dashboard opens.

### 2.3 Signing in

#### Sign in (client, project manager, developer)

**Who:** Everyone except the administrator. The administrator has one extra step (see the next task).

1. Open the **Sign in** page.
2. Type your e-mail in **Email**.
3. Type your password in **Password**. Click **Show** to see what you typed, and **Hide** to hide it again.
4. Click **Sign in**.

**What you will see:** The **Dashboard** opens with your name at the top. If the e-mail or password is wrong you see **The email or password is incorrect.**

> Tip: The system gives the same message when the e-mail is not known, so that nobody can find out who has an account.

#### Sign in as administrator (second code)

**Who:** Administrators.

1. Sign in with your **Email** and **Password** as above and click **Sign in**.
2. The page **Check your email** opens. Open your e-mail and find the 6-digit code.
3. Type it in **6-digit code**.
4. Click **Continue**.

**What you will see:** The **Dashboard** opens. If the code is wrong you see **That code is not right.** If it is older than 10 minutes you see **That code has expired. Ask for a new one.** After 5 wrong tries you see **Too many wrong attempts. Ask for a new code.**

> Tip: To get a new code, click **Back to sign in** and sign in again. If you do this within one minute of the last code, wait one minute first.

> Note: If you see a box marked "Development only" with a code on the code page, the system is in a test setup. Use the code shown there.

#### First sign-in: choose your own password

**Who:** Every staff member the first time they sign in, and anyone whose password was reset by an administrator.

1. Sign in with your e-mail and the temporary password you received. The page **Choose a new password** opens.
2. Type your new password in **New password**.
3. Type it again in **Repeat new password**.
4. Click **Save and continue**.

**What you will see:** You are signed in. Administrators then see the **Check your email** page and must type their 6-digit code (see the task above). If the two passwords differ you see **The two passwords are not the same.** If you choose the temporary password again you see **Choose a different password from the temporary one.**

> Note: Click **Cancel** to go back to the sign-in page. This step is valid for 15 minutes. After that you see **This step has expired. Please sign in again.**

### 2.4 Password rules and locking

A password needs at least 8 characters, with letters and a number. The most it can have is 100 characters. The system shows **Use at least 8 characters, with letters and a number.** when a password is too weak.

If you type a wrong password 5 times in a row, your account is locked for 15 minutes. You see **Too many wrong passwords. Try again in a few minutes.** Wait 15 minutes and try again. An administrator can also unlock you by resetting your password.

If your account was deactivated you see **This account is deactivated. Ask the administrator to reactivate it.**

### 2.5 Forgotten password

There is no "forgot password" link. Ask the administrator to reset your password. The administrator gives you a temporary password. You sign in with it and choose a new password (see "First sign-in: choose your own password").

> Tip: Resetting also unlocks an account that was locked after wrong passwords.

> Note: The system has no profile page, so you cannot change your own password while you are signed in. Ask the administrator to reset it if you need a new one.

### 2.6 Signing out

#### Sign out

**Who:** Everyone.

1. Look at the bottom of the left menu. It shows your name and role.
2. Click **Sign out**.

**What you will see:** The **Sign in** page opens.

> Tip: You stay signed in for about 8 hours, and a page refresh keeps you signed in. If you close the browser tab you are signed out. If you are signed out in the middle of work you go back to the sign-in page; sign in again.

### 2.7 The screen layout

After you sign in, every page has the same frame.

- **Left menu.** The list of pages for your role, in groups (Overview, Delivery, Billing and care, Administration). The page you are on is highlighted. The **Notifications** entry shows a number when you have unread messages.
- **Top bar.** A greeting such as **Good morning, Jean**, today's date, the bell and your name.
- **Bell.** Shows a number for unread notifications. Click it to open the list.
- **Bottom of the left menu.** Your name, your role (for clients also the company) and **Sign out**.
- **Page area.** The page you opened.

On a phone or a narrow window the menu is hidden. Click the menu button (three lines) at the top left to open it. Press the Escape key to close the menu or the bell list.

The menu for each role:

| Role | Menu |
|---|---|
| Client | Dashboard, Requests and quotes, My projects, Invoices, My websites and renewals, Support, Notifications |
| Project manager | Dashboard, Requests and quotes, Projects, My tasks, Invoices, Renewals, Support tickets, Reports, Notifications |
| Developer | Dashboard, My tasks, Projects, Renewals, My tickets, Notifications |
| Administrator | Dashboard, Requests and quotes, Projects, Invoices, Renewals, Support tickets, Reports, Notifications, Users and access, Audit trail |

> Note: If you type the address of a page that is not for your role, the system takes you back to the dashboard.

#### Use the bell and notifications

**Who:** Everyone.

1. Look at the bell at the top right. A number shows how many notifications are unread.
2. Click the bell. The list **Notifications** opens with your newest messages. New ones have a **New** mark.
3. Click a message. It is marked as read and the page it is about opens.
4. Click **Mark all as read** to mark every message as read.
5. Click **See all notifications** to open the full page.

**What you will see:** On the **Notifications** page every message shows **New** or **Read**. The list shows your 50 newest messages. The number on the bell updates by itself about every 30 seconds. When there is nothing you see **Nothing new. We will tell you here when something needs you.**

> Tip: Section 7.8 lists what makes the system send you a notification.

### 2.8 The dashboard

The **Dashboard** is the first page after you sign in. The title is **Welcome, Jean** for clients, **Hello, Eric** for developers and **Delivery and care overview** for project managers and administrators. Each number is a link to the page it is about.

The first row of numbers:

| Number | Who sees it | What it counts |
|---|---|---|
| Open requests | Client, project manager, administrator | Requests that are not finished: Waiting for a quote, Quote sent or Changes asked. Staff also see how many "need a quote". |
| Active projects | Everyone | Projects that are not yet Launched. |
| My tasks | Project manager, developer, administrator | Tasks given to you that are not done. The small text shows how many are late. |
| Pending approvals | Client, project manager, administrator | Items waiting for the client's answer. The client sees "Waiting for your answer". |

The second row, "Money and care":

| Number | Who sees it | What it counts |
|---|---|---|
| Unpaid invoices (clients see Balance due) | Client, project manager, administrator | Sent or partly paid invoices, with the amount still to be paid. |
| Overdue invoices | Client, project manager, administrator | Invoices past their due date, with the amount. Staff also see how many drafts exist. |
| Expiring renewals | Everyone | Domains, hosting and similar items that expire within 30 days. The small text shows how many have already expired. |
| Open tickets | Everyone | Support tickets not yet resolved. Staff see how many are past deadline and unassigned. Clients see how many wait for them. |
| Tickets past deadline | Project manager, developer, administrator | Open tickets whose answer deadline was missed, and how many more are at risk. |

Below the numbers:

- **Notifications.** Your 5 newest messages and how many are unread.
- **Needs attention.** Small bars that show how much of your work is late: **Late tasks**, **Overdue invoices**, **Tickets past deadline** and **Renewals expired**.
- **Where to start.** Shortcut tiles for your role. The administrator also sees an **Administration** box with **Users and access** and **Audit trail**.

> Note: Developers never see the invoice numbers. If the numbers cannot load you see **Could not load the numbers. Please reload the page.**

## 3. Guide for clients

### 3.1 Requests and quotes

A request is your message to ITZETech that says what you need. ITZETech answers with a quote.

#### Send a request

**Who:** Clients.

1. In the left menu click **Requests and quotes**.
2. Click **New request**.
3. In **What do you need?** write a short title, for example **Booking website**.
4. In **Describe the work** explain what you need.
5. In **Your budget (optional)** you may write your idea of the budget, for example **RWF 3,000,000**.
6. In **Wanted by** choose the date you need the work finished. It must be after today.
7. Click **Send request**.

**What you will see:** The request page opens with the status **Waiting for a quote**. The project managers are told. The page says **A project manager is preparing your quote. We will tell you here when it is ready.** If something is missing you see messages such as **Enter a short title.** or **Describe what you need.**

#### Find your requests

**Who:** Clients.

1. Click **Requests and quotes** in the left menu.
2. Click a filter button to narrow the list: **All**, **Waiting for a quote**, **Quote sent**, **Changes asked**, **Accepted** or **Declined**.
3. Click a request to open it.

**What you will see:** A table with **Request**, **Status**, **Quote** and **Sent**. Under each title you see **Wanted by** and the date. If you sent nothing yet you see **You have not sent any requests yet. Select "New request" to start.**

#### Read your quote

**Who:** Clients.

1. Open the notification "sent you a quote", or open the request from **Requests and quotes**. The status is **Quote sent**.
2. Read the card **Quote**. It lists each piece of work with its amount.
3. Read the **Total**, the number of weeks and the **Valid until** date.

**What you will see:** The quote with who sent it and when. On the right the card **Your answer** shows your three choices.

> Warning: A quote is only valid until the date on it. After that date you cannot accept it. You see **This quote is no longer valid. Ask the project manager to send a new quote.**

#### Accept the quote

**Who:** Clients.

1. Open the request with the status **Quote sent**.
2. In the card **Your answer** click **Accept the quote**.

**What you will see:** The message **Quote accepted. Your project has been set up.** The status becomes **Accepted** and a card **Project created** appears with the button **Open the project**. The project starts in **Planning**, with a budget equal to the quote. The project managers are told.

#### Ask for changes to the quote

**Who:** Clients.

1. Open the request with the status **Quote sent**.
2. In the card **Your answer** click **Ask for changes**.
3. In **What should change?** write what you want different. This box must be filled in.
4. Click **Send**. Click **Cancel** if you changed your mind.

**What you will see:** The message **Your request for changes was sent.** The status becomes **Changes asked** and the page says **You asked for changes. We will send a revised quote.** Your words show in a yellow box **Changes asked for**. When the project manager sends a revised quote the status returns to **Quote sent**.

#### Decline the quote

**Who:** Clients.

1. Open the request with the status **Quote sent**.
2. In the card **Your answer** click **Decline**.
3. In **Reason (optional)** you may write why.
4. Click **Decline the quote**. Click **Cancel** if you changed your mind.

**What you will see:** The message **The quote was declined.** The status becomes **Declined**. Your reason shows in the box **Reason for declining**.

> Warning: A declined request is closed. To start again, send a new request.

### 3.2 Your projects

#### Open a project and follow the progress

**Who:** Clients.

1. In the left menu click **My projects**.
2. Click the card of your project. Each card shows the project name, its status, who manages it, the **Progress** percentage, the due date, the **Budget** and warnings such as **2 late** or **1 to approve**.
3. On the project page read **Overall progress** and the four phases: **Discovery**, **Design**, **Build** and **Test and launch**.
4. Read **Start date**, **Due** and **Budget** under the bars.

**What you will see:** The project status is one of **Planning**, **In progress**, **In review** or **Launched**. In **Planning** you see **Work has not started yet. We will tell you when it does.** The percentage is the share of planned work that is done. If you have no project yet you see **Your project appears here as soon as you accept a quote.**

> Tip: The tabs on the project page are **Tasks**, **Approvals**, **Change requests**, **Money** and **Updates**. Click a tab to open it.

#### See the tasks

**Who:** Clients.

1. Open your project.
2. Click the tab **Tasks**.

**What you will see:** A table with **Task**, **Phase**, **Status** and **Due**. A task that is behind shows a red **Late** mark. Task statuses are **To do**, **Doing**, **In review** and **Done**. You do not see who does a task or how many hours it takes. If there are no tasks you see **No tasks yet.**

#### Approve a design, page content or document

**Who:** Clients.

1. Open the notification "to approve", or open your project. A yellow bar says **1 item is waiting for your answer.** Click **Review now**, or click the tab **Approvals**.
2. Find the item with the mark **Waiting for answer**. Open the link under its name to look at the work. The name also shows the version, for example **version 2**, and the type: **Design**, **Page content** or **Document**.
3. Click **Approve**.
4. In **Comment (optional)** you may write a remark.
5. Click **Approve** again to confirm. Click **Cancel** to go back.

**What you will see:** The message **Approved. Thank you.** The item shows **Approved**. If the item was linked to a task, that task becomes **Done** and the progress goes up. The project manager is told and the **Updates** tab gets a new line.

#### Ask for changes to an approval item

**Who:** Clients.

1. Open the tab **Approvals** of your project.
2. Find the item with the mark **Waiting for answer** and click **Request changes**.
3. In **What should change?** write what must be different. This box must be filled in.
4. Click **Ask for changes**.

**What you will see:** The message **Your request for changes was sent.** The item shows **Changes asked**. The project manager sends a new version, which appears as a new line with a higher version number. If you leave the box empty you see **Tell us what should change.**

> Note: Once an item is answered you cannot answer it again. If you try, the system says **This was already answered.**

#### Answer a change request

**Who:** Clients.

A change request is extra work with its own price, proposed by ITZETech after your quote was accepted.

1. Open your project and click the tab **Change requests**.
2. Find the line with the mark **Waiting for answer**. It shows the extra work and its price.
3. Click **Accept** with the amount, for example **Accept (400,000 RWF)**, or click **Decline**.

**What you will see:** After accepting you see **Change accepted. The budget was updated.** and the price is added to the project **Budget**. After declining you see **Change declined** and the budget stays the same. The line shows **Accepted** or **Declined** with the date.

> Tip: Accepting adds the price to the project budget, so check the amount before you click.

#### Read the updates feed

**Who:** Clients.

1. Open your project and click the tab **Updates**.

**What you will see:** A list of plain sentences with the date and time, newest first, for example **Work has started on your project.**, **Finished: Design** or **Waiting for your approval: Home page design (version 1)**. If nothing happened yet you see **No updates yet.**

### 3.3 Invoices and payments

#### See your invoices and what you owe

**Who:** Clients.

1. In the left menu click **Invoices**.
2. Read the four boxes at the top: **Invoiced**, **Paid**, **Balance due** and **Overdue**.
3. Click a status button to filter the list: **All**, **Sent**, **Partly paid**, **Overdue**, **Paid** or **Void**.
4. Click an invoice to open it.

**What you will see:** A table with **Invoice**, **Issued**, **Due**, **Total**, **Balance** and **Status**. An invoice past its due date has the red status **Overdue** and a line such as **12 days late**. You only see invoices after ITZETech has sent them. If there are none you see **No invoices to show. When ITZETech sends you an invoice it appears here.**

#### Read one invoice

**Who:** Clients.

1. Open the invoice from **Invoices**.
2. Read the summary: **Invoice total**, **Paid so far**, **Balance due** and **Due date**.
3. Read the card **Lines** for the work that is billed, with **Subtotal**, tax and **Total**.
4. Read the card **Payments** for payments ITZETech has recorded.
5. Read the card **History** for what happened and when.

**What you will see:** A red bar tells you when the invoice is overdue. A grey bar tells you when an invoice was voided (cancelled); nothing is owed on it. The statuses are **Sent**, **Partly paid**, **Paid**, **Overdue** and **Void**.

#### See your payment history

**Who:** Clients.

1. In the left menu click **Invoices**.
2. Scroll down to the card **Payment history**.

**What you will see:** A table with **Date**, **Invoice**, **Method**, **Reference** and **Amount** for every payment ITZETech recorded. A payment that was cancelled shows the mark **Reversed**. If there is none you see **No payments yet.**

> Note: The history is built from your 30 newest sent invoices.

#### See the money of one project

**Who:** Clients.

1. Open your project and click the tab **Money**.

**What you will see:** The boxes **Budget**, **Invoiced**, **Paid**, **Balance due** and **Not invoiced yet**. If something is overdue a box **Overdue** appears. Below is the list **Invoices of this project**.

#### How paying works today

Paying is done outside the system. The system has no online payment button yet.

1. Open the invoice you want to pay. A blue box **How to pay.** tells you the amount and the invoice number.
2. Pay the balance by mobile money, bank transfer or cash, as agreed with ITZETech. Always quote the invoice number, for example INV-2026-0001.
3. ITZETech records your payment in the system when it arrives.
4. You receive a notification, for example **We received your payment of 400,000 RWF for invoice INV-2026-0001.** The invoice then shows **Partly paid** or **Paid**.

**What you will see:** The balance goes down and the payment appears in **Payments** on the invoice and in **Payment history**.

> Note: Online payment with mobile money (MTN MoMo) is planned and will come later. Until then, payments are recorded by the project manager and you cannot pay inside the system.

> Tip: If you paid and the invoice still shows a balance, wait until ITZETech has recorded the payment, or contact your project manager.

### 3.4 Websites and renewals

The page **My websites and renewals** lists the domains, hosting plans, SSL certificates and maintenance plans of your projects, with their expiry dates. ITZETech also sends you reminders by notification and e-mail 60, 30, 14 and 7 days before the expiry date, and once on the expiry day.

#### See expiry dates

**Who:** Clients.

1. In the left menu click **My websites and renewals**.
2. Read the boxes **Expiring soon**, **Expired** and **Active**.
3. Use the buttons **All**, **Expiring soon**, **Expired** and **Active** to filter, the list **Type** to choose one kind of item, and **Expiring within (days)** to see items that end soon.
4. Click an item to open its page.

**What you will see:** A table with **Item**, **Project**, **Expires**, **Status** and **Price**. Under the date you read how long is left, for example **in 12 days**. The status is **Active** (more than 30 days left), **Expiring soon** (30 days or less) or **Expired**. A yellow bar tells you how many items need attention. The price is the price of the next renewal. You do not see the internal note.

#### Request a renewal

**Who:** Clients.

1. Open **My websites and renewals**.
2. On the line of an item that is **Expiring soon** or **Expired**, click **Request renewal**. You can also open the item and click **Request renewal** at the top.

**What you will see:** The message **We told ITZETech you want this renewed.** The button changes to **Requested** (in the list) or **Renewal requested** (on the item page). ITZETech renews it and you receive a notification when it is renewed. The page **Renewal history** shows each earlier renewal.

> Note: In the list the button only appears for items that are not **Active**. On the item page you can always ask.

### 3.5 Support

When something is wrong with your website after launch, open a support ticket. The page is called **Support**.

#### Open a support ticket

**Who:** Clients.

1. In the left menu click **Support**.
2. Click **New ticket**.
3. In **Subject** write a short title, for example **The booking page shows an error**.
4. In **What happened?** describe the problem.
5. In **Priority** choose how urgent it is: **Urgent (answer within 4 h)**, **High (answer within 8 h)**, **Medium (answer within 24 h)** or **Low (answer within 72 h)**.
6. In **Project (optional)** choose a project, or leave **Not about one project**.
7. Click **Send ticket**.

**What you will see:** The message **Ticket TCK-0007 sent** (with your number) and the ticket page opens with the status **Open**. ITZETech staff are told. If a field is empty you see **Add a short title.** or **Describe the problem.**

> Tip: Choose the priority honestly. A higher priority means a shorter answer time, and the project manager can change it.

#### Follow the answer deadline

**Who:** Clients.

1. Open **Support** and look at the column **Answer deadline**, or open the ticket and read the card **Answer deadline**.

**What you will see:** A mark and the time left: **On track**, **At risk** (the deadline is close), **Deadline missed**, or **Deadline met** after the ticket is resolved. Text such as **2 h 5 min left** or **breached by 40 min**, and the exact deadline date and hour. The boxes at the top show **Open**, **Past deadline**, **At risk** and **Waiting for you**.

> Note: The hours are normal calendar hours. Nights and weekends count. The clock does not stop while ITZETech waits for your answer.

#### Read and reply to a ticket

**Who:** Clients.

1. Open **Support** and click your ticket.
2. Read **What happened** and the **Conversation**.
3. If you see **ITZETech is waiting for your answer. Write a comment below to continue.**, answer the question.
4. In **Write a reply** type your message.
5. Click **Send reply**.

**What you will see:** The toast **Reply sent** and your message in the conversation. A ticket in **Waiting for the client** returns to **In progress** when you reply. You never see internal notes.

> Warning: You cannot write on a ticket that is **Closed**. You see **Closed tickets cannot receive new messages.**

#### Reopen a resolved ticket

**Who:** Clients.

1. Open the ticket with the status **Resolved**.
2. In the card **Actions** click **Reopen: not fixed**.

**What you will see:** The toast **Ticket reopened**. The status becomes **In progress** and the answer deadline starts again.

> Warning: You can reopen a ticket only once. After that you see **This ticket was already reopened once, so it cannot be reopened again.** Open a new ticket and mention the old number.

#### Close a ticket

**Who:** Clients.

1. Open the ticket.
2. In the card **Actions** click **Close the ticket**.

**What you will see:** The toast **Ticket closed**. The status becomes **Closed**. A closed ticket cannot be changed. If the problem comes back, open a new ticket.

## 4. Guide for project managers

Administrators can do everything in this chapter too.

### 4.1 Requests and quotes

#### Open a new request

**Who:** Project managers and administrators.

1. Click **Requests and quotes** in the left menu. You also get a notification **New request from ...** when a client sends one.
2. Use the buttons **All**, **Waiting for a quote**, **Quote sent**, **Changes asked**, **Accepted** or **Declined** to filter.
3. Click the request to open it.

**What you will see:** The card **The request** shows **Client**, **Sent by**, **Wanted by**, **Budget idea** and **Description**. The column **Client** is also in the table.

#### Write a quote

**Who:** Project managers and administrators.

1. Open a request with the status **Waiting for a quote**.
2. In the card **Write the quote**, type in **What is included** the first piece of work.
3. In **Amount (RWF)** type its price.
4. Click **Add a line** for each extra piece of work. A quote can have up to 30 lines. Click **Remove** to delete a line.
5. Check the **Total**.
6. In **Weeks of work** type the number of weeks (1 to 104).
7. In **Valid until** choose the last day the quote is valid. It must be today or later.
8. Click **Send quote to client**.

**What you will see:** The toast **Quote sent to the client**. The status becomes **Quote sent** and the client is told. If something is wrong you see **Please check the quote.** and messages such as **Each amount must be more than zero.**

#### Revise a quote

**Who:** Project managers and administrators.

1. Open a request with the status **Changes asked**, or **Quote sent**. Read the yellow box **Changes asked for**.
2. In the card **Revise the quote** the lines of the last quote are already filled in. Change them, add or remove lines, and adjust **Weeks of work** and **Valid until**.
3. Click **Send revised quote**.

**What you will see:** The status becomes **Quote sent** again and the client is told that a revised quote is ready.

> Warning: You cannot quote a request that is **Accepted** or **Declined**.

#### What happens when the client accepts

When the client accepts, the system creates the project at once. The status is **Planning**, the budget equals the quote total, and four starter tasks are made: **Kick-off** (given to you, 8 hours), **Design** (20 hours), **Development** (60 hours) and **Testing and launch** (16 hours). The manager of the project is the person who wrote the quote. You get a notification. On the request page click **Open the project**.

### 4.2 Projects and tasks

#### Open a project

**Who:** Project managers and administrators.

1. Click **Projects** in the left menu.
2. Click the card of the project.

**What you will see:** The page shows the status, the client, **Managed by**, **Overall progress**, the four phases, **Start date**, **Due**, **Budget**, **Hours** (for example **11.75 logged of 104 planned**) and **Late tasks**. The tabs are **Tasks**, **Approvals**, **Change requests**, **Time**, **Money** and **Updates**. The administrator also has **Developer access**.

> Note: A task is late when it is not done and its due date is before today. Progress is the share of planned hours that belong to tasks that are done.

#### Add a task

**Who:** Project managers and administrators.

1. Open the project and the tab **Tasks**.
2. Click **Add a task**. The button is not shown when the project is **Launched**.
3. In **Task name** type the name.
4. In **Phase** choose **Discovery**, **Design**, **Build** or **Test and launch**.
5. In **Planned hours** type the hours (1 to 2000).
6. In **Due date** choose a date from today onwards.
7. In **Assign to (optional)** choose a person, or leave **Nobody yet**.
8. Click **Save**.

**What you will see:** The toast **Task added** and a new card in the column **To do**. If a field is wrong you see **Please correct the fields marked below.** with messages such as **Enter the task name.**

#### Assign or reassign a task

**Who:** Project managers and administrators.

1. On the task card click **Assign** (or **Reassign** if someone already has it).
2. In **Give the task to** choose a person. The list shows active developers and project managers with their role.
3. In **Due date** choose the date.
4. In **Note for them (optional)** you may write advice.
5. Click **Save**.

**What you will see:** The toast **Task assigned**. The person gets a notification with your note. A person who had the task is told it was reassigned. You see **Choose a person.** if you forgot to choose.

> Note: A task that is **Done** cannot be reassigned. You can assign tasks while the project is still in **Planning**.

#### Start the project

**Who:** Project managers and administrators.

1. Open a project with the status **Planning**. The blue box says **This project is in planning. Assign the tasks, then start it. Developers can only move tasks and log time after it starts.**
2. Click **Start the project** at the top right.

**What you will see:** The toast **Project started**. The status becomes **In progress**. The client is told and the updates feed gets **Work has started on your project.** Everyone with a task is told **Your tasks are ready.**

> Warning: Before the project starts, nobody can move a task, hand in work or log time. The system says **This project has not started yet. A project manager must start it before work can be moved, handed in or logged.**

### 4.3 Reviewing work

#### Review submitted work

**Who:** Project managers and administrators.

1. Open the notification "submitted ... for review", or open the project tab **Tasks**.
2. Look in the column **In review**. The card shows **Submitted by** with the person, the link or file name, and the note. Click the link to look at the work.
3. If the work is good, click **Mark done**.
4. If it needs changes, click **Send back**. In **What must change?** write clear comments and click **Save**.

**What you will see:** After **Mark done** the toast **Task marked done**, the card moves to **Done**, the progress goes up and the updates feed gets **Finished: ...**. After **Send back** the toast **Task sent back with your comments**, the card returns to **Doing** with a yellow box **Sent back:** and your words, and the developer is told.

> Tip: When the last task is marked done the project becomes **Launched** automatically, and the client and you get a notification.

#### Move a task yourself

**Who:** Project managers and administrators.

1. On a task card in **To do** or **Doing** find the list **Move to**.
2. Choose **To do**, **Doing** or **Done**.

**What you will see:** The toast with the new status, for example **Moved to doing**. This works on every task, not only your own. You cannot move a task to **In review** this way, because that needs a submission from the developer.

### 4.4 Approvals and change requests

#### Send an item for approval

**Who:** Project managers and administrators.

1. Open the project and the tab **Approvals**.
2. Click **Send for approval**.
3. In **Item name** type the name, for example **Home page design**.
4. In **Type** choose **Design**, **Page content** or **Document**.
5. In **Finishes task (optional)** choose a task if approving the item should finish that task, or leave **No task**.
6. In **Link or file name** paste the link or type the file name.
7. In **Message to the client (optional)** write a note.
8. Click **Send to client**.

**What you will see:** The toast **Sent to the client for approval**. The item shows **Waiting for answer** and the client is told. If you did not fill in the link you see **Add the file name or a link.**

> Tip: Use the same name again to send a new version, for example after the client asked for changes. The list shows **version 2**, **version 3** and so on.

#### Follow the answers

**Who:** Project managers and administrators.

1. Open the tab **Approvals** of the project.
2. Read the marks: **Waiting for answer**, **Approved** or **Changes asked**.

**What you will see:** For each item who sent it, when, who answered and the client's comment under **Answer:**. You get a notification each time the client answers.

#### Propose a change (extra work)

**Who:** Project managers and administrators.

1. Open the project and the tab **Change requests**.
2. Click **Propose a change**.
3. In **What is the extra work?** describe it.
4. In **Cost (RWF)** type the price.
5. Click **Send to client**.

**What you will see:** The toast **Change request sent to the client**. The line shows **Waiting for answer**. When the client accepts, the price is added to the project **Budget**.

> Note: There is no button to withdraw a change request. If one was sent by mistake, ask the client to decline it and propose a new one.

### 4.5 Time logs

#### Check hours on a project

**Who:** Project managers and administrators.

1. Open the project and the tab **Time**.

**What you will see:** The sentence **11.75 hours logged of 104 planned**, a table **Task**, **Person**, **Planned**, **Logged**, and below **Entries** with **Day**, **Task**, **Person**, **Hours** and **Note**. Without entries you see **No time has been logged yet.**

> Note: You log hours on the task card, and only on tasks that are given to you (see section 5.3). The screens have no button to change or delete a time entry.

### 4.6 Invoices

#### Find invoices

**Who:** Project managers and administrators.

1. Click **Invoices** in the left menu.
2. Read the boxes **Invoiced**, **Paid**, **Outstanding** and **Overdue**.
3. Use the status buttons **All**, **Drafts**, **Sent**, **Partly paid**, **Overdue**, **Paid** and **Void**.
4. Use **Client**, **Project**, **Issued from** and **Issued to** to narrow the list.
5. Click an invoice to open it.

**What you will see:** A table with **Invoice**, **Client**, **Issued**, **Due**, **Total**, **Balance** and **Status**, and the line **N invoices shown.** Overdue invoices are marked red with **N days late**. A draft shows **—** as balance.

#### Make a new invoice

**Who:** Project managers and administrators.

1. Click **Invoices** and then **New invoice**. You can also open a project, click the tab **Money** and click **New invoice**; the project is then already chosen.
2. In **Project (optional)** choose the project. Choose **No project (company invoice, for example a renewal fee)** for an invoice that is not about a project; then choose **Client company**.
3. Check **Issue date** (today) and **Due date** (14 days later). Change them if needed. The due date cannot be before the issue date.
4. If you chose a project, a blue box **Suggested from the project** shows the budget, what is already invoiced and what is not invoiced yet. Click **Add ... h logged at ...** to add a line with the hours logged on the project, billed at the hourly rate (normally 25,000 RWF per hour). Click **Add remaining budget (...)** to add a line with the part of the budget not yet invoiced.
5. Fill in or correct the lines: **Description**, **Quantity** and **Unit price (RWF)**. The **Line total** is shown. Click **Add a line** for more (up to 50). Click **Remove** to delete a line.
6. In **Tax rate (%)** type the tax, or leave 0.
7. In **Note for the client (optional)** write a note.
8. Check **Subtotal**, **Tax** and **Total**.
9. Click **Save draft**.

**What you will see:** The toast **Draft INV-2026-0001 saved** (with the new number) and the invoice opens with the status **Draft**. A blue bar says **This is a draft. The client cannot see it until you send it, and it can still be edited.**

> Tip: Nothing is saved until you click **Save draft**. A suggested line is only a help; you can change the text, quantity and price.

#### Edit a draft

**Who:** Project managers and administrators.

1. Open the invoice with the status **Draft**.
2. Click **Edit draft**.
3. Change what you need. The client cannot be changed.
4. Click **Save draft**.

**What you will see:** The toast **Draft ... saved**. The number does not change.

> Warning: Only a draft can be edited. A sent invoice cannot be changed. If it is wrong, void it and make a new one.

#### Send an invoice to the client

**Who:** Project managers and administrators.

1. Open the invoice with the status **Draft**.
2. Click **Send to client**.
3. Read the question **Send INV-... to the client?** It shows the total and the due date.
4. Click **Send invoice**.

**What you will see:** The toast **Invoice sent to the client**. The status becomes **Sent**. The client company is notified and e-mailed. The invoice can no longer be edited.

#### Record a payment

**Who:** Project managers and administrators.

1. Open an invoice with the status **Sent**, **Partly paid** or **Overdue**.
2. Click **Record payment**.
3. In **Amount (RWF)** type what the client really paid, in whole francs. The balance is already filled in; change it for a part payment.
4. In **Paid on** choose the date of payment. It cannot be in the future.
5. In **Method** choose **Mobile money**, **Bank transfer** or **Cash**.
6. In **Reference (optional)** type a reference, for example **MoMo 123456**.
7. Click **Record payment**.

**What you will see:** The toast **Payment recorded**, or **Payment recorded. The invoice is now paid in full.** The status becomes **Partly paid** or **Paid** and the client is told. The payment is added to the card **Payments**.

> Warning: A payment cannot be more than the balance. You see **That is more than the balance. Enter ... or less.** and nothing is recorded. An overdue invoice can still receive payments.

#### Reverse a payment

**Who:** Project managers and administrators.

1. Open the invoice. In the card **Payments** find the payment.
2. Click **Reverse**.
3. In **Reason (required)** write why, for example **cheque bounced**.
4. Click **Reverse payment**.

**What you will see:** The toast **Payment reversed**. The payment stays in the list with the mark **Reversed** and the reason. The balance and the status are worked out again. The client is told.

> Tip: To correct a wrong payment: reverse it, then record the correct payment.

#### Void an invoice

**Who:** Project managers and administrators.

1. Open the invoice.
2. Click **Void**.
3. In **Reason (optional)** you may write why, for example **wrong client**.
4. Click **Void invoice**.

**What you will see:** The toast **Invoice voided** and the status **Void**. A void invoice cannot be sent, paid or edited, and its number is not used again. If the invoice had been sent, the client is told it was cancelled.

> Warning: The button **Void** is only shown while the invoice has no payment. If a payment exists, reverse it first. The system says **This invoice has a payment on it, so it cannot be voided. Reverse the payment first, then void the invoice.**

#### Send a payment reminder

**Who:** Project managers and administrators.

1. Open an invoice with the status **Sent**, **Partly paid** or **Overdue**.
2. Click **Send reminder**.

**What you will see:** The toast **Reminder sent to the client**. The client company gets a notification and an e-mail with the balance and the due date.

> Note: The system sends no automatic reminders for overdue invoices. You send them by hand.

#### Export invoices to a CSV file

**Who:** Project managers and administrators.

1. Click **Invoices** in the left menu.
2. Set the filters you want (status, client, project, dates).
3. Click **Export CSV**.

**What you will see:** The button shows **Preparing…** and then your browser saves the file invoices.csv. You can open it in a spreadsheet program. It has one line per invoice: number, status, client, project, dates, subtotal, tax, total, paid and balance.

#### Check the money of a project

**Who:** Project managers and administrators.

1. Open the project and the tab **Money**.

**What you will see:** **Budget**, **Invoiced**, **Paid**, **Balance due**, **Not invoiced yet** and (if something is overdue) **Overdue**, with the list **Invoices of this project**. The empty text is **No invoices yet. Select "New invoice" to bill hours or the remaining budget.**

### 4.7 Renewals

#### Add a renewal

**Who:** Project managers and administrators.

1. Click **Renewals** in the left menu.
2. Click **Add a renewal**.
3. In **Project** choose the project the item belongs to.
4. In **Type** choose **Domain**, **Hosting**, **SSL certificate** or **Maintenance plan**.
5. In **Expires on** choose the expiry date.
6. In **Name** type the domain or plan name, for example **acmehotels.example** or **Business hosting plan**.
7. In **Provider (optional)** type the supplier.
8. In **Price of the next renewal (RWF)** type the price.
9. In **Internal note (optional, the client does not see it)** write a note if needed.
10. Click **Save**.

**What you will see:** The toast **Renewal added** and the renewal page opens. The form says that reminders go out 60, 30, 14 and 7 days before the expiry date and once on the day it expires.

> Tip: If you enter a date in the past, the item is saved as **Expired**.

#### Find renewals

**Who:** Project managers and administrators, and developers (read only).

1. Click **Renewals**.
2. Read the boxes **Expiring soon** (within 30 days), **Expired** and **Active** (more than 30 days left).
3. Use the buttons **All**, **Expiring soon**, **Expired**, **Active**, and the lists **Type** and **Client**, and the box **Expiring within (days)**.

**What you will see:** The table lists **Item**, **Client**, **Project**, **Expires**, **Status** and **Price**. A yellow bar tells how many items need action. Expiring items are marked yellow and expired ones red.

#### Renew an item

**Who:** Project managers and administrators.

1. Open the renewal.
2. Click **Renew**.
3. In **New expiry date** choose the new date. It is already set one year after the old date and must be later than the current date.
4. In **Price paid (RWF)** check the price.
5. In **Note (optional)** write a note.
6. Click **Mark as renewed**.

**What you will see:** The toast **Marked as renewed. The client was told.** The new date is shown, **Renewed** counts up and the card **Renewal history** gets a line with the dates, the price and your name. The reminders start again for the new date.

#### Edit or delete a renewal

**Who:** Project managers and administrators.

1. Open the renewal.
2. To change it, click **Edit**, change the fields and click **Save**. The project cannot be changed.
3. To remove it, click **Delete** and then **Delete** again in the question.

**What you will see:** After editing the toast **Renewal saved**; changing the expiry date starts the reminders again. After deleting the toast **Renewal deleted**.

> Warning: An item that was renewed before cannot be deleted. You see **This item has been renewed before, so it cannot be deleted. Its history must be kept.**

### 4.8 Support tickets

#### See and filter tickets

**Who:** Project managers and administrators.

1. Click **Support tickets** in the left menu.
2. Read the boxes **Open**, **Past deadline**, **At risk** and **Unassigned**. A red bar tells you how many tickets are past the answer deadline.
3. Use the buttons **All**, **Open**, **In progress**, **Waiting for the client**, **Resolved** and **Closed**, the list **Priority**, and the tick boxes **Past deadline only**, **Unassigned only** and **Assigned to me**.
4. Click a ticket to open it.

**What you will see:** A table with **Ticket**, **Client**, **Priority**, **Assigned to**, **Answer deadline** and **Status**. Open tickets come first, with the nearest deadline at the top. Tickets past the deadline have a red line.

#### Assign a ticket

**Who:** Project managers and administrators.

1. Open the ticket.
2. In the card **Actions**, in **Assign to**, choose a person.
3. Click **Assign**.

**What you will see:** The toast **Ticket assigned**. The person and the client are told. **Assigned to** shows the name. Only active developers and project managers can be chosen.

#### Change the priority

**Who:** Project managers and administrators.

1. Open a ticket that is not resolved or closed.
2. In the card **Actions**, in **Priority**, choose the new priority. The list shows the hours: **Urgent (4 h)**, **High (8 h)**, **Medium (24 h)**, **Low (72 h)**.
3. Click **Change**.

**What you will see:** The toast **Priority changed. The deadline was recalculated.** The deadline is counted again from the start of the clock. The client and the assigned person are told.

#### Reply to the client

**Who:** Project managers, administrators and developers who have the ticket.

1. Open the ticket.
2. In **Write a reply** type your message. Leave the tick box **Internal note: hide this from the client** empty.
3. Click **Send reply**.

**What you will see:** The toast **Reply sent**. The client is told. If the ticket was **Open**, it moves to **In progress**.

#### Write an internal note

**Who:** Project managers, administrators and developers who have the ticket.

1. Open the ticket.
2. Tick **Internal note: hide this from the client**. The box title changes to **Internal note**.
3. Type your note.
4. Click **Add internal note**.

**What you will see:** The toast **Internal note added**. The note appears with the label **Internal note · staff only** and the client never sees it. The assigned person is told if somebody else wrote it.

> Warning: Check the tick box before you click. A public reply is seen by the client.

#### Move, resolve and close a ticket

**Who:** Project managers and administrators (developers except for closing).

1. Open the ticket and look at the card **Actions**.
2. Click the button for the new status: **Move to in progress**, **Wait for the client**, **Mark as resolved**, **Reopen** (for a resolved ticket) or **Close the ticket** (for a resolved ticket).

**What you will see:** The toast **Status updated**. The client is told about every change. A resolved ticket shows **Deadline met** or **Deadline missed**. After **Close the ticket** the ticket is **Closed** and cannot change any more.

> Note: A manager can close only a resolved ticket. A client can close a ticket in any state. **Reopen** restarts the answer deadline.

### 4.9 Reports

#### Read the reports

**Who:** Project managers and administrators.

1. Click **Reports** in the left menu.
2. To look at a period, choose **From** and **To** and click **Apply**. Click **All time** to remove the period.
3. Read the boxes **Invoiced**, **Collected**, **Outstanding**, **Overdue**, **Projects**, **Tickets open**, **Resolved on time** and **Renewals needing action**.
4. Read **Revenue by month**. Click **6 months**, **12 months** or **24 months**. Click **Show the same numbers as a table** to see the numbers.
5. Read **Revenue by client**, **Hours by developer**, **Tickets by priority**, **Renewals by type** and **Project profit**.

**What you will see:** The period limits invoices (by issue date), projects (by start date), tickets (by creation date) and hours (by work date). **Renewals by type** and **Project profit** always show today. **Project profit** shows **Budget**, **Hours logged**, **Cost so far**, **Forecast cost** and **Forecast margin**, with the mark (loss) when the margin is below zero. Cost is hours at the hourly rate, normally 25,000 RWF per hour.

> Warning: **From** must not be later than **To**. The system says **The "from" date must be on or before the "to" date.**

#### Export the report to a CSV file

**Who:** Project managers and administrators.

1. Choose the period if you want one and click **Apply**.
2. Click **Export CSV** at the top right.

**What you will see:** The browser saves the file reports-summary.csv with the columns Section, Item and Value.

### 4.10 Notifications and project access

Notifications work in the same way for every role (see section 2.7). Project managers get a notification when a client sends a request, answers a quote, answers an approval or change request, when developers hand in work, when tickets arrive or come close to the deadline, and when clients ask for a renewal. Section 7.8 lists them all.

Only administrators can give a developer access to a project without a task (see section 6.2). If a developer needs that, ask an administrator.

## 5. Guide for developers

### 5.1 My tasks

#### Open My tasks

**Who:** Developers.

1. Click **My tasks** in the left menu. You can also click the **My tasks** number on the dashboard.

**What you will see:** A table with **Task** (with its phase), **Project** (a link and the project status), **Status**, **Due**, **Hours** (logged and planned, for example **3.75/20**) and the buttons for each task. Late tasks have a red **Late**. If the project has not started you see **Not started** instead of buttons. With no tasks you see **No tasks are assigned to you yet.**

> Note: You see only your own tasks. Tasks of other people are not shown here.

#### Start working on a task

**Who:** Developers.

1. Open **My tasks**.
2. On a task with the status **To do**, click **Start**.

**What you will see:** The toast **Task moved**. The status becomes **Doing**.

> Warning: If the project is still in **Planning**, you see **Not started** and no buttons. The project manager must start the project first.

#### Move a task back

**Who:** Developers.

1. Open the project, tab **Tasks**, and find your task in the column **Doing**.
2. Click **Back to to do** to put it back.

**What you will see:** The toast **Moved to to do** and the card moves to the column **To do**.

> Note: You cannot mark a task **Done**. Only a project manager can. If you try, the system says **Only a project manager can mark a task as done. Submit your work for review instead.**

### 5.2 Logging hours

#### Log your hours

**Who:** Developers.

1. Open **My tasks**.
2. On a task with the status **To do** or **Doing**, click **Log time**. (On the project page, the same button is on the task card.)
3. In **Day** choose the day you worked. It cannot be a future day.
4. In **Hours** type the hours. The field moves in steps of 0.25 (a quarter of an hour), for example 2.5.
5. In **What did you do? (optional)** write a short note.
6. Click **Save**.

**What you will see:** The toast **Time logged**. The column **Hours** goes up. The manager sees the entry in the **Time** tab.

> Warning: One entry must be more than 0 and at most 24 hours. More than 24 gives **A day has only 24 hours.** and 0 gives **Hours must be more than zero.** A future day gives **You cannot log time for a day that has not happened yet.**

> Note: You cannot log time on a task that is **Done**. You see **This task is finished, so it cannot be changed, reassigned or get more time.** You can only log time on tasks given to you. The screens have no button to change or delete an entry; tell your project manager about a mistake.

### 5.3 Submitting work

#### Submit work for review

**Who:** Developers.

1. Make sure the task is in the column **Doing**. If not, click **Start** first.
2. Open the project (click the project name on **My tasks**, or click **Submit work** there). Click the tab **Tasks**.
3. On your task card click **Submit work**.
4. In **Link or file name** paste the link to the work, or type the name of the file. It must be filled in. It can be up to 300 characters.
5. In **Note (optional)** write something for the reviewer.
6. Click **Save**.

**What you will see:** The toast **Work submitted for review**. The card moves to **In review** with **Submitted by** and your link. The project manager is told.

> Tip: The system keeps a link or a file name, it does not store files. Put the file somewhere shared, such as a cloud folder, and paste the link.

> Warning: If you click **Save** with an empty link you see **Add the file name or a link to your work.**

#### Take back submitted work

**Who:** Developers.

1. On **My tasks**, on a task with the status **In review**, click **Take back**.

**What you will see:** The toast **Task moved**. The task returns to **Doing** so you can improve it and submit again.

### 5.4 When work is sent back

#### Handle work that was sent back

**Who:** Developers.

1. Open the notification **... sent "..." back with comments**, or open the project tab **Tasks**.
2. Find your task in the column **Doing**. A yellow box **Sent back:** shows the comments of the project manager.
3. Do the changes. Log your hours with **Log time** as usual.
4. Click **Submit work**, paste the new link in **Link or file name** and click **Save**.

**What you will see:** The task is **In review** again and the old comments disappear. When the manager marks it done you get a notification **... was marked done.**

### 5.5 Projects and the Time tab

#### See your projects

**Who:** Developers.

1. Click **Projects** in the left menu.
2. Click a project card.

**What you will see:** You see the projects where a task is given to you, or where the administrator gave you access. Each card shows the status, the progress, the due date and **N late**. There is no budget. If you have no project you see **You will see a project here once a task is assigned to you, or an administrator gives you access.** On the project page the tabs are **Tasks**, **Time** and **Updates**. The page shows **Hours**: **x logged of y planned**.

> Note: On the project page you can move, hand in and log time on your own tasks only. The page tells you: **You can move, hand in and log time on your own tasks only.**

#### Check your logged time

**Who:** Developers.

1. Open the project and click the tab **Time**.

**What you will see:** Your tasks with **Planned** and **Logged** hours and the list **Entries** with **Day**, **Task**, **Person**, **Hours** and **Note**. You see only your own tasks.

### 5.6 Tickets assigned to you

#### Work on a ticket

**Who:** Developers.

1. Click **My tickets** in the left menu.
2. Click a ticket. Only tickets assigned to you are listed. If there are none you see **No tickets match.**
3. Read **What happened**, the **Conversation** and the card **Answer deadline**.
4. Reply to the client: type your message in **Write a reply** and click **Send reply**.
5. Or leave a note for staff only: tick **Internal note: hide this from the client**, type the note and click **Add internal note**.
6. Change the status in the card **Actions**: **Move to in progress**, **Wait for the client** or **Mark as resolved**. After resolving, **Reopen** is also possible.

**What you will see:** The toasts **Reply sent**, **Internal note added** or **Status updated**. The client is told about replies and status changes.

> Warning: You cannot close a ticket, assign it or change its priority. Ask a project manager.

### 5.7 What you cannot see

You never see invoices, payments, budgets, reports, renewal prices, requests, quotes, approvals or change requests. You see renewals and the dashboard numbers for renewals and tickets, but never money. You see only projects where you have a task or got access, and only tickets assigned to you. If you type the address of a page that is not for you, the system takes you back to the dashboard.

## 6. Guide for administrators

Administrators can do everything in chapter 4 and use the extra pages below.

### 6.1 Users and access

Open the page with **Users and access** in the left menu. A blue box explains: staff are added here, clients register themselves and appear in the list by themselves.

#### Find a person

**Who:** Administrators.

1. Click **Users and access**.
2. Type a name, an e-mail or a company in **Search**.
3. Choose a role in **Role**: **All roles**, **Administrator**, **Project manager**, **Developer** or **Client**.
4. Click a line to open the side panel of that person.

**What you will see:** A table with **Person**, **Role**, **Status** and **Last sign-in** (or **Never**). Status is **Active**, **Invited** (has not chosen a password yet) or **Deactivated**. A client who has not typed the e-mail code yet shows the status PENDING_VERIFICATION.

#### Add a staff member

**Who:** Administrators.

1. Click **Users and access**, then **Add staff member**.
2. In **Full name** type the name.
3. In **Email** type the work e-mail.
4. In **Role** choose **Developer**, **Project manager** or **Administrator**.
5. Click **Add staff member**.

**What you will see:** A green box **... was added as ...** and either the temporary password (marked "Development only" in a test setup) or the sentence **Their temporary password was sent to ...** Click **Done**. The new person has the status **Invited** and must choose a new password at the first sign-in.

> Warning: If the e-mail is already in use you see **An account with this email already exists.** Clients cannot be added here; they create their own account.

> Tip: If the temporary password is shown on screen, give it to the person once and do not keep it.

#### Change a role

**Who:** Administrators.

1. Click the person in the list. The side panel opens.
2. In the box **Role**, in **Change role**, choose the new role.
3. Click **Save role**.

**What you will see:** The toast **Role changed**. The panel shows the new role.

> Note: You cannot change your own role. Client roles cannot be changed. The system always keeps at least one active administrator; otherwise it says **There must always be at least one active administrator.**

#### Deactivate and reactivate an account

**Who:** Administrators.

1. Click the person in the list.
2. In the box **Access** click **Deactivate account** to stop the person, or **Reactivate account** to let the person come back.

**What you will see:** The toast **Account deactivated** or **Account reactivated**. The status changes to **Deactivated** or back. A deactivated person is stopped at once, even if signed in, and sees **This account is deactivated. Ask the administrator to reactivate it.** at the next sign-in. Reactivating also clears a lock after wrong passwords.

> Warning: You cannot deactivate your own account. The panel says **You cannot deactivate your own account.**

#### Reset a password

**Who:** Administrators.

1. Click the person in the list.
2. In the box **Access** click **Reset password**.

**What you will see:** The toast **Password reset**. A new temporary password is shown (marked "Development only") or the box **A temporary password was sent to ...** The status becomes **Invited**. The person must choose a new password at the next sign-in. A lock after wrong passwords is also cleared.

> Warning: You cannot reset a deactivated account or a client who has not confirmed the e-mail. The system says **This account is not active. Reactivate it first.** You cannot reset your own password.

### 6.2 Developer access to a project

A developer sees a project when a task is given to them. Use developer access to let a developer read a project without a task.

#### Give a developer access

**Who:** Administrators.

1. Open the project and click the tab **Developer access**.
2. In **Developer** choose **Choose a developer…** and then the person.
3. Click **Give access**.

**What you will see:** The toast **Access given**. The person appears in the list with the e-mail and **since** the date, and is told **You can now see the project ...**. The developer can read the project, its updates and its time, without a task.

#### Remove developer access

**Who:** Administrators.

1. Open the tab **Developer access** of the project.
2. On the line of the person click **Remove access**.

**What you will see:** The toast **Access removed**. A developer who still has a task on the project keeps seeing the project. If nobody has extra access you see **No extra access given.**

### 6.3 Audit trail

#### Read the audit trail

**Who:** Administrators.

1. Click **Audit trail** in the left menu.
2. To find something, type in **Search** (placeholder **Action, person or subject**).

**What you will see:** A table with **When** (date and time to the second), **Who**, **Action**, **About** and **Reference**. The newest line is first. The page says **Showing the latest N entries**, up to 100. Entries cannot be edited. Work done by the system itself, such as daily reminders, shows **System** as the person.

The audit trail records:

- Signing in, choosing a new password, a client registering, creating a user, changing a role, deactivating, reactivating and resetting a password.
- Requests sent, quotes sent, revised, accepted, declined or asked to be changed.
- Projects started and launched, tasks added, assigned, reassigned, moved, submitted, sent back and marked done, time logged.
- Approvals requested, given or changed, change requests sent, accepted or declined, developer access given or removed.
- Invoices created, updated, sent, voided, payments recorded or reversed, invoice reminders sent.
- Renewals added, updated, renewed, deleted, requested, and reminders sent.
- Support tickets opened, assigned, moved, reopened, closed, priority changes and deadline alerts.

> Tip: Search for a person's name, an action word such as Invoice voided, or part of a project or invoice number.

### 6.4 Reports

Administrators use **Reports** in the same way as project managers. See section 4.9.

### 6.5 The reminder tool and the deadline check

Two checks run by themselves. The renewal reminders run every day at 07:00. The ticket deadline check runs every 15 minutes. Administrators can also run them by hand.

#### Run reminders now

**Who:** Administrators.

1. Click **Renewals** in the left menu.
2. Find the card **Reminders**.
3. Leave **Pretend today is (optional)** empty to use today's date.
4. Click **Run reminders now**.

**What you will see:** A message such as **For 12 Nov 2026: checked 12, sent 2 reminders.** and a list of the items, each with its days left. If nothing is new you see **Nothing new to send for this day (each reminder is sent once).**

> Warning: Reminders are real. They are sent to the client company, the administrators and the project managers. Each reminder is sent only once for each time limit, so a pretend date uses it up. Only use **Pretend today is** for a demonstration.

#### Run the ticket deadline check now

**Who:** Administrators.

1. Click **Support tickets** in the left menu.
2. Find the card **Deadline alerts**.
3. Leave **Pretend it is (optional)** empty to use the real time.
4. Click **Run SLA check now**.

**What you will see:** A message such as **Checked 8 tickets: 1 at risk, 2 past deadline (new alerts sent).** and a list with the ticket numbers, for example **TCK-0007: deadline missed, assigned to ...** If nothing is new you see **Nothing new to alert about (each alert is sent once per ticket).**

> Warning: The alerts are real and go to the assigned person and to the project managers and administrators. Each alert is sent only once for each ticket.

### 6.6 Security rules the administrator should know

- Administrators sign in with a password and a 6-digit code sent by e-mail. The code works for 10 minutes.
- 5 wrong passwords lock an account for 15 minutes.
- A temporary password works only to choose a new password. The person must choose one at the first sign-in or after a reset.
- A deactivated person is stopped at once, even while signed in.
- There must always be at least one active administrator.
- You cannot change, deactivate or reset your own account.
- Staff accounts are created only by administrators. Clients create their own accounts and confirm their e-mail with a code.
- Everything important is written in the audit trail, and nobody can edit it.
- Developers never see money.

## 7. Rules the system follows

### 7.1 Support ticket answer deadlines

| Priority | Answer within | "At risk" starts when this is left |
|---|---|---|
| Urgent | 4 hours | 1 hour |
| High | 8 hours | 2 hours |
| Medium | 24 hours | 6 hours |
| Low | 72 hours | 18 hours |

| Rule | What it means |
|---|---|
| Calendar hours | Nights and weekends count. |
| Start of the clock | The moment the ticket is created. |
| Waiting for the client | The clock does not stop. |
| Reopening | The clock starts again from the moment of the reopen. |
| Changing the priority | The deadline is counted again from the start of the clock with the new hours. |
| At risk | A ticket gets this mark when a quarter or less of the time is left. |
| Deadline missed | The ticket is still open after the deadline. |
| Alerts | The system checks every 15 minutes and sends one "at risk" and one "deadline missed" alert per ticket. |
| Reopen by the client | A client can reopen a ticket only once. |

### 7.2 Renewals

| Rule | What it means |
|---|---|
| Active | More than 30 days left. |
| Expiring soon | 30 days or less left, including the expiry day. |
| Expired | The expiry date is before today. |
| Reminder days | 60, 30, 14 and 7 days before the expiry date, and once on the day it expires. |
| How often | Each reminder is sent only once. If days were missed, only the nearest time limit that was reached is sent. |
| Who gets them | Every active person of the client company, the administrators and the project managers. |
| When | The check runs every day at 07:00. An administrator can run it by hand. |
| After renewing | The reminders start again for the new date. Changing the expiry date does the same. |
| Deleting | Only an item that was never renewed can be deleted. |

### 7.3 Invoices

| Rule | What it means |
|---|---|
| Number | INV, the year of the issue date, and a running number of 4 digits, for example INV-2026-0001. Numbers have no gaps and are never used again. |
| When the number is given | When the draft is created. |
| Draft | Only staff see it. It can be edited. |
| Sent | The client sees it. It can no longer be edited. |
| Statuses | Draft, Sent, Partly paid, Paid, Void. Overdue is shown on top of Sent and Partly paid. |
| Overdue | The invoice is Sent or Partly paid and its due date is before today. The screen shows how many days late. |
| Reminders | None are sent automatically. A project manager clicks Send reminder. |
| Due date | It cannot be before the issue date. |
| Lines | 1 to 50 lines. Each line has a description, a quantity (up to 2 decimals) and a unit price of at least 1 RWF. |
| Tax | A percentage from 0 to 100, with up to 2 decimals. Amounts are rounded to whole francs. |
| Void | Only when the invoice has no active payment. |
| Money | All amounts are whole Rwandan francs (RWF). |
| Hourly rate | The suggested hours line and the cost in Reports use the company hourly rate, normally 25,000 RWF. |

### 7.4 Payments

| Rule | What it means |
|---|---|
| Who records | Only project managers and administrators. Clients cannot pay inside the system. |
| Amount | A whole number of francs above 0, and not more than the balance. |
| Date | Not in the future. |
| Invoices | Only a Sent or Partly paid invoice (also an overdue one) can receive a payment. |
| Methods | Mobile money, Bank transfer, Cash. |
| Reference | Optional, up to 100 characters. |
| Mistakes | A payment cannot be edited. It is reversed with a reason (up to 300 characters) and stays visible with the mark Reversed. |
| Status | Worked out from the payments that were not reversed: none is Sent, some is Partly paid, all is Paid. |

### 7.5 Passwords, codes and signing in

| Rule | What it means |
|---|---|
| Password | At least 8 characters, with letters and a number. At most 100 characters. |
| Wrong passwords | After 5 wrong tries the account is locked for 15 minutes. |
| Temporary password | Given to new staff and after a reset. It must be replaced at the first sign-in, and it cannot be chosen again. |
| E-mail code | 6 digits, valid for 10 minutes, at most 5 tries. A new code can be asked after 1 minute. |
| Administrator sign-in | Password first, then a 6-digit code by e-mail. |
| Steps | The step after the password (new password or code) must be finished within 15 minutes. |
| Session | You stay signed in for about 8 hours. Closing the browser tab signs you out. |
| Deactivated account | Stopped at once, even if signed in. |

### 7.6 Time logs

| Rule | What it means |
|---|---|
| Who | Only the person the task is given to. |
| Project | It must be started (not in Planning). |
| Task | It must not be Done. |
| Hours | More than 0 and at most 24 in one entry. The field moves in steps of 0.25. |
| Day | Not a future day. |
| Note | Optional, up to 500 characters. |
| Changing | There is no button to change or delete an entry. |

### 7.7 Requests, quotes, projects and tasks

| Rule | What it means |
|---|---|
| Request deadline | The "Wanted by" date must be after today. |
| Quote | 1 to 30 lines, each with an amount above 0. 1 to 104 weeks. Valid until today or later. |
| Expired quote | The client cannot accept it. The project manager sends a new quote. |
| Accepting | Creates the project in Planning, with a budget equal to the quote and four starter tasks. |
| Change request | Accepting it adds its price to the budget. |
| Task | Planned hours 1 to 2000. Due date today or later. |
| Starting | Moves, hand-ins and time logs are possible only after Start the project. |
| Late task | Not Done and the due date is before today. |
| Progress | The share of planned hours that belong to Done tasks. |
| Project status | Planning, then In progress. In review when every task is handed in or done. Launched when every task is Done. |
| Launched | No new tasks can be added. |
| Approvals | The same name again is a new version. Approving an item linked to a task makes that task Done. |
| Marking Done | Only a project manager or an administrator, or the client by approving a linked item. |

### 7.8 What sends a notification

| What happens | Who is told |
|---|---|
| A client sends a request | Project managers and administrators |
| A quote is sent or revised | The client company |
| A client accepts a quote | Project managers and administrators |
| A client asks for changes or declines | The project managers and the person who wrote the quote |
| A client registers and confirms the e-mail | Administrators |
| A project starts | The client company and everyone who has a task |
| A task is assigned or reassigned | The new person, and the previous person |
| Work is handed in | The project manager |
| Work is sent back | The developer |
| A task is marked done | The developer |
| A project launches | The client company and the project manager |
| An item is sent for approval | The client company |
| The client answers an approval | The person who sent it and the project manager |
| A change request is proposed | The client company |
| The client answers a change request | The project manager and the person who proposed it |
| Developer access is given or removed | The developer |
| An invoice is sent (also by e-mail) | The client company |
| A payment is recorded or reversed | The client company |
| An invoice is voided after it was sent | The client company |
| A payment reminder is sent (also by e-mail) | The client company |
| A renewal reminder (also by e-mail) | The client company, administrators, project managers |
| A renewal is done | The client company |
| A client asks for a renewal | Administrators and project managers |
| A new support ticket | Administrators and project managers |
| A ticket is assigned | The assigned person and the client company |
| A reply, a status change or a priority change on a ticket | The client company (and the assigned person for a client reply or a change by someone else) |
| An internal note on a ticket | The assigned person (never the client) |
| A ticket is at risk or past its deadline (also by e-mail) | The assigned person, administrators and project managers |

## 8. Questions and problems

### 8.1 Signing in

**I cannot sign in. What does the message mean?**

**The email or password is incorrect.** means the e-mail or the password is wrong. Check for spaces and capital letters. Click **Show** to see what you typed.

**Too many wrong passwords. Try again in a few minutes.** means the account is locked for 15 minutes after 5 wrong tries. Wait and try again, or ask the administrator to reset your password.

**This account is deactivated. Ask the administrator to reactivate it.** means an administrator stopped the account. Ask an administrator to reactivate it.

**Please confirm your email address first. We can send you a new code.** means you did not finish the e-mail code. Click **Confirm your email**.

**I did not get my code by e-mail.**

Wait a minute and look in the spam folder. Then click **Send a new code** on the **Confirm your email** page. Administrators click **Back to sign in** and sign in again. If you ask too soon you see **A code was sent a moment ago. Please wait a minute before asking for another one; the code we already sent still works.** If no e-mail ever arrives, ask ITZETech to check your address.

**The code is not accepted.**

**That code is not right.** means a wrong digit; type it again. **That code has expired.** means it is older than 10 minutes; ask for a new one. **Too many wrong attempts. Ask for a new code.** means the code stopped working after 5 wrong tries; ask for a new one.

**I forgot my password.**

Ask the administrator to reset it. You then sign in with the temporary password and choose a new one.

**I want to change my password.**

The system has no profile page. Ask the administrator to reset your password.

**I was signed out suddenly.**

You are signed out after about 8 hours, when you close the browser tab, or when an administrator deactivates your account. Sign in again.

**"This step has expired. Please sign in again."**

The step after the password (a new password or the code) is only valid for 15 minutes. Sign in again from the start.

### 8.2 Pages, buttons and messages

**The page will not load.**

If you see **Cannot reach the server. Check your connection and try again.**, check your internet connection and try again. If you see **Something went wrong on our side. Please try again in a moment.**, wait a minute and reload the page. If it continues, tell ITZETech.

**I cannot see a button or a menu entry.**

The screens show only what your role may do (see section 1.4). A button can also be missing because of the status. For example **Edit draft** is only on a draft, **Record payment** only on a sent invoice with a balance, and **Void** only when there is no payment.

**The page says "We could not find that."**

The full message is **We could not find that. It may have been removed, or you may not have access to it.** Check that you opened the right item. Clients and developers get this message for items that belong to someone else.

**It says "You do not have access to this."**

Your role cannot do that action. Ask a project manager or an administrator.

**It says "That change is not possible from the current status. Reload the page to see the latest status."**

Someone else changed the item first. Reload the page and look again.

**It says "This was already done, or it is not possible in the current status."**

The action was already done. Reload the page.

**I cannot move a task, hand in work or log time.**

The project has not started. A project manager must click **Start the project**. You also cannot work on a task that is **Done** or that belongs to someone else (**This task belongs to someone else.**).

**I cannot mark a task as done.**

Developers cannot. Click **Submit work** and the project manager marks it done. (**Only a project manager can mark a task as done. Submit your work for review instead.**)

**I cannot add a task.**

The project is **Launched**. The system says **This project has launched, so no new tasks can be added.**

**A date is not accepted.**

Some dates must be today or later (task due date, quote valid until) or after today (request deadline). Time cannot be logged for a future day, and a payment date cannot be in the future.

### 8.3 Money

**I made a mistake in a payment.**

Open the invoice, click **Reverse** on the payment, write a reason and click **Reverse payment**. Then click **Record payment** and enter the right payment. Clients cannot change payments; tell the project manager.

**I made a mistake in an invoice.**

If it is a draft, click **Edit draft**. If it was sent, void it (reverse any payment first) and make a new invoice. The number of the voided invoice is not used again.

**The system refuses my payment.**

**That payment is more than the amount still owed on this invoice. Enter the balance or less. Nothing was recorded.** means the amount is too high. **A payment can only be recorded on an invoice that was sent and is not fully paid yet.** means the invoice is a draft, paid or void.

**I paid, but the invoice still shows a balance.**

ITZETech records the payment by hand when it arrives. Wait, or tell your project manager the invoice number and the date.

**Can I pay inside the system?**

Not yet. Pay as agreed and quote the invoice number. Online payment with mobile money (MTN MoMo) is planned for later.

**I cannot void an invoice.**

It has an active payment. Reverse the payment first, then void the invoice.

### 8.4 Quotes, projects and support

**My quote expired.**

A client sees **This quote is no longer valid. Ask the project manager to send a new quote.** The project manager opens the request, changes **Valid until** and clicks **Send revised quote**.

**I cannot send a quote again.**

The request is **Accepted** or **Declined**. A new request is needed.

**My colleague cannot see my requests and projects.**

Each account creates its own company space, so colleagues who registered separately do not see each other's items. Agree on one account for the company.

**I cannot write on my ticket.**

The ticket is **Closed**. Open a new ticket.

**I cannot reopen my ticket.**

A ticket can be reopened only once. Open a new ticket and mention the old number.

**I cannot delete a renewal.**

It was renewed before, so its history must be kept.

**An item is "Not assigned yet".**

A project manager has not yet given it to a person. The manager can do it from the ticket or the task.

**I see a box marked "Development only".**

The system is in a test setup and shows codes and temporary passwords on screen. Use them as shown.

## 9. Words used in this manual

### 9.1 Main words

| Word | Meaning |
|---|---|
| Request | The client's message that says what work is needed and by when. |
| Quote | The offer with prices, weeks and a valid-until date. |
| Project | The work that starts when the client accepts a quote. |
| Task | One piece of work in a project, with a phase, planned hours and a due date. |
| Phase | One of four steps of a project: Discovery, Design, Build, Test and launch. |
| Approval | An item (design, page content, document) the client must approve or ask to change. |
| Change request | Extra work with its own price, proposed after the quote was accepted. |
| Invoice | The bill with lines, tax, a due date and a number such as INV-2026-0001. |
| Payment | Money received for an invoice, recorded by a project manager. |
| Balance | What is still owed on an invoice. |
| Renewal | A domain, hosting plan, SSL certificate or maintenance plan that expires and must be renewed. |
| Ticket | A support request for a launched website, with a number such as TCK-0007. |
| Deadline | The time by which ITZETech must answer a ticket. |
| Internal note | A message on a ticket that only staff can read. |
| Notification | A short message in the bell. |
| Audit trail | The list of every important action, seen only by administrators. |
| Draft | An invoice that is not sent yet. Only staff see it. |
| Void | An invoice that was cancelled. |
| Reverse | To cancel a recorded payment. It stays in the list marked Reversed. |
| CSV | A simple file that a spreadsheet program can open. |

### 9.2 Status words

| Where | Statuses |
|---|---|
| Request | Waiting for a quote, Quote sent, Changes asked, Accepted, Declined |
| Project | Planning, In progress, In review, Launched |
| Task | To do, Doing, In review, Done (and Late as a warning) |
| Approval | Waiting for answer, Approved, Changes asked |
| Change request | Waiting for answer, Accepted, Declined |
| Invoice | Draft, Sent, Partly paid, Paid, Overdue, Void |
| Renewal | Active, Expiring soon, Expired |
| Ticket | Open, In progress, Waiting for the client, Resolved, Closed |
| Ticket deadline | On track, At risk, Deadline missed, Deadline met |
| Person | Active, Invited, Deactivated |
| Priority | Urgent, High, Medium, Low |
| Payment method | Mobile money, Bank transfer, Cash |

## 10. Quick reference

### 10.1 Clients

| I want to | Go to | Click |
|---|---|---|
| Ask for new work | Requests and quotes | New request, then Send request |
| Read my quote | Requests and quotes, then the request | The request title |
| Accept a quote | Requests and quotes, then the request | Accept the quote |
| Ask for changes to a quote | Requests and quotes, then the request | Ask for changes, then Send |
| Decline a quote | Requests and quotes, then the request | Decline, then Decline the quote |
| See my project and progress | My projects | The project card |
| See the tasks | My projects, then the project | Tasks tab |
| Approve a design | My projects, then the project | Approvals tab, then Approve |
| Ask for changes to a design | My projects, then the project | Approvals tab, then Request changes |
| Answer extra work with a price | My projects, then the project | Change requests tab, then Accept or Decline |
| Read what happened | My projects, then the project | Updates tab |
| See what I owe | Invoices | Balance due box, or the invoice |
| See my payments | Invoices | Payment history card |
| See the money of one project | My projects, then the project | Money tab |
| Pay an invoice | Invoices, then the invoice | Read How to pay, then pay outside the system |
| See when my domain expires | My websites and renewals | The item |
| Ask for a renewal | My websites and renewals | Request renewal |
| Report a problem | Support | New ticket, then Send ticket |
| Answer ITZETech on a ticket | Support, then the ticket | Write a reply, then Send reply |
| Reopen a ticket | Support, then the ticket | Reopen: not fixed |
| Close a ticket | Support, then the ticket | Close the ticket |
| Read my messages | The bell, or Notifications | The message |
| Sign out | The bottom of the left menu | Sign out |

### 10.2 Project managers (and administrators)

| I want to | Go to | Click |
|---|---|---|
| Write a quote | Requests and quotes, then the request | Send quote to client |
| Revise a quote | Requests and quotes, then the request | Send revised quote |
| Add a task | Projects, then the project | Tasks tab, then Add a task |
| Give a task to a person | Projects, then the project | Assign (or Reassign) |
| Start a project | Projects, then the project | Start the project |
| Mark submitted work as done | Projects, then the project | Tasks tab, then Mark done |
| Send work back | Projects, then the project | Tasks tab, then Send back |
| Send a design to the client | Projects, then the project | Approvals tab, then Send for approval |
| Propose extra work | Projects, then the project | Change requests tab, then Propose a change |
| See hours | Projects, then the project | Time tab |
| Make an invoice | Invoices | New invoice, then Save draft |
| Send an invoice | Invoices, then the invoice | Send to client, then Send invoice |
| Record a payment | Invoices, then the invoice | Record payment |
| Reverse a payment | Invoices, then the invoice | Reverse |
| Void an invoice | Invoices, then the invoice | Void, then Void invoice |
| Remind about an invoice | Invoices, then the invoice | Send reminder |
| Export invoices | Invoices | Export CSV |
| Add a domain or hosting item | Renewals | Add a renewal |
| Renew an item | Renewals, then the item | Renew, then Mark as renewed |
| Assign a ticket | Support tickets, then the ticket | Assign to, then Assign |
| Change a ticket priority | Support tickets, then the ticket | Priority, then Change |
| Reply to a client | Support tickets, then the ticket | Write a reply, then Send reply |
| Write a note for staff only | Support tickets, then the ticket | Tick Internal note, then Add internal note |
| Resolve or close a ticket | Support tickets, then the ticket | Mark as resolved, then Close the ticket |
| See the reports | Reports | Apply, All time, Export CSV |
| See my messages | The bell, or Notifications | The message |

### 10.3 Developers

| I want to | Go to | Click |
|---|---|---|
| See my tasks | My tasks | The page itself |
| Start a task | My tasks | Start |
| Log hours | My tasks | Log time, then Save |
| Hand in work | Projects, then the project | Tasks tab, then Submit work, then Save |
| Take work back | My tasks | Take back |
| Put a task back to To do | Projects, then the project | Tasks tab, then Back to to do |
| Read why work was sent back | Projects, then the project | Tasks tab, the yellow Sent back box |
| See my hours on a project | Projects, then the project | Time tab |
| Work on a ticket | My tickets, then the ticket | Send reply, Add internal note or a status button |
| See renewals | Renewals | The item |

### 10.4 Administrators (extra tasks)

| I want to | Go to | Click |
|---|---|---|
| Add a staff member | Users and access | Add staff member |
| Find a person | Users and access | Search, or Role |
| Change a role | Users and access, then the person | Change role, then Save role |
| Stop a person | Users and access, then the person | Deactivate account |
| Let a person come back | Users and access, then the person | Reactivate account |
| Give a new temporary password | Users and access, then the person | Reset password |
| Let a developer see a project | Projects, then the project | Developer access tab, then Give access |
| See what happened | Audit trail | Search |
| Send renewal reminders now | Renewals | Run reminders now |
| Check ticket deadlines now | Support tickets | Run SLA check now |
