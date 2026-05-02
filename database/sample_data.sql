-- =============================================================
--  AuctionSystem - Sample Data
--  Chạy SAU schema.sql:
--  mysql -u root -p auction_db < sample_data.sql
-- =============================================================
SET SQL_SAFE_UPDATES = 0;

USE auction_db;

-- Xóa dữ liệu cũ (theo thứ tự FK)
DELETE FROM bid_transactions;
DELETE FROM auctions;
DELETE FROM items;
DELETE FROM users;

-- =============================================================
--  USERS
--  password hash = SHA-256("password123")
--  = ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f
-- =============================================================
INSERT INTO users (user_id, username, password, email, role, is_banned) VALUES
-- Admin
('user-admin-001',
 'admin',
 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'admin@auction.com',
 'ADMIN',
 0),

-- Sellers
('user-seller-001',
 'seller_alice',
 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'alice@seller.com',
 'SELLER',
 0),

('user-seller-002',
 'seller_bob',
 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'bob@seller.com',
 'SELLER',
 0),

-- Bidders
('user-bidder-001',
 'bidder_charlie',
 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'charlie@bid.com',
 'BIDDER',
 0),

('user-bidder-002',
 'bidder_diana',
 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'diana@bid.com',
 'BIDDER',
 0),

('user-bidder-003',
 'bidder_eve',
 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'eve@bid.com',
 'BIDDER',
 0),

-- Bidder bị ban (để test isBanned)
('user-bidder-banned',
 'bidder_banned',
 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'banned@bid.com',
 'BIDDER',
 1);

-- =============================================================
--  ITEMS
-- =============================================================
INSERT INTO items (item_id, seller_id, type, name, description, starting_price) VALUES

-- Electronics
('item-elec-001',
 'user-seller-001',
 'ELECTRONICS',
 'iPhone 15 Pro Max 256GB',
 'Mới 100%, còn bảo hành 12 tháng. Màu Titan Đen.',
 25000000.00),

('item-elec-002',
 'user-seller-001',
 'ELECTRONICS',
 'MacBook Pro M3 14 inch',
 'Chip M3, RAM 16GB, SSD 512GB. Hộp đầy đủ phụ kiện.',
 45000000.00),

('item-elec-003',
 'user-seller-002',
 'ELECTRONICS',
 'Samsung OLED TV 55 inch',
 'Smart TV 4K, HDR10+, Dolby Atmos. Đã qua sử dụng nhẹ.',
 15000000.00),

-- Art Items
('item-art-001',
 'user-seller-002',
 'ART',
 'Tranh sơn dầu Hồ Gươm',
 'Tác phẩm của họa sĩ Nguyễn Văn A, năm 2020. Có chứng nhận.',
 5000000.00),

('item-art-002',
 'user-seller-001',
 'ART',
 'Tượng gốm cổ triều Nguyễn',
 'Đồ cổ thế kỷ 19, có giấy chứng nhận nguồn gốc.',
 80000000.00),

-- Vehicles
('item-veh-001',
 'user-seller-002',
 'VEHICLE',
 'Honda Wave RSX 2022',
 'Xe máy số, màu đỏ đen. ODO: 12.000km. Xe đẹp như mới.',
 28000000.00),

('item-veh-002',
 'user-seller-001',
 'VEHICLE',
 'Toyota Camry 2.5Q 2021',
 'Sedan cao cấp, màu trắng ngọc. ODO: 35.000km. Đủ đồ chơi.',
 900000000.00);

-- =============================================================
--  AUCTIONS
-- =============================================================
INSERT INTO auctions
    (auction_id, item_id, seller_id, starting_price, current_price,
     status, start_time, end_time, winner_id)
VALUES

-- Phiên đang RUNNING (sẽ kết thúc sau vài giờ)
('auction-run-001',
 'item-elec-001',
 'user-seller-001',
 25000000.00,
 27500000.00,
 'RUNNING',
 NOW() - INTERVAL 2 HOUR,
 NOW() + INTERVAL 3 HOUR,
 'user-bidder-001'),

-- Phiên RUNNING khác
('auction-run-002',
 'item-veh-002',
 'user-seller-001',
 900000000.00,
 920000000.00,
 'RUNNING',
 NOW() - INTERVAL 1 HOUR,
 NOW() + INTERVAL 5 HOUR,
 'user-bidder-002'),

