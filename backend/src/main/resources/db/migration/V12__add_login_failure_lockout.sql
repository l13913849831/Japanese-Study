alter table user_identity
    add column failed_login_count integer not null default 0,
    add column last_failed_login_at timestamptz,
    add column locked_until timestamptz,
    add constraint ck_user_identity_failed_login_count_non_negative
        check (failed_login_count >= 0);

create index idx_user_identity_locked_until
    on user_identity(locked_until)
    where locked_until is not null;
