var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var UploadApiFactory = require('./../factories/uploadApiFactory.js');
var ProtocolMessage = require('nodejs-protocol');
var jwt = require('jsonwebtoken');
var uuid = require('uuid');
var logger = require('nodejs-logger')();
var StandardErrors = require('nodejs-errors');

module.exports = {
    // POST /addDocument
    addDocument: function (req, res, cb) {
        var params = {};
        var document = null;

        try {
            async.series([
                // Check jwt token
                function (next) {
                    if (_.has(req.body, 'token')) {
                        jwt.verify(
                            req.body.token,
                            Config.auth.publicKey,
                            { algorithms: [Config.auth.algorithm] },
                            function (err, decoded) {
                                if (err)
                                  return next(StandardErrors['ERR_TOKEN_NOT_VALID']);

                                return next(false);
                            }
                        );
                    } else {
                        return next(StandardErrors['ERR_TOKEN_NOT_VALID']);
                    }
                },
                // Check if there is a file_content
                function (next) {
                    if (_.has(req, 'file')) {

                        logger.debug('httpDocumentApi/addDocument - file info being uploaded',
                            JSON.stringify(req.file));

                        var path = req.file.path;
                        if (_.isUndefined(req.file.path)) {
                            logger.warn("httpDocumentApi:addDocument file path missing", JSON.stringify(req.file));
                            path = req.file.destination + "/" + req.file.filename;
                        }

                        params.path = req.file.path;
                        params.key = uuid.v4();

                        return next(false);
                    }
                    return next(StandardErrors['ERR_VALIDATION_FAILED']);
                },
                // Upload to storage
                function (next) {
                  try {
                    UploadApiFactory.uploadSecureStorage(params, next);
                  } catch (e) {
                    logger.error("httpDocumentApi.addDocument - upload error",
                      JSON.stringify(params), e);
                    return next(StandardErrors['ERR_FATAL_ERROR']);
                  }
                },
            ], function (err) {
                if (err) {
                    logger.error('httpDocumentApi.addDocument global failure', err);
                    return _apiError('media/addDocument', err, res, cb);
                }
                var message = _message('media/addDocumentResponse');
                message.setContent({ document: { key: params.key } });
                return _processData(err, message, res, cb);
            });
        } catch (e) {
            _apiError('media/addDocument', e, res, cb);
        }
    },
}

function _message(api) {
    var pm = new ProtocolMessage();
    pm.setMessage(api);
    return pm;
}

function _processData(err, data, res, next) {
    if (err) {
        var message = err.getMessageContainer();
        res.status(401).send(message);
        res.end();
        if (next) {
            next(true);
        }
    } else {
        var message = data.getMessageContainer();
        res.send(message);
        res.end();
        if (next) {
            next(false);
        }
    }
}

function _apiError(api, err, res, next) {
    var error = new ProtocolMessage();
    error.setMessage(api + 'Response');
    if (err && _.isObject(err) && _.has(err, 'stack')) {
        error.setError(StandardErrors['ERR_FATAL_ERROR']);
    } else {
        error.setError(err);
    }
    var message = error.getMessageContainer();
    res.status(401).send(message);
    res.end();
}
