-- Simulated dump from a local mall_product MySQL database.
-- This file is intentionally idempotent so the learning migration can be repeated.

CREATE DATABASE IF NOT EXISTS `mall_product`;
USE `mall_product`;

CREATE TABLE IF NOT EXISTS `products` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `stock` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `products` (`id`, `name`, `description`, `price`, `stock`)
VALUES (10001, 'Migrated AWS Demo Product',
        'Simulated local MySQL record migrated into EKS', 149.99, 25)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `description` = VALUES(`description`),
  `price` = VALUES(`price`),
  `stock` = VALUES(`stock`);
