-- Phase 2: requests and quotes, projects and tasks, approvals, change requests, time logs and the project updates feed.

create table client_request (
    id                uuid primary key,
    company_id        uuid          not null references client_company (id),
    created_by        uuid          not null references user_account (id),
    title             varchar(200)  not null,
    description       varchar(4000) not null,
    budget_text       varchar(100),
    deadline          date          not null,
    status            varchar(25)   not null,
    change_note       varchar(1000),
    quote_weeks       integer,
    quote_valid_until date,
    quote_sent_at     timestamptz,
    quoted_by         uuid references user_account (id),
    project_id        uuid,
    created_at        timestamptz   not null,
    updated_at        timestamptz   not null,
    constraint ck_request_status check (status in ('NEW', 'QUOTED', 'CHANGES_REQUESTED', 'ACCEPTED', 'DECLINED'))
);
create index ix_request_company on client_request (company_id, created_at desc);
create index ix_request_status on client_request (status);

create table quote_item (
    id          uuid primary key,
    request_id  uuid         not null references client_request (id) on delete cascade,
    sort_order  integer      not null,
    description varchar(200) not null,
    amount      bigint       not null,
    constraint ck_quote_amount check (amount > 0)
);
create index ix_quote_item_request on quote_item (request_id, sort_order);

create table project (
    id         uuid primary key,
    request_id uuid unique references client_request (id),
    company_id uuid         not null references client_company (id),
    name       varchar(200) not null,
    status     varchar(20)  not null,
    manager_id uuid         not null references user_account (id),
    start_date date         not null,
    due_date   date         not null,
    budget     bigint       not null,
    created_at timestamptz  not null,
    constraint ck_project_status check (status in ('PLANNING', 'IN_PROGRESS', 'IN_REVIEW', 'LAUNCHED')),
    constraint ck_project_budget check (budget >= 0)
);
create index ix_project_company on project (company_id, created_at desc);

alter table client_request add constraint fk_request_project foreign key (project_id) references project (id);

create table project_task (
    id               uuid primary key,
    project_id       uuid          not null references project (id) on delete cascade,
    phase            integer       not null,
    sort_order       integer       not null,
    title            varchar(200)  not null,
    assignee_id      uuid references user_account (id),
    status           varchar(10)   not null,
    due_date         date          not null,
    planned_hours    integer       not null,
    logged_hours     numeric(8, 2) not null default 0,
    submission_file  varchar(300),
    submission_note  varchar(1000),
    submitted_at     timestamptz,
    submitted_by     uuid references user_account (id),
    sent_back_note   varchar(1000),
    sent_back_at     timestamptz,
    sent_back_by     uuid references user_account (id),
    created_at       timestamptz   not null,
    constraint ck_task_phase check (phase between 0 and 3),
    constraint ck_task_status check (status in ('TODO', 'DOING', 'REVIEW', 'DONE')),
    constraint ck_task_hours check (planned_hours > 0)
);
create index ix_task_project on project_task (project_id, phase, sort_order);
create index ix_task_assignee on project_task (assignee_id);

-- Extra access the administrator gives a developer to a project where they have no task (yet).
create table project_access (
    id         uuid primary key,
    project_id uuid        not null references project (id) on delete cascade,
    user_id    uuid        not null references user_account (id) on delete cascade,
    granted_by uuid references user_account (id),
    granted_at timestamptz not null,
    constraint uq_project_access unique (project_id, user_id)
);
create index ix_project_access_user on project_access (user_id);

create table approval (
    id          uuid primary key,
    project_id  uuid          not null references project (id) on delete cascade,
    task_id     uuid references project_task (id) on delete set null,
    item        varchar(200)  not null,
    item_key    varchar(200)  not null,
    item_type   varchar(20)   not null,
    file_ref    varchar(300),
    version     integer       not null,
    message     varchar(1000),
    status      varchar(20)   not null,
    answer_note varchar(1000),
    sent_by     uuid          not null references user_account (id),
    sent_at     timestamptz   not null,
    answered_by uuid references user_account (id),
    answered_at timestamptz,
    constraint ck_approval_type check (item_type in ('DESIGN', 'PAGE_CONTENT', 'DOCUMENT')),
    constraint ck_approval_status check (status in ('PENDING', 'APPROVED', 'CHANGES_REQUESTED'))
);
create index ix_approval_project on approval (project_id, sent_at desc);

create table change_request (
    id          uuid primary key,
    project_id  uuid         not null references project (id) on delete cascade,
    title       varchar(200) not null,
    cost        bigint       not null,
    status      varchar(10)  not null,
    proposed_by uuid         not null references user_account (id),
    created_at  timestamptz  not null,
    answered_by uuid references user_account (id),
    answered_at timestamptz,
    constraint ck_change_cost check (cost > 0),
    constraint ck_change_status check (status in ('PROPOSED', 'ACCEPTED', 'DECLINED'))
);
create index ix_change_project on change_request (project_id, created_at desc);

create table time_log (
    id         uuid primary key,
    task_id    uuid          not null references project_task (id) on delete cascade,
    user_id    uuid          not null references user_account (id),
    work_date  date          not null,
    hours      numeric(5, 2) not null,
    note       varchar(500),
    created_at timestamptz   not null,
    constraint ck_time_hours check (hours > 0 and hours <= 24)
);
create index ix_time_task on time_log (task_id, work_date desc);

-- Plain-language history a client can read ("Finished: Checkout flow wireframes").
create table project_update (
    id         uuid primary key,
    project_id uuid         not null references project (id) on delete cascade,
    message    varchar(300) not null,
    created_at timestamptz  not null
);
create index ix_update_project on project_update (project_id, created_at desc);
