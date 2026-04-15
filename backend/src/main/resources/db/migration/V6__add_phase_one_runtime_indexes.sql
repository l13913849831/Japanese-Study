create index if not exists idx_card_instance_plan_due_date_type_status
    on card_instance(plan_id, due_date, card_type, status);

create index if not exists idx_export_job_plan_target_date
    on export_job(plan_id, target_date);
