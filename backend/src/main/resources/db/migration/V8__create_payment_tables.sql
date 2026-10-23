create table perfumestore.payment_attempts (
    id uuid primary key,
    order_id uuid not null references perfumestore.orders(id) on delete restrict,
    provider varchar(40) not null,
    provider_payment_id varchar(120),
    checkout_session_id varchar(120),
    idempotency_key varchar(100) not null,
    request_fingerprint char(64) not null,
    amount numeric(12,2) not null,
    currency char(3) not null default 'BRL',
    status varchar(30) not null,
    error_code varchar(80),
    error_message varchar(255),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    completed_at timestamptz,
    expires_at timestamptz not null,
    last_provider_check_at timestamptz,
    constraint payment_attempts_fingerprint_ck check (request_fingerprint ~ '^[0-9a-f]{64}$'),
    constraint payment_attempts_money_ck check (amount >= 0),
    constraint payment_attempts_currency_ck check (currency = 'BRL'),
    constraint payment_attempts_status_ck check (status in ('PENDING','APPROVED','REJECTED','FAILED','CANCELLED','EXPIRED','UNKNOWN','REVIEW')),
    constraint payment_attempts_key_ck check (length(trim(idempotency_key)) between 8 and 100)
);
create unique index payment_attempts_order_key_uq on perfumestore.payment_attempts(order_id, idempotency_key);
create unique index payment_attempts_provider_payment_uq on perfumestore.payment_attempts(provider, provider_payment_id)
    where provider_payment_id is not null;
create unique index payment_attempts_checkout_session_uq on perfumestore.payment_attempts(provider, checkout_session_id)
    where checkout_session_id is not null;
create index payment_attempts_order_status_idx on perfumestore.payment_attempts(order_id, status, created_at desc);
create index payment_attempts_reconcile_idx on perfumestore.payment_attempts(status, expires_at, updated_at);

create table perfumestore.payment_attempt_history (
    id uuid primary key,
    payment_attempt_id uuid not null references perfumestore.payment_attempts(id) on delete cascade,
    from_status varchar(30),
    to_status varchar(30) not null,
    event varchar(60) not null,
    correlation_id varchar(64),
    created_at timestamptz not null,
    constraint payment_attempt_history_status_ck check (to_status in ('PENDING','APPROVED','REJECTED','FAILED','CANCELLED','EXPIRED','UNKNOWN','REVIEW'))
);
create index payment_attempt_history_attempt_idx on perfumestore.payment_attempt_history(payment_attempt_id, created_at, id);

create table perfumestore.payment_events (
    id uuid primary key,
    provider varchar(40) not null,
    provider_event_id varchar(120) not null,
    event_type varchar(40) not null,
    provider_payment_id varchar(120) not null,
    occurred_at timestamptz not null,
    received_at timestamptz not null,
    amount numeric(12,2),
    currency char(3),
    payload_hash char(64) not null,
    signature_valid boolean not null,
    processing_status varchar(30) not null,
    processing_error_code varchar(80),
    processed_at timestamptz,
    constraint payment_events_hash_ck check (payload_hash ~ '^[0-9a-f]{64}$'),
    constraint payment_events_amount_ck check (amount is null or amount >= 0),
    constraint payment_events_currency_ck check (currency is null or currency = 'BRL'),
    constraint payment_events_status_ck check (processing_status in ('RECEIVED','PROCESSED','IGNORED','REVIEW','REJECTED')),
    constraint payment_events_provider_event_uq unique(provider, provider_event_id)
);
create index payment_events_payment_idx on perfumestore.payment_events(provider, provider_payment_id, occurred_at);
create index payment_events_processing_idx on perfumestore.payment_events(processing_status, received_at);

create table perfumestore.payment_reconciliations (
    id uuid primary key,
    payment_attempt_id uuid not null references perfumestore.payment_attempts(id) on delete cascade,
    provider varchar(40) not null,
    provider_status varchar(40),
    result_status varchar(30) not null,
    error_code varchar(80),
    requested_at timestamptz not null,
    completed_at timestamptz,
    constraint payment_reconciliations_status_ck check (result_status in ('PENDING','APPLIED','NO_CHANGE','FAILED','REVIEW'))
);
create index payment_reconciliations_attempt_idx on perfumestore.payment_reconciliations(payment_attempt_id, requested_at desc);

create table perfumestore.mock_payment_states (
    provider_payment_id varchar(120) primary key,
    payment_attempt_id uuid not null unique references perfumestore.payment_attempts(id) on delete cascade,
    status varchar(30) not null,
    amount numeric(12,2) not null,
    currency char(3) not null default 'BRL',
    last_event_id varchar(120),
    occurred_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint mock_payment_states_status_ck check (status in ('PENDING','APPROVED','REJECTED','FAILED','CANCELLED','EXPIRED')),
    constraint mock_payment_states_money_ck check (amount >= 0),
    constraint mock_payment_states_currency_ck check (currency = 'BRL')
);
