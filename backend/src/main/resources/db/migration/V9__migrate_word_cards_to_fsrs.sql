alter table card_instance
    add column due_at timestamptz,
    add column fsrs_card_json jsonb,
    add column review_count integer not null default 0,
    add column last_reviewed_at timestamptz;

update card_instance
set due_at = due_date::timestamp at time zone 'UTC'
where due_at is null;

with review_summary as (
    select
        ci.plan_id,
        ci.word_entry_id,
        count(rl.id)::integer as review_count,
        max(rl.reviewed_at) as last_reviewed_at
    from card_instance ci
    left join review_log rl on rl.card_instance_id = ci.id
    group by ci.plan_id, ci.word_entry_id
)
update card_instance ci
set review_count = review_summary.review_count,
    last_reviewed_at = review_summary.last_reviewed_at,
    card_type = case when review_summary.review_count > 0 then 'REVIEW' else 'NEW' end
from review_summary
where ci.plan_id = review_summary.plan_id
  and ci.word_entry_id = review_summary.word_entry_id;

with pending_rows as (
    select
        ci.id,
        row_number() over (
            partition by ci.plan_id, ci.word_entry_id
            order by ci.due_at asc, ci.stage_no asc, ci.id asc
        ) as pending_rank
    from card_instance ci
    where ci.status = 'PENDING'
)
delete from card_instance ci
using pending_rows pending_rows
where ci.id = pending_rows.id
  and pending_rows.pending_rank > 1
  and not exists (
      select 1
      from review_log rl
      where rl.card_instance_id = ci.id
  );

alter table card_instance
    alter column due_at set not null;

alter table card_instance
    add constraint ck_card_instance_review_count_non_negative
        check (review_count >= 0);

create index idx_card_instance_plan_due_at on card_instance(plan_id, due_at);
