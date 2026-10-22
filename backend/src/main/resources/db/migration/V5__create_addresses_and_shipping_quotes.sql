create table perfumestore.addresses (
    id uuid primary key,
    user_id uuid not null references perfumestore.app_users(id) on delete restrict,
    recipient_name varchar(120) not null,
    label varchar(60),
    postal_code varchar(8) not null,
    street varchar(180) not null,
    number varchar(30) not null,
    complement varchar(120),
    neighborhood varchar(120) not null,
    city varchar(120) not null,
    state char(2) not null,
    reference_text varchar(180),
    version bigint not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint addresses_postal_code_ck check (postal_code ~ '^[0-9]{8}$'),
    constraint addresses_state_ck check (state in (
        'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA','PB','PR',
        'PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')),
    constraint addresses_version_ck check (version >= 0)
);

create index addresses_user_active_idx
    on perfumestore.addresses (user_id, deleted_at, updated_at desc, id asc);

create table perfumestore.shipping_quotes (
    id uuid primary key,
    user_id uuid not null references perfumestore.app_users(id) on delete restrict,
    address_id uuid not null references perfumestore.addresses(id) on delete restrict,
    postal_code varchar(8) not null,
    city varchar(120) not null,
    state char(2) not null,
    serviceable boolean not null,
    method varchar(60) not null,
    amount numeric(12,2) not null,
    currency char(3) not null,
    estimated_min_days integer not null,
    estimated_max_days integer not null,
    rule_version varchar(100) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    constraint shipping_quotes_amount_ck check (amount >= 0),
    constraint shipping_quotes_currency_ck check (currency = 'BRL'),
    constraint shipping_quotes_min_days_ck check (estimated_min_days >= 0),
    constraint shipping_quotes_max_days_ck check (estimated_max_days >= estimated_min_days)
);

create index shipping_quotes_user_created_idx
    on perfumestore.shipping_quotes (user_id, created_at desc, id asc);

create index shipping_quotes_expiry_idx
    on perfumestore.shipping_quotes (expires_at);
