USE blogapi;

DROP TABLE IF EXISTS `address`;
DROP TABLE IF EXISTS `albums`;
DROP TABLE IF EXISTS `categories`;
DROP TABLE IF EXISTS `comments`;
DROP TABLE IF EXISTS `company`;
DROP TABLE IF EXISTS `geo`;
DROP TABLE IF EXISTS `photos`;
DROP TABLE IF EXISTS `posts`;
DROP TABLE IF EXISTS `post_tag`;
DROP TABLE IF EXISTS `roles`;
DROP TABLE IF EXISTS `tags`;
DROP TABLE IF EXISTS `todos`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `user_role`;




CREATE TABLE `categories` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `created_at` datetime NOT NULL,
                              `updated_at` datetime NOT NULL,
                              `created_by` bigint DEFAULT NULL,
                              `updated_by` bigint DEFAULT NULL,
                              `name` varchar(255) DEFAULT NULL,
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `tags` (
                        `id` bigint NOT NULL AUTO_INCREMENT,
                        `created_at` datetime NOT NULL,
                        `updated_at` datetime NOT NULL,
                        `created_by` bigint DEFAULT NULL,
                        `updated_by` bigint DEFAULT NULL,
                        `name` varchar(255) DEFAULT NULL,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


CREATE TABLE `geo` (
                       `id` bigint NOT NULL AUTO_INCREMENT,
                       `created_at` datetime NOT NULL,
                       `updated_at` datetime NOT NULL,
                       `created_by` bigint DEFAULT NULL,
                       `updated_by` bigint DEFAULT NULL,
                       `lat` varchar(255) DEFAULT NULL,
                       `lng` varchar(255) DEFAULT NULL,
                       PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `company` (
                           `id` bigint NOT NULL AUTO_INCREMENT,
                           `created_at` datetime NOT NULL,
                           `updated_at` datetime NOT NULL,
                           `created_by` bigint DEFAULT NULL,
                           `updated_by` bigint DEFAULT NULL,
                           `bs` varchar(255) DEFAULT NULL,
                           `catch_phrase` varchar(255) DEFAULT NULL,
                           `name` varchar(255) DEFAULT NULL,
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `address` (
                           `id` bigint NOT NULL AUTO_INCREMENT,
                           `street` varchar(255),
                           `suite` varchar(255),
                           `city` varchar(255),
                           `zipcode` varchar(255),
                           `geo_id` bigint DEFAULT NULL,
                           `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `created_by` bigint(19) unsigned DEFAULT NULL,
                           `updated_by` bigint(19) unsigned DEFAULT NULL,
                           PRIMARY KEY (`id`),
                           KEY `fk_geo` (`geo_id`),
                           CONSTRAINT `fk_geo` FOREIGN KEY (`geo_id`) REFERENCES `geo` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `users` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `created_at` datetime NOT NULL,
                         `updated_at` datetime NOT NULL,
                         `email` varchar(40) DEFAULT NULL,
                         `first_name` varchar(40) DEFAULT NULL,
                         `last_name` varchar(40) DEFAULT NULL,
                         `password` varchar(100) DEFAULT NULL,
                         `phone` varchar(255) DEFAULT NULL,
                         `username` varchar(15) DEFAULT NULL,
                         `website` varchar(255) DEFAULT NULL,
                         `address_id` bigint DEFAULT NULL,
                         `company_id` bigint DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
                         UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
                         CONSTRAINT `fk_address` FOREIGN KEY (`address_id`) REFERENCES `address` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
                         CONSTRAINT `fk_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


CREATE TABLE `todos` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `created_at` datetime NOT NULL,
                         `updated_at` datetime NOT NULL,
                         `created_by` bigint DEFAULT NULL,
                         `updated_by` bigint DEFAULT NULL,
                         `completed` bit(1) DEFAULT NULL,
                         `title` varchar(255) DEFAULT NULL,
                         `user_id` bigint DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         KEY `fk_user_todos` (`user_id`),
                         CONSTRAINT `fk_user_todos` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `albums` (
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `title` varchar(255) NOT NULL,
                          `user_id` BIGINT DEFAULT NULL,
                          `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          `created_by` bigint(19) unsigned DEFAULT NULL,
                          `updated_by` bigint(19) unsigned DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          KEY `fk_user_album` (`user_id`),
                          CONSTRAINT `fk_user_album` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `photos` (
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `created_at` datetime NOT NULL,
                          `updated_at` datetime NOT NULL,
                          `created_by` bigint DEFAULT NULL,
                          `updated_by` bigint DEFAULT NULL,
                          `thumbnail_url` varchar(255) DEFAULT NULL,
                          `title` varchar(255) DEFAULT NULL,
                          `url` varchar(255) DEFAULT NULL,
                          `album_id` bigint DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          KEY `fk_album` (`album_id`),
                          CONSTRAINT `fk_album` FOREIGN KEY (`album_id`) REFERENCES `albums` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `posts` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `created_at` datetime NOT NULL,
                         `updated_at` datetime NOT NULL,
                         `created_by` bigint DEFAULT NULL,
                         `updated_by` bigint DEFAULT NULL,
                         `body` varchar(255) DEFAULT NULL,
                         `title` varchar(255) DEFAULT NULL,
                         `category_id` bigint DEFAULT NULL,
                         `user_id` bigint DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         KEY `fk_user_post` (`user_id`),
                         KEY `fk_category` (`category_id`),
                         CONSTRAINT `fk_user_post` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                         CONSTRAINT `fk_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


CREATE TABLE `post_tag` (
                            `tag_id` bigint NOT NULL,
                            `post_id` bigint NOT NULL,
                            CONSTRAINT `fk_posttag_post_id` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
                            CONSTRAINT `fk_posttag_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE  `comments` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `created_at` datetime NOT NULL,
                             `updated_at` datetime NOT NULL,
                             `created_by` bigint DEFAULT NULL,
                             `updated_by` bigint DEFAULT NULL,
                             `body` varchar(255) DEFAULT NULL,
                             `email` varchar(50) DEFAULT NULL,
                             `name` varchar(50) DEFAULT NULL,
                             `post_id` bigint DEFAULT NULL,
                             `user_id` bigint DEFAULT NULL,
                             PRIMARY KEY (`id`),
                             KEY `fk_comment_post` (`post_id`),
                             KEY `fk_comment_user` (`user_id`),
                             CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
                             CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `roles` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `name` varchar(255) DEFAULT NULL,
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


CREATE TABLE `user_role` (
                             `user_id` bigint NOT NULL,
                             `role_id` bigint NOT NULL,
                             KEY `fk_security_user_id` (`user_id`),
                             KEY `fk_security_role_id` (`role_id`),
                             CONSTRAINT `fk_security_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                             CONSTRAINT `fk_security_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

INSERT INTO `roles` VALUES (1,'ROLE_ADMIN'),(2,'ROLE_USER');

