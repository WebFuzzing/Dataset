USE `arimaadockermysqldb`;

-- Views for Arimaa ERDB

DROP VIEW IF EXISTS `view_total_matches`;
CREATE VIEW `view_total_matches` AS
SELECT COUNT(*) AS total_matches
FROM Matches;

DROP VIEW IF EXISTS `view_total_players`;
CREATE VIEW `view_total_players` AS
SELECT COUNT(*) AS total_players
FROM Players;

DROP VIEW IF EXISTS `view_players_with_countries`;
CREATE VIEW `view_players_with_countries` AS
SELECT
    p.id AS player_id,
    p.username,
    p.email,
    p.rating,
    p.RU,
    p.games_played,
    p.create_time,
    c.id AS country_id,
    c.name AS country_name
FROM Players p
         LEFT JOIN Countries c
                   ON p.countries_id = c.id;

DROP VIEW IF EXISTS `view_player_count_by_country`;
CREATE VIEW `view_player_count_by_country` AS
SELECT
    c.id AS country_id,
    c.name AS country,
    COUNT(p.id) AS player_count
FROM Countries c
         LEFT JOIN Players p
                   ON c.id = p.countries_id
GROUP BY c.id, c.name;

DROP VIEW IF EXISTS `view_matches_with_players`;
CREATE VIEW `view_matches_with_players` AS
SELECT
    m.id AS match_id,
    m.termination_type,
    m.match_result,
    m.timestamp,
    silver.id AS silver_player_id,
    silver.username AS silver_player_username,
    gold.id AS gold_player_id,
    gold.username AS gold_player_username,
    gt.id AS gametype_id,
    gt.name AS gametype_name,
    gt.time_increment,
    gt.time_reserve,
    e.id AS event_id,
    e.name AS event_name,
    e.isRated
FROM Matches m
         JOIN Players silver
              ON m.player_id_silver = silver.id
         JOIN Players gold
              ON m.player_id_gold = gold.id
         LEFT JOIN GameTypes gt
                   ON m.gameTypes_id = gt.id
         LEFT JOIN Events e
                   ON m.events_id = e.id;

DROP VIEW IF EXISTS `view_matches_without_moves`;
CREATE VIEW `view_matches_without_moves` AS
SELECT
    m.id AS match_id,
    m.termination_type,
    m.match_result,
    m.timestamp,
    m.player_id_silver,
    m.player_id_gold,
    m.events_id,
    m.gameTypes_id
FROM Matches m
         LEFT JOIN Moves mv
                   ON m.id = mv.matches_id
WHERE mv.id IS NULL;

DROP VIEW IF EXISTS `view_event_participation`;
CREATE VIEW `view_event_participation` AS
SELECT
    e.id AS event_id,
    e.name AS event_name,
    COUNT(m.id) AS matches_played
FROM Events e
         LEFT JOIN Matches m
                   ON e.id = m.events_id
GROUP BY e.id, e.name;

DROP VIEW IF EXISTS `view_player_activity_by_month`;
CREATE VIEW `view_player_activity_by_month` AS
SELECT
        YEAR(`timestamp`) AS `year`,
        MONTH(`timestamp`) AS `month`,
        COUNT(*) AS matches_played
        FROM Matches
        GROUP BY YEAR(`timestamp`), MONTH(`timestamp`);

DROP VIEW IF EXISTS `view_match_moves`;
CREATE VIEW `view_match_moves` AS
SELECT
    mv.id AS move_id,
    mv.matches_id AS match_id,
    mv.turn,
    mv.sequence,
    mv.direction,
    mv.status,
    p.id AS position_id,
    p.color,
    p.piece,
    p.cordinate
FROM Moves mv
         JOIN Position p
              ON mv.position_id = p.id;

DROP VIEW IF EXISTS `view_turn_moves`;
CREATE VIEW `view_turn_moves` AS
SELECT
    mv.matches_id AS match_id,
    mv.turn,
    mv.id AS move_id,
    mv.sequence,
    mv.direction,
    mv.status,
    p.id AS position_id,
    p.color,
    p.piece,
    p.cordinate
FROM Moves mv
         JOIN Position p
              ON mv.position_id = p.id;


-- -----------------------------------------------------
-- Full replay-style view
-- Useful for showing a match together with its moves
-- -----------------------------------------------------

DROP VIEW IF EXISTS `view_match_replay`;
CREATE VIEW `view_match_replay` AS
SELECT
    m.id AS match_id,
    silver.username AS silver_player,
    gold.username AS gold_player,
    gt.name AS gametype_name,
    e.name AS event_name,
    m.match_result,
    m.timestamp,
    mv.id AS move_id,
    mv.turn,
    mv.sequence,
    mv.direction,
    mv.status,
    p.color,
    p.piece,
    p.cordinate
FROM Matches m
         JOIN Players silver
              ON m.player_id_silver = silver.id
         JOIN Players gold
              ON m.player_id_gold = gold.id
         LEFT JOIN GameTypes gt
                   ON m.gameTypes_id = gt.id
         LEFT JOIN Events e
                   ON m.events_id = e.id
         JOIN Moves mv
              ON mv.matches_id = m.id
         JOIN Position p
              ON mv.position_id = p.id;

DROP VIEW IF EXISTS `view_solutions_with_puzzles`;
CREATE VIEW `view_solutions_with_puzzles` AS
SELECT
    s.id AS solution_id,
    s.puzzles_id AS puzzle_id,
    pu.name AS puzzle_name,
    pu.objective,
    pu.playerSide,
    pu.rounds,
    s.turn,
    s.sequence,
    s.direction,
    s.status,
    p.id AS position_id,
    p.color,
    p.piece,
    p.cordinate
FROM Solutions s
         JOIN Puzzles pu
              ON s.puzzles_id = pu.id
         JOIN Position p
              ON s.position_id = p.id;

DROP VIEW IF EXISTS `view_openings_by_match_details`;
CREATE VIEW `view_openings_by_match_details` AS
SELECT
    obm.id AS opening_by_match_id,
    obm.matches_id AS match_id,
    p.id AS position_id,
    p.color,
    p.piece,
    p.cordinate
FROM OpeningsByMatch obm
         JOIN Position p
              ON obm.position_id = p.id;

DROP VIEW IF EXISTS `view_openings_by_puzzle_details`;
CREATE VIEW `view_openings_by_puzzle_details` AS
SELECT
    obp.id AS opening_by_puzzle_id,
    obp.puzzles_id AS puzzle_id,
    pu.name AS puzzle_name,
    pu.objective,
    pu.playerSide,
    pu.rounds,
    p.id AS position_id,
    p.color,
    p.piece,
    p.cordinate
FROM OpeningsByPuzzle obp
         JOIN Puzzles pu
              ON obp.puzzles_id = pu.id
         JOIN Position p
              ON obp.position_id = p.id;