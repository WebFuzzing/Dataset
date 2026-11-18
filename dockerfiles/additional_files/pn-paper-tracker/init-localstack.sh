#!/bin/bash

# The SUT creates neither its DynamoDB tables nor its SQS queues, and its producers look the queues up
# at startup. Same resources as the SUT's src/test/resources/testcontainers/init.sh. LocalStack runs this once ready.

set -e

for table in pn-PaperTrackerDryRunOutputs pn-PaperTrackingsErrors; do
  awslocal dynamodb create-table --table-name "$table" \
    --billing-mode PAY_PER_REQUEST \
    --attribute-definitions AttributeName=trackingId,AttributeType=S \
                            AttributeName=created,AttributeType=S \
    --key-schema AttributeName=trackingId,KeyType=HASH \
                 AttributeName=created,KeyType=RANGE
done

awslocal dynamodb create-table --table-name pn-PaperTrackings \
  --billing-mode PAY_PER_REQUEST \
  --attribute-definitions AttributeName=trackingId,AttributeType=S \
                          AttributeName=ocrRequestId,AttributeType=S \
                          AttributeName=attemptId,AttributeType=S \
                          AttributeName=pcRetry,AttributeType=S \
  --key-schema AttributeName=trackingId,KeyType=HASH \
  --global-secondary-indexes '[{
        "IndexName": "ocrRequestId-index",
        "KeySchema": [{"AttributeName": "ocrRequestId", "KeyType": "HASH"}],
        "Projection": {"ProjectionType": "ALL"}
      }, {
        "IndexName": "attemptId-pcRetry-index",
        "KeySchema": [{"AttributeName": "attemptId", "KeyType": "HASH"},
                      {"AttributeName": "pcRetry", "KeyType": "RANGE"}],
        "Projection": {"ProjectionType": "ALL"}
      }]'

# the health check waits for the last one
for queue in pn-ocr_outputs dl-sqs pn-external_channel_to_paper_tracker pn-external_channel_outputs \
             pn-external_channel_to_paper_channel_dryrun pn-external_channel_to_paper_channel; do
  awslocal sqs create-queue --queue-name "$queue"
done
