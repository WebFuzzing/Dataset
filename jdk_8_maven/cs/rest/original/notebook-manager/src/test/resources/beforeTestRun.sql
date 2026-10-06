CREATE TABLE IF NOT EXISTS `note_book` (
 `id` int(11) NOT NULL AUTO_INCREMENT,
 `current_price` double DEFAULT NULL,
 `last_update` datetime DEFAULT NULL,
 `name` varchar(255) DEFAULT NULL,
 PRIMARY KEY (`id`),
 UNIQUE KEY `UK_name` (`name`)
);

INSERT INTO `note_book` (`id`, `current_price`, `last_update`, `name`) VALUES
(1, 211.9, '2022-02-19 14:33:09', 'Asus Vivo Book S'),
(2, 299.9, '2022-02-19 14:33:11', 'HP Inspiron'),
(3, 300, '2022-02-19 14:33:11', 'Dell Magic'),
(4, 709.9, '2022-02-19 14:33:12', 'Apple MacBook'),
(5, 299.9, '2022-02-19 14:33:12', 'LG D230'),
(6, 199.9, '2022-02-19 14:33:12', 'Acer P700'),
(7, 199.9, '2022-02-19 14:33:12', 'Dell B3');