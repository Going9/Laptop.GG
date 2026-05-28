ALTER TABLE public.crawler_run
    ADD COLUMN IF NOT EXISTS detail_refresh_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS price_only_updated_count integer NOT NULL DEFAULT 0;
