-- Stage 3: invoices and payments, renewals with reminders, support tickets with deadlines.

-- Invoice numbers: one running counter per year. The upsert in InvoiceService takes a row lock, so two people
-- creating an invoice at the same moment can never get the same number.
create table invoice_counter (
    year        integer primary key,
    last_number integer not null
);

create table invoice (
    id          uuid primary key,
    number      varchar(20)   not null unique,
    project_id  uuid references project (id),
    company_id  uuid          not null references client_company (id),
    status      varchar(20)   not null,
    issue_date  date          not null,
    due_date    date          not null,
    tax_rate    numeric(5, 2) not null default 0,
    subtotal    bigint        not null,
    tax_amount  bigint        not null,
    total       bigint        not null,
    paid_amount bigint        not null default 0,
    note        varchar(1000),
    created_by  uuid          not null references user_account (id),
    created_at  timestamptz   not null,
    updated_at  timestamptz   not null,
    sent_at     timestamptz,
    voided_at   timestamptz,
    void_reason varchar(300),
    constraint ck_invoice_status check (status in ('DRAFT', 'SENT', 'PARTIALLY_PAID', 'PAID', 'VOID')),
    constraint ck_invoice_amounts check (subtotal >= 0 and tax_amount >= 0 and total = subtotal + tax_amount and paid_amount >= 0),
    constraint ck_invoice_tax check (tax_rate >= 0 and tax_rate <= 100),
    constraint ck_invoice_dates check (due_date >= issue_date)
);
create index ix_invoice_company on invoice (company_id, issue_date desc);
create index ix_invoice_project on invoice (project_id);
create index ix_invoice_status on invoice (status);

create table invoice_line (
    id          uuid primary key,
    invoice_id  uuid           not null references invoice (id) on delete cascade,
    sort_order  integer        not null,
    description varchar(200)   not null,
    quantity    numeric(10, 2) not null,
    unit_price  bigint         not null,
    line_total  bigint         not null,
    constraint ck_invoice_line check (quantity > 0 and unit_price > 0 and line_total >= 0)
);
create index ix_invoice_line_invoice on invoice_line (invoice_id, sort_order);

create table invoice_payment (
    id             uuid primary key,
    invoice_id     uuid        not null references invoice (id) on delete cascade,
    amount         bigint      not null,
    paid_on        date        not null,
    method         varchar(20) not null,
    reference      varchar(100),
    recorded_by    uuid        not null references user_account (id),
    recorded_at    timestamptz not null,
    reversed       boolean     not null default false,
    reversed_at    timestamptz,
    reversed_by    uuid references user_account (id),
    reverse_reason varchar(300),
    constraint ck_payment_amount check (amount > 0),
    constraint ck_payment_method check (method in ('MOBILE_MONEY', 'BANK_TRANSFER', 'CASH'))
);
create index ix_payment_invoice on invoice_payment (invoice_id, recorded_at);
create index ix_payment_paid_on on invoice_payment (paid_on);

-- Renewals: things that expire for a client's website (domain, hosting, SSL, maintenance plan).
create table renewal (
    id         uuid primary key,
    project_id uuid         not null references project (id),
    company_id uuid         not null references client_company (id),
    type       varchar(15)  not null,
    label      varchar(200) not null,
    provider   varchar(150),
    expires_on date         not null,
    price      bigint       not null,
    note       varchar(500),
    created_by uuid         not null references user_account (id),
    created_at timestamptz  not null,
    updated_at timestamptz  not null,
    constraint ck_renewal_type check (type in ('DOMAIN', 'HOSTING', 'SSL', 'MAINTENANCE')),
    constraint ck_renewal_price check (price >= 0)
);
create index ix_renewal_company on renewal (company_id, expires_on);
create index ix_renewal_project on renewal (project_id);
create index ix_renewal_expires on renewal (expires_on);

create table renewal_history (
    id                  uuid primary key,
    renewal_id          uuid        not null references renewal (id) on delete cascade,
    renewed_on          date        not null,
    previous_expires_on date        not null,
    new_expires_on      date        not null,
    price               bigint      not null,
    note                varchar(500),
    renewed_by          uuid        not null references user_account (id),
    created_at          timestamptz not null
);
create index ix_renewal_history_renewal on renewal_history (renewal_id, created_at desc);

-- One row per reminder that was sent. The unique key is what makes "exactly once per renewal, threshold and expiry date"
-- true even if the daily job and the admin endpoint run at the same time.
create table renewal_reminder (
    id             uuid primary key,
    renewal_id     uuid        not null references renewal (id) on delete cascade,
    expires_on     date        not null,
    threshold_days integer     not null,
    sent_at        timestamptz not null,
    constraint uq_renewal_reminder unique (renewal_id, expires_on, threshold_days)
);

-- Support tickets
create sequence ticket_number_seq;

create table ticket (
    id                     uuid primary key,
    number                 varchar(12)   not null unique,
    company_id             uuid          not null references client_company (id),
    project_id             uuid references project (id),
    subject                varchar(200)  not null,
    description            varchar(4000) not null,
    priority               varchar(10)   not null,
    status                 varchar(20)   not null,
    created_by             uuid          not null references user_account (id),
    assignee_id            uuid references user_account (id),
    sla_hours              integer       not null,
    sla_started_at         timestamptz   not null,
    due_at                 timestamptz   not null,
    created_at             timestamptz   not null,
    updated_at             timestamptz   not null,
    resolved_at            timestamptz,
    resolved_within_sla    boolean,
    closed_at              timestamptz,
    reopen_count           integer       not null default 0,
    sla_warned_at          timestamptz,
    sla_breach_notified_at timestamptz,
    constraint ck_ticket_priority check (priority in ('URGENT', 'HIGH', 'MEDIUM', 'LOW')),
    constraint ck_ticket_status check (status in ('OPEN', 'IN_PROGRESS', 'WAITING_CLIENT', 'RESOLVED', 'CLOSED'))
);
create index ix_ticket_company on ticket (company_id, created_at desc);
create index ix_ticket_assignee on ticket (assignee_id);
create index ix_ticket_status on ticket (status, due_at);

create table ticket_comment (
    id         uuid primary key,
    ticket_id  uuid          not null references ticket (id) on delete cascade,
    author_id  uuid          not null references user_account (id),
    body       varchar(4000) not null,
    internal   boolean       not null default false,
    created_at timestamptz   not null
);
create index ix_ticket_comment_ticket on ticket_comment (ticket_id, created_at);
