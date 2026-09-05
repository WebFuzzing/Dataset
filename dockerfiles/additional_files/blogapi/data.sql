LOCK TABLES `users` WRITE;
INSERT INTO `users` (first_name, last_name, username, password, email, address_id, phone, website, company_id, created_at, updated_at) VALUES ('foo can', 'adminoglu', 'user', '$2a$10$iTXPjffYCIaUBD3iGVcv..AT6mB21IpcBe50.HlC1rbDAIsutL1Qi', 'user@bar.com', null, null, null, null, '2025-02-11 14:51:00', '2025-02-11 14:51:00');
INSERT INTO `users` (first_name, last_name, username, password, email, address_id, phone, website, company_id, created_at, updated_at) VALUES ('admin gul', 'adminoglu', 'admin', '$2a$10$YZA7FB23laZhRhrWMChNsOoveLITZmrY7Ca9NEwuo67vLbXt1u6ky', 'admin@bar.com', null, null, null, null, '2025-02-11 14:51:20', '2025-02-11 14:51:20');
UNLOCK TABLES;

LOCK TABLES `user_role` WRITE;
INSERT INTO `user_role` (user_id, role_id) VALUES (1, 2);
INSERT INTO `user_role` (user_id, role_id) VALUES (2, 2);
UNLOCK TABLES;
