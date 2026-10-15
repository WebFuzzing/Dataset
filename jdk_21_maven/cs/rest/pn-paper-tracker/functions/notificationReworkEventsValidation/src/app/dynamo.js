const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  QueryCommand,
  UpdateCommand,
  PutCommand
} = require("@aws-sdk/lib-dynamodb");

const client = new DynamoDBClient({});
const docClient = DynamoDBDocumentClient.from(client);

const NOTIFICATION_REWORK_TABLE = process.env.NOTIFICATION_REWORK_TABLE;
const EVENTS_ERROR_TABLE = process.env.NOTIFICATION_REWORK_EVENTS_ERROR_TABLE;

async function getLatestReworkRequestByIun(iun, requestId) {
    const params = {
        TableName: NOTIFICATION_REWORK_TABLE,
        KeyConditionExpression: "iun = :iun",
        ExpressionAttributeValues: {
          ":iun": iun
        },
        ScanIndexForward: false
    };

    const result = await docClient.send(new QueryCommand(params));
    const recIndex = getRecIndexFromRequestId(requestId);
    if (!result.Items || result.Items.length === 0) {
      return null;
    }

    return result.Items.reduce((latest, item) => {
      if (item.recIndex !== recIndex || !item.createdAt) {
        return latest;
      }

      if (!latest) {
        return item;
      }

      return Date.parse(item.createdAt) > Date.parse(latest.createdAt)
        ? item
        : latest;
    }, null);
}

function getRecIndexFromRequestId(requestId){
  if (!requestId) return null;
  const match = requestId.match(/(RECINDEX_\d+)/);
  return match ? match[1] : null;
}

async function appendReceivedStatusCode(iun, reworkId, receivedStatusCodesEntry, analogMail) {
    try {
        await docClient.send(new UpdateCommand({
          TableName: NOTIFICATION_REWORK_TABLE,
          Key: { iun, reworkId },
          UpdateExpression: "SET receivedStatusCodes = list_append(if_not_exists(receivedStatusCodes, :empty), :new)",
          ConditionExpression: "NOT contains(receivedStatusCodes, :check)",
          ExpressionAttributeValues: {
            ":new": [receivedStatusCodesEntry],
            ":check": receivedStatusCodesEntry,
            ":empty": []
          }
        }));
      } catch (error) {
        if (error.name === 'ConditionalCheckFailedException') {
          // Elemento già presente, non è un errore
          console.log(`Status code ${receivedStatusCodesEntry} già presente per iun=${iun}, reworkId=${reworkId}`);
          await insertEventsError(iun, reworkId, analogMail, "Evento duplicato");
        }
      }
}

async function insertEventsError(iun, reworkId, analogMail, errorMessage) {
  const now = new Date().toISOString();
  const uniqueReworkId = reworkId ? `${reworkId}~${crypto.randomUUID()}` : crypto.randomUUID();

  const item = {
    iun,
    reworkId: uniqueReworkId,
    requestId: analogMail.requestId,
    statusCode: analogMail.statusCode,
    statusDateTime: analogMail.statusDateTime,
    productType: analogMail.productType,
    deliveryFailureCause: analogMail.deliveryFailureCause,
    attachments: analogMail.attachments,
    errorMessage,
    created: now
  };
  
  const command = new PutCommand({
    TableName: EVENTS_ERROR_TABLE,
    Item: item
  });
  await docClient.send(command);
}

module.exports = {
  getLatestReworkRequestByIun,
  appendReceivedStatusCode,
  insertEventsError
};