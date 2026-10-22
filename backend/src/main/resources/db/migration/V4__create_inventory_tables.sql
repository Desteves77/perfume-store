create table perfumestore.inventory_balances (
    presentation_id uuid primary key,
    physical_quantity integer not null default 0,
    reserved_quantity integer not null default 0,
    version bigint not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_inventory_balance_presentation foreign key (presentation_id)
        references perfumestore.product_presentations (id) on delete restrict,
    constraint ck_inventory_physical_non_negative check (physical_quantity >= 0),
    constraint ck_inventory_reserved_non_negative check (reserved_quantity >= 0),
    constraint ck_inventory_reserved_not_above_physical check (reserved_quantity <= physical_quantity)
);

create table perfumestore.stock_reservations (
    id uuid primary key,
    reservation_key varchar(100) not null,
    presentation_id uuid not null,
    quantity integer not null,
    state varchar(20) not null,
    order_id uuid,
    expires_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint uq_stock_reservations_key unique (reservation_key),
    constraint fk_stock_reservation_presentation foreign key (presentation_id)
        references perfumestore.product_presentations (id) on delete restrict,
    constraint ck_stock_reservation_quantity_positive check (quantity > 0),
    constraint ck_stock_reservation_state check (state in ('ACTIVE', 'CONFIRMED', 'RELEASED'))
);

create index ix_stock_reservations_presentation_state
    on perfumestore.stock_reservations (presentation_id, state, id);
create index ix_stock_reservations_order on perfumestore.stock_reservations (order_id);

create table perfumestore.inventory_movements (
    id uuid primary key,
    presentation_id uuid not null,
    movement_type varchar(20) not null,
    physical_delta integer not null,
    reserved_delta integer not null,
    physical_before integer not null,
    physical_after integer not null,
    reserved_before integer not null,
    reserved_after integer not null,
    reason varchar(255) not null,
    operation_key varchar(100) not null,
    reservation_id uuid,
    actor_user_id uuid,
    correlation_id varchar(64),
    created_at timestamp with time zone not null,
    constraint uq_inventory_movement_operation_key unique (operation_key),
    constraint fk_inventory_movement_presentation foreign key (presentation_id)
        references perfumestore.product_presentations (id) on delete restrict,
    constraint fk_inventory_movement_reservation foreign key (reservation_id)
        references perfumestore.stock_reservations (id) on delete restrict,
    constraint fk_inventory_movement_actor foreign key (actor_user_id)
        references perfumestore.app_users (id) on delete restrict,
    constraint ck_inventory_movement_type check (movement_type in
        ('ENTRY', 'ADJUSTMENT', 'RESERVATION', 'CONFIRMATION', 'RELEASE')),
    constraint ck_inventory_movement_deltas check (physical_delta <> 0 or reserved_delta <> 0),
    constraint ck_inventory_movement_balances check (
        physical_before >= 0 and physical_after >= 0 and
        reserved_before >= 0 and reserved_after >= 0 and
        reserved_before <= physical_before and reserved_after <= physical_after
    )
);

create index ix_inventory_movements_presentation_created
    on perfumestore.inventory_movements (presentation_id, created_at desc, id desc);
create index ix_inventory_movements_created on perfumestore.inventory_movements (created_at desc, id desc);
