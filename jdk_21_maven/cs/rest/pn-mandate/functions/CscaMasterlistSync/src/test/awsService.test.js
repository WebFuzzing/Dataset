const { expect } = require('chai');
const { mockClient } = require('aws-sdk-client-mock');
const { S3Client, PutObjectCommand } = require('@aws-sdk/client-s3');
const { SSMClient, GetParameterCommand, PutParameterCommand } = require('@aws-sdk/client-ssm');
const { ECSClient, UpdateServiceCommand } = require('@aws-sdk/client-ecs');
const awsService = require('../app/awsService');

const s3Mock = mockClient(S3Client);
const ssmMock = mockClient(SSMClient);
const ecsMock = mockClient(ECSClient);

describe('awsService', () => {
  afterEach(() => {
    s3Mock.reset();
    ssmMock.reset();
    ecsMock.reset();
  });

  describe('getSsmParameter', () => {
    it('should return the parameter value if it exists', async () => {
      ssmMock.on(GetParameterCommand).resolves({
        Parameter: { Value: 'test-value' }
      });

      const result = await awsService.getSsmParameter('/test/param');
      expect(result).to.equal('test-value');
    });

    it('should return null if the parameter is not found', async () => {
      const notFoundError = new Error('Parameter not found');
      notFoundError.name = 'ParameterNotFound';
      ssmMock.on(GetParameterCommand).rejects(notFoundError);

      const result = await awsService.getSsmParameter('/test/param');
      expect(result).to.be.null;
    });

    it('should re-throw other AWS SDK errors naturally', async () => {
      const accessError = new Error('Access denied');
      accessError.name = 'AccessDeniedException';
      ssmMock.on(GetParameterCommand).rejects(accessError);

      try {
        await awsService.getSsmParameter('/test/param');
        expect.fail('Should have thrown error');
      } catch (error) {
        expect(error.message).to.equal('Access denied');
      }
    });
  });

  describe('updateSsmParameter', () => {
    it('should call PutParameterCommand with correct parameters', async () => {
      ssmMock.on(PutParameterCommand).resolves({});

      await awsService.updateSsmParameter('/test/param', 'new-value');

      const calls = ssmMock.commandCalls(PutParameterCommand);
      expect(calls).to.have.lengthOf(1);
      expect(calls[0].args[0].input).to.deep.equal({
        Name: '/test/param',
        Value: 'new-value',
        Type: 'String',
        Overwrite: true,
        Tier: 'Standard'
      });
    });

    it('should let AWS SDK errors propagate naturally', async () => {
      const error = new Error('SSM error');
      ssmMock.on(PutParameterCommand).rejects(error);

      try {
        await awsService.updateSsmParameter('/test/param', 'value');
        expect.fail('Should have thrown error');
      } catch (err) {
        expect(err.message).to.equal('SSM error');
      }
    });
  });

  describe('saveFileToS3', () => {
    it('should call PutObjectCommand with correct parameters', async () => {
      s3Mock.on(PutObjectCommand).resolves({});
      const buffer = Buffer.from('test content');

      await awsService.saveFileToS3('test-bucket', 'test/key.zip', buffer);

      const calls = s3Mock.commandCalls(PutObjectCommand);
      expect(calls).to.have.lengthOf(1);
      expect(calls[0].args[0].input).to.deep.equal({
        Bucket: 'test-bucket',
        Key: 'test/key.zip',
        Body: buffer,
        ContentType: 'application/zip'
      });
    });

    it('should let AWS SDK errors propagate naturally', async () => {
      const error = new Error('S3 error');
      s3Mock.on(PutObjectCommand).rejects(error);

      try {
        await awsService.saveFileToS3('bucket', 'key', Buffer.from('test'));
        expect.fail('Should have thrown error');
      } catch (err) {
        expect(err.message).to.equal('S3 error');
      }
    });
  });

  describe('updateEcsService', () => {
    it('should call ECS UpdateService with correct parameters', async () => {
      const mockResponse = {
        service: {
          serviceArn: 'arn:aws:ecs:region:account:service/cluster/service',
          status: 'ACTIVE',
          desiredCount: 2,
          deployments: [{ id: 'ecs-svc/123' }]
        }
      };

      ecsMock.on(UpdateServiceCommand).resolves(mockResponse);

      const result = await awsService.updateEcsService('mock-cluster', 'mock-service');

      expect(result).to.deep.equal(mockResponse);

      const calls = ecsMock.commandCalls(UpdateServiceCommand);
      expect(calls).to.have.lengthOf(1);
      expect(calls[0].args[0].input).to.deep.equal({
        cluster: 'mock-cluster',
        service: 'mock-service',
        forceNewDeployment: true
      });
    });

    it('should return ECS service response with deployment info', async () => {
      const mockResponse = {
        service: {
          serviceArn: 'arn:aws:ecs:region:account:service/cluster/service',
          status: 'ACTIVE',
          desiredCount: 3,
          deployments: [
            { 
              id: 'ecs-svc/456',
              status: 'PRIMARY',
              desiredCount: 3
            }
          ]
        }
      };

      ecsMock.on(UpdateServiceCommand).resolves(mockResponse);

      const result = await awsService.updateEcsService('cluster', 'service');

      expect(result.service.serviceArn).to.equal('arn:aws:ecs:region:account:service/cluster/service');
      expect(result.service.deployments).to.have.lengthOf(1);
      expect(result.service.deployments[0].id).to.equal('ecs-svc/456');
    });

    it('should propagate ServiceNotFoundException', async () => {
      const ecsError = new Error('Service not found');
      ecsError.name = 'ServiceNotFoundException';
      
      ecsMock.on(UpdateServiceCommand).rejects(ecsError);

      try {
        await awsService.updateEcsService('cluster', 'non-existent');
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).to.equal('Service not found');
        expect(error.name).to.equal('ServiceNotFoundException');
      }
    });

    it('should propagate ClusterNotFoundException', async () => {
      const clusterError = new Error('Cluster not found');
      clusterError.name = 'ClusterNotFoundException';
      
      ecsMock.on(UpdateServiceCommand).rejects(clusterError);

      try {
        await awsService.updateEcsService('non-existent', 'service');
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).to.equal('Cluster not found');
        expect(error.name).to.equal('ClusterNotFoundException');
      }
    });

    it('should propagate AccessDeniedException', async () => {
      const accessError = new Error('User is not authorized to perform: ecs:UpdateService');
      accessError.name = 'AccessDeniedException';
      
      ecsMock.on(UpdateServiceCommand).rejects(accessError);

      try {
        await awsService.updateEcsService('cluster', 'service');
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).to.include('not authorized');
        expect(error.name).to.equal('AccessDeniedException');
      }
    });

    it('should propagate generic ECS errors', async () => {
      const genericError = new Error('Internal service error');
      genericError.name = 'InternalServerException';
      
      ecsMock.on(UpdateServiceCommand).rejects(genericError);

      try {
        await awsService.updateEcsService('cluster', 'service');
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).to.equal('Internal service error');
        expect(error.name).to.equal('InternalServerException');
      }
    });
  });
});