create table perfumestore.categories (
    id uuid primary key,
    slug varchar(120) not null,
    name varchar(120) not null,
    active boolean not null default true,
    version bigint not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_categories_slug unique (slug),
    constraint ck_categories_slug check (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    constraint ck_categories_name check (length(trim(name)) between 2 and 120)
);

create index ix_categories_active_name on perfumestore.categories (active, name, id);

create table perfumestore.products (
    id uuid primary key,
    slug varchar(180) not null,
    name varchar(180) not null,
    brand varchar(120) not null,
    description text not null,
    category_id uuid not null,
    active boolean not null default false,
    version bigint not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_products_slug unique (slug),
    constraint fk_products_category foreign key (category_id)
        references perfumestore.categories (id) on delete restrict,
    constraint ck_products_slug check (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    constraint ck_products_name check (length(trim(name)) between 2 and 180),
    constraint ck_products_brand check (length(trim(brand)) between 2 and 120),
    constraint ck_products_description check (length(trim(description)) between 1 and 5000)
);

create index ix_products_category_active on perfumestore.products (category_id, active, name, id);
create index ix_products_active_name on perfumestore.products (active, name, id);

create table perfumestore.product_presentations (
    id uuid primary key,
    product_id uuid not null,
    sku varchar(80) not null,
    sku_normalized varchar(80) not null,
    volume numeric(8,2) not null,
    unit varchar(12) not null,
    price numeric(12,2) not null,
    currency char(3) not null default 'BRL',
    active boolean not null default true,
    version bigint not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_presentations_product foreign key (product_id)
        references perfumestore.products (id) on delete restrict,
    constraint uq_presentations_sku unique (sku_normalized),
    constraint ck_presentations_volume check (volume > 0),
    constraint ck_presentations_unit check (unit in ('ML')),
    constraint ck_presentations_price check (price > 0),
    constraint ck_presentations_currency check (currency = 'BRL')
);

create index ix_presentations_product_active on perfumestore.product_presentations (product_id, active, volume, id);

create table perfumestore.product_images (
    id uuid primary key,
    product_id uuid not null,
    storage_key varchar(255) not null,
    content_type varchar(50) not null,
    byte_size bigint not null,
    width integer not null,
    height integer not null,
    alt_text varchar(180) not null,
    position integer not null,
    is_primary boolean not null default false,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_images_product foreign key (product_id)
        references perfumestore.products (id) on delete restrict,
    constraint uq_images_storage_key unique (storage_key),
    constraint ck_images_byte_size check (byte_size > 0),
    constraint ck_images_dimensions check (width > 0 and height > 0),
    constraint ck_images_position check (position >= 0),
    constraint ck_images_alt_text check (length(trim(alt_text)) between 2 and 180)
);

create index ix_images_product_visible on perfumestore.product_images (product_id, deleted_at, position, id);
create unique index uq_images_active_primary on perfumestore.product_images (product_id)
    where deleted_at is null and is_primary;
