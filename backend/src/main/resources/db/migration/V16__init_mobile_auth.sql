alter table user_identity
    drop constraint if exists ck_user_identity_provider;

alter table user_identity
    add constraint ck_user_identity_provider
        check (provider in ('LOCAL', 'KEYCLOAK', 'WECHAT_MP', 'WECHAT_MINIAPP'));

create table mobile_session (
    id bigserial primary key,
    user_id bigint not null,
    token_hash varchar(128) not null,
    expires_at timestamptz not null,
    last_used_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_mobile_session_user
        foreign key (user_id) references user_account(id) on delete cascade,
    constraint uk_mobile_session_token_hash
        unique (token_hash),
    constraint ck_mobile_session_token_hash_not_blank
        check (btrim(token_hash) <> '')
);

create index idx_mobile_session_user on mobile_session(user_id);
create index idx_mobile_session_expires_at on mobile_session(expires_at);
create index idx_mobile_session_active on mobile_session(user_id, expires_at) where revoked_at is null;
