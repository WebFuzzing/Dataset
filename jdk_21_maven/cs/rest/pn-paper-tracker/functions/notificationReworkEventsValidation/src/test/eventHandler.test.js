const assert = require('assert');
const proxyquire = require('proxyquire');

// Mock modules
let mockDynamo = {};
let mockSqs = {};
let mockLog = {};
let handleEvent;

// Setup mocks before requiring handler
function setupMocks() {
  // Reset mocks
  mockDynamo = {
    getLatestReworkRequestByIun: async () => null,
    appendReceivedStatusCode: async () => {},
    insertEventsError: async () => {}
  };

  mockSqs = {
    sendToQueue: async () => {},
    createMessageAttributes: () => ({})
  };

  mockLog = {
    logOperation: () => {}
  };
}

// Helper functions
function createValidRecord(overrides = {}) {
  const record = {
    messageId: 'test-message-id',
    body: JSON.stringify({
      analogMail: {
        statusCode: 'RECAG001',
        statusDateTime: '2025-01-15T10:00:00Z',
        iun: 'TEST-IUN-123',
        attachments: [{ documentType: 'AR' }],
        deliveryFailureCause: null
      }
    }),
    messageAttributes: {}
  };

  if (overrides.messageId) record.messageId = overrides.messageId;
  if (overrides.body) record.body = overrides.body;
  if (overrides.analogMail) {
    const body = JSON.parse(record.body);
    body.analogMail = { ...body.analogMail, ...overrides.analogMail };
    record.body = JSON.stringify(body);
  }

  return record;
}

function createInvalidRecord(overrides = {}) {
  const record = {
    messageId: 'test-message-id',
    body: JSON.stringify({
      analogMail: {
        statusCode: 'RECAG003',
        statusDateTime: '2025-01-15T10:00:00Z',
        iun: 'TEST-IUN-123',
        attachments: [{ documentType: 'AR' }],
        deliveryFailureCause: null
      }
    }),
    messageAttributes: {}
  };

  if (overrides.messageId) record.messageId = overrides.messageId;
  if (overrides.body) record.body = overrides.body;
  if (overrides.analogMail) {
    const body = JSON.parse(record.body);
    body.analogMail = { ...body.analogMail, ...overrides.analogMail };
    record.body = JSON.stringify(body);
  }

  return record;
}

function createReworkEntry(overrides = {}) {
  return {
    iun: 'TEST-IUN-123',
    reworkId: 'rework-123',
    status: 'READY',
    expectedStatusCodes: [
      { statusCode: 'RECAG001', attachments: ['AR'] },
      { statusCode: 'RECAG002', attachments: [] }
    ],
    receivedStatusCodes: [],
    ...overrides
  };
}

describe("handleEvent - Successful Processing", () => {
  beforeEach(() => {
    setupMocks(); });

  it("should process valid record successfully", async () => {
    let appendCalled = false;
    let queueCalled = false;

    // Set up mocks before loading handler
    mockDynamo.getLatestReworkRequestByIun = async (iun) => {
      assert.strictEqual(iun, 'TEST-IUN-123');
      return createReworkEntry();
    };

    mockDynamo.appendReceivedStatusCode = async () => {
      appendCalled = true;
    };

    mockSqs.sendToQueue = async () => {
      queueCalled = true;
    };

    mockSqs.createMessageAttributes = () => ({ reworkId: 'rework-123' });

    // Reload handler with updated mocks
    handleEvent = proxyquire('../app/eventHandler', {
      './dynamo': mockDynamo,
      './sqs': mockSqs,
      './log': mockLog
    }).handleEvent;

    const event = { Records: [createValidRecord()] };
    const result = await handleEvent(event);

    assert.deepStrictEqual(result, { status: 'ok' });
    assert.strictEqual(appendCalled, true, 'appendReceivedStatusCode should be called');
    assert.strictEqual(queueCalled, true, 'sendToQueue should be called');
  });

  it("should process multiple records successfully", async () => {
    let sqsCounter = 0;
    let appendCounter = 0;

    // Set up mocks before loading handler
    mockDynamo.getLatestReworkRequestByIun = async (iun) => {
      assert.strictEqual(iun, 'TEST-IUN-123');
      return createReworkEntry();
    };

    mockDynamo.appendReceivedStatusCode = async () => {
      appendCounter++;
    };

    mockSqs.sendToQueue = async () => {
      sqsCounter++;
    };

    mockSqs.createMessageAttributes = () => ({ reworkId: 'rework-123' });

    // Reload handler with updated mocks
    handleEvent = proxyquire('../app/eventHandler', {
      './dynamo': mockDynamo,
      './sqs': mockSqs,
      './log': mockLog
    }).handleEvent;

    const event = {
      Records: [
        createValidRecord({ messageId: 'msg-1' }),
        createValidRecord({ messageId: 'msg-2' }),
        createValidRecord({ messageId: 'msg-3' })
      ]
    };

    const result = await handleEvent(event);

    assert.deepStrictEqual(result, { status: 'ok' });
    assert.strictEqual(appendCounter, 3, 'appendReceivedStatusCode should be called 3 times');
    assert.strictEqual(sqsCounter, 3, 'sendToQueue should be called 3 times');
  });
});

