-- Foundation: client companies, user accounts, one-time codes, notifications and the audit trail.

create table client_company (
    id           uuid primary key,
    name         varchar(200) not null,
    contact_name varchar(150),
    phone        varchar(40),
    created_at   timestamptz  not null
);

create table user_account (
    id                   uuid primary key,
    email                varchar(254) not null unique,
    full_name            varchar(150) not null,
    password_hash        varchar(100) not null,
    role                 varchar(20)  not null,
    status               varchar(25)  not null,
    client_company_id    uuid references client_company (id),
    must_change_password boolean      not null default false,
    failed_attempts      integer      not null default 0,
    locked_until         timestamptz,
    last_login_at        timestamptz,
    created_at           timestamptz  not null,
    created_by           uuid references user_account (id),
    constraint ck_user_role check (role in ('ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'CLIENT')),
    constraint ck_user_status check (status in ('PENDING_VERIFICATION', 'INVITED', 'ACTIVE', 'DEACTIVATED')),
    constraint ck_client_company check ((role = 'CLIENT') = (client_company_id is not null))
);
create index ix_user_role on user_account (role);
create index ix_user_company on user_account (client_company_id);

create table one_time_code (
    id         uuid primary key,
    user_id    uuid         not null references user_account (id) on delete cascade,
    purpose    varchar(20)  not null,
    code_hash  varchar(100) not null,
    expires_at timestamptz  not null,
    attempts   integer      not null default 0,
    used       boolean      not null default false,
    created_at timestamptz  not null
);
create index ix_otc_user_purpose on one_time_code (user_id, purpose);

create table notification (
    id         uuid primary key,
    user_id    uuid         not null references user_account (id) on delete cascade,
    message    varchar(500) not null,
    link       varchar(200),
    created_at timestamptz  not null,
    read_at    timestamptz
);
create index ix_notification_user on notification (user_id, created_at desc);

create table audit_entry (
    id            uuid primary key,
    occurred_at   timestamptz  not null,
    actor_user_id uuid,
    actor_name    varchar(150) not null,
    action        varchar(100) not null,
    subject       varchar(300),
    ref           varchar(12)  not null
);
create index ix_audit_time on audit_entry (occurred_at desc);
