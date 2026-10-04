#!/bin/bash

# The "news" index is created by collector-service, which is not part of WFD, and the API is read-only:
# without this seed every read is empty. Safe to rerun, so it is used as the healthcheck.

set -e

ES=http://localhost:9200

if curl -sf -o /dev/null "$ES/news/_doc/8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a05"; then
  exit 0
fi

# Same analyzers (news-es-analysis.json) and mapping collector-service uses
curl -sf -o /dev/null -X PUT "$ES/news" -H 'Content-Type: application/json' -d '{
  "settings": {"analysis": {
    "analyzer": {
      "my_analyzer": {"type": "custom", "tokenizer": "standard", "filter": ["lowercase", "edge_ngram_filter"]},
      "my_search_analyzer": {"type": "custom", "tokenizer": "standard", "filter": ["lowercase"]}},
    "filter": {"edge_ngram_filter": {"type": "edge_ngram", "min_gram": 1, "max_gram": 20}}}},
  "mappings": {"properties": {
    "title": {"type": "text", "analyzer": "my_analyzer", "search_analyzer": "my_search_analyzer"},
    "text": {"type": "text", "analyzer": "my_analyzer", "search_analyzer": "my_search_analyzer"},
    "category": {"type": "text", "analyzer": "my_analyzer", "search_analyzer": "my_search_analyzer"},
    "datetime": {"type": "date"}}}}' || true

# Same seed as the WB drivers: one news per category of categorizer-service
curl -sf -X POST "$ES/_bulk?refresh=true" -H 'Content-Type: application/json' --data-binary @- <<'EOF' | grep -q '"errors":false'
{"index":{"_index":"news","_id":"8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a01"}}
{"id":"8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a01","title":"Brazil wins the cup","text":"The final in Brasilia ended two to one","category":"Sport","datetime":"2026-01-10T09:00:00Z"}
{"index":{"_index":"news","_id":"8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a02"}}
{"id":"8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a02","title":"Summit in Geneva","text":"World leaders meet to discuss trade","category":"World","datetime":"2026-01-11T10:30:00Z"}
{"index":{"_index":"news","_id":"8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a03"}}
{"id":"8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a03","title":"Mars rover finds water","text":"New data from the rover confirms ice","category":"Science","datetime":"2026-01-12T12:15:00Z"}
{"index":{"_index":"news","_id":"8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a04"}}
{"id":"8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a04","title":"Film festival opens","text":"Directors from forty countries arrive","category":"Entertainment","datetime":"2026-01-13T18:45:00Z"}
{"index":{"_index":"news","_id":"8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a05"}}
{"id":"8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a05","title":"New vaccine approved","text":"Health authorities approve the vaccine","category":"Health","datetime":"2026-01-14T07:20:00Z"}
EOF
