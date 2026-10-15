const { getLatestReworkRequestByIun, appendReceivedStatusCode, insertEventsError } = require('../app/dynamo');
const { DynamoDBDocumentClient, QueryCommand, UpdateCommand, PutCommand } = require("@aws-sdk/lib-dynamodb");
const { mockClient } = require('aws-sdk-client-mock');
const assert = require('assert');

const ddbMock = mockClient(DynamoDBDocumentClient);

describe("getLatestReworkRequestByIun", () => {
  beforeEach(() => {
    ddbMock.reset();
  });

  it("returns the latest rework request when items are found", async () => {
    ddbMock.on(QueryCommand).resolves({
      Items: [{ iun: "test-iun", reworkId: "rework-123" , recIndex: "RECINDEX_0", createdAt: "2025-11-27T11:48:17.194069270Z" },
        { iun: "test-iun", reworkId: "rework-124" , recIndex: "RECINDEX_0", createdAt: "2025-11-27T14:52:17.194069270Z" },
        { iun: "test-iun", reworkId: "rework-125" , recIndex: "RECINDEX_0", createdAt: "2025-11-27T19:48:17.194069270Z"},
      ]
    });

    const result = await getLatestReworkRequestByIun("test-iun", "PREPARE_ANALOG_DOMICILE.IUN_12345.RECINDEX_0.ATTEMPT_0");

    assert(ddbMock.commandCalls(QueryCommand).length == 1);
    assert.deepStrictEqual(result.reworkId, "rework-125");
  });

  it("returns null when no items are found", async () => {
    ddbMock.on(QueryCommand).resolves({ Items: [] });

    const result = await getLatestReworkRequestByIun("test-iun");

    assert(ddbMock.commandCalls(QueryCommand).length == 1);
    assert.strictEqual(result, null);
  });
});

describe("appendReceivedStatusCode", () => {
  beforeEach(() => {
    ddbMock.reset();
  });

  it("appends a new received status code entry", async () => {
    ddbMock.on(UpdateCommand).resolves({});

    await appendReceivedStatusCode( "test-iun", "rework-123",{ statusCode: "RECRN001A", attachments: [], statusDateTime: '2025-05-01'}, null);

    assert(ddbMock.commandCalls(UpdateCommand).length == 1);
  });

   it("appends a new received status code entry with error", async () => {
      const analogMail = {
        requestId: "req-123",
        clientRequestTimeStamp: "2023-01-01T00:00:00Z",
        statusCode: 500,
        statusDateTime: "2023-01-01T00:00:00Z",
        productType: "type-a",
        deliveryFailureCause: "cause-a",
        attachments: ["file1"],
        registeredLetterCode: "code-123"
      };
      ddbMock.on(UpdateCommand).rejects({ name: "ConditionalCheckFailedException" });
      await appendReceivedStatusCode( "test-iun", "rework-123",{ statusCode: "RECRN001A", attachments: [], statusDateTime: '2025-05-01'}, analogMail);

      assert(ddbMock.commandCalls(UpdateCommand).length == 1);
      assert(ddbMock.commandCalls(PutCommand).length == 1);
    });
});

describe("insertEventsError", () => {
  beforeEach(() => {
    ddbMock.reset();
  });

  it("inserts an event error into the table", async () => {
    ddbMock.on(PutCommand).resolves({});

    const analogMail = {
      requestId: "req-123",
      clientRequestTimeStamp: "2023-01-01T00:00:00Z",
      statusCode: 500,
      statusDateTime: "2023-01-01T00:00:00Z",
      productType: "type-a",
      deliveryFailureCause: "cause-a",
      attachments: ["file1"],
      registeredLetterCode: "code-123"
    };

    await insertEventsError("test-iun", "rework-123", analogMail, "Error message");

    assert(ddbMock.commandCalls(PutCommand).length == 1);
  });
});
