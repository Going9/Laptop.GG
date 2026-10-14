alter table laptop add column price_checked_at timestamp with time zone;

-- Historical crawler timestamps were written in UTC. Unknown observations must expire.
update laptop l
set price_checked_at = coalesce(
    (select max(h.captured_at) from laptop_price_history h where h.laptop_id = l.id) at time zone 'UTC',
    timestamp with time zone '1970-01-01 00:00:00+00'
);

alter table laptop alter column price_checked_at set default current_timestamp;
alter table laptop alter column price_checked_at set not null;
