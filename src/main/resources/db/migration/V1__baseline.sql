CREATE TABLE public.appeals (
    id uuid NOT NULL,
    author_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    justification character varying(1000) NOT NULL,
    status character varying(255) NOT NULL,
    trust_case_id uuid NOT NULL,
    CONSTRAINT appeals_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'RESOLVED'::character varying])::text[])))
);

CREATE TABLE public.moderation_decisions (
    id uuid NOT NULL,
    decided_at timestamp(6) with time zone NOT NULL,
    decision character varying(255) NOT NULL,
    moderator_id uuid NOT NULL,
    reason character varying(255),
    trust_case_id uuid NOT NULL,
    CONSTRAINT moderation_decisions_decision_check CHECK (((decision)::text = ANY ((ARRAY['APPROVE'::character varying, 'REJECT'::character varying])::text[])))
);

CREATE TABLE public.outbox_events (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    payload text NOT NULL,
    published_at timestamp(6) with time zone,
    routing_key character varying(100) NOT NULL
);

CREATE TABLE public.trust_cases (
    id uuid NOT NULL,
    author_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    decided_at timestamp(6) with time zone,
    duplicate_of_case_id uuid,
    falseness_score double precision NOT NULL,
    incident_id uuid NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    occurred_at timestamp(6) with time zone NOT NULL,
    status character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    CONSTRAINT trust_cases_status_check CHECK (((status)::text = ANY ((ARRAY['CLEARED'::character varying, 'DUPLICATE'::character varying, 'HELD_FOR_MODERATION'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.moderation_decisions
    ADD CONSTRAINT moderation_decisions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.trust_cases
    ADD CONSTRAINT trust_cases_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.trust_cases
    ADD CONSTRAINT ukju0eh6t7d27re7a1cxrbr8vo8 UNIQUE (incident_id);

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT fkkj10n2qs1uyyatlsux5pho5ju FOREIGN KEY (trust_case_id) REFERENCES public.trust_cases(id);

ALTER TABLE ONLY public.moderation_decisions
    ADD CONSTRAINT fktmo5n41268j9cf7811qpwdcmh FOREIGN KEY (trust_case_id) REFERENCES public.trust_cases(id);
