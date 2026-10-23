create table perfumestore.carts (
    id uuid primary key,
    user_id uuid references perfumestore.app_users(id) on delete restrict,
    visitor_token_hash varchar(64),
    state varchar(20) not null,
    expires_at timestamp with time zone not null,
    last_activity_at timestamp with time zone not null,
    version bigint not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint carts_state_ck check (state in ('ACTIVE', 'EXPIRED', 'TRANSFERRED')),
    constraint carts_owner_ck check (user_id is not null or visitor_token_hash is not null),
    constraint carts_token_hash_ck check (visitor_token_hash is null or visitor_token_hash ~ '^[0-9a-f]{64}$'),
    constraint carts_version_ck check (version >= 0)
);

create unique index carts_active_user_uq
    on perfumestore.carts (user_id)
    where state = 'ACTIVE' and user_id is not null;

create unique index carts_visitor_token_hash_uq
    on perfumestore.carts (visitor_token_hash)
    where visitor_token_hash is not null;

create index carts_cleanup_idx
    on perfumestore.carts (state, expires_at, updated_at, id);

create table perfumestore.cart_items (
    id uuid primary key,
    cart_id uuid not null references perfumestore.carts(id) on delete cascade,
    presentation_id uuid not null references perfumestore.product_presentations(id) on delete restrict,
    quantity integer not null,
    price_reference numeric(12,2) not null,
    version bigint not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint cart_items_quantity_ck check (quantity > 0),
    constraint cart_items_price_reference_ck check (price_reference > 0),
    constraint cart_items_version_ck check (version >= 0),
    constraint cart_items_cart_presentation_uq unique (cart_id, presentation_id)
);

create index cart_items_cart_idx
    on perfumestore.cart_items (cart_id, created_at, id);

create index cart_items_presentation_idx
    on perfumestore.cart_items (presentation_id);

create table perfumestore.cart_mutation_keys (
    id uuid primary key,
    cart_id uuid not null references perfumestore.carts(id) on delete cascade,
    operation_key varchar(100) not null,
    fingerprint varchar(64) not null,
    created_at timestamp with time zone not null,
    constraint cart_mutation_keys_key_ck check (length(trim(operation_key)) between 8 and 100),
    constraint cart_mutation_keys_fingerprint_ck check (fingerprint ~ '^[0-9a-f]{64}$'),
    constraint cart_mutation_keys_uq unique (cart_id, operation_key)
);

create index cart_mutation_keys_created_idx
    on perfumestore.cart_mutation_keys (created_at);
