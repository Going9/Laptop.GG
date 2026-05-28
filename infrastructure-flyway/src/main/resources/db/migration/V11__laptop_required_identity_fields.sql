DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_laptop_name_required'
    ) THEN
        ALTER TABLE public.laptop
            ADD CONSTRAINT chk_laptop_name_required
                CHECK (name IS NOT NULL AND btrim(name) <> '') NOT VALID;
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_laptop_image_url_required'
    ) THEN
        ALTER TABLE public.laptop
            ADD CONSTRAINT chk_laptop_image_url_required
                CHECK (image_url IS NOT NULL AND btrim(image_url) <> '') NOT VALID;
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_laptop_detail_page_required'
    ) THEN
        ALTER TABLE public.laptop
            ADD CONSTRAINT chk_laptop_detail_page_required
                CHECK (detail_page IS NOT NULL AND btrim(detail_page) <> '') NOT VALID;
    END IF;
END
$$;
