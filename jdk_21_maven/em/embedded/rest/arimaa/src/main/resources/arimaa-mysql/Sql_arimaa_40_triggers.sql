-- Triggers
-- Requires: Sql_arimaa_init.sql, Sql_arimaa_20_support.sql

USE `arimaadockermysqldb`;

DELIMITER $$

DROP TRIGGER IF EXISTS `trg_players_update_audit`$$
CREATE TRIGGER `trg_players_update_audit`
AFTER UPDATE ON Players
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (
        table_name,
        operation,
        record_id,
        old_value,
        new_value
    )
    VALUES (
        'Players',
        'UPDATE',
        OLD.id,
        JSON_OBJECT(
            'rating', OLD.rating,
            'country', OLD.countries_id
        ),
        JSON_OBJECT(
            'rating', NEW.rating,
            'country', NEW.countries_id
        )
    );
END$$

DROP TRIGGER IF EXISTS `trg_match_insert_update_games`$$
DROP TRIGGER IF EXISTS `trg_prevent_self_play`$$
DROP TRIGGER IF EXISTS `trg_set_match_time`$$

DROP TRIGGER IF EXISTS `trg_matches_before_insert`$$
CREATE TRIGGER `trg_matches_before_insert`
BEFORE INSERT ON Matches
FOR EACH ROW
BEGIN
    IF NEW.player_id_silver = NEW.player_id_gold THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'A player cannot play against themselves';
    END IF;

    IF NEW.`timestamp` IS NULL THEN
        SET NEW.`timestamp` = CURRENT_TIMESTAMP;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_matches_before_update`$$
CREATE TRIGGER `trg_matches_before_update`
BEFORE UPDATE ON Matches
FOR EACH ROW
BEGIN
    IF NEW.player_id_silver = NEW.player_id_gold THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'A player cannot play against themselves';
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_matches_after_insert`$$
CREATE TRIGGER `trg_matches_after_insert`
AFTER INSERT ON Matches
FOR EACH ROW
BEGIN
    DECLARE v_gametype VARCHAR(45);

    UPDATE Players
    SET games_played = IFNULL(games_played, 0) + 1
    WHERE id IN (NEW.player_id_silver, NEW.player_id_gold);

    SELECT name INTO v_gametype
    FROM GameTypes
    WHERE id = NEW.gameTypes_id;

    IF v_gametype IS NOT NULL THEN
        INSERT INTO match_summary (gametype, total_matches)
        VALUES (v_gametype, 1)
        ON DUPLICATE KEY UPDATE total_matches = total_matches + 1;
    END IF;

    INSERT INTO audit_log (
        table_name,
        operation,
        record_id,
        old_value,
        new_value
    )
    VALUES (
        'Matches',
        'INSERT',
        NEW.id,
        NULL,
        JSON_OBJECT(
            'player_id_silver', NEW.player_id_silver,
            'player_id_gold', NEW.player_id_gold,
            'match_result', NEW.match_result,
            'events_id', NEW.events_id,
            'gameTypes_id', NEW.gameTypes_id,
            'timestamp', NEW.`timestamp`
        )
    );
END$$

