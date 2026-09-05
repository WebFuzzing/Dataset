#!/bin/bash
mongoimport --host localhost --port 27017 --db test --collection User --file /fixtures/init.json --jsonArray