describe("handleEvent - Error Cases", () => {
  beforeEach(() => {
    setupMocks();
  });

  it("should handle missing rework entry", async () => {
    let callAppend = false;
    let callSqs = false;

    mockDynamo.appendReceivedStatusCode = async () => {
      callAppend = true;
    };

    mockSqs.sendToQueue = async () => {
      callSqs = true;
    };

    mockDynamo.insertEventsError = async (...args) => {
      errorInserted = true;
      capturedArgs = args;
    };

    // Reload handler with updated mocks
    handleEvent = proxyquire('../app/eventHandler', {
      './dynamo': mockDynamo,
      './sqs': mockSqs,
      './log': mockLog
    }).handleEvent;

    const event = { Records: [createValidRecord()] };
    const result = await handleEvent(event);

    assert.deepStrictEqual(result, {"status": "ok"});
    assert.strictEqual(errorInserted, true);
    assert.strictEqual(capturedArgs[3], 'Richiesta di rework non presente');
    assert.strictEqual(callAppend,false, 'appendReceivedStatusCode should not be called');
    assert.strictEqual(callSqs, false, 'sendToQueue should not be called');
  });

  it("should reject rework with COMPLETED status", async () => {
    let callAppend = false;
    let callSqs = false;
    let reworkEntry = createReworkEntry();
    reworkEntry.status = "DONE";

    mockDynamo.getLatestReworkRequestByIun = async (iun) => {
      assert.strictEqual(iun, 'TEST-IUN-123');
      return reworkEntry;
    };

    mockDynamo.appendReceivedStatusCode = async () => {
      callAppend = true;
    };

    mockSqs.sendToQueue = async () => {
      callSqs = true;
    };

    mockDynamo.insertEventsError = async (...args) => {
      errorInserted = true;
      capturedArgs = args;
    };

    // Reload handler with updated mocks
    handleEvent = proxyquire('../app/eventHandler', {
      './dynamo': mockDynamo,
      './sqs': mockSqs,
      './log': mockLog
    }).handleEvent;

    const event = { Records: [createValidRecord()] };
    const result = await handleEvent(event);

    assert.deepStrictEqual(result, {"status": "ok"});
    assert.strictEqual(errorInserted, true);
    assert.strictEqual(capturedArgs[3], `La richiesta di rework è in uno stato non valido: DONE`);
    assert.strictEqual(callAppend,false, 'appendReceivedStatusCode should not be called');
    assert.strictEqual(callSqs, false, 'sendToQueue should not be called');
  });

  it("should reject rework with ERROR status", async () => {
   let callAppend = false;
    let callSqs = false;
    let reworkEntry = createReworkEntry();
    reworkEntry.status = "ERROR";

    mockDynamo.getLatestReworkRequestByIun = async (iun) => {
      assert.strictEqual(iun, 'TEST-IUN-123');
      return reworkEntry;
    };

    mockDynamo.appendReceivedStatusCode = async () => {
      callAppend = true;
    };

    mockSqs.sendToQueue = async () => {
      callSqs = true;
    };

    mockDynamo.insertEventsError = async (...args) => {
      errorInserted = true;
      capturedArgs = args;
    };

    // Reload handler with updated mocks
    handleEvent = proxyquire('../app/eventHandler', {
      './dynamo': mockDynamo,
      './sqs': mockSqs,
      './log': mockLog
    }).handleEvent;

    const event = { Records: [createValidRecord()] };
    const result = await handleEvent(event);

    assert.deepStrictEqual(result, {"status": "ok"});
    assert.strictEqual(errorInserted, true);
    assert.strictEqual(capturedArgs[3], `La richiesta di rework è in uno stato non valido: ERROR`);
    assert.strictEqual(callAppend,false, 'appendReceivedStatusCode should not be called');
    assert.strictEqual(callSqs, false, 'sendToQueue should not be called');
  });

  it("should reject rework with DeliveryFailureCause not valid", async () => {
    let callAppend = false;
    let callSqs = false;
    let errorInserted = false;
    let capturedArgs = [];
    let reworkEntry = createReworkEntry();
    reworkEntry.expectedStatusCodes.push({ statusCode: 'RECAG011A', attachments: ['AR'] });

    mockDynamo.getLatestReworkRequestByIun = async (iun) => {
      assert.strictEqual(iun, 'TEST-IUN-123');
      return reworkEntry;
    };

    mockDynamo.appendReceivedStatusCode = async () => {
      callAppend = true;
    };

    mockSqs.sendToQueue = async () => {
      callSqs = true;
    };

    mockDynamo.insertEventsError = async (...args) => {
      errorInserted = true;
      capturedArgs = args;
    };

    handleEvent = proxyquire('../app/eventHandler', {
      './dynamo': mockDynamo,
      './sqs': mockSqs,
      './log': mockLog
    }).handleEvent;

    const record = createValidRecord({
      analogMail: { 
        statusCode: 'RECAG011A',
        deliveryFailureCause: 'INVALID_CAUSE'
      }
    });
    const event = { Records: [record] };
    const result = await handleEvent(event);

    assert.deepStrictEqual(result, {"status": "ok"});
    assert.strictEqual(errorInserted, true);
    assert.strictEqual(capturedArgs[3], 'DeliveryFailureCause non valida.');
    assert.strictEqual(callAppend, false, 'appendReceivedStatusCode should not be called');
    assert.strictEqual(callSqs, false, 'sendToQueue should not be called');
  });
});

