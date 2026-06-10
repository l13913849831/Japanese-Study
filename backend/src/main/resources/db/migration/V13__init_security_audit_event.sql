create table security_audit_event (
    id bigserial primary key,
    event_type varchar(64) not null,
    outcome varchar(32) not null,
    user_id bigint,
    username varchar(255),
    ip_address varchar(64),
    user_agent varchar(512),
    message varchar(512),
    created_at timestamptz not null default now(),
    constraint fk_security_audit_event_user
        foreign key (user_id) references user_account(id) on delete set null,
    constraint ck_security_audit_event_type
        check (event_type in ('LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGIN_LOCKED', 'LOGIN_DISABLED', 'LOGOUT')),
    constraint ck_security_audit_event_outcome
        check (outcome in ('SUCCESS', 'FAILURE', 'BLOCKED'))
);

create index idx_security_audit_event_created_at on security_audit_event(created_at desc, id desc);
create index idx_security_audit_event_user_created_at on security_audit_event(user_id, created_at desc, id desc);
