create table anki_template (
    id bigserial primary key,
    name varchar(128) not null,
    description varchar(512),
    field_mapping jsonb not null default '{}'::jsonb,
    front_template text not null,
    back_template text not null,
    css_template text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_anki_template_name unique (name),
    constraint ck_anki_template_field_mapping_object
        check (jsonb_typeof(field_mapping) = 'object')
);

create table md_template (
    id bigserial primary key,
    name varchar(128) not null,
    description varchar(512),
    template_content text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_md_template_name unique (name)
);

create table study_plan (
    id bigserial primary key,
    name varchar(128) not null,
    word_set_id bigint not null,
    start_date date not null,
    daily_new_count integer not null,
    review_offsets jsonb not null,
    anki_template_id bigint,
    md_template_id bigint,
    status varchar(32) not null default 'DRAFT',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_study_plan_word_set
        foreign key (word_set_id) references word_set(id) on delete restrict,
    constraint fk_study_plan_anki_template
        foreign key (anki_template_id) references anki_template(id) on delete set null,
    constraint fk_study_plan_md_template
        foreign key (md_template_id) references md_template(id) on delete set null,
    constraint ck_study_plan_daily_new_count_positive
        check (daily_new_count > 0),
    constraint ck_study_plan_review_offsets_array
        check (jsonb_typeof(review_offsets) = 'array'),
    constraint ck_study_plan_status
        check (status in ('DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED'))
);

create index idx_study_plan_word_set_id on study_plan(word_set_id);
create index idx_study_plan_status on study_plan(status);
create index idx_study_plan_start_date on study_plan(start_date);
