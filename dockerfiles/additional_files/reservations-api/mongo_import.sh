#!/bin/bash


MAX_ATTEMPTS=30
attempt=1

echo "Waiting for Mongo PRIMARY..."

while ! mongo --host localhost --eval "rs.isMaster().ismaster" | grep -q true; do
  if [ $attempt -ge $MAX_ATTEMPTS ]; then
    echo "Mongo PRIMARY did not become ready after $MAX_ATTEMPTS attempts, exiting."
    exit 1
  fi
  attempt=$((attempt+1))
  sleep 1
done

echo "Mongo PRIMARY is ready, running import..."

#mongoimport --host db --port 27017 --db reservations-api --collection users --file /fixtures/init.json --jsonArray
mongoimport --host localhost --port 27017 --db reservations-api --collection users --file /fixtures/init.json --jsonArray