-- Phiên OPEN (chưa bắt đầu)
('auction-open-001',
 'item-elec-002',
 'user-seller-001',
 45000000.00,
 45000000.00,
 'OPEN',
 NOW() + INTERVAL 1 HOUR,
 NOW() + INTERVAL 25 HOUR,
 NULL),

-- Phiên OPEN khác
('auction-open-002',
 'item-art-002',
 'user-seller-001',
 80000000.00,
 80000000.00,
 'OPEN',
 NOW() + INTERVAL 2 HOUR,
 NOW() + INTERVAL 48 HOUR,
 NULL),

-- Phiên đã FINISHED
('auction-fin-001',
 'item-art-001',
 'user-seller-002',
 5000000.00,
 7800000.00,
 'FINISHED',
 NOW() - INTERVAL 3 DAY,
 NOW() - INTERVAL 1 DAY,
 'user-bidder-003'),

-- Phiên đã PAID
('auction-paid-001',
 'item-elec-003',
 'user-seller-002',
 15000000.00,
 18500000.00,
 'PAID',
 NOW() - INTERVAL 5 DAY,
 NOW() - INTERVAL 3 DAY,
 'user-bidder-001'),

-- Phiên CANCELED
('auction-cancel-001',
 'item-veh-001',
 'user-seller-002',
 28000000.00,
 28000000.00,
 'CANCELED',
 NOW() - INTERVAL 2 DAY,
 NOW() - INTERVAL 1 DAY,
 NULL);

-- =============================================================
--  BID_TRANSACTIONS
-- =============================================================
INSERT INTO bid_transactions
    (transaction_id, auction_id, bidder_id, bid_amount, bid_time, is_auto_bid)
VALUES

-- Lịch sử bid cho auction-run-001 (iPhone)
('tx-001', 'auction-run-001', 'user-bidder-002', 26000000.00,
 NOW() - INTERVAL 100 MINUTE, 0),
('tx-002', 'auction-run-001', 'user-bidder-001', 26500000.00,
 NOW() - INTERVAL 90 MINUTE,  0),
('tx-003', 'auction-run-001', 'user-bidder-002', 27000000.00,
 NOW() - INTERVAL 60 MINUTE,  1),   -- auto-bid
('tx-004', 'auction-run-001', 'user-bidder-001', 27500000.00,
 NOW() - INTERVAL 30 MINUTE,  0),

-- Lịch sử bid cho auction-run-002 (Camry)
('tx-005', 'auction-run-002', 'user-bidder-003', 905000000.00,
 NOW() - INTERVAL 50 MINUTE,  0),
('tx-006', 'auction-run-002', 'user-bidder-002', 920000000.00,
 NOW() - INTERVAL 20 MINUTE,  0),

-- Lịch sử bid cho auction-fin-001 (tranh)
('tx-007', 'auction-fin-001', 'user-bidder-001', 5500000.00,
 NOW() - INTERVAL 3 DAY + INTERVAL 1 HOUR,  0),
('tx-008', 'auction-fin-001', 'user-bidder-003', 6000000.00,
 NOW() - INTERVAL 3 DAY + INTERVAL 3 HOUR,  0),
('tx-009', 'auction-fin-001', 'user-bidder-001', 7000000.00,
 NOW() - INTERVAL 2 DAY - INTERVAL 6 HOUR,  0),
('tx-010', 'auction-fin-001', 'user-bidder-003', 7800000.00,
 NOW() - INTERVAL 2 DAY - INTERVAL 2 HOUR,  0),

-- Lịch sử bid cho auction-paid-001 (TV)
('tx-011', 'auction-paid-001', 'user-bidder-001', 16000000.00,
 NOW() - INTERVAL 5 DAY + INTERVAL 2 HOUR,  0),
('tx-012', 'auction-paid-001', 'user-bidder-002', 17000000.00,
 NOW() - INTERVAL 4 DAY - INTERVAL 18 HOUR, 0),
('tx-013', 'auction-paid-001', 'user-bidder-001', 18500000.00,
 NOW() - INTERVAL 4 DAY - INTERVAL 6 HOUR,  1);  -- auto-bid

-- =============================================================
--  VERIFY (optional - uncomment để kiểm tra)
-- =============================================================
-- SELECT 'users'             AS tbl, COUNT(*) AS cnt FROM users
-- UNION ALL
-- SELECT 'items',              COUNT(*) FROM items
-- UNION ALL
-- SELECT 'auctions',           COUNT(*) FROM auctions
-- UNION ALL
-- SELECT 'bid_transactions',   COUNT(*) FROM bid_transactions;
