-- ============================================================
-- DEMO TENANT
-- ============================================================

INSERT INTO tenant (
    id,
    name,
    created_at
)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Demo Tenant',
    CURRENT_TIMESTAMP
);


-- ============================================================
-- DEMO ADMIN USER
-- email: admin@test.com
-- password: password123
-- ============================================================

INSERT INTO app_user (
    id,
    tenant_id,
    email,
    password_hash,
    role,
    created_at
)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'admin@test.com',
    '$2a$10$bEtzdEFWwrv5bjrxGgglkuSv1wodDaV6D14X6t/k30nP.IcdC1hy2',
    'ADMIN',
    CURRENT_TIMESTAMP
);


-- ============================================================
-- DEMO WAREHOUSE
-- ============================================================

INSERT INTO warehouse (
    id,
    tenant_id,
    code,
    name,
    created_at
)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'WH-1',
    'Main Warehouse',
    CURRENT_TIMESTAMP
);


-- ============================================================
-- DEMO PRODUCT
-- ============================================================

INSERT INTO product (
    id,
    tenant_id,
    sku,
    name,
    description,
    created_at,
    updated_at
)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    '11111111-1111-1111-1111-111111111111',
    'SKU-FLASH-1',
    'Flash Sale Product',
    'Demo product for reservation testing',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- DEMO INVENTORY
-- ============================================================

INSERT INTO inventory (
    id,
    tenant_id,
    product_id,
    warehouse_id,
    on_hand,
    reserved,
    created_at,
    updated_at
)
VALUES (
    '55555555-5555-5555-5555-555555555555',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444444',
    '33333333-3333-3333-3333-333333333333',
    5,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);