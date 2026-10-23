alter table perfumestore.inventory_movements drop constraint ck_inventory_movement_type;
alter table perfumestore.inventory_movements add constraint ck_inventory_movement_type check (movement_type in
    ('ENTRY', 'ADJUSTMENT', 'RESERVATION', 'CONFIRMATION', 'RELEASE', 'RESTOCK'));

create table perfumestore.order_logistics (
    order_id uuid primary key references perfumestore.orders(id) on delete restrict,
    carrier varchar(120),
    tracking_code varchar(120),
    shipped_at timestamptz,
    shipped_by uuid references perfumestore.app_users(id) on delete restrict,
    delivered_at timestamptz,
    delivered_by uuid references perfumestore.app_users(id) on delete restrict,
    updated_at timestamptz not null,
    constraint order_logistics_tracking_ck check (tracking_code is null or length(trim(tracking_code)) between 3 and 120)
);

create table perfumestore.admin_order_history (
    id uuid primary key,
    order_id uuid not null references perfumestore.orders(id) on delete restrict,
    operation varchar(60) not null,
    operation_key varchar(100) not null unique,
    actor_user_id uuid not null references perfumestore.app_users(id) on delete restrict,
    from_order_status varchar(30),
    to_order_status varchar(30),
    from_delivery_status varchar(30),
    to_delivery_status varchar(30),
    reason varchar(255),
    external_reference varchar(160),
    correlation_id varchar(64),
    created_at timestamptz not null,
    constraint admin_order_history_operation_ck check (length(trim(operation)) between 3 and 60)
);
create index admin_order_history_order_idx on perfumestore.admin_order_history(order_id, created_at, id);

create table perfumestore.refunds (
    id uuid primary key,
    order_id uuid not null references perfumestore.orders(id) on delete restrict,
    payment_attempt_id uuid not null references perfumestore.payment_attempts(id) on delete restrict,
    amount numeric(12,2) not null,
    currency char(3) not null default 'BRL',
    reason varchar(255) not null,
    status varchar(30) not null,
    idempotency_key varchar(100) not null,
    request_fingerprint char(64) not null,
    provider_ref varchar(160),
    requested_by uuid not null references perfumestore.app_users(id) on delete restrict,
    requested_at timestamptz not null,
    updated_at timestamptz not null,
    completed_at timestamptz,
    constraint refunds_order_uq unique(order_id),
    constraint refunds_order_key_uq unique(order_id, idempotency_key),
    constraint refunds_money_ck check (amount > 0 and currency = 'BRL'),
    constraint refunds_status_ck check (status in ('REQUESTED','PENDING_PROVIDER','CONFIRMED','FAILED','REVIEW_REQUIRED')),
    constraint refunds_fingerprint_ck check (request_fingerprint ~ '^[0-9a-f]{64}$')
);
create index refunds_status_idx on perfumestore.refunds(status, updated_at, id);

create table perfumestore.refund_history (
    id uuid primary key,
    refund_id uuid not null references perfumestore.refunds(id) on delete restrict,
    from_status varchar(30),
    to_status varchar(30) not null,
    event varchar(60) not null,
    actor_user_id uuid references perfumestore.app_users(id) on delete restrict,
    provider_ref varchar(160),
    reason varchar(255),
    correlation_id varchar(64),
    created_at timestamptz not null,
    constraint refund_history_status_ck check (to_status in ('REQUESTED','PENDING_PROVIDER','CONFIRMED','FAILED','REVIEW_REQUIRED'))
);
create index refund_history_refund_idx on perfumestore.refund_history(refund_id, created_at, id);

create table perfumestore.mock_refund_states (
    provider_ref varchar(160) primary key,
    refund_id uuid not null unique references perfumestore.refunds(id) on delete restrict,
    status varchar(30) not null,
    amount numeric(12,2) not null,
    currency char(3) not null default 'BRL',
    occurred_at timestamptz not null,
    updated_at timestamptz not null,
    constraint mock_refund_status_ck check (status in ('PENDING','APPROVED','FAILED','UNKNOWN')),
    constraint mock_refund_money_ck check (amount > 0 and currency = 'BRL')
);

create table perfumestore.returns (
    id uuid primary key,
    order_id uuid not null references perfumestore.orders(id) on delete restrict,
    order_item_id uuid not null references perfumestore.order_items(id) on delete restrict,
    presentation_id uuid not null references perfumestore.product_presentations(id) on delete restrict,
    quantity integer not null,
    received_quantity integer not null default 0,
    approved_quantity integer not null default 0,
    rejected_quantity integer not null default 0,
    reason varchar(255) not null,
    status varchar(35) not null,
    requested_by uuid not null references perfumestore.app_users(id) on delete restrict,
    received_by uuid references perfumestore.app_users(id) on delete restrict,
    inspected_by uuid references perfumestore.app_users(id) on delete restrict,
    requested_at timestamptz not null,
    received_at timestamptz,
    inspected_at timestamptz,
    condition_note varchar(255),
    inspection_reason varchar(255),
    idempotency_key varchar(100) not null,
    updated_at timestamptz not null,
    constraint returns_quantity_ck check (quantity > 0 and received_quantity >= 0 and approved_quantity >= 0 and rejected_quantity >= 0),
    constraint returns_received_ck check (received_quantity <= quantity and approved_quantity + rejected_quantity <= received_quantity),
    constraint returns_status_ck check (status in ('REQUESTED','IN_TRANSIT','AWAITING_INSPECTION','INSPECTED','RESTOCKED')),
    constraint returns_order_key_uq unique(order_id, idempotency_key)
);
create index returns_order_idx on perfumestore.returns(order_id, requested_at, id);
create index returns_item_idx on perfumestore.returns(order_item_id, status, id);

create table perfumestore.return_history (
    id uuid primary key,
    return_id uuid not null references perfumestore.returns(id) on delete restrict,
    from_status varchar(35),
    to_status varchar(35) not null,
    event varchar(60) not null,
    actor_user_id uuid references perfumestore.app_users(id) on delete restrict,
    reason varchar(255),
    created_at timestamptz not null,
    constraint return_history_status_ck check (to_status in ('REQUESTED','IN_TRANSIT','AWAITING_INSPECTION','INSPECTED','RESTOCKED'))
);
create index return_history_return_idx on perfumestore.return_history(return_id, created_at, id);

create table perfumestore.return_restock_operations (
    id uuid primary key,
    return_id uuid not null references perfumestore.returns(id) on delete restrict,
    presentation_id uuid not null references perfumestore.product_presentations(id) on delete restrict,
    quantity integer not null,
    operation_key varchar(100) not null unique,
    inventory_operation_key varchar(100) not null unique,
    actor_user_id uuid not null references perfumestore.app_users(id) on delete restrict,
    created_at timestamptz not null,
    constraint return_restock_quantity_ck check (quantity > 0)
);
create index return_restock_return_idx on perfumestore.return_restock_operations(return_id, created_at, id);
