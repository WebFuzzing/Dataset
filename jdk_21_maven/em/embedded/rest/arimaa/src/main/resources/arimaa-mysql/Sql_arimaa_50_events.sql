-- Events
-- Requires: Sql_arimaa_init.sql, Sql_arimaa_20_support.sql, Sql_arimaa_30_procedures.sql

USE `arimaadockermysqldb`;

DELIMITER $$

-- Requires event scheduler enabled.
SET GLOBAL event_scheduler = ON$$

DROP EVENT IF EXISTS `ev_cleanup_audit_log`$$
CREATE EVENT `ev_cleanup_audit_log`
ON SCHEDULE EVERY 30 DAY
DO
DELETE FROM audit_log
WHERE changed_at < DATE_SUB(NOW(), INTERVAL 1 YEAR)$$

DROP EVENT IF EXISTS `ev_recalculate_games`$$
CREATE EVENT `ev_recalculate_games`
ON SCHEDULE EVERY 1 DAY
DO
CALL recalc_games_played()$$

DROP EVENT IF EXISTS `ev_daily_match_stats`$$
CREATE EVENT `ev_daily_match_stats`
ON SCHEDULE EVERY 1 DAY
DO
INSERT INTO daily_match_stats (stat_date, total_matches)
SELECT CURDATE(), COUNT(*)
FROM Matches
WHERE DATE(`timestamp`) = CURDATE()
ON DUPLICATE KEY UPDATE total_matches = VALUES(total_matches)$$

DELIMITER ;

-- Note: Event based on Events.end_date is omitted because the column does not exist in Sql_arimaa_init.sql.
