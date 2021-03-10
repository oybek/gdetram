
alter table usr rename to user_info;
alter table user_info add column last_stop_id integer references stop(id);
alter table user_info add column last_month_active_days integer not null default 0;
