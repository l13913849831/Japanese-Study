create table word_set (
    id bigserial primary key,
    name varchar(128) not null,
    description varchar(512),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_word_set_name unique (name)
);

create table word_entry (
    id bigserial primary key,
    word_set_id bigint not null,
    expression varchar(255) not null,
    reading varchar(255),
    meaning text not null,
    part_of_speech varchar(64),
    example_jp text,
    example_zh text,
    level varchar(32),
    tags jsonb not null default '[]'::jsonb,
    source_order integer not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_word_entry_word_set
        foreign key (word_set_id) references word_set(id) on delete cascade,
    constraint ck_word_entry_tags_array
        check (jsonb_typeof(tags) = 'array'),
    constraint ck_word_entry_source_order_positive
        check (source_order > 0),
    constraint uk_word_entry_unique
        unique (word_set_id, expression, reading)
);

create index idx_word_entry_word_set_id on word_entry(word_set_id);
create index idx_word_entry_word_set_source_order on word_entry(word_set_id, source_order);
create index idx_word_entry_expression on word_entry(expression);
