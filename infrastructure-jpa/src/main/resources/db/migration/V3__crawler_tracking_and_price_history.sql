ALTER TABLE public.laptop
    ADD COLUMN IF NOT EXISTS last_detailed_crawled_at timestamp without time zone;

UPDATE public.laptop
SET last_detailed_crawled_at = COALESCE(last_detailed_crawled_at, NOW())
WHERE last_detailed_crawled_at IS NULL;

CREATE TABLE IF NOT EXISTS public.laptop_price_history (
    id bigint NOT NULL,
    laptop_id bigint NOT NULL,
    price integer NOT NULL,
    captured_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS public.laptop_price_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.laptop_price_history_id_seq OWNED BY public.laptop_price_history.id;

ALTER TABLE ONLY public.laptop_price_history
    ALTER COLUMN id SET DEFAULT nextval('public.laptop_price_history_id_seq'::regclass);

ALTER TABLE ONLY public.laptop_price_history
    ADD CONSTRAINT laptop_price_history_pkey PRIMARY KEY (id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_laptop_price_history_laptop'
    ) THEN
        ALTER TABLE ONLY public.laptop_price_history
            ADD CONSTRAINT fk_laptop_price_history_laptop
                FOREIGN KEY (laptop_id) REFERENCES public.laptop(id);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.idx_laptop_price_history_laptop_captured_at') IS NULL THEN
        CREATE INDEX idx_laptop_price_history_laptop_captured_at
            ON public.laptop_price_history (laptop_id, captured_at DESC);
    END IF;
END
$$;

INSERT INTO public.laptop_price_history (laptop_id, price, captured_at)
SELECT l.id, l.price, COALESCE(l.last_detailed_crawled_at, NOW())
FROM public.laptop l
WHERE l.price IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM public.laptop_price_history h
    WHERE h.laptop_id = l.id
  );
