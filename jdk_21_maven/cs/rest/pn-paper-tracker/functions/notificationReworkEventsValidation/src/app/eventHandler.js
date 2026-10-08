const { getLatestReworkRequestByIun, appendReceivedStatusCode, insertEventsError } = require("./dynamo");
const { sendToQueue, createMessageAttributes } = require("./sqs");
const { logOperation } = require("./log");
const {checkRejectedStatusCode, checkAlreadyReceived, checkExpected, checkAttachments, checkDeliveryFailureCause, buildReceivedStatusCodeEntry} = require("./utils");

async function handleEvent(event){
  const batchItemFailures = [];
  // Processa tutti i record, collezionando i fallimenti
  for (const record of event.Records) {
    try {
      await processRecord(record);
    } catch (error) {
      batchItemFailures.push({ itemIdentifier: record.messageId});
      logOperation("ERROR", record.messageId, {  message: "Record added to batch failures for retry", error: error.message,});
    }
  }

  if (batchItemFailures.length > 0) {
    return {
      batchItemFailures: batchItemFailures
    };
  }
  return { status: "ok" };
};

function parseBody(record) {
    try {
      return JSON.parse(record.body);
    } catch (err) {
      logOperation("ERROR", messageId, {
        error: "Invalid JSON",
        rawBody: record.body,
      });
      throw err;
    }
}

/**
 * Processa un singolo record SQS
 */
const processRecord = async (record) => {
    const { messageId } = record;
    logOperation("INFO", messageId, { message: "Processing message",record });

    let body = parseBody(record);

    const analogMail = body?.analogMail;

    const statusCode = analogMail.statusCode;
    const statusDateTime = analogMail.statusDateTime;
    const attachments = analogMail.attachments?.map(att => att.documentType) || [];
    const deliveryFailureCause = analogMail.deliveryFailureCause;
    const iun = analogMail.iun;
    const requestId = analogMail.requestId;

    const reworkEntry = await getLatestReworkRequestByIun(iun, requestId);
    if (!reworkEntry) {
        logOperation("FATAL", messageId, { message: "No notification rework request found for IUN", iun });
        await insertEventsError(iun, null, analogMail, "Richiesta di rework non presente");
        return;
    }

    if(reworkEntry.status != "READY" && reworkEntry.status != "IN_PROGRESS"){
        logOperation("FATAL", messageId, { message: "Rework request is in invalid status", reworkStatus: reworkEntry.status });
        const reworkId = reworkEntry?.reworkId;
        await insertEventsError(iun, reworkId, analogMail, `La richiesta di rework è in uno stato non valido: ${reworkEntry.status}`);
        return;
    }

    const reworkId = reworkEntry?.reworkId;
    let continueProcessing = await checkStatusCode(messageId, reworkEntry, analogMail, statusCode, statusDateTime, attachments, deliveryFailureCause);
    if(continueProcessing){
        const attributes = await createMessageAttributes(record.messageAttributes, reworkId);
        await sendToQueue(body, attributes);
        const receivedStatusCodeEntry = buildReceivedStatusCodeEntry(statusCode, attachments, statusDateTime);
        await appendReceivedStatusCode(iun, reworkId, receivedStatusCodeEntry, analogMail);
    }

    logOperation("DEBUG", messageId, { message: "Message processed successfully" });
}

async function checkStatusCode(messageId, reworkEntry, analogMail, statusCode, statusDateTime, attachments, deliveryFailureCause){
    const reworkId = reworkEntry?.reworkId;
    const iun = reworkEntry?.iun;
    const rejected = checkRejectedStatusCode(statusCode);
    if (rejected) {
        logOperation("FATAL", messageId, { message: "Scartato in quanto evento di furto, smarrimento o deterioramento"});
        await insertEventsError(iun, reworkId, analogMail, "Evento di furto, smarrimento o deterioramento");
        return false;
    }

    const expected = checkExpected(reworkEntry, statusCode);

    if(!expected){
        const message = "Evento non atteso. Gli eventi accettati sono: " + JSON.stringify(reworkEntry.expectedStatusCodes);
        logOperation("ERROR", messageId, {
            message: message,
            expectedStatusCodes: JSON.stringify(reworkEntry.expectedStatusCode)
        });
        await insertEventsError(iun, reworkId, analogMail, message);
        return false;
    }

    const attachment = checkAttachments(reworkEntry, statusCode, attachments);
    if(!attachment){
        const message = "Allegati non validi. Gli eventi accettati sono: " + JSON.stringify(reworkEntry.expectedStatusCodes);
        logOperation("ERROR", messageId, {
            message: message,
            expectedStatusCodes: JSON.stringify(reworkEntry.expectedStatusCode)
        });
        await insertEventsError(iun, reworkId, analogMail, message);
        return false;
    }

    if(statusCode.endsWith("A") || statusCode.endsWith("D")){
      const deliveryFailureCauseValidation = checkDeliveryFailureCause(reworkEntry, deliveryFailureCause);
      if(!deliveryFailureCauseValidation){
        const message = "DeliveryFailureCause non valida.";
        logOperation("ERROR", messageId, {
            message: message
        });
        await insertEventsError(iun, reworkId, analogMail, message);
        return false;
      }
    }


    const alreadyReceived = checkAlreadyReceived(reworkEntry, statusCode, attachments, statusDateTime);

    if(alreadyReceived){
        logOperation("ERROR", messageId, {  message: "Evento duplicato"});
        await insertEventsError(iun, reworkId, analogMail, "Evento duplicato");
        return false;
    }
    return true
}

module.exports = {
    handleEvent
}