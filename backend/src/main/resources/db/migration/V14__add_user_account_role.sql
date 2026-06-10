alter table user_account
    add column role varchar(32) not null default 'USER',
    add constraint ck_user_account_role
        check (role in ('USER', 'ADMIN'));

create index idx_user_account_role on user_account(role);
