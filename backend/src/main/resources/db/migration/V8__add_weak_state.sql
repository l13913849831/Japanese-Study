alter table card_instance
    add column weak_flag boolean not null default false,
    add column weak_marked_at timestamptz,
    add column weak_review_count integer not null default 0,
    add column last_review_rating varchar(16);

alter table card_instance
    add constraint ck_card_instance_weak_review_count_non_negative
        check (weak_review_count >= 0),
    add constraint ck_card_instance_last_review_rating
        check (last_review_rating is null or last_review_rating in ('AGAIN', 'HARD', 'GOOD', 'EASY'));

create index idx_card_instance_weak_flag on card_instance(weak_flag, weak_marked_at desc, id desc);

alter table note
    add column weak_flag boolean not null default false,
    add column weak_marked_at timestamptz,
    add column last_review_rating varchar(16);

alter table note
    add constraint ck_note_last_review_rating
        check (last_review_rating is null or last_review_rating in ('AGAIN', 'HARD', 'GOOD', 'EASY'));

create index idx_note_weak_flag on note(weak_flag, weak_marked_at desc, id desc);
