/**
 * Log strutturato per operazioni
 * @param {string} level - Livello di log
 * @param {string} messageId - ID del messaggio
 * @param {object} details - Dettagli aggiuntivi
 */
function logOperation(level, messageId, details = {}){
  const logData = {
    level,
    messageId,
    ...details,
  };
  if(level === "FATAL"){
    console.fatal(JSON.stringify(logData));
  }
  else if (level === "ERROR") {
    console.error(JSON.stringify(logData));
  } else if (level === "DEBUG") {
    console.debug(JSON.stringify(logData));
  } else {
    console.log(JSON.stringify(logData));
  }
}

module.exports = {
    logOperation
}
