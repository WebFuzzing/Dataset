const { handleEvent } = require("./src/app/eventHandler.js");

async function handler(event, context) {
  console.info("New Execution scheduled ", event);
  return handleEvent(event, context);
}

exports.handler = handler;
