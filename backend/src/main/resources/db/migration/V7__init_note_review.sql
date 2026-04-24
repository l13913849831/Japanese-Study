create table note (
    id bigserial primary key,
    title varchar(255) not null,
    content text not null,
    tags jsonb not null default '[]'::jsonb,
    review_count integer not null default 0,
    mastery_status varchar(32) not null default 'UNSTARTED',
    due_at timestamptz not null,
    last_reviewed_at timestamptz,
    fsrs_card_json text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_note_tags_is_array
        check (jsonb_typeof(tags) = 'array'),
    constraint ck_note_review_count_non_negative
        check (review_count >= 0),
    constraint ck_note_mastery_status
        check (mastery_status in ('UNSTARTED', 'LEARNING', 'CONSOLIDATING', 'MASTERED'))
);

create index idx_note_due_at on note(due_at);
create index idx_note_mastery_status on note(mastery_status);
create index idx_note_updated_at on note(updated_at desc, id desc);

create table note_review_log (
    id bigserial primary key,
    note_id bigint not null,
    reviewed_at timestamptz not null,
    rating varchar(16) not null,
    response_time_ms bigint,
    note_text text,
    fsrs_review_log_json text not null,
    created_at timestamptz not null default now(),
    constraint fk_note_review_log_note
        foreign key (note_id) references note(id) on delete cascade,
    constraint ck_note_review_log_rating
        check (rating in ('AGAIN', 'HARD', 'GOOD', 'EASY')),
    constraint ck_note_review_log_response_time_non_negative
        check (response_time_ms is null or response_time_ms >= 0)
);

create index idx_note_review_log_note_reviewed_at on note_review_log(note_id, reviewed_at desc);
create index idx_note_review_log_reviewed_at on note_review_log(reviewed_at desc);
