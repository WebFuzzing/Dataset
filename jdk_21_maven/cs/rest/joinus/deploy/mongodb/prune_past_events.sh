#!/usr/bin/env bash
set -euo pipefail

: "${MONGODB_URI:?Missing MONGODB_URI}"
: "${MONGODB_DB:?Missing MONGODB_DB}"

mongosh "$MONGODB_URI" --quiet --eval '
const dbName = process.env.MONGODB_DB;
const database = db.getSiblingDB(dbName);
const now = new Date();

for (const c of ["groups", "members"]) {
  database.getCollection(c).updateMany(
    { "upcoming_events.event_time": { $lt: now } },
    { $pull: { upcoming_events: { event_time: { $lt: now } } } }
  );
}
'