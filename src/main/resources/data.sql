-- =============================================================================
-- Data Seed: Khởi tạo 5 Danh mục và 10 Sản phẩm mẫu
-- Sử dụng ON CONFLICT để đảm bảo tính Idempotent khi restart ứng dụng
-- =============================================================================

-- 1. Thêm 5 Danh mục (Categories)
INSERT INTO categories (code, name, is_deleted, created_at)
VALUES 
    ('PHONE', 'Điện thoại & Máy tính bảng', FALSE, CURRENT_TIMESTAMP),
    ('LAPTOP', 'Laptop & Máy tính để bàn', FALSE, CURRENT_TIMESTAMP),
    ('AUDIO', 'Thiết bị âm thanh', FALSE, CURRENT_TIMESTAMP),
    ('WATCH', 'Đồng hồ thông minh', FALSE, CURRENT_TIMESTAMP),
    ('ACCESSORY', 'Phụ kiện công nghệ', FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- 2. Thêm 10 Sản phẩm (Products)
INSERT INTO products (category_id, sku, name, price, status, is_deleted, created_at)
VALUES 
    ((SELECT id FROM categories WHERE code = 'PHONE'), 'IPHONE-15-PRO-MAX', 'iPhone 15 Pro Max 256GB', 34990000.00, 'ACTIVE', FALSE, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE code = 'PHONE'), 'SAMSUNG-S24-ULTRA', 'Samsung Galaxy S24 Ultra 512GB', 31990000.00, 'ACTIVE', FALSE, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE code = 'LAPTOP'), 'MACBOOK-PRO-M3', 'MacBook Pro 14 M3 Pro 18GB/512GB', 49990000.00, 'ACTIVE', FALSE, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE code = 'LAPTOP'), 'DELL-XPS-15', 'Dell XPS 15 9530 Core i7-13700H', 45500000.00, 'ACTIVE', FALSE, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE code = 'AUDIO'), 'SONY-WH-1000XM5', 'Tai nghe chụp tai Sony WH-1000XM5 Chống ồn', 8490000.00, 'ACTIVE', FALSE, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE code = 'AUDIO'), 'AIRPODS-PRO-2', 'Apple AirPods Pro Gen 2 USB-C', 5990000.00, 'ACTIVE', FALSE, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE code = 'WATCH'), 'APPLE-WATCH-ULTRA-2', 'Apple Watch Ultra 2 GPS + Cellular 49mm', 21490000.00, 'ACTIVE', FALSE, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE code = 'WATCH'), 'GARMIN-FENIX-7-PRO', 'Garmin Fenix 7 Pro Sapphire Solar', 23990000.00, 'ACTIVE', FALSE, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE code = 'ACCESSORY'), 'ANKER-737-GAN-140W', 'Củ sạc Anker 737 GaNPrime 140W 3 cổng', 1890000.00, 'ACTIVE', FALSE, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE code = 'ACCESSORY'), 'LOGITECH-MX-MASTER-3S', 'Chuột không dây Logitech MX Master 3S', 2490000.00, 'ACTIVE', FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (sku) DO NOTHING;
