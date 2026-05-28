DO $$
DECLARE
    duplicate_product_code_count integer;
    duplicate_detail_page_count integer;
BEGIN
    SELECT count(*)
    INTO duplicate_product_code_count
    FROM (
        SELECT btrim(product_code) AS product_code
        FROM public.laptop
        WHERE product_code IS NOT NULL
          AND btrim(product_code) <> ''
        GROUP BY btrim(product_code)
        HAVING count(*) > 1
    ) duplicates;

    IF duplicate_product_code_count > 0 THEN
        RAISE EXCEPTION
            'crawler identity preflight failed: % duplicate product_code group(s). Run ops/sql/crawler-identity-diagnostics.sql before crawling.',
            duplicate_product_code_count;
    END IF;

    SELECT count(*)
    INTO duplicate_detail_page_count
    FROM (
        SELECT btrim(detail_page) AS detail_page
        FROM public.laptop
        WHERE detail_page IS NOT NULL
          AND btrim(detail_page) <> ''
        GROUP BY btrim(detail_page)
        HAVING count(*) > 1
    ) duplicates;

    IF duplicate_detail_page_count > 0 THEN
        RAISE EXCEPTION
            'crawler identity preflight failed: % duplicate detail_page group(s). Run ops/sql/crawler-identity-diagnostics.sql before crawling.',
            duplicate_detail_page_count;
    END IF;
END
$$;
