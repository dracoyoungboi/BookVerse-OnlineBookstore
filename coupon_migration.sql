-- Migration script to add coupons table
-- Run this script to create the coupons table in your database

CREATE TABLE IF NOT EXISTS `coupons` (
  `coupon_id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `discount_type` varchar(20) NOT NULL,
  `discount_value` double NOT NULL,
  `min_purchase_amount` double NOT NULL DEFAULT '0',
  `max_discount_amount` double DEFAULT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `usage_limit` int DEFAULT NULL,
  `used_count` int NOT NULL DEFAULT '0',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`coupon_id`),
  UNIQUE KEY `UK_coupon_code` (`code`),
  KEY `idx_coupon_active` (`active`),
  KEY `idx_coupon_expiry` (`expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Insert sample coupon data
INSERT INTO `coupons` (`code`, `discount_type`, `discount_value`, `min_purchase_amount`, `max_discount_amount`, `expiry_date`, `usage_limit`, `used_count`, `active`, `created_at`, `description`) VALUES
('WELCOME10', 'PERCENTAGE', 10.0, 0.0, 20.0, '2025-12-31 23:59:59.000000', 100, 0, 1, NOW(), 'Welcome coupon: 10% off up to $20'),
('SAVE20', 'PERCENTAGE', 20.0, 50.0, 50.0, '2025-12-31 23:59:59.000000', 50, 0, 1, NOW(), 'Save 20% on orders over $50, maximum discount $50'),
('FIXED5', 'FIXED_AMOUNT', 5.0, 0.0, NULL, '2025-12-31 23:59:59.000000', NULL, 0, 1, NOW(), 'Fixed $5 off any order'),
('BIG50', 'FIXED_AMOUNT', 50.0, 100.0, NULL, '2025-12-31 23:59:59.000000', 10, 0, 1, NOW(), '$50 off orders over $100 (Limited to 10 uses)');

