-- Support objects for routines/triggers/events
-- Execution order: after Sql_arimaa_init.sql

USE `arimaadockermysqldb`;

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `table_name` VARCHAR(50),
  `operation` VARCHAR(10),
  `record_id` INT,
  `old_value` JSON,
  `new_value` JSON,
  `changed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `match_summary` (
  `gametype` VARCHAR(45) NOT NULL,
  `total_matches` INT NOT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`gametype`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `daily_match_stats` (
  `stat_date` DATE NOT NULL,
  `total_matches` INT NOT NULL,
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Matches_Archive` LIKE `Matches`;
