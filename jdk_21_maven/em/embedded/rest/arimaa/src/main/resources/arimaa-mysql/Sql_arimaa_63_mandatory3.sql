-- This script is now redundant for fresh installations as the columns are already in Sql_arimaa_10_init.sql.
-- Using a stored procedure to safely add columns only if they don't exist.
DELIMITER //

CREATE PROCEDURE AddRatingsColumns()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = 'arimaadockermysqldb' 
        AND TABLE_NAME = 'Matches' 
        AND COLUMN_NAME = 'gold_rating'
    ) THEN
        ALTER TABLE `arimaadockermysqldb`.`Matches` 
        ADD COLUMN `gold_rating` INT NULL DEFAULT NULL AFTER `player_id_gold`;
    END IF;

    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = 'arimaadockermysqldb' 
        AND TABLE_NAME = 'Matches' 
        AND COLUMN_NAME = 'silver_rating'
    ) THEN
        ALTER TABLE `arimaadockermysqldb`.`Matches` 
        ADD COLUMN `silver_rating` INT NULL DEFAULT NULL AFTER `gold_rating`;
    END IF;
END //

DELIMITER ;

CALL AddRatingsColumns();
DROP PROCEDURE AddRatingsColumns;