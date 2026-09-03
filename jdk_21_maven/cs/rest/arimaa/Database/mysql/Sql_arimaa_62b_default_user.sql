USE `arimaadockermysqldb`;

INSERT IGNORE INTO `Users` (`username`, `email`, `password`, `role`, `created_at`, `updated_at`) 
VALUES
    ('admin1', 'admin1@example.com', '$2a$10$a7Dii8pcWQMclYxLt9Kb1eWpbRNbAPTsMRlJkm7ZT.wYIemq4oiBi', 'ADMIN', NOW(), NOW()),
    ('user1',  'user1@example.com', '$2a$10$DK1T8LJLPBcPLWhm7i/L1esnux0b7mV0HjMbB02CL794blj0M0lYG', 'USER', NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    `password` = VALUES(`password`), 
    `role` = VALUES(`role`), 
    `updated_at` = NOW();