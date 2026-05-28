DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_laptop_usage_value_required'
    ) THEN
        ALTER TABLE public.laptop_usage
            ADD CONSTRAINT chk_laptop_usage_value_required
                CHECK (laptop_usage IS NOT NULL AND btrim(laptop_usage) <> '') NOT VALID;
    END IF;
END
$$;
