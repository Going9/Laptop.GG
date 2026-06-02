create table recommendation_score (
    id bigserial primary key,
    laptop_id bigint not null references laptop(id),
    use_case varchar(64) not null,
    gate_score integer not null,
    static_score double precision not null,
    budget_weight double precision not null,
    updated_at timestamp not null default now(),
    constraint uk_recommendation_score_laptop_use_case unique (laptop_id, use_case)
);

create index idx_recommendation_score_use_case_gate_static
    on recommendation_score(use_case, gate_score, static_score desc);

insert into recommendation_score (
    laptop_id,
    use_case,
    gate_score,
    static_score,
    budget_weight,
    updated_at
)
select
    p.laptop_id,
    scores.use_case,
    scores.gate_score,
    scores.static_score,
    scores.budget_weight,
    now()
from laptop_profile p
cross join lateral (
    values
        (
            'NOT_SURE',
            round((p.office_score + p.battery_score + p.casual_game_score)::numeric / 3.0)::integer,
            (p.office_score * 0.24) +
                (p.battery_score * 0.20) +
                (p.portability_score * 0.16) +
                (p.display_score * 0.10) +
                (p.ram_score * 0.08) +
                (p.gpu_performance_score * 0.08),
            0.14
        ),
        (
            'OFFICE_STUDY',
            p.office_score,
            (p.portability_score * 0.20) +
                (p.battery_score * 0.15) +
                (p.display_score * 0.10) +
                (p.office_score * 0.30),
            0.25
        ),
        (
            'PORTABLE_OFFICE',
            p.office_score,
            (p.portability_score * 0.35) +
                (p.battery_score * 0.25) +
                (p.office_score * 0.20) +
                (p.display_score * 0.10),
            0.10
        ),
        (
            'BATTERY_FIRST',
            p.battery_score,
            (p.battery_score * 0.45) +
                (p.portability_score * 0.20) +
                (p.office_score * 0.15) +
                (p.low_power_cpu_score * 0.10),
            0.10
        ),
        (
            'CASUAL_GAME',
            p.casual_game_score,
            (p.gpu_performance_score * 0.35) +
                (p.cpu_performance_score * 0.20) +
                (p.ram_score * 0.15) +
                (p.display_score * 0.10) +
                (p.portability_score * 0.10),
            0.10
        ),
        (
            'ONLINE_GAME',
            p.online_game_score,
            (p.gpu_performance_score * 0.40) +
                (p.cpu_performance_score * 0.20) +
                (p.ram_score * 0.15) +
                (p.tgp_score * 0.10) +
                (p.display_score * 0.10),
            0.05
        ),
        (
            'AAA_GAME',
            p.aaa_game_score,
            (p.gpu_performance_score * 0.45) +
                (p.tgp_score * 0.20) +
                (p.cpu_performance_score * 0.15) +
                (p.ram_score * 0.10) +
                (p.display_score * 0.05),
            0.05
        ),
        (
            'CREATOR',
            p.creator_score,
            (p.cpu_performance_score * 0.20) +
                (least(p.gpu_performance_score + p.gpu_creator_bonus, 100) * 0.20) +
                (p.ram_score * 0.20) +
                (p.display_score * 0.20) +
                (p.battery_score * 0.05) +
                (p.portability_score * 0.05),
            0.10
        )
) as scores(use_case, gate_score, static_score, budget_weight)
on conflict (laptop_id, use_case) do nothing;
