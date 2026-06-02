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

CREATE INDEX IF NOT EXISTS idx_laptop_product_code
    ON public.laptop (product_code);
