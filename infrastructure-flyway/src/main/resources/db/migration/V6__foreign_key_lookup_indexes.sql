CREATE INDEX IF NOT EXISTS idx_comment_laptop_id
    ON public.comment (laptop_id);

CREATE INDEX IF NOT EXISTS idx_laptop_usage_laptop_id
    ON public.laptop_usage (laptop_id);
