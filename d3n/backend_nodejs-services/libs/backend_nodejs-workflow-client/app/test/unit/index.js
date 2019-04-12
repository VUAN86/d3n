//process.env.VALIDATE_FULL_MESSAGE='false';
var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000';

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeEventService = require('../classes/FakeEventService.js');
var FakeRegistryService = require('../classes/FakeRegistryService.js');
var FakeWorkflowService = require('../classes/FakeWorkflowService.js');
var DefaultService = require('nodejs-default-service');
var logger = require('nodejs-logger')();
var WorkflowClient = require('../../../index.js');
var assert = require('chai').assert;
var fs = require('fs');
var Errors = require('../../../app/config/errors.js');
var Constants = require('../../../app/config/constants.js');
var sinon = require('sinon');
var sinonSandbox;
var ProtocolMessage = require('nodejs-protocol');

//var serviceRegistryInstancesConfig = [];
var instancesConfig = [];

// service registry instances
_.each(registryServiceURIs.split(','), function (uri) {
    var hostConfig = nodeUrl.parse(uri);
    instancesConfig.push({
        ip: hostConfig.hostname,
        port: hostConfig.port,
        secure: hostConfig.protocol === 'wss:',
        serviceName: 'serviceRegistry',
        registryServiceURIs: '',
        
        class: FakeRegistryService
    });
});

// other instances
instancesConfig.push({
    ip: ip,
    port: port+1,
    secure: true,
    serviceName: 'workflow',
    registryServiceURIs: registryServiceURIs,
    
    class: FakeWorkflowService
});

instancesConfig.push({
    ip: ip,
    port: port+2,
    secure: true,
    serviceName: 'event',
    registryServiceURIs: registryServiceURIs,
    
    class: FakeEventService
});


for(var i=0; i<instancesConfig.length; i++) {
    instancesConfig[i].key = fs.readFileSync(__dirname + '/../ssl-certificate/key.pem', 'utf8');
    instancesConfig[i].cert = fs.readFileSync(__dirname + '/../ssl-certificate/cert.pem', 'utf8');
    instancesConfig[i].auth = {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/../jwt-keys/pubkey.pem', 'utf8')
    };
    instancesConfig[i].validateFullMessage=true;
}


var workflowClient = null;
var workflowService = null;
var concurrency = 50;

var serviceInstances = [];


var testDataStartSuccess = [
    {
        taskId: 'question-1',
        taskType: 'Publish',
        userId: '111',
        tenantId: '222',
        description: 'question-1-description',
        parameters: {
            key1: 'val1',
            key2: 'val2'
        }
    },
    {
        taskId: 'question-2',
        taskType: 'Archive',
        userId: '111',
        tenantId: '222'
    }
];

var testDataStartError = [
    {
        taskId: null,
        taskType: 'Publish',
        userId: '111',
        tenantId: '222',
        description: 'question-1-description',
        parameters: {
            key1: 'val1',
            key2: 'val2'
        }
    },
    {
        taskId: 'question-2',
        taskType: null,
        userId: '111',
        tenantId: '222'
    }
];

var testDataPerformSuccess = [
    {
        taskId: 'question-1',
        actionType: 'Publish',
        userId: '111',
        tenantId: '222',
        parameters: {
            key1: 'val1',
            key2: 'val2'
        }
    },
    {
        taskId: 'question-2',
        actionType: 'Archive',
        userId: '111',
        tenantId: '222'
    }
];

var testDataPerformError = [
    {
        taskId: null,
        actionType: 'Publish',
        userId: '111',
        tenantId: '222',
        parameters: {
            key1: 'val1',
            key2: 'val2'
        }
    },
    {
        taskId: 'question-2',
        actionType: null,
        userId: '111',
        tenantId: '222'
    }
];


