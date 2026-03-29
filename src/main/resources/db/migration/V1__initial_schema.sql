CREATE TABLE public.comment (
    id bigint NOT NULL,
    author character varying(255),
    content character varying(255),
    pass_word character varying(255),
    laptop_id bigint
);

CREATE SEQUENCE public.comment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.comment_id_seq OWNED BY public.comment.id;

CREATE TABLE public.laptop (
    id bigint NOT NULL,
    battery_capacity double precision,
    brightness integer,
    cpu character varying(255),
    cpu_manufacturer character varying(255),
    detail_page character varying(255),
    graphics_type character varying(255),
    image_url character varying(255),
    is_ram_replaceable boolean,
    is_supports_pd_charging boolean,
    name character varying(255),
    os character varying(255),
    price integer,
    product_code character varying(255),
    ram_size integer,
    ram_type character varying(255),
    refresh_rate integer,
    resolution character varying(255),
    screen_size integer,
    sd_card character varying(255),
    storage_capacity integer,
    storage_slot_count integer,
    tgp integer,
    thunderbolt_count integer,
    usbacount integer,
    usbccount integer,
    weight double precision
);

CREATE SEQUENCE public.laptop_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.laptop_id_seq OWNED BY public.laptop.id;

CREATE TABLE public.laptop_profile (
    id bigint NOT NULL,
    aaa_game_score integer NOT NULL,
    battery_score integer NOT NULL,
    battery_tier character varying(255),
    casual_game_score integer NOT NULL,
    cpu_class character varying(255),
    creator_score integer NOT NULL,
    gpu_class character varying(255),
    office_score integer NOT NULL,
    online_game_score integer NOT NULL,
    portability_tier character varying(255),
    laptop_id bigint NOT NULL,
    CONSTRAINT laptop_profile_battery_tier_check CHECK (((battery_tier)::text = ANY ((ARRAY['VERY_LOW'::character varying, 'LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'VERY_HIGH'::character varying, 'UNKNOWN'::character varying])::text[]))),
    CONSTRAINT laptop_profile_cpu_class_check CHECK (((cpu_class)::text = ANY ((ARRAY['ULTRA_LOW_POWER'::character varying, 'LOW_POWER'::character varying, 'BALANCED'::character varying, 'PERFORMANCE'::character varying, 'ENTHUSIAST'::character varying, 'WORKSTATION'::character varying, 'UNKNOWN'::character varying])::text[]))),
    CONSTRAINT laptop_profile_gpu_class_check CHECK (((gpu_class)::text = ANY ((ARRAY['INTEGRATED_ENTRY'::character varying, 'INTEGRATED_MAINSTREAM'::character varying, 'INTEGRATED_HIGH'::character varying, 'DISCRETE_ENTRY'::character varying, 'DISCRETE_MAINSTREAM'::character varying, 'DISCRETE_HIGH'::character varying, 'DISCRETE_ENTHUSIAST'::character varying, 'WORKSTATION'::character varying, 'UNKNOWN'::character varying])::text[]))),
    CONSTRAINT laptop_profile_portability_tier_check CHECK (((portability_tier)::text = ANY ((ARRAY['TABLET_LIGHT'::character varying, 'ULTRALIGHT'::character varying, 'LIGHT'::character varying, 'BALANCED'::character varying, 'HEAVY'::character varying, 'UNKNOWN'::character varying])::text[])))
);

CREATE SEQUENCE public.laptop_profile_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.laptop_profile_id_seq OWNED BY public.laptop_profile.id;

CREATE TABLE public.laptop_usage (
    id bigint NOT NULL,
    laptop_usage character varying(255),
    laptop_id bigint NOT NULL
);

CREATE SEQUENCE public.laptop_usage_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.laptop_usage_id_seq OWNED BY public.laptop_usage.id;

ALTER TABLE ONLY public.comment
    ALTER COLUMN id SET DEFAULT nextval('public.comment_id_seq'::regclass);

ALTER TABLE ONLY public.laptop
    ALTER COLUMN id SET DEFAULT nextval('public.laptop_id_seq'::regclass);

ALTER TABLE ONLY public.laptop_profile
    ALTER COLUMN id SET DEFAULT nextval('public.laptop_profile_id_seq'::regclass);

ALTER TABLE ONLY public.laptop_usage
    ALTER COLUMN id SET DEFAULT nextval('public.laptop_usage_id_seq'::regclass);

ALTER TABLE ONLY public.comment
    ADD CONSTRAINT comment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.laptop
    ADD CONSTRAINT laptop_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.laptop_profile
    ADD CONSTRAINT laptop_profile_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.laptop_usage
    ADD CONSTRAINT laptop_usage_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.laptop_profile
    ADD CONSTRAINT uk_j8cetkmbjqmsbc95parksbwm1 UNIQUE (laptop_id);

ALTER TABLE ONLY public.laptop_usage
    ADD CONSTRAINT fk4ykw0oc060ndumb531i3xcnkb FOREIGN KEY (laptop_id) REFERENCES public.laptop(id);

ALTER TABLE ONLY public.laptop_profile
    ADD CONSTRAINT fkqhdwajc6hagouhpjpkiec6a1 FOREIGN KEY (laptop_id) REFERENCES public.laptop(id);

ALTER TABLE ONLY public.comment
    ADD CONSTRAINT fkt72yqmisq1grwfoxw67bem85w FOREIGN KEY (laptop_id) REFERENCES public.laptop(id);
