DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'laptop'
          AND column_name = 'product_code'
    ) THEN
        ALTER TABLE public.laptop
            ADD COLUMN product_code character varying(255);
    END IF;
END
$$;

UPDATE public.laptop
SET product_code = substring(detail_page from '(?:\\?|&)pcode=([^&#]+)')
WHERE product_code IS NULL
  AND detail_page IS NOT NULL
  AND detail_page LIKE '%pcode=%';

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_product_code') IS NULL THEN
        CREATE INDEX idx_laptop_product_code ON public.laptop (product_code);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_detail_page') IS NULL THEN
        CREATE INDEX idx_laptop_detail_page ON public.laptop (detail_page);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_recommend_price_weight') IS NULL THEN
        CREATE INDEX idx_laptop_recommend_price_weight ON public.laptop (price, weight);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_recommend_price_screen') IS NULL THEN
        CREATE INDEX idx_laptop_recommend_price_screen ON public.laptop (price, screen_size);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_profile_office_score') IS NULL THEN
        CREATE INDEX idx_laptop_profile_office_score ON public.laptop_profile (office_score);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_profile_battery_score') IS NULL THEN
        CREATE INDEX idx_laptop_profile_battery_score ON public.laptop_profile (battery_score);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_profile_casual_game_score') IS NULL THEN
        CREATE INDEX idx_laptop_profile_casual_game_score ON public.laptop_profile (casual_game_score);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_profile_online_game_score') IS NULL THEN
        CREATE INDEX idx_laptop_profile_online_game_score ON public.laptop_profile (online_game_score);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_profile_aaa_game_score') IS NULL THEN
        CREATE INDEX idx_laptop_profile_aaa_game_score ON public.laptop_profile (aaa_game_score);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_profile_creator_score') IS NULL THEN
        CREATE INDEX idx_laptop_profile_creator_score ON public.laptop_profile (creator_score);
    END IF;
END
$$;
