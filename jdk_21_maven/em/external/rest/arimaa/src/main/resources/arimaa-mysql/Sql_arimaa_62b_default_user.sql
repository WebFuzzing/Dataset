USE `arimaadockermysqldb`;

INSERT IGNORE INTO `Users` (`username`, `email`, `password`, `role`, `created_at`, `updated_at`)
VALUES
    ('admin1', 'admin1@example.com', '$2a$10$a7Dii8pcWQMclYxLt9Kb1eWpbRNbAPTsMRlJkm7ZT.wYIemq4oiBi', 'ADMIN', NOW(), NOW()),
    ('user1',  'user1@example.com', '$2a$10$DK1T8LJLPBcPLWhm7i/L1esnux0b7mV0HjMbB02CL794blj0M0lYG', 'USER', NOW(), NOW()),
    -- MODIFIED: admin1's password is unknown, and self-registration can never grant ADMIN, so a
    -- second known-credential ADMIN account is needed to test broken access control between admins.
    ('wfd_admin1', 'wfd_admin1@example.com', '$2a$10$3mawxiylgOPZOvvJO24bxeuUcQyQf.n53SzzAX9fBNiT7bfzZn4WO', 'ADMIN', NOW(), NOW()),
    ('wfd_admin2', 'wfd_admin2@example.com', '$2a$10$8h.Zwu6RHMf0VZsEIZ2rcewbF4CU/nLpzoSSPjE1M1D.nof/9ZQVy', 'ADMIN', NOW(), NOW())
    -- MODIFIED
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `role` = VALUES(`role`),
    `updated_at` = NOW();