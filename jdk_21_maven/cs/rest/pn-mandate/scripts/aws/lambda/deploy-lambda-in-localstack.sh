#!/bin/bash
cd "$(dirname "$0")"

endpoint_url='http://localhost:4566'
region='us-east-1'
# recupero l'ARN dello stream della tabella (cambia ogni volta che viene creata)
stream_arn=$(aws --endpoint-url=$endpoint_url --region $region dynamodb describe-table --table-name Mandate | grep -o '"LatestStreamArn": "[^"]*' | grep -o '[^"]*$')
# elimino eventuale lambda presente
DEPLOYED_FUNCTIONS=$(aws --endpoint-url=http://localhost:4566 --region $region lambda list-functions)

if [[ $DEPLOYED_FUNCTIONS =~ pn-mandate-expired-trigger ]]
then
    aws lambda delete-function --endpoint-url=$endpoint_url --region $region --function-name pn-mandate-expired-trigger
else
    echo "pn-mandate-expired-trigger is not currently deployed, no need to delete-function"
fi


# creo la lambda
aws lambda create-function --endpoint-url=$endpoint_url --region $region --function-name pn-mandate-expired-trigger --zip-file fileb://./lambda.zip --handler index.handler --environment "Variables={QUEUE_URL=http://localstack:4566/000000000000/local-mandate-inputs.fifo,ENV=LOCAL}" --runtime 'nodejs16.x' --role a
# creo lo stream
aws lambda create-event-source-mapping  --endpoint-url=$endpoint_url --region $region --function-name pn-mandate-expired-trigger --event-source $stream_arn --batch-size 10 --starting-position TRIM_HORIZON
# elimino il file index.js usato di supporto
rm -f index.js
rm -f lambda.zip


