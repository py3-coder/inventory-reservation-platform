-- ============================================================
-- TENANT
-- ============================================================

CREATE TABLE tenant (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);


-- ============================================================
-- USER
-- ============================================================

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_user_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenant(id),

    CONSTRAINT uk_user_tenant_email
        UNIQUE (tenant_id, email)
);


-- ============================================================
-- PRODUCT
-- ============================================================

CREATE TABLE product (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    sku VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_product_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenant(id),

    CONSTRAINT uk_product_tenant_sku
        UNIQUE (tenant_id, sku)
);


-- ============================================================
-- WAREHOUSE
-- ============================================================

CREATE TABLE warehouse (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_warehouse_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenant(id),

    CONSTRAINT uk_warehouse_tenant_code
        UNIQUE (tenant_id, code)
);


-- ============================================================
-- INVENTORY
-- ============================================================

CREATE TABLE inventory (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    on_hand INTEGER NOT NULL,
    reserved INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_inventory_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenant(id),

    CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id)
        REFERENCES product(id),

    CONSTRAINT fk_inventory_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES warehouse(id),

    CONSTRAINT uk_inventory_product_warehouse
        UNIQUE (tenant_id, product_id, warehouse_id),

    CONSTRAINT chk_inventory_on_hand
        CHECK (on_hand >= 0),

    CONSTRAINT chk_inventory_reserved
        CHECK (reserved >= 0),

    CONSTRAINT chk_inventory_reserved_not_exceed_on_hand
        CHECK (reserved <= on_hand)
);


-- ============================================================
-- RESERVATION
-- ============================================================

CREATE TABLE reservation (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_reservation_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenant(id),

    CONSTRAINT fk_reservation_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id)
);


-- ============================================================
-- RESERVATION ITEM
-- ============================================================

CREATE TABLE reservation_item (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,

    CONSTRAINT fk_reservation_item_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservation(id),

    CONSTRAINT fk_reservation_item_product
        FOREIGN KEY (product_id)
        REFERENCES product(id),

    CONSTRAINT chk_reservation_item_quantity
        CHECK (quantity > 0)
);


-- ============================================================
-- RESERVATION ALLOCATION
-- ============================================================

CREATE TABLE reservation_allocation (
    id UUID PRIMARY KEY,
    reservation_item_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    quantity INTEGER NOT NULL,

    CONSTRAINT fk_allocation_reservation_item
        FOREIGN KEY (reservation_item_id)
        REFERENCES reservation_item(id),

    CONSTRAINT fk_allocation_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES warehouse(id),

    CONSTRAINT chk_allocation_quantity
        CHECK (quantity > 0)
);


-- ============================================================
-- ORDER
-- ============================================================

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    reservation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_order_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenant(id),

    CONSTRAINT fk_order_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservation(id),

    CONSTRAINT fk_order_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id),

    CONSTRAINT uk_order_reservation
        UNIQUE (reservation_id)
);


-- ============================================================
-- PAYMENT
-- ============================================================

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    provider_reference VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_payment_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenant(id),

    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id),

    CONSTRAINT uk_payment_order
        UNIQUE (order_id)
);


-- ============================================================
-- IDEMPOTENCY RECORD
-- ============================================================
-- ============================================================
-- IDEMPOTENCY RECORD
-- ============================================================

CREATE TABLE idempotency_record (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(255) NOT NULL,
    reservation_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_idempotency_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenant(id),

    CONSTRAINT fk_idempotency_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id),

    CONSTRAINT fk_idempotency_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservation(id),

    CONSTRAINT uk_idempotency_tenant_user_key
        UNIQUE (tenant_id, user_id, idempotency_key)
);


-- ============================================================
-- DOMAIN EVENT
-- ============================================================

CREATE TABLE domain_event (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_domain_event_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenant(id)
);


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_inventory_tenant_product
    ON inventory (tenant_id, product_id);

CREATE INDEX idx_reservation_expiry
    ON reservation (tenant_id, status, expires_at);

CREATE INDEX idx_domain_event_tenant_created
    ON domain_event (tenant_id, created_at);

CREATE INDEX idx_domain_event_tenant_type
    ON domain_event (tenant_id, event_type);