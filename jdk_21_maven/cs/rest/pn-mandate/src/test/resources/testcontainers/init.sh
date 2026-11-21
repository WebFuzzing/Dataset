#!/bin/bash
echo "test"

echo "### CREATE QUEUES FIFO ###"

queues_fifo="local-mandate-inputs.fifo"

for qn in  $( echo $queues_fifo | tr " " "\n" ) ; do

    echo creating queue fifo $qn ...

    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2","FifoQueue": "true","ContentBasedDeduplication": "true"}' \
        --queue-name $qn


done


echo " - Create pn-mandates TABLES"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name Mandate  \
    --attribute-definitions \
        AttributeName=pk,AttributeType=S \
        AttributeName=sk,AttributeType=S \
        AttributeName=s_delegate,AttributeType=S \
        AttributeName=i_state,AttributeType=N \
    --key-schema \
        AttributeName=pk,KeyType=HASH \
        AttributeName=sk,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --stream-specification \
        StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\": \"delegate-state-gsi\",
            \"KeySchema\": [{\"AttributeName\":\"s_delegate\",\"KeyType\":\"HASH\"},
                            {\"AttributeName\":\"i_state\",\"KeyType\":\"RANGE\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        }
    ]"

  
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name MandateHistory  \
    --attribute-definitions \
        AttributeName=pk,AttributeType=S \
        AttributeName=sk,AttributeType=S \
    --key-schema \
        AttributeName=pk,KeyType=HASH \
        AttributeName=sk,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    ssm put-parameter \
    --name "AARQrUrlConfigs" \
    --type String \
    --value "{\"1.0.0\": {\"directAccessUrlTemplatePhysical\": \"https://cittadini.dev.notifichedigitali.it/\",\"directAccessUrlTemplateLegal\": \"https://imprese.dev.notifichedigitali.it/\",\"quickAccessUrlAarDetailSuffix\": \"?aar\"}}"


echo "Initialization terminated"





