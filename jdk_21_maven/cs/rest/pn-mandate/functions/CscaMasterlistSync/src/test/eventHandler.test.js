const { expect } = require('chai');
const sinon = require('sinon');
const crypto = require('crypto');
const proxyquire = require('proxyquire').noCallThru();
const path = require('path');

// Polyfill fetch for Node.js < 18 (used in CodeBuild with Node 16)
if (!global.fetch) {
  const nodeFetch = require('node-fetch');
  global.fetch = nodeFetch;
  global.Headers = nodeFetch.Headers;
  global.Request = nodeFetch.Request;
  global.Response = nodeFetch.Response;
}

// Mock global fetch
let fetchStub;

// Mock AWS service module
const awsServiceMock = {
  getSsmParameter: sinon.stub(),
  updateSsmParameter: sinon.stub(),
  saveFileToS3: sinon.stub(),
  updateEcsService: sinon.stub()
};

const eventHandlerPath = path.resolve(__dirname, '../app/eventHandler');

describe('eventHandler', () => {
  let originalEnv;
  let handler;

  beforeEach(() => {
    originalEnv = { ...process.env };
    process.env.CSCA_MASTERLIST_URL = 'https://example.com/masterlist.zip';
    process.env.SHA256_SSM_PARAMETER_NAME = '/test/sha256';
    process.env.S3_BUCKET_NAME = 'test-bucket';
    process.env.S3_OBJECT_KEY = 'test/masterlist.zip';
    process.env.ECS_CLUSTER_NAME = 'mock-cluster';
    process.env.ECS_SERVICE_NAME = 'mock-service';

    // Mock global fetch
    fetchStub = sinon.stub(global, 'fetch');

    // Load handler with injected mocks
    const handlerModule = proxyquire(eventHandlerPath, {
      './awsService': awsServiceMock
    });
    handler = handlerModule.handler;
  });

  afterEach(() => {
    process.env = originalEnv;
    sinon.restore();
    fetchStub.restore();
    awsServiceMock.getSsmParameter.reset();
    awsServiceMock.updateSsmParameter.reset();
    awsServiceMock.saveFileToS3.reset();
    awsServiceMock.updateEcsService.reset();
  });

  describe('successful scenarios', () => {
    it('should return NOT_MODIFIED when SHA256 is unchanged', async () => {
      const fileContent = Buffer.from('existing content');
      const existingSha256 = crypto.createHash('sha256').update(fileContent).digest('hex');
      
      fetchStub.resolves({
        ok: true,
        status: 200,
        arrayBuffer: () => Promise.resolve(fileContent.buffer.slice(
          fileContent.byteOffset, 
          fileContent.byteOffset + fileContent.byteLength
        ))
      });

      awsServiceMock.getSsmParameter.withArgs('/test/sha256').resolves(existingSha256);

      const result = await handler({}, {});

      expect(result.status).to.equal('NOT_MODIFIED');
      expect(result.sha256).to.equal(existingSha256);
      expect(awsServiceMock.saveFileToS3.called).to.be.false;
      expect(awsServiceMock.updateSsmParameter.called).to.be.false;
      expect(awsServiceMock.updateEcsService.called).to.be.false;
    });

    it('should download, save new file, and trigger ECS redeploy when SHA256 is different', async () => {
      const fileContent = Buffer.from('new file content');
      const oldSha256 = 'old_hash_value';
      const newSha256 = crypto.createHash('sha256').update(fileContent).digest('hex');
      
      const mockEcsResponse = {
        service: {
          serviceArn: 'arn:aws:ecs:region:account:service/mock-cluster/mock-service',
          deployments: [{ id: 'ecs-svc/123456' }]
        }
      };

      fetchStub.resolves({
        ok: true,
        status: 200,
        arrayBuffer: () => Promise.resolve(fileContent.buffer.slice(
          fileContent.byteOffset, 
          fileContent.byteOffset + fileContent.byteLength
        ))
      });

      awsServiceMock.getSsmParameter.withArgs('/test/sha256').resolves(oldSha256);
      awsServiceMock.saveFileToS3.resolves();
      awsServiceMock.updateSsmParameter.resolves();
      awsServiceMock.updateEcsService.withArgs('mock-cluster', 'mock-service').resolves(mockEcsResponse);

      const result = await handler({}, {});

      expect(result.status).to.equal('SUCCESS');
      expect(result.newSha256).to.equal(newSha256);
      expect(result.ecsServiceArn).to.equal(mockEcsResponse.service.serviceArn);
      expect(result.ecsDeploymentId).to.equal('ecs-svc/123456');
      expect(awsServiceMock.saveFileToS3.calledOnce).to.be.true;
      expect(awsServiceMock.updateSsmParameter.calledOnceWith('/test/sha256', newSha256)).to.be.true;
      expect(awsServiceMock.updateEcsService.calledOnceWith('mock-cluster', 'mock-service')).to.be.true;
    });

    it('should handle first run and trigger ECS redeploy when no previous SHA256 exists', async () => {
      const fileContent = Buffer.from('first download content');
      const newSha256 = crypto.createHash('sha256').update(fileContent).digest('hex');
      
      const mockEcsResponse = {
        service: {
          serviceArn: 'arn:aws:ecs:region:account:service/mock-cluster/mock-service',
          deployments: [{ id: 'ecs-svc/789012' }]
        }
      };

      fetchStub.resolves({
        ok: true,
        status: 200,
        arrayBuffer: () => Promise.resolve(fileContent.buffer.slice(
          fileContent.byteOffset, 
          fileContent.byteOffset + fileContent.byteLength
        ))
      });

      awsServiceMock.getSsmParameter.withArgs('/test/sha256').resolves(null);
      awsServiceMock.saveFileToS3.resolves();
      awsServiceMock.updateSsmParameter.resolves();
      awsServiceMock.updateEcsService.resolves(mockEcsResponse);

      const result = await handler({}, {});

      expect(result.status).to.equal('SUCCESS');
      expect(result.newSha256).to.equal(newSha256);
      expect(result.ecsServiceArn).to.equal(mockEcsResponse.service.serviceArn);
      expect(awsServiceMock.saveFileToS3.calledOnce).to.be.true;
      expect(awsServiceMock.updateSsmParameter.calledOnceWith('/test/sha256', newSha256)).to.be.true;
      expect(awsServiceMock.updateEcsService.calledOnce).to.be.true;
    });
  });

  describe('error scenarios', () => {
    it('should throw error when CSCA required environment variables are missing', async () => {
      delete process.env.CSCA_MASTERLIST_URL;
      
      // Clear module cache
      Object.keys(require.cache).forEach(key => {
        if (key.includes('eventHandler')) {
          delete require.cache[key];
        }
      });
      
      try {
        const { handler: freshHandler } = proxyquire(eventHandlerPath, {
          './awsService': awsServiceMock
        });
        await freshHandler({}, {});
        expect.fail('Expected an error to be thrown');
      } catch (error) {
        expect(error.message).to.include('Missing required environment variables');
        expect(error.message).to.include('CSCA_MASTERLIST_URL');
      }
    });

    it('should throw error when ECS required environment variables are missing', async () => {
      delete process.env.ECS_CLUSTER_NAME;
      
      // Clear module cache
      Object.keys(require.cache).forEach(key => {
        if (key.includes('eventHandler')) {
          delete require.cache[key];
        }
      });
      
      try {
        const { handler: freshHandler } = proxyquire(eventHandlerPath, {
          './awsService': awsServiceMock
        });
        await freshHandler({}, {});
        expect.fail('Expected an error to be thrown');
      } catch (error) {
        expect(error.message).to.include('Missing required environment variables');
        expect(error.message).to.include('ECS_CLUSTER_NAME');
      }
    });

    it('should throw error when download fails with non-retryable status', async () => {
      fetchStub.resolves({ 
        ok: false, 
        status: 404,
        text: () => Promise.resolve('Not Found')
      });

      try {
        await handler({}, {});
        expect.fail('Expected an error to be thrown');
      } catch (error) {
        expect(error.message).to.include("HTTP error! status: 404");
        expect(fetchStub.calledOnce).to.be.true;
      }
    });

    it('should propagate AWS S3 errors', async () => {
      const fileContent = Buffer.from('test content');
      fetchStub.resolves({
        ok: true,
        status: 200,
        arrayBuffer: () => Promise.resolve(fileContent.buffer.slice(
          fileContent.byteOffset, 
          fileContent.byteOffset + fileContent.byteLength
        ))
      });

      awsServiceMock.getSsmParameter.resolves('old_sha');
      awsServiceMock.saveFileToS3.rejects(new Error('S3 access denied'));

      try {
        await handler({}, {});
        expect.fail('Expected an error to be thrown');
      } catch (error) {
        expect(error.message).to.equal('S3 access denied');
      }
    });

    it('should propagate AWS SSM errors', async () => {
      const fileContent = Buffer.from('test content');
      fetchStub.resolves({
        ok: true,
        status: 200,
        arrayBuffer: () => Promise.resolve(fileContent.buffer.slice(
          fileContent.byteOffset, 
          fileContent.byteOffset + fileContent.byteLength
        ))
      });

      awsServiceMock.getSsmParameter.rejects(new Error('SSM access denied'));

      try {
        await handler({}, {});
        expect.fail('Expected an error to be thrown');
      } catch (error) {
        expect(error.message).to.equal('SSM access denied');
      }
    });

    it('should propagate ECS errors after successful S3 and SSM updates', async () => {
      const fileContent = Buffer.from('test content');
      fetchStub.resolves({
        ok: true,
        status: 200,
        arrayBuffer: () => Promise.resolve(fileContent.buffer.slice(
          fileContent.byteOffset, 
          fileContent.byteOffset + fileContent.byteLength
        ))
      });

      awsServiceMock.getSsmParameter.resolves('old_sha');
      awsServiceMock.saveFileToS3.resolves();
      awsServiceMock.updateSsmParameter.resolves();
      awsServiceMock.updateEcsService.rejects(new Error('ECS service not found'));

      try {
        await handler({}, {});
        expect.fail('Expected an error to be thrown');
      } catch (error) {
        expect(error.message).to.equal('ECS service not found');
        expect(awsServiceMock.saveFileToS3.calledOnce).to.be.true;
        expect(awsServiceMock.updateSsmParameter.calledOnce).to.be.true;
      }
    });
  });

  describe('retry logic', () => {
    it('should retry on retryable status codes and succeed on second attempt', async () => {
      const fileContent = Buffer.from('success content');
      const mockEcsResponse = {
        service: {
          serviceArn: 'arn:aws:ecs:region:account:service/mock-cluster/mock-service',
          deployments: []
        }
      };

      fetchStub.onFirstCall().resolves({ 
        ok: false, 
        status: 500,
        text: () => Promise.resolve('Internal Server Error')
      });
      
      fetchStub.onSecondCall().resolves({
        ok: true,
        status: 200,
        arrayBuffer: () => Promise.resolve(fileContent.buffer.slice(
          fileContent.byteOffset, 
          fileContent.byteOffset + fileContent.byteLength
        ))
      });

      awsServiceMock.getSsmParameter.resolves(null);
      awsServiceMock.updateSsmParameter.resolves();
      awsServiceMock.saveFileToS3.resolves();
      awsServiceMock.updateEcsService.resolves(mockEcsResponse);

      const result = await handler({}, {});

      expect(result.status).to.equal('SUCCESS');
      expect(fetchStub.calledTwice).to.be.true;
      expect(awsServiceMock.updateEcsService.calledOnce).to.be.true;
    });

    it('should fail after maximum retry attempts with retryable errors', async () => {
      const networkError = new Error('Connection reset');
      networkError.code = 'ECONNRESET';
      fetchStub.rejects(networkError);

      try {
        await handler({}, {});
        expect.fail('Expected an error to be thrown');
      } catch (error) {
        expect(error.message).to.equal('Connection reset');
        expect(fetchStub.calledThrice).to.be.true;
      }
    });

    it('should retry on timeout errors', async () => {
      const fileContent = Buffer.from('success after timeout');
      const mockEcsResponse = {
        service: {
          serviceArn: 'arn:aws:ecs:region:account:service/mock-cluster/mock-service',
          deployments: []
        }
      };

      const timeoutError = new Error('The operation was aborted due to timeout');
      timeoutError.name = 'AbortError';
      
      fetchStub.onFirstCall().rejects(timeoutError);
      fetchStub.onSecondCall().resolves({
        ok: true,
        status: 200,
        arrayBuffer: () => Promise.resolve(fileContent.buffer.slice(
          fileContent.byteOffset, 
          fileContent.byteOffset + fileContent.byteLength
        ))
      });

      awsServiceMock.getSsmParameter.resolves(null);
      awsServiceMock.updateSsmParameter.resolves();
      awsServiceMock.saveFileToS3.resolves();
      awsServiceMock.updateEcsService.resolves(mockEcsResponse);

      const result = await handler({}, {});

      expect(result.status).to.equal('SUCCESS');
      expect(fetchStub.calledTwice).to.be.true;
      expect(awsServiceMock.updateEcsService.calledOnce).to.be.true;
    });
  });
});