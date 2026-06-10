create table learning_link (
    id bigserial primary key,
    user_id bigint not null,
    word_entry_id bigint not null,
    note_id bigint not null,
    source varchar(32) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_learning_link_user
        foreign key (user_id) references user_account(id) on delete cascade,
    constraint fk_learning_link_word_entry
        foreign key (word_entry_id) references word_entry(id) on delete cascade,
    constraint fk_learning_link_note
        foreign key (note_id) references note(id) on delete cascade,
    constraint ck_learning_link_source
        check (source in ('MANUAL', 'REVIEW')),
    constraint uk_learning_link_user_word_note
        unique (user_id, word_entry_id, note_id)
);

create index idx_learning_link_user_word on learning_link(user_id, word_entry_id);
create index idx_learning_link_user_note on learning_link(user_id, note_id);
