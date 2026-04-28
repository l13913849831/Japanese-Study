create table user_account (
    id bigserial primary key,
    display_name varchar(128) not null,
    status varchar(32) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_user_account_status
        check (status in ('ACTIVE', 'DISABLED'))
);

create table user_identity (
    id bigserial primary key,
    user_id bigint not null,
    provider varchar(32) not null,
    provider_subject varchar(255) not null,
    password_hash varchar(255),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_user_identity_user
        foreign key (user_id) references user_account(id) on delete cascade,
    constraint uk_user_identity_provider_subject
        unique (provider, provider_subject),
    constraint ck_user_identity_provider
        check (provider in ('LOCAL', 'KEYCLOAK', 'WECHAT_MP')),
    constraint ck_user_identity_local_password
        check (
            provider <> 'LOCAL'
            or (password_hash is not null and btrim(password_hash) <> '')
        )
);

create index idx_user_identity_user_provider on user_identity(user_id, provider);

create table user_setting (
    user_id bigint primary key,
    preferred_learning_order varchar(32) not null default 'WORD_FIRST',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_user_setting_user
        foreign key (user_id) references user_account(id) on delete cascade,
    constraint ck_user_setting_learning_order
        check (preferred_learning_order in ('WORD_FIRST', 'NOTE_FIRST'))
);

insert into user_account (id, display_name, status, created_at, updated_at)
values (1, 'Bootstrap User', 'ACTIVE', now(), now());

select setval(
    pg_get_serial_sequence('user_account', 'id'),
    (select max(id) from user_account)
);

insert into user_setting (user_id, preferred_learning_order, created_at, updated_at)
values (1, 'WORD_FIRST', now(), now());

alter table word_set
    add column scope varchar(32),
    add column owner_user_id bigint;

update word_set
set scope = 'USER',
    owner_user_id = 1
where scope is null;

alter table word_set
    alter column scope set not null;

alter table word_set
    drop constraint uk_word_set_name;

alter table word_set
    add constraint fk_word_set_owner_user
        foreign key (owner_user_id) references user_account(id) on delete set null,
    add constraint ck_word_set_scope
        check (
            (scope = 'SYSTEM' and owner_user_id is null)
            or (scope = 'USER' and owner_user_id is not null)
        );

create unique index uk_word_set_system_name on word_set(name) where scope = 'SYSTEM';
create unique index uk_word_set_user_name on word_set(owner_user_id, name) where scope = 'USER';
create index idx_word_set_scope_owner on word_set(scope, owner_user_id);

alter table study_plan
    add column user_id bigint;

update study_plan
set user_id = 1
where user_id is null;

alter table study_plan
    alter column user_id set not null;

alter table study_plan
    add constraint fk_study_plan_user
        foreign key (user_id) references user_account(id) on delete cascade;

create index idx_study_plan_user_status on study_plan(user_id, status);

alter table anki_template
    add column scope varchar(32),
    add column owner_user_id bigint;

update anki_template
set scope = case when name = 'Default Japanese Anki' then 'SYSTEM' else 'USER' end,
    owner_user_id = case when name = 'Default Japanese Anki' then null else 1 end
where scope is null;

alter table anki_template
    alter column scope set not null;

alter table anki_template
    drop constraint uk_anki_template_name;

alter table anki_template
    add constraint fk_anki_template_owner_user
        foreign key (owner_user_id) references user_account(id) on delete set null,
    add constraint ck_anki_template_scope
        check (
            (scope = 'SYSTEM' and owner_user_id is null)
            or (scope = 'USER' and owner_user_id is not null)
        );

create unique index uk_anki_template_system_name on anki_template(name) where scope = 'SYSTEM';
create unique index uk_anki_template_user_name on anki_template(owner_user_id, name) where scope = 'USER';
create index idx_anki_template_scope_owner on anki_template(scope, owner_user_id);

alter table md_template
    add column scope varchar(32),
    add column owner_user_id bigint;

update md_template
set scope = case when name = 'Default Daily Markdown' then 'SYSTEM' else 'USER' end,
    owner_user_id = case when name = 'Default Daily Markdown' then null else 1 end
where scope is null;

alter table md_template
    alter column scope set not null;

alter table md_template
    drop constraint uk_md_template_name;

alter table md_template
    add constraint fk_md_template_owner_user
        foreign key (owner_user_id) references user_account(id) on delete set null,
    add constraint ck_md_template_scope
        check (
            (scope = 'SYSTEM' and owner_user_id is null)
            or (scope = 'USER' and owner_user_id is not null)
        );

create unique index uk_md_template_system_name on md_template(name) where scope = 'SYSTEM';
create unique index uk_md_template_user_name on md_template(owner_user_id, name) where scope = 'USER';
create index idx_md_template_scope_owner on md_template(scope, owner_user_id);
