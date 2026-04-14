create table export_job (
    id bigserial primary key,
    plan_id bigint not null,
    export_type varchar(32) not null,
    target_date date,
    file_name varchar(255),
    file_path varchar(1024),
    status varchar(32) not null default 'PENDING',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_export_job_plan
        foreign key (plan_id) references study_plan(id) on delete cascade,
    constraint ck_export_job_type
        check (export_type in ('ANKI_CSV', 'ANKI_TSV', 'MARKDOWN')),
    constraint ck_export_job_status
        check (status in ('PENDING', 'SUCCESS', 'FAILED'))
);

create index idx_export_job_plan_created_at on export_job(plan_id, created_at);
create index idx_export_job_status on export_job(status);
