const { SQSClient, SendMessageCommand } = require("@aws-sdk/client-sqs");

const SQS_QUEUE_URL = process.env.EXTERNAL_TO_PAPER_QUEUE_URL;
const sqsClient = new SQSClient({});

/**
 * Invia un messaggio a una coda SQS
 * @param {string} queueUrl - URL della coda
 * @param {object} messageBody - Corpo del messaggio
 * @param {object} messageAttributes - Attributi del messaggio
 * @returns {Promise} Promise dell'invio
 */
async function sendToQueue (messageBody, messageAttributes) {
  return await sqsClient.send(
    new SendMessageCommand({
      QueueUrl: SQS_QUEUE_URL,
      MessageBody: JSON.stringify(messageBody),
      MessageAttributes: messageAttributes,
    })
  );
};

/**
 * Crea gli attributi del messaggio copiando quelli originali
 * @param {object} originalAttributes - Attributi originali del record SQS
 * @param {boolean} addDryRun - Se aggiungere l'attributo dryRun
 * @returns {object} Attributi del messaggio
 */
async function createMessageAttributes (originalAttributes = {}, reworkId) {
  const attributes = {};
  // Copia attributi originali
    for (const [key, attr] of Object.entries(originalAttributes)) {
        // Saltiamo reworkId qui perché verrà sovrascritto esplicitamente dopo
        if (key === 'reworkId') continue;

        attributes[key] = {
          DataType: attr.dataType || attr.DataType || 'String', // Gestisce sia input camelCase che PascalCase
          StringValue: attr.stringValue || attr.StringValue
        };
    }

    attributes["reworkId"] = {
      DataType: "String",
      StringValue: reworkId,
    };
  return attributes;
}

module.exports = {
    sendToQueue,
    createMessageAttributes
}