-- Auth + Workspace foundation (decisions report §4.2): every domain table will
-- reference workspace_id, never user_id directly. app_user (not "user" —
-- reserved word in Postgres).

create table app_user (
    id             uuid primary key,
    version        bigint       not null default 0,
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now(),
    name           varchar(150) not null,
    email          varchar(255) not null,
    password_hash  varchar(255),
    provider       varchar(20)  not null
);

create unique index ux_app_user_email on app_user (email);

create table workspace (
    id          uuid primary key,
    version     bigint       not null default 0,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    name        varchar(150) not null
);

create table workspace_member (
    id            uuid primary key,
    version       bigint      not null default 0,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    workspace_id  uuid        not null references workspace (id) on delete cascade,
    user_id       uuid        not null references app_user (id) on delete cascade,
    role          varchar(20) not null,
    constraint ux_workspace_member_workspace_user unique (workspace_id, user_id)
);

create index ix_workspace_member_user on workspace_member (user_id);

create table refresh_token (
    id           uuid primary key,
    version      bigint       not null default 0,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),
    user_id      uuid         not null references app_user (id) on delete cascade,
    token_hash   varchar(64)  not null,
    expires_at   timestamptz  not null,
    revoked      boolean      not null default false
);

create unique index ux_refresh_token_token_hash on refresh_token (token_hash);
create index ix_refresh_token_user_revoked on refresh_token (user_id, revoked);
