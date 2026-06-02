ALTER TABLE public.laptop_profile
    ADD COLUMN IF NOT EXISTS cpu_performance_score integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS low_power_cpu_score integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS gpu_performance_score integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS gpu_creator_bonus integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS portability_score integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS display_score integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ram_score integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tgp_score integer NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_laptop_profile_portability_score
    ON public.laptop_profile (portability_score);

CREATE INDEX IF NOT EXISTS idx_laptop_profile_gpu_performance_score
    ON public.laptop_profile (gpu_performance_score);
