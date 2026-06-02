CREATE TABLE IF NOT EXISTS public.comment_invalid_legacy (
    original_comment_id bigint PRIMARY KEY,
    author character varying(255),
    content character varying(255),
    pass_word character varying(255),
    laptop_id bigint,
    archived_at timestamp without time zone NOT NULL DEFAULT now(),
    reason text NOT NULL
);

INSERT INTO public.comment_invalid_legacy (
    original_comment_id,
    author,
    content,
    pass_word,
    laptop_id,
    reason
)
SELECT
    c.id,
    c.author,
    c.content,
    c.pass_word,
    c.laptop_id,
    concat_ws(
        ',',
        CASE WHEN c.author IS NULL THEN 'missing_author' END,
        CASE WHEN c.content IS NULL THEN 'missing_content' END,
        CASE WHEN c.pass_word IS NULL THEN 'missing_password_hash' END,
        CASE WHEN c.laptop_id IS NULL THEN 'missing_laptop' END
    )
FROM public.comment c
WHERE c.author IS NULL
   OR c.content IS NULL
   OR c.pass_word IS NULL
   OR c.laptop_id IS NULL
ON CONFLICT (original_comment_id) DO NOTHING;

DELETE FROM public.comment
WHERE author IS NULL
   OR content IS NULL
   OR pass_word IS NULL
   OR laptop_id IS NULL;

ALTER TABLE public.comment
    ALTER COLUMN author SET NOT NULL,
    ALTER COLUMN content SET NOT NULL,
    ALTER COLUMN pass_word SET NOT NULL,
    ALTER COLUMN laptop_id SET NOT NULL;
