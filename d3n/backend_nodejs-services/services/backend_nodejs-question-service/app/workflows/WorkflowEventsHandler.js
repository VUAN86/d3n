var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var WorkflowClient = require('nodejs-workflow-client');
var QuestionWorkflow = require('./QuestionWorkflow.js');
var QuestionTranslationWorkflow = require('./QuestionTranslationWorkflow.js');
var QuestionTranslationUnpublishWorkflow = require('./QuestionTranslationUnpublishWorkflow.js');
var QuestionUnpublishWorkflow = require('./QuestionUnpublishWorkflow.js');
var QuestionArchiveWorkflow = require('./QuestionArchiveWorkflow.js');
var _service = null;

var workflowClient = new WorkflowClient({
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
});


var taskTypeToWorkflow = {
    'Question': QuestionWorkflow,
    'QuestionTranslation': QuestionTranslationWorkflow,
    'QuestionTranslationUnpublish': QuestionTranslationUnpublishWorkflow,
    'QuestionUnpublish': QuestionUnpublishWorkflow,
    'QuestionArchive': QuestionArchiveWorkflow
};

function onStateChangeHandler (eventContent) {
    try {
        if (!eventContent) {
            logger.warn('WorkflowEventsHandler empty event content:', eventContent);
            return;
        }
        var arr = eventContent.taskId.split('_');

        var workflow = arr[0];
        if (!_.has(taskTypeToWorkflow, workflow)) {
            logger.error('WorkflowEventsHandler no workflow for event content:', eventContent);
            return;
        }
        var taskId = parseInt(arr[1]);
        var inst = taskTypeToWorkflow[workflow].getInstance(taskId);
        inst._service = _service;
        inst._onStateChange(eventContent);
    } catch (e) {
        logger.error('WorkflowEventsHandler catched error:', e);
    }
};


module.exports = {
    start: function (service, cb) {
        try {
            _service = service;
            workflowClient.onStateChange(onStateChangeHandler, cb);
        } catch (e) {
            return setImmediate(cb);
        }
    }
};