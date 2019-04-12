var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var S3Client = require('nodejs-s3client').getInstance();

var Config = {
    serviceName: 'question',
    serviceNamespaces: [
        'question',
        'questionLifetime',
        'questionRating',
        'questionVoting',
        'questionTemplate',
        'workorder',
        'billing'
    ],
    serviceApi: path.join(__dirname, '..', 'api'),
    numCPUs: process.env.DEBUG ? 1 : require('os').cpus().length,
    ip: process.env.HOST || 'localhost',
    port: process.env.PORT || '9090',
    secure: true,
    key: fs.readFileSync(path.join(__dirname,  'ssl-certificate/key.pem'), 'utf8'),
    cert: fs.readFileSync(path.join(__dirname,  'ssl-certificate/cert.pem'), 'utf8'),
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS || 'ws://localhost:9093',
    addMediaServiceURIs: process.env.ADD_MEDIA_SERVICE_URIS || 'https://media.f4mgamesys.com/addMedia',
    validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
    validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
    protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    umWaitForResponse: _.isUndefined(process.env.USER_MESSAGE_WAIT_FOR_RESPONSE) ? true : (process.env.USER_MESSAGE_WAIT_FOR_RESPONSE === 'true'),
    entryBlockTimeSeconds: 2 * 60 * 60, // 2h
    backgroundProcesses: {
        // Question service jobs
        jobs: path.join(__dirname, '../', 'jobs'),
        // If true, run background processing servise and load job instances from CronMasterJob (app/jobs/*.js) classes
        enabled: true,
        // If true, run background processing servise as Node Child Process (currently not supported)
        asChildProcess: false,
        // If true, run GC explicitly on TICK_STARTED, prints out differences to console on TICK_COMPLETE - may take extra 20 seconds to process
        jobHeapDirrerences: false,
        // If true, exports heap snapshot first on TICK_STARTED, then second on TICK_COMPLETE
        jobHeapSnapshots: false,
    },
    memoryWatching: {
        // If true, run memory watching servise, which prints out GC state on change (state event) and monitors possible memory leaks (leak event) and then prints out heap snapshots
        enabled: false,
    },
    gateway: {
        ip: process.env.HOST || 'localhost',
        port: (parseInt(process.env.PORT) + 9) || '9099',
        secure: true,
        key: path.join(__dirname,  'ssl-certificate/key.pem'),
        cert: path.join(__dirname,  'ssl-certificate/cert.pem'),
        auth: {
            algorithm: 'RS256',
            publicKey: fs.readFileSync(path.join(__dirname,  'jwt-keys/pubkey.pem'), 'utf8'),
        },
    },
    auth: {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(path.join(__dirname,  'jwt-keys/pubkey.pem'), 'utf8'),
        privateKey: fs.readFileSync(path.join(__dirname,  'jwt-keys/privkey.pem'), 'utf8')
    },
    httpUpload: {
        tempFolder: process.platform === 'win32' ? os.tmpdir() : '/tmp' + '/questionLoad'
    },
    blob: {
        cipher: 'AES-128-CTR',
        iv: new Buffer([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]),
        inputBucket: process.env.BLOB_INPUT_BUCKET || S3Client.Config.buckets.question.blobInput,
        outputBucket: process.env.BLOB_BUCKET || S3Client.Config.buckets.question.blobOutput
    },
    userMessage: {
        workorderAssignEmail: {
            subject: 'workorder.assign.subject',
            message: 'workorder.assign.message',
        },
        workorderAssignSms: {
            message: 'workorder.assign.sms',
        },
        workorderActivateEmail: {
            subject: 'workorder.activate.subject',
            message: 'workorder.activate.message',
        },
        workflowQuestionActivateEmailAuthor: {
            subject: 'workflow.question.activate.email.subject',
            message: 'workflow.question.activate.email.author.message',
        },
        workflowQuestionActivateEmailAdmin: {
            subject: 'workflow.question.activate.email.subject',
            message: 'workflow.question.activate.email.admin.message',
        },
        workflowQuestionDeactivateEmailAuthor: {
            subject: 'workflow.question.deactivate.email.subject',
            message: 'workflow.question.deactivate.email.author.message',
        },
        workflowQuestionDeactivateEmailAdmin: {
            subject: 'workflow.question.deactivate.email.subject',
            message: 'workflow.question.deactivate.email.admin.message',
        },
        workflowQuestionTranslationActivateEmailAuthor: {
            subject: 'workflow.questionTranslation.activate.email.subject',
            message: 'workflow.questionTranslation.activate.email.author.message',
        },
        workflowQuestionTranslationActivateEmailAdmin: {
            subject: 'workflow.questionTranslation.activate.email.subject',
            message: 'workflow.questionTranslation.activate.email.admin.message',
        },
        workflowQuestionTranslationDeactivateEmailAuthor: {
            subject: 'workflow.questionTranslation.deactivate.email.subject',
            message: 'workflow.questionTranslation.deactivate.email.author.message',
        },
        workflowQuestionTranslationDeactivateEmailAdmin: {
            subject: 'workflow.questionTranslation.deactivate.email.subject',
            message: 'workflow.questionTranslation.deactivate.email.admin.message',
        },
    },
};

var Database = require('nodejs-database').getInstance();
Config = _.assign(Config, Database.Config);

module.exports = Config;

Config.httpURL = function (uri, testport) {
    var secure = !(process.env.TEST === 'true') && Config.secure;
    return 'http' + (secure ? 's' : '') + '://' + Config.ip + ':' + Config.port + (uri ? uri : '/');
}

Config.wsURL = function (uri, testport) {
    var secure = !(process.env.TEST === 'true') && Config.secure;
    return 'ws' + (secure ? 's' : '') + '://' + Config.ip + ':' + Config.port + (uri ? uri : '/');
}
