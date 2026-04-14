create table card_instance (
    id bigserial primary key,
    plan_id bigint not null,
    word_entry_id bigint not null,
    card_type varchar(32) not null,
    sequence_no integer not null,
    stage_no integer not null,
    due_date date not null,
    status varchar(32) not null default 'PENDING',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_card_instance_plan
        foreign key (plan_id) references study_plan(id) on delete cascade,
    constraint fk_card_instance_word_entry
        foreign key (word_entry_id) references word_entry(id) on delete cascade,
    constraint ck_card_instance_card_type
        check (card_type in ('NEW', 'REVIEW')),
    constraint ck_card_instance_sequence_no_positive
        check (sequence_no > 0),
    constraint ck_card_instance_stage_no_non_negative
        check (stage_no >= 0),
    constraint ck_card_instance_status
        check (status in ('PENDING', 'DONE', 'SKIPPED')),
    constraint uk_card_instance_plan_word_stage
        unique (plan_id, word_entry_id, stage_no)
);

create index idx_card_instance_plan_due_date on card_instance(plan_id, due_date);
create index idx_card_instance_plan_sequence_no on card_instance(plan_id, sequence_no);
create index idx_card_instance_word_entry_id on card_instance(word_entry_id);

create table review_log (
    id bigserial primary key,
    card_instance_id bigint not null,
    reviewed_at timestamptz not null,
    rating varchar(16) not null,
    response_time_ms bigint,
    note text,
    created_at timestamptz not null default now(),
    constraint fk_review_log_card_instance
        foreign key (card_instance_id) references card_instance(id) on delete cascade,
    constraint ck_review_log_rating
        check (rating in ('AGAIN', 'HARD', 'GOOD', 'EASY')),
    constraint ck_review_log_response_time_non_negative
        check (response_time_ms is null or response_time_ms >= 0)
);

create index idx_review_log_card_instance_reviewed_at on review_log(card_instance_id, reviewed_at);
