create table perfumestore.orders (
    id uuid primary key,
    public_number varchar(40) not null unique,
    user_id uuid not null references perfumestore.app_users(id) on delete restrict,
    cart_id uuid,
    idempotency_key varchar(100) not null,
    request_fingerprint char(64) not null,
    order_status varchar(30) not null,
    payment_status varchar(30) not null,
    delivery_status varchar(30) not null,
    subtotal numeric(12,2) not null,
    shipping_amount numeric(12,2) not null,
    total numeric(12,2) not null,
    currency char(3) not null default 'BRL',
    created_at timestamptz not null,
    updated_at timestamptz not null,
    payment_expires_at timestamptz not null,
    constraint orders_idempotency_key_ck check (length(trim(idempotency_key)) between 8 and 100),
    constraint orders_fingerprint_ck check (request_fingerprint ~ '^[0-9a-f]{64}$'),
    constraint orders_status_ck check (order_status in ('PENDING_PAYMENT','CONFIRMED','CANCELLED','EXPIRED','PAYMENT_REVIEW')),
    constraint orders_payment_status_ck check (payment_status in ('PENDING','APPROVED','REJECTED','FAILED','EXPIRED','REFUNDED')),
    constraint orders_delivery_status_ck check (delivery_status in ('NOT_STARTED','PREPARING','SHIPPED','DELIVERED')),
    constraint orders_money_ck check (subtotal >= 0 and shipping_amount >= 0 and total >= 0 and total = subtotal + shipping_amount),
    constraint orders_currency_ck check (currency = 'BRL')
);
create unique index orders_user_idempotency_uq on perfumestore.orders(user_id, idempotency_key);
create index orders_user_created_idx on perfumestore.orders(user_id, created_at desc, id desc);
create index orders_expiration_idx on perfumestore.orders(order_status, payment_expires_at, id);

create table perfumestore.order_items (
    id uuid primary key,
    order_id uuid not null references perfumestore.orders(id) on delete cascade,
    product_id uuid not null references perfumestore.products(id) on delete restrict,
    presentation_id uuid not null references perfumestore.product_presentations(id) on delete restrict,
    sku varchar(80) not null,
    product_name varchar(180) not null,
    volume numeric(10,3) not null,
    unit varchar(20) not null,
    quantity integer not null,
    unit_price numeric(12,2) not null,
    line_total numeric(12,2) not null,
    currency char(3) not null default 'BRL',
    constraint order_items_quantity_ck check (quantity > 0),
    constraint order_items_money_ck check (unit_price > 0 and line_total = unit_price * quantity),
    constraint order_items_volume_ck check (volume > 0),
    constraint order_items_currency_ck check (currency = 'BRL'),
    constraint order_items_presentation_uq unique(order_id, presentation_id)
);
create index order_items_order_idx on perfumestore.order_items(order_id, id);

create table perfumestore.order_address_snapshots (
    order_id uuid primary key references perfumestore.orders(id) on delete cascade,
    recipient_name varchar(120) not null,
    label varchar(60),
    postal_code char(8) not null,
    street varchar(180) not null,
    number varchar(30) not null,
    complement varchar(120),
    neighborhood varchar(120) not null,
    city varchar(120) not null,
    state char(2) not null,
    reference_text varchar(180)
);

create table perfumestore.order_shipping_snapshots (
    order_id uuid primary key references perfumestore.orders(id) on delete cascade,
    quote_id uuid not null,
    method varchar(80) not null,
    amount numeric(12,2) not null,
    currency char(3) not null default 'BRL',
    estimated_min_days integer not null,
    estimated_max_days integer not null,
    day_type varchar(20) not null,
    rule_version varchar(80) not null,
    postal_code char(8) not null,
    city varchar(120) not null,
    state char(2) not null,
    constraint order_shipping_money_ck check (amount >= 0),
    constraint order_shipping_days_ck check (estimated_min_days >= 0 and estimated_max_days >= estimated_min_days),
    constraint order_shipping_currency_ck check (currency = 'BRL')
);

create table perfumestore.order_reservations (
    id uuid primary key,
    order_id uuid not null references perfumestore.orders(id) on delete cascade,
    reservation_id uuid not null references perfumestore.stock_reservations(id) on delete restrict,
    reservation_key varchar(70) not null unique,
    presentation_id uuid not null references perfumestore.product_presentations(id) on delete restrict,
    quantity integer not null,
    created_at timestamptz not null,
    constraint order_reservations_quantity_ck check (quantity > 0),
    constraint order_reservations_presentation_uq unique(order_id, presentation_id)
);
create index order_reservations_order_idx on perfumestore.order_reservations(order_id, presentation_id);

create table perfumestore.order_status_history (
    id uuid primary key,
    order_id uuid not null references perfumestore.orders(id) on delete cascade,
    from_status varchar(30),
    to_status varchar(30) not null,
    event varchar(60) not null,
    actor_user_id uuid references perfumestore.app_users(id) on delete set null,
    correlation_id varchar(64),
    created_at timestamptz not null
);
create index order_status_history_order_idx on perfumestore.order_status_history(order_id, created_at, id);