describe("handleEvent - Status Code Validation", () => {
  beforeEach(() => {
    setupMocks();
  });

  it("should reject unexpected status codes", async () => {
    let appendCalled = false;
    let queueCalled = false;
    let errorInserted = false;
    const reworkEntry = createReworkEntry();
    const message = "Evento non atteso. Gli eventi accettati sono: " + JSON.stringify(reworkEntry.expectedStatusCodes);

    // Set up mocks before loading handler
    mockDynamo.getLatestReworkRequestByIun = async (iun) => {
      assert.strictEqual(iun, 'TEST-IUN-123');
      return reworkEntry;
    };

    mockDynamo.appendReceivedStatusCode = async () => {
      appendCalled = true;
    };

    mockSqs.sendToQueue = async () => {
      queueCalled = true;
    };

    mockDynamo.insertEventsError = async (...args) => {
      errorInserted = true;
      capturedArgs = args;
    };

    // Reload handler with updated mocks
    handleEvent = proxyquire('../app/eventHandler', {
      './dynamo': mockDynamo,
      './sqs': mockSqs,
      './log': mockLog
    }).handleEvent;

    const event = { Records: [createInvalidRecord()] };
    const result = await handleEvent(event);

     assert.deepStrictEqual(result, {"status": "ok"});
    assert.strictEqual(errorInserted, true);
    assert.strictEqual(capturedArgs[3], message);
    assert.strictEqual(appendCalled,false, 'appendReceivedStatusCode should not be called');
    assert.strictEqual(queueCalled, false, 'sendToQueue should not be called');
  });

  it("should reject stolen/lost/damaged events", async () => {
     let appendCalled = false;
    let queueCalled = false;
    let errorInserted = false;
    const reworkEntry = createReworkEntry();
    process.env.INVALID_STATUS_CODE = 'RECAG001';

    // Set up mocks before loading handler
    mockDynamo.getLatestReworkRequestByIun = async (iun) => {
      assert.strictEqual(iun, 'TEST-IUN-123');
      return reworkEntry;
    };

    mockDynamo.appendReceivedStatusCode = async () => {
      appendCalled = true;
    };

    mockSqs.sendToQueue = async () => {
      queueCalled = true;
    };

    mockDynamo.insertEventsError = async (...args) => {
      errorInserted = true;
      capturedArgs = args;
    };

    // Reload handler with updated mocks
    handleEvent = proxyquire('../app/eventHandler', {
      './dynamo': mockDynamo,
      './sqs': mockSqs,
      './log': mockLog
    }).handleEvent;

    let record = createValidRecord();
    const event = { Records: [record] };
    const result = await handleEvent(event);

     assert.deepStrictEqual(result, {"status": "ok"});
    assert.strictEqual(errorInserted, true);
    assert.strictEqual(capturedArgs[3], "Evento di furto, smarrimento o deterioramento");
    assert.strictEqual(appendCalled,false, 'appendReceivedStatusCode should not be called');
    assert.strictEqual(queueCalled, false, 'sendToQueue should not be called');
  });
});