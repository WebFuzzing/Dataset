// Run in Neo4j Browser after first startup (or via cypher-shell).

// Primary IDs (mirrors SQL PKs stored in `id`)
CREATE CONSTRAINT country_id IF NOT EXISTS FOR (n:Country) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT player_id IF NOT EXISTS FOR (n:Player) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT event_id IF NOT EXISTS FOR (n:Event) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT game_type_id IF NOT EXISTS FOR (n:GameType) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT match_id IF NOT EXISTS FOR (n:Match) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT position_id IF NOT EXISTS FOR (n:Position) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT move_id IF NOT EXISTS FOR (n:Move) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT puzzle_id IF NOT EXISTS FOR (n:Puzzle) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT solution_id IF NOT EXISTS FOR (n:Solution) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT user_id IF NOT EXISTS FOR (n:User) REQUIRE n.id IS UNIQUE;

// Uniques from SQL schema (identity lives on User, not Player)
CREATE CONSTRAINT country_name IF NOT EXISTS FOR (n:Country) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT puzzle_name IF NOT EXISTS FOR (n:Puzzle) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT user_username IF NOT EXISTS FOR (n:User) REQUIRE n.username IS UNIQUE;
CREATE CONSTRAINT user_email IF NOT EXISTS FOR (n:User) REQUIRE n.email IS UNIQUE;
