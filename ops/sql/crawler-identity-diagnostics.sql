SELECT product_code,
       count(*) AS duplicate_count,
       array_agg(id ORDER BY id) AS laptop_ids,
       array_agg(detail_page ORDER BY id) AS detail_pages,
       array_agg(name ORDER BY id) AS laptop_names
FROM public.laptop
WHERE product_code IS NOT NULL
  AND btrim(product_code) <> ''
GROUP BY product_code
HAVING count(*) > 1
ORDER BY duplicate_count DESC, product_code
LIMIT 50;

SELECT detail_page,
       count(*) AS duplicate_count,
       array_agg(id ORDER BY id) AS laptop_ids,
       array_agg(product_code ORDER BY id) AS product_codes,
       array_agg(name ORDER BY id) AS laptop_names
FROM public.laptop
WHERE detail_page IS NOT NULL
  AND btrim(detail_page) <> ''
GROUP BY detail_page
HAVING count(*) > 1
ORDER BY duplicate_count DESC, detail_page
LIMIT 50;