DROP TRIGGER IF EXISTS `trg_matches_after_update`$$
CREATE TRIGGER `trg_matches_after_update`
AFTER UPDATE ON Matches
FOR EACH ROW
BEGIN
    DECLARE v_old_gametype VARCHAR(45);
    DECLARE v_new_gametype VARCHAR(45);

    IF OLD.player_id_silver <> NEW.player_id_silver THEN
        UPDATE Players
        SET games_played = GREATEST(IFNULL(games_played, 0) - 1, 0)
        WHERE id = OLD.player_id_silver;

        UPDATE Players
        SET games_played = IFNULL(games_played, 0) + 1
        WHERE id = NEW.player_id_silver;
    END IF;

    IF OLD.player_id_gold <> NEW.player_id_gold THEN
        UPDATE Players
        SET games_played = GREATEST(IFNULL(games_played, 0) - 1, 0)
        WHERE id = OLD.player_id_gold;

        UPDATE Players
        SET games_played = IFNULL(games_played, 0) + 1
        WHERE id = NEW.player_id_gold;
    END IF;

    IF OLD.gameTypes_id <> NEW.gameTypes_id THEN
        SELECT name INTO v_old_gametype
        FROM GameTypes
        WHERE id = OLD.gameTypes_id;

        SELECT name INTO v_new_gametype
        FROM GameTypes
        WHERE id = NEW.gameTypes_id;

        IF v_old_gametype IS NOT NULL THEN
            UPDATE match_summary
            SET total_matches = GREATEST(total_matches - 1, 0)
            WHERE gametype = v_old_gametype;

            DELETE FROM match_summary
            WHERE gametype = v_old_gametype
              AND total_matches <= 0;
        END IF;

        IF v_new_gametype IS NOT NULL THEN
            INSERT INTO match_summary (gametype, total_matches)
            VALUES (v_new_gametype, 1)
            ON DUPLICATE KEY UPDATE total_matches = total_matches + 1;
        END IF;
    END IF;

    INSERT INTO audit_log (
        table_name,
        operation,
        record_id,
        old_value,
        new_value
    )
    VALUES (
        'Matches',
        'UPDATE',
        NEW.id,
        JSON_OBJECT(
            'player_id_silver', OLD.player_id_silver,
            'player_id_gold', OLD.player_id_gold,
            'match_result', OLD.match_result,
            'events_id', OLD.events_id,
            'gameTypes_id', OLD.gameTypes_id,
            'timestamp', OLD.`timestamp`
        ),
        JSON_OBJECT(
            'player_id_silver', NEW.player_id_silver,
            'player_id_gold', NEW.player_id_gold,
            'match_result', NEW.match_result,
            'events_id', NEW.events_id,
            'gameTypes_id', NEW.gameTypes_id,
            'timestamp', NEW.`timestamp`
        )
    );
END$$

DROP TRIGGER IF EXISTS `trg_matches_after_delete`$$
CREATE TRIGGER `trg_matches_after_delete`
AFTER DELETE ON Matches
FOR EACH ROW
BEGIN
    DECLARE v_gametype VARCHAR(45);

    UPDATE Players
    SET games_played = GREATEST(IFNULL(games_played, 0) - 1, 0)
    WHERE id IN (OLD.player_id_silver, OLD.player_id_gold);

    SELECT name INTO v_gametype
    FROM GameTypes
    WHERE id = OLD.gameTypes_id;

    IF v_gametype IS NOT NULL THEN
        UPDATE match_summary
        SET total_matches = GREATEST(total_matches - 1, 0)
        WHERE gametype = v_gametype;

        DELETE FROM match_summary
        WHERE gametype = v_gametype
          AND total_matches <= 0;
    END IF;

    INSERT INTO audit_log (
        table_name,
        operation,
        record_id,
        old_value,
        new_value
    )
    VALUES (
        'Matches',
        'DELETE',
        OLD.id,
        JSON_OBJECT(
            'player_id_silver', OLD.player_id_silver,
            'player_id_gold', OLD.player_id_gold,
            'match_result', OLD.match_result,
            'events_id', OLD.events_id,
            'gameTypes_id', OLD.gameTypes_id,
            'timestamp', OLD.`timestamp`
        ),
        NULL
    );
END$$

DROP TRIGGER IF EXISTS `trg_moves_before_insert_validate`$$
CREATE TRIGGER `trg_moves_before_insert_validate`
BEFORE INSERT ON Moves
FOR EACH ROW
BEGIN
    IF NEW.direction NOT IN ("n", "s", "e", "w", "x") THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid move direction. Use N, S, E, or W.';
    END IF;

    IF NEW.status NOT IN ('0', '1') THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid move status. Use 0 or 1.';
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_moves_before_update_validate`$$
CREATE TRIGGER `trg_moves_before_update_validate`
BEFORE UPDATE ON Moves
FOR EACH ROW
BEGIN
    IF NEW.direction NOT IN ("n", "s", "e", "w", "x") THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid move direction. Use N, S, E, or W.';
    END IF;

    IF NEW.status NOT IN ('0', '1') THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid move status. Use 0 or 1.';
    END IF;
END$$

DELIMITER ;
