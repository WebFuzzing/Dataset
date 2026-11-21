const { handler: eventHandler } = require('./src/app/eventHandler');

/**
 * AWS Lambda function entry point.
 *
 * @param {object} event - The event object from the trigger (e.g., EventBridge).
 * @param {object} context - The Lambda context object.
 * @returns {Promise<object>} The response object.
 */
exports.handler = async (event, context) => {
  console.log("CSCA Masterlist Sync Lambda invocation started", { event, context });
  return eventHandler(event, context);
};