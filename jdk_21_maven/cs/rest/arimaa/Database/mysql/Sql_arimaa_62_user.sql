-- =====================================================
-- Migration: Refactor Players → Users + Players
--
-- Phase 1: Add user_id column to existing Players table
-- Phase 2: Seed Users from Players, populate user_id
-- Phase 3: Enforce constraints, drop obsolete columns
-- =====================================================


-- ─────────────────────────────────────────────────────
-- PHASE 1: Add user_id FK column to existing Players
--          (nullable for now, so existing rows don't
--           break before we populate it in Phase 2)
-- ─────────────────────────────────────────────────────

ALTER TABLE `arimaadockermysqldb`.`Players`
  ADD COLUMN `user_id` BIGINT NULL AFTER `id`;


-- ─────────────────────────────────────────────────────
-- PHASE 2: Create Users table, seed it from Players,
--          then back-fill user_id on every Player row
-- ─────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS `arimaadockermysqldb`.`Users` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(50)   NOT NULL,
  `email`      VARCHAR(254)  NOT NULL,
  `password`   VARCHAR(100)  NOT NULL,
  `role`       VARCHAR(50)   NOT NULL,
  `created_at` DATETIME(6)   NOT NULL,
  `updated_at` DATETIME(6)   NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `username_UNIQUE` (`username`),
  UNIQUE INDEX `email_UNIQUE`    (`email`)
) ENGINE = InnoDB;

START TRANSACTION;

-- Seed Users from every existing Player row.
-- Notes:
--   • email: was nullable in Players, NOT NULL in Users → placeholder used if missing
--   • password: copied as-is; re-hash with bcrypt/argon2 before going to production
--   • created_at/updated_at: seeded from Players.create_time
--   • role: defaulted to 'PLAYER'; adjust if your UserRole enum uses a different value
INSERT INTO `arimaadockermysqldb`.`Users`
  (`username`, `email`, `password`, `role`, `created_at`, `updated_at`)
SELECT
  `username`,
  COALESCE(`email`, CONCAT(`username`, '@unknown.invalid')),
  `password`,
  'PLAYER',
  COALESCE(`create_time`, CURRENT_TIMESTAMP(6)),
  COALESCE(`create_time`, CURRENT_TIMESTAMP(6))
FROM `arimaadockermysqldb`.`Players`;

-- Back-fill user_id on each Player by matching username
UPDATE `arimaadockermysqldb`.`Players` p
  JOIN `arimaadockermysqldb`.`Users` u ON u.`username` = p.`username`
SET p.`user_id` = u.`id`;

COMMIT;


-- ─────────────────────────────────────────────────────
-- PHASE 3: Enforce NOT NULL + FK on user_id,
--          then drop the columns now owned by Users
-- ─────────────────────────────────────────────────────

-- Verify every Player has a user_id before locking it down.
-- Uncomment and run manually if you want a safety check first:
-- SELECT COUNT(*) AS missing_user_id
-- FROM `arimaadockermysqldb`.`Players`
-- WHERE `user_id` IS NULL;

ALTER TABLE `arimaadockermysqldb`.`Players`
  -- Lock down user_id
  MODIFY COLUMN `user_id` BIGINT NOT NULL,
  ADD UNIQUE  INDEX `user_id_UNIQUE` (`user_id`), -- this line ensures that the user is indexed so lookup is O(log(n))
  ADD CONSTRAINT `fk_Players_Users`
        FOREIGN KEY (`user_id`)
        REFERENCES `arimaadockermysqldb`.`Users` (`id`)
        ON DELETE CASCADE
        ON UPDATE NO ACTION,

  -- Drop indexes that guarded the now-migrated columns
  DROP INDEX `username_UNIQUE`,
  DROP INDEX `email_UNIQUE`,

  -- Drop the columns that now live in Users
  DROP COLUMN `username`,
  DROP COLUMN `email`,
  DROP COLUMN `password`,
  DROP COLUMN `create_time`;