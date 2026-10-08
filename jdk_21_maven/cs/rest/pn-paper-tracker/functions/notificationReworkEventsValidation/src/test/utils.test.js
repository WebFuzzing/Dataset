const assert = require('assert');
const { checkExpected, checkAlreadyReceived, checkAttachments, checkDeliveryFailureCause, checkRejectedStatusCode, buildReceivedStatusCodeEntry } = require('../app/utils');

describe("checkExpected", () => {
  it("returns false if reworkEntry has no expectedStatusCodes", () => {
    const result = checkExpected({}, "RECRN001A", [], null);
    assert.strictEqual(result, false);
  });

  it("returns true if statusCode matches and attachments are a subset", () => {
     const reworkEntry = {
         expectedStatusCodes: [
           { statusCode: "RECRN001B", attachments: ["a", "b"] }
         ]
       };
       const result = checkAttachments(reworkEntry, "RECRN001B", ["a"]);
       assert.strictEqual(result, true);
  });

   it("returns false if statusCode matches but attachments is not present in entity", () => {
      const reworkEntry = {
        expectedStatusCodes: [
          { statusCode: "RECRN001B", attachments: [] }
        ]
      };
      const result = checkAttachments(reworkEntry, "RECRN001B", ["c"]);
      assert.strictEqual(result, false);
    });

    it("returns false if statusCode matches but attachments is not present in entity and there is another expected status code", () => {
      const reworkEntry = {
        expectedStatusCodes: [
          { statusCode: "RECRN001B", attachments: ["Plico"] },
          { statusCode: "RECRN001A", attachments: [] }
        ]
      };
      const result = checkAttachments(reworkEntry, "RECRN001B", []);
      assert.strictEqual(result, false);
    });

  it("returns false if statusCode matches but attachments are not a subset", () => {
    const reworkEntry = {
      expectedStatusCodes: [
        { statusCode: "RECRN001B", attachments: ["a", "b"] }
      ]
    };
    const result = checkAttachments(reworkEntry, "RECRN001B", ["c"]);
    assert.strictEqual(result, false);
  });

  it("returns false if statusCode matches but attachments are not a subset 2", () => {
      const reworkEntry = {
        expectedStatusCodes: [
          { statusCode: "RECRN001B", attachments: ["a", "b"] }
        ]
      };
      const result = checkAttachments(reworkEntry, "RECRN001B", ["a", "b", "c"]);
      assert.strictEqual(result, false);
    });

  it("returns true if deliveryFailureCause matches", () => {
    const reworkEntry = {
      expectedStatusCodes: [{ statusCode: "RECRN002A" }],
      expectedDeliveryFailureCause: "M02"
    };
    const result = checkDeliveryFailureCause(reworkEntry, "M02");
    assert.strictEqual(result, true);
  });

  it("returns false if deliveryFailureCause not matches", () => {
      const reworkEntry = {
        expectedStatusCodes: [{ statusCode: "RECRN002A" }],
        expectedDeliveryFailureCause: "M05"
      };
      const result = checkDeliveryFailureCause(reworkEntry, "M02");
      assert.strictEqual(result, false);
    });
});

describe("checkAlreadyReceived", () => {
  it("returns false if reworkEntry has no receivedStatusCodes", () => {
    const result = checkAlreadyReceived({}, "RECRN002A", [], "2023-01-01T00:00:00Z");
    assert.strictEqual(result, false);
  });

  it("returns true if statusCode, statusDateTime, and attachments match", () => {
    const reworkEntry = {
      receivedStatusCodes: [
        { statusCode: "RECRN002B", statusDateTime: "2023-01-01T00:00:00Z", attachments: ["a"] }
      ]
    };
    const result = checkAlreadyReceived(reworkEntry, "RECRN002B", ["a"], "2023-01-01T00:00:00Z");
    assert.strictEqual(result, true);
  });

  it("returns false if statusDateTime does not match", () => {
    const reworkEntry = {
      receivedStatusCodes: [
        { statusCode: "RECRN002B", statusDateTime: "2023-01-01T00:00:00Z", attachments: ["a"] }
      ]
    };
    const result = checkAlreadyReceived(reworkEntry, "RECRN002B", ["a"], "2023-01-02T00:00:00Z");
    assert.strictEqual(result, false);
  });

  it("returns false if attachments do not match", () => {
      const reworkEntry = {
        receivedStatusCodes: [
          { statusCode: "RECRN002B", statusDateTime: "2023-01-01T00:00:00Z", attachments: ["b"] }
        ]
      };
      const result = checkAlreadyReceived(reworkEntry, "RECRN002B", ["a"], "2023-01-01T00:00:00Z");
      assert.strictEqual(result, false);
    });

    it("returns false if attachments do not match2", () => {
      const reworkEntry = {
        receivedStatusCodes: [
          { statusCode: "RECRN002B", statusDateTime: "2023-01-01T00:00:00Z", attachments: ["a"] }
        ]
      };
      const result = checkAlreadyReceived(reworkEntry, "RECRN002B", ["a", "b"], "2023-01-01T00:00:00Z");
      assert.strictEqual(result, false);
    });

     it("returns false if attachments do not match3", () => {
          const reworkEntry = {
            receivedStatusCodes: [
              { statusCode: "RECRN002B", statusDateTime: "2023-01-01T00:00:00Z", attachments: ["a", "b"] }
            ]
          };
          const result = checkAlreadyReceived(reworkEntry, "RECRN002B", ["b"], "2023-01-01T00:00:00Z");
          assert.strictEqual(result, false);
        });
});

describe("checkRejectedStatusCode", () => {
  it("returns true if statusCode is in INVALID_STATUS_CODE", () => {
    process.env.INVALID_STATUS_CODE = "RECRN006,RECRI005";
    const result = checkRejectedStatusCode("RECRN006");
    assert.strictEqual(result, true);
  });

  it("returns false if statusCode is not in INVALID_STATUS_CODE", () => {
    process.env.INVALID_STATUS_CODE = "RECRN006,RECRI005";
    const result = checkRejectedStatusCode("RECRN002A");
    assert.strictEqual(result, false);
  });
});

describe("buildReceivedStatusCodeEntry", () => {
  it("creates an entry with statusCode, attachments, and statusDateTime", () => {
    const result = buildReceivedStatusCodeEntry("RECRN002B", ["a"], "2023-01-01T00:00:00Z");
    assert.deepStrictEqual(result, {
      statusCode: "RECRN002B",
      attachments: ["a"],
      statusDateTime: "2023-01-01T00:00:00Z"
    });
  });

  it("creates an entry with empty attachments if none are provided", () => {
    const result = buildReceivedStatusCodeEntry("RECRN002B", [], "2023-01-01T00:00:00Z");
    assert.deepStrictEqual(result, {
      statusCode: "RECRN002B",
      attachments: [],
      statusDateTime: "2023-01-01T00:00:00Z"
    });
  });
});
