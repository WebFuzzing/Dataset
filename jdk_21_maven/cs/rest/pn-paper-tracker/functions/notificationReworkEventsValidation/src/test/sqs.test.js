const { test, describe, beforeEach, mock } = require('node:test');
const assert = require('node:assert');
const { sendToQueue, createMessageAttributes } = require('../app/sqs');
const { SQSClient } = require("@aws-sdk/client-sqs");

describe("sendToQueue", () => {
  test("sends a message to the specified SQS queue", async () => {
    const queueUrl = "https://example-queue-url";
    const messageBody = { key: "value" };
    const messageAttributes = { attr: { DataType: "String", StringValue: "test" } };

    const sendMock = mock.fn();
    sendMock.mock.mockImplementation(() => Promise.resolve({ MessageId: "12345" }));
    SQSClient.prototype.send = sendMock;

    const result = await sendToQueue(queueUrl, messageBody, messageAttributes);

    assert.strictEqual(sendMock.mock.callCount(), 1);
    const callArgs = sendMock.mock.calls[0].arguments[0];
    assert.strictEqual(callArgs.input.QueueUrl, queueUrl);
    assert.strictEqual(callArgs.input.MessageBody, JSON.stringify(messageBody));
    assert.deepStrictEqual(callArgs.input.MessageAttributes, messageAttributes);
    assert.deepStrictEqual(result, { MessageId: "12345" });
  });

  test("throws an error if the SQS client fails", async () => {
    const queueUrl = "https://example-queue-url";
    const messageBody = { key: "value" };
    const messageAttributes = { attr: { DataType: "String", StringValue: "test" } };

    const sendMock = mock.fn();
    sendMock.mock.mockImplementation(() => Promise.reject(new Error("SQS error")));
    SQSClient.prototype.send = sendMock;

    await assert.rejects(
      () => sendToQueue(queueUrl, messageBody, messageAttributes),
      { message: "SQS error" }
    );
  });
});

describe("createMessageAttributes", () => {
  test("creates message attributes with reworkId", async () => {
    const originalAttributes = {
      attr1: { DataType: "String", StringValue: "value1" },
    };
    const reworkId = "rework-123";

    const result = await createMessageAttributes(originalAttributes, reworkId);

    assert.deepStrictEqual(result, {
      attr1: { DataType: "String", StringValue: "value1" },
      reworkId: { DataType: "String", StringValue: "rework-123" },
    });
  });

  test("returns an empty object if no original attributes are provided", async () => {
    const reworkId = "rework-123";

    const result = await createMessageAttributes({}, reworkId);

    assert.deepStrictEqual(result, {
      reworkId: { DataType: "String", StringValue: "rework-123" },
    });
  });

  test("overrides existing reworkId attribute if present in original attributes", async () => {
    const originalAttributes = {
      reworkId: { DataType: "String", StringValue: "old-rework-id" },
    };
    const reworkId = "new-rework-id";

    const result = await createMessageAttributes(originalAttributes, reworkId);

    assert.deepStrictEqual(result, {
      reworkId: { DataType: "String", StringValue: "new-rework-id" },
    });
  });
});
