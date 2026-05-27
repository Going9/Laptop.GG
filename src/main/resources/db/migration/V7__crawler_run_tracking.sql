CREATE TABLE IF NOT EXISTS public.crawler_run (
    id bigint NOT NULL,
    filter_profile character varying(64) NOT NULL,
    start_page integer NOT NULL,
    limit_count integer,
    status character varying(32) NOT NULL,
    processed_count integer NOT NULL DEFAULT 0,
    created_count integer NOT NULL DEFAULT 0,
    updated_count integer NOT NULL DEFAULT 0,
    degraded_count integer NOT NULL DEFAULT 0,
    failed_count integer NOT NULL DEFAULT 0,
    failure_samples text,
    error_message text,
    started_at timestamp without time zone NOT NULL,
    ended_at timestamp without time zone
);

CREATE SEQUENCE IF NOT EXISTS public.crawler_run_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.crawler_run_id_seq OWNED BY public.crawler_run.id;

ALTER TABLE ONLY public.crawler_run
    ALTER COLUMN id SET DEFAULT nextval('public.crawler_run_id_seq'::regclass);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'crawler_run_pkey'
    ) THEN
        ALTER TABLE ONLY public.crawler_run
            ADD CONSTRAINT crawler_run_pkey PRIMARY KEY (id);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_crawler_run_started_at
    ON public.crawler_run (started_at DESC);

CREATE INDEX IF NOT EXISTS idx_crawler_run_status_started_at
    ON public.crawler_run (status, started_at DESC);
