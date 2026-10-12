echo "### CREATE QUEUES ###"

queues="pn-ocr_outputs dl-sqs pn-external_channel_to_paper_tracker pn-external_channel_outputs pn-external_channel_to_paper_channel_dryrun pn-external_channel_to_paper_channel"
for qn in $(echo $queues | tr " " "\n"); do
    echo creating queue $qn ...
    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn

    echo "Queue $qn created successfully"
done


awslocal sqs list-queues

echo "### CREATE TABLES ###"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-PaperTrackerDryRunOutputs  \
    --attribute-definitions \
        AttributeName=trackingId,AttributeType=S \
        AttributeName=created,AttributeType=S \
    --key-schema \
        AttributeName=trackingId,KeyType=HASH \
        AttributeName=created,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-PaperTrackingsErrors  \
    --attribute-definitions \
        AttributeName=trackingId,AttributeType=S \
        AttributeName=created,AttributeType=S \
    --key-schema \
        AttributeName=trackingId,KeyType=HASH \
        AttributeName=created,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-PaperTrackings  \
    --attribute-definitions \
        AttributeName=trackingId,AttributeType=S \
        AttributeName=ocrRequestId,AttributeType=S \
        AttributeName=attemptId,AttributeType=S \
        AttributeName=pcRetry,AttributeType=S \
    --key-schema \
        AttributeName=trackingId,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
     --global-secondary-indexes \
                    "[
                        {
                            \"IndexName\": \"ocrRequestId-index\",
                            \"KeySchema\": [{\"AttributeName\":\"ocrRequestId\",\"KeyType\":\"HASH\"}],
                            \"Projection\":{
                                \"ProjectionType\":\"ALL\"
                            },
                             \"ProvisionedThroughput\": {
                                 \"ReadCapacityUnits\": 10,
                                 \"WriteCapacityUnits\": 5
                             }
                        },
                        {
                            \"IndexName\": \"attemptId-pcRetry-index\",
                            \"KeySchema\": [{\"AttributeName\":\"attemptId\",\"KeyType\":\"HASH\"},
                                           {\"AttributeName\":\"pcRetry\",\"KeyType\":\"RANGE\"}],
                            \"Projection\":{
                                \"ProjectionType\":\"ALL\"
                            },
                             \"ProvisionedThroughput\": {
                                 \"ReadCapacityUnits\": 10,
                                 \"WriteCapacityUnits\": 5
                             }
                        }
                    ]"

echo "Initialization terminated"
