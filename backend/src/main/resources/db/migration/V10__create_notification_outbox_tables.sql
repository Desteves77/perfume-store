create table perfumestore.notifications (
    id uuid primary key,
    event_type varchar(40) not null,
    event_key varchar(180) not null unique,
    order_id uuid references perfumestore.orders(id) on delete cascade,
    user_id uuid references perfumestore.app_users(id) on delete cascade,
    recipient_email varchar(320) not null,
    template_name varchar(80) not null,
    template_version varchar(20) not null,
    subject varchar(200) not null,
    body_text text not null,
    body_html text not null,
    status varchar(30) not null,
    attempt_count integer not null default 0,
    next_attempt_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    last_attempt_at timestamptz,
    sent_at timestamptz,
    locked_until timestamptz,
    last_error_code varchar(100),
    constraint notifications_event_type_ck check (event_type in ('ORDER_RECEIVED','PAYMENT_APPROVED','ORDER_SHIPPED')),
    constraint notifications_status_ck check (status in ('PENDING','PROCESSING','SENT','RETRY_PENDING','FAILED')),
    constraint notifications_attempt_ck check (attempt_count >= 0),
    constraint notifications_recipient_ck check (length(trim(recipient_email)) between 3 and 320),
    constraint notifications_template_ck check (length(trim(template_name)) between 3 and 80),
    constraint notifications_subject_ck check (length(trim(subject)) between 1 and 200),
    constraint notifications_body_ck check (length(body_text) > 0 and length(body_html) > 0)
);

create index notifications_due_idx on perfumestore.notifications(status, next_attempt_at, created_at, id);
create index notifications_order_idx on perfumestore.notifications(order_id, created_at, id);
create index notifications_user_idx on perfumestore.notifications(user_id, created_at desc, id);

create table perfumestore.notification_attempts (
    id uuid primary key,
    notification_id uuid not null references perfumestore.notifications(id) on delete cascade,
    attempt_number integer not null,
    started_at timestamptz not null,
    finished_at timestamptz,
    successful boolean,
    error_code varchar(100),
    constraint notification_attempts_number_ck check (attempt_number > 0),
    constraint notification_attempts_result_ck check (successful is null or successful = true or error_code is not null),
    constraint notification_attempts_uq unique(notification_id, attempt_number)
);

create index notification_attempts_notification_idx on perfumestore.notification_attempts(notification_id, attempt_number);
