-- =============================================================================
-- Schema DDL: Định nghĩa cấu trúc bảng cho Categories và Products
-- Sử dụng cú pháp tương thích PostgreSQL và bộ parser DDLDatabase của jOOQ
-- =============================================================================

-- 1. Bảng Categories
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Index tìm kiếm Category theo code
CREATE INDEX IF NOT EXISTS idx_categories_code ON categories(code);

-- 2. Bảng Products
CREATE TABLE IF NOT EXISTS products (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    sku         VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    price       NUMERIC(15, 2) NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
    );

-- Index tối ưu truy vấn join và tìm kiếm theo SKU, Category ID
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