describe('TEST WORKFLOW CLIENT', function() {
    this.timeout(20000);
    
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });
    
    it('create service instances', function (done) {
        
        function createInstances(configs, cb) {
            async.mapSeries(configs, function (config, cbItem) {
                var cls = config.class;
                delete config.class;
                var inst = new cls(config);
                if (config.serviceName === 'workflow') {
                    workflowService = inst;
                }
                inst.build(function (err) {
                    logger.debug('build err:', err);
                    cbItem(err);
                });
            }, cb);
        };
        createInstances(instancesConfig, done);
    });
    
    it('create user message client instance', function () {
        workflowClient = new WorkflowClient({
            registryServiceURIs: registryServiceURIs
        });
    });

    it('start success', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var wsStartSpy = sinonSandbox.spy(workflowService.messageHandlers, "start");
        async.mapSeries(testDataStartSuccess, function (item, cbItem) {
            wsStartSpy.reset();
            workflowClient.start(
                item.taskId,
                item.taskType,
                item.userId,
                item.tenantId,
                item.description,
                item.parameters,
                function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        assert.strictEqual(wsStartSpy.callCount, 1);
                        assert.deepEqual(wsStartSpy.getCall(0).args[0].getContent(), item);
                        return cbItem();
                    } catch (e) {
                        return cbItem(e);
                    }
                }
            );
        }, done);

    });
    
    it('start validation error', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var wsStartSpy = sinonSandbox.spy(workflowService.messageHandlers, "start");
        async.mapSeries(testDataStartError, function (item, cbItem) {
            wsStartSpy.reset();
            workflowClient.start(
                item.taskId,
                item.taskType,
                item.userId,
                item.tenantId,
                item.description,
                item.parameters,
                function (err, response) {
                    try {
                        assert.instanceOf(err, Error);
                        assert.isUndefined(response);
                        assert.strictEqual(wsStartSpy.callCount, 0);
                        assert.strictEqual(err.message, Errors.ERR_VALIDATION_FAILED.message);
                        return cbItem();
                    } catch (e) {
                        return cbItem(e);
                    }
                }
            );
        }, done);
    });
    
    it('start error already started', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var wsStartSpy = sinonSandbox.stub(workflowService.messageHandlers, "start", function (message, clientSession) {
            var response = new ProtocolMessage(message);
            response.setContent(null);
            response.setError({
                type: 'client',
                message: 'ERR_TASK_ALREADY_STARTED'
            });
            clientSession.sendMessage(response);
        });
        
        workflowClient.start(
            'taskId',
            'Publish',
            '111',
            '222',
            null,
            null,
            function (err, response) {
                try {
                    assert.instanceOf(err, Error);
                    assert.strictEqual(err.message, 'ERR_TASK_ALREADY_STARTED');
                    return done();
                } catch (e) {
                    return done(e);
                }
            }
        );
    });
    
    
    it('perform success', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var wsPerformSpy = sinonSandbox.spy(workflowService.messageHandlers, "perform");
        async.mapSeries(testDataPerformSuccess, function (item, cbItem) {
            wsPerformSpy.reset();
            workflowClient.perform(
                item.taskId,
                item.actionType,
                item.userId,
                item.tenantId,
                item.parameters,
                function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        assert.strictEqual(wsPerformSpy.callCount, 1);
                        assert.deepEqual(wsPerformSpy.getCall(0).args[0].getContent(), item);
                        return cbItem();
                    } catch (e) {
                        return cbItem(e);
                    }
                }
            );
        }, done);

    });
    
    it('perform validation error', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var wsPerformSpy = sinonSandbox.spy(workflowService.messageHandlers, "perform");
        async.mapSeries(testDataPerformError, function (item, cbItem) {
            wsPerformSpy.reset();
            workflowClient.perform(
                item.taskId,
                item.actionType,
                item.userId,
                item.tenantId,
                item.parameters,
                function (err, response) {
                    try {
                        assert.instanceOf(err, Error);
                        assert.isUndefined(response);
                        assert.strictEqual(wsPerformSpy.callCount, 0);
                        assert.strictEqual(err.message, Errors.ERR_VALIDATION_FAILED.message);
                        return cbItem();
                    } catch (e) {
                        return cbItem(e);
                    }
                }
            );
        }, done);
    });
    
    it('perform error no rights', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var wsPerformSpy = sinonSandbox.stub(workflowService.messageHandlers, "perform", function (message, clientSession) {
            var response = new ProtocolMessage(message);
            response.setContent(null);
            response.setError({
                type: 'client',
                message: 'ERR_NO_RIGHTS'
            });
            clientSession.sendMessage(response);
        });
        
        workflowClient.perform(
            'taskId',
            'Publish',
            '111',
            '222',
            null,
            function (err, response) {
                try {
                    assert.instanceOf(err, Error);
                    assert.strictEqual(err.message, 'ERR_NO_RIGHTS');
                    return done();
                } catch (e) {
                    return done(e);
                }
            }
        );
    });
    
    
    it('state success', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var wsStateSpy = sinonSandbox.spy(workflowService.messageHandlers, "state");
        var taskId = 'the task id';
        workflowClient.state(
            taskId,
            function (err, response) {
                try {
                    assert.ifError(err);
                    assert.isNull(response.getError());
                    assert.strictEqual(wsStateSpy.callCount, 1);
                    assert.deepEqual(wsStateSpy.getCall(0).args[0].getContent().taskId, taskId);
                    return done();
                } catch (e) {
                    return done(e);
                }
            }
        );
    });
    
    it('state error', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var wsStateSpy = sinonSandbox.stub(workflowService.messageHandlers, "state", function (message, clientSession) {
            var response = new ProtocolMessage(message);
            response.setContent(null);
            response.setError({
                type: 'client',
                message: 'ERR_NO_SUCH_TASK'
            });
            clientSession.sendMessage(response);
        });
        
        var taskId = 'the task id';
        workflowClient.state(
            taskId,
            function (err, response) {
                try {
                    assert.instanceOf(err, Error);
                    assert.strictEqual(err.message, 'ERR_NO_SUCH_TASK');
                    return done();
                } catch (e) {
                    return done(e);
                }
            }
        );
    });
    
    it('abort success', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var wsAbortSpy = sinonSandbox.spy(workflowService.messageHandlers, "abort");
        var taskId = 'the task id';
        workflowClient.abort(
            taskId,
            function (err, response) {
                try {
                    assert.ifError(err);
                    assert.isNull(response.getError());
                    assert.strictEqual(wsAbortSpy.callCount, 1);
                    assert.deepEqual(wsAbortSpy.getCall(0).args[0].getContent(), {taskId: taskId});
                    return done();
                } catch (e) {
                    return done(e);
                }
            }
        );
    });
    
    it('abort error', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var wsAbortSpy = sinonSandbox.stub(workflowService.messageHandlers, "abort", function (message, clientSession) {
            var response = new ProtocolMessage(message);
            response.setContent(null);
            response.setError({
                type: 'client',
                message: 'ERR_NO_SUCH_TASK'
            });
            clientSession.sendMessage(response);
        });
        
        var taskId = 'the task id';
        workflowClient.abort(
            taskId,
            function (err, response) {
                try {
                    assert.instanceOf(err, Error);
                    assert.strictEqual(err.message, 'ERR_NO_SUCH_TASK');
                    return done();
                } catch (e) {
                    return done(e);
                }
            }
        );
    });
    
    it('onStateChange success', function (done) {
        var eventContent = {
            triggeredAction: 'val_triggeredActionType'
        };
        
        workflowClient.onStateChange(function (data) {
            try {
                assert.deepEqual(eventContent, data);
                return done();
            } catch (e) {
                return done(e);
            }
        }, function (err) {
            try {
                assert.ifError(err);
                setTimeout(function () {
                    // simulate event service emit event
                    workflowClient._eventClient.emit('event:' + Constants.WORKFLOW_STATE_CHANGE, eventContent);
                }, 500);
            } catch (e) {
                return done(e);
            }
            
        });
    });
    
});