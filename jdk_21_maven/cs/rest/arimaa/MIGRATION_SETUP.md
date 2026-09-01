# Migration Setup Guide (MySQL -> Neo4j and MongoDB)

This guide explains how the migration system works in this project and how to add new MongoDB migrations.

The goal is simple:
- Read data from MySQL (source of truth)
- Write to one or more targets (Neo4j and/or MongoDB)
- Choose exactly which targets and steps to run

---

## 1) How the migration architecture works

### Core pieces

- `MigrationRunner`  
  Runs all migration steps in order when profile `migration` is active.

- `MigrationStep` (interface)  
  Every migration step implements this interface and provides:
  - `stepName()` -> unique id used by config filtering
  - `targets()` -> which store(s) this step writes to (`NEO4J`, `MONGODB`)
  - `getOrder()` -> execution order (lower runs first)
  - `migrate(context)` -> actual migration logic

- `MigrationProperties`  
  Reads config from `migration.*` keys:
  - `enabled-targets`
  - `enabled-steps`
  - `batch-size`
  - `dry-run`

- `MigrationTarget` enum  
  Supported values:
  - `neo4j`
  - `mongodb` (or `mongo`)

### Folder structure

- Neo4j steps: `src/main/java/com/example/arimaabackend/migration/steps/neo4j`
- MongoDB steps: `src/main/java/com/example/arimaabackend/migration/steps/mongodb`

---

## 2) How step filtering works

The runner applies two filters for each step:

1. **Target filter** (`migration.enabled-targets`)  
   Step is skipped if its `targets()` does not match selected targets.

2. **Step name filter** (`migration.enabled-steps`)  
   Step is skipped if `stepName()` is not listed.

If either list is empty, it means "all" for that dimension.

So:
- empty targets + empty steps -> run everything
- targets only -> run all steps for selected targets
- steps only -> run listed step names across all targets
- targets + steps -> run intersection

---

## 3) Configuration examples

Use `src/main/resources/application-migration.properties` (or CLI args).

### Run all migrations

No `enabled-targets` and no `enabled-steps` set.

### Run only Neo4j migrations

```properties
migration.enabled-targets[0]=neo4j
```

### Run only MongoDB migrations

```properties
migration.enabled-targets[0]=mongodb
```

### Run only selected steps

```properties
migration.enabled-steps[0]=country
migration.enabled-steps[1]=user-mongo
```

### Dry run

```properties
migration.dry-run=true
```

Dry run should log counts and skip writes.

---

## 4) How to add a new MongoDB migration

Use `UserMongoMigration` as a reference.

### Step-by-step

1. Create a new class in:
   - `migration/steps/mongodb`
2. Annotate with:
   - `@Service`
   - `@Profile("migration")`
3. Implement `MigrationStep`
4. Return a unique `stepName()` (example: `puzzle-mongo`)
5. Set target explicitly:
   - `targets()` -> `EnumSet.of(MigrationTarget.MONGODB)`
6. Choose order (`getOrder()`) based on dependencies
7. Implement `migrate(MigrationContext context)`:
   - read from SQL repository
   - map entity -> Mongo document
   - on `dryRun`, log count and return
   - otherwise write via Mongo repository

---

## 5) MySQL prerequisite

The migrator reads the **current** JPA schema (`Users` + `Players.user_id`). That requires the Docker MySQL init scripts to have run through [`Database/mysql/Sql_arimaa_62_user.sql`](Database/mysql/Sql_arimaa_62_user.sql) (not `Sql_arimaa_10_init.sql` alone).

Start MySQL with the project compose file so all init scripts apply in order.

---

## 6) Step order and step ids

### Neo4j (lower `getOrder()` runs first)

`neo4j-schema` → `country` → `event` → `game-type` → `position` → `puzzle` → **`user`** → `player` → `match` → `move` → `solution` → `opening-by-match` → `opening-by-puzzle`

`user` must run before `player` so `HAS_USER` edges can link to existing `:User` nodes.

### MongoDB

`user-mongo` (121) → `player-mongo` (122) → `match-mongo` (123) → `puzzle-mongo` (124)

### All step ids

`country`, `event`, `game-type`, `position`, `puzzle`, `user`, `player`, `match`, `move`, `solution`, `opening-by-match`, `opening-by-puzzle`, `neo4j-schema`, `user-mongo`, `player-mongo`, `match-mongo`, `puzzle-mongo`

---

## 7) Common issues and fixes

- **Bean conflict with same migration class name**  
  Usually caused by duplicate class/package names or stale `target/classes`.  
  Fix: keep only one class path + run `mvn clean`.

- **Step not running**  
  Check `stepName()` matches `migration.enabled-steps[...]` exactly (case-insensitive, spelling still matters).

- **Unexpected target execution**  
  Check `targets()` in the class and `migration.enabled-targets[...]` config.

