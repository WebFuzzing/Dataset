const { S3Client, PutObjectCommand } = require("@aws-sdk/client-s3");
const { SSMClient, GetParameterCommand, PutParameterCommand } = require("@aws-sdk/client-ssm");
const { ECSClient, UpdateServiceCommand } = require("@aws-sdk/client-ecs");

// Clients initialized outside the handler 
const s3Client = new S3Client({});
const ssmClient = new SSMClient({});
const ecsClient = new ECSClient({});

/**
 * Retrieves a parameter from AWS SSM Parameter Store.
 * @param {string} parameterName - The name of the parameter to retrieve.
 * @returns {Promise<string|null>} The value of the parameter, or null if not found.
 */
async function getSsmParameter(parameterName) {
  try {
    const response = await ssmClient.send(new GetParameterCommand({
      Name: parameterName,
      WithDecryption: false,
    }));
    return response.Parameter.Value;
  } catch (error) {
    if (error.name === 'ParameterNotFound') {
      console.log(`SSM Parameter "${parameterName}" not found. This is expected on first run.`);
      return null;
    }
    throw error;
  }
}

/**
 * Updates a parameter in AWS SSM Parameter Store.
 * @param {string} parameterName - The name of the parameter to update.
 * @param {string} value - The new value for the parameter.
 * @returns {Promise<void>}
 */
async function updateSsmParameter(parameterName, value) {
  await ssmClient.send(new PutParameterCommand({
    Name: parameterName,
    Value: value,
    Type: 'String',
    Overwrite: true,
    Tier: 'Standard',
  }));
  console.log(`Successfully updated SSM Parameter "${parameterName}".`);
}

/**
 * Saves a file buffer to an S3 bucket.
 * @param {string} bucketName - The name of the S3 bucket.
 * @param {string} objectKey - The key (path) for the object in the bucket.
 * @param {Buffer} fileBuffer - The content of the file to save.
 * @returns {Promise<void>}
 */
async function saveFileToS3(bucketName, objectKey, fileBuffer) {
  await s3Client.send(new PutObjectCommand({
    Bucket: bucketName,
    Key: objectKey,
    Body: fileBuffer,
    ContentType: 'application/zip' // Specific for the CSCA masterlist
  }));
  console.log(`Successfully saved file to S3 at s3://${bucketName}/${objectKey}`, {
    fileSize: fileBuffer.length
  });
}

/**
 * Triggers a forced redeployment of an ECS service.
 * @param {string} clusterName - The name of the ECS cluster.
 * @param {string} serviceName - The name of the ECS service.
 * @returns {Promise<object>} The ECS UpdateService response.
 */
async function updateEcsService(clusterName, serviceName) {
  const command = new UpdateServiceCommand({
    cluster: clusterName,
    service: serviceName,
    forceNewDeployment: true
  });

  const response = await ecsClient.send(command);
  
  console.log('ECS UpdateService command executed successfully', {
    serviceArn: response.service.serviceArn,
    status: response.service.status,
    desiredCount: response.service.desiredCount
  });

  return response;
}

module.exports = {
  getSsmParameter,
  updateSsmParameter,
  saveFileToS3,
  updateEcsService
};