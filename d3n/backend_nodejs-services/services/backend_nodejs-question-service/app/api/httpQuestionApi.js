var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ProtocolMessage = require('nodejs-protocol');
var RegionalSettingApiFactory = require('./../factories/regionalSettingApiFactory.js');
var jwt = require('jsonwebtoken');
var logger = require('nodejs-logger')();
var fs = require('fs');
var zip = require('machinepack-zip');
var xlsxj = require("xlsx-to-json");

let imagesFolder = Config.httpUpload.tempFolder + '/images';



module.exports = {
    // POST
    loadImages: function (req, res, cb) {
        logger.debug("loadImages start")
        let images=[];
        let params = {};
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
                                    return next('ERR_TOKEN_NOT_VALID');

                                params.creatorResourceId = decoded.userId;
                                params.createDate = _.now();

                                return next(false);
                            }
                        );
                    } else {
                        return next('ERR_TOKEN_NOT_VALID');
                    }
                },
                function (next) {
                    if (_.has(req, 'file')) {
                        params._fileInfo = req.file;
                        logger.debug('httpQuestionApi/loadImages - file info being uploaded',
                            JSON.stringify(req.file));
                        params.fileName = params._fileInfo.originalname;
                        var tempFolder = Config.httpUpload.tempFolder;
                        params._originalTempPath = tempFolder + '/' + params.fileName;
                        return next(false);
                    }
                    return next('ERR_VALIDATION_FAILED');
                },
                function (next) {
                    zip.unzip({
                        source: params._fileInfo.path,
                        destination: imagesFolder,
                    }).exec({
                        error: function (err) {
                            logger.error(err);
                            return next('ERR_ARCHIVE_NOT_VALID');
                        },
                        success: function () {
                            images = fs.readdirSync(imagesFolder);
                            return next(false);
                        },
                    });
                }
            ], function (err) {
                if (err) {
                    logger.error('httpQuestionApi.loadImages global failure', err);
                    return _apiError('question/loadImages', err, res, cb);
                }
                if (params._fileInfo.path && fs.existsSync(params._fileInfo.path)) {
                    fs.unlinkSync(params._fileInfo.path)
                }

                var message = _message('question/loadImagesResponse');
                message.setContent({images: images});
                return _processData(err, message, res, cb);
            });
        } catch (e) {
            _apiError('question/loadImages', e, res, next);
        }
    },

    loadQuestions: function (req, res, cb) {
        logger.debug("loadQuestions start")
        let questions;
        let regionList = [];
        let langList = [];
        let params = {};
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
                                    return next('ERR_TOKEN_NOT_VALID');

                                params.creatorResourceId = decoded.userId;
                                params.createDate = _.now();

                                return next(false);
                            }
                        );
                    } else {
                        return next('ERR_TOKEN_NOT_VALID');
                    }
                },
                function (next) {
                    if (_.has(req, 'file')) {
                        params._fileInfo = req.file;
                        logger.debug('httpQuestionApi/loadImages - file info being uploaded',
                            JSON.stringify(req.file));
                        params.fileName = params._fileInfo.originalname;
                        var tempFolder = Config.httpUpload.tempFolder;
                        params._originalTempPath = tempFolder + '/' + params.fileName;
                        return next(false);
                    }
                    return next('ERR_VALIDATION_FAILED');
                },
                function (next) {
                    try {
                        xlsxj({
                            input: params._fileInfo.path,
                            output: 'output.json'
                        }, function (err, result) {
                            if (err) {
                                return next('ERR_FILE_CONVERTING');
                            } else {
                                questions = result.filter(item =>  item["Language"]);
                                return next();
                            }
                        });
                    } catch (e) {
                        logger.error('ERR_FILE_CONVERT',e);
                        return next('ERR_FILE_CONVERT');
                    }
                },
                function (next) {
                    RegionalSettingApiFactory.regionalSettingList({limit:0,offset:0},null,null,true,function (err, data) {
                        if (err) {
                            logger.error("regionalSettingList getting error:",err);
                            return next('ERR_REGIONAL_LIST_EXCEPTION')
                        }
                        data.items.forEach(function (item) {
                            regionList.push(item.iso);
                        });
                        return next();
                    });

                },
                function (next) {
                    RegionalSettingApiFactory.languageList({limit:0,offset:0}, null, null, true, function (err, data) {
                        if (err) {
                            logger.error("languageListSettingList getting error:",err);
                            return next('ERR_LANGUAGE_LIST_EXCEPTION')
                        }
                        data.items.forEach(function (item) {
                            langList.push(item.iso);
                        });
                        return next();
                    });

                },
                function (next) {
                    let errors = "";
                    if (questions, questions.length < 1) {
                        next('ERR_NO_QUESTIONS');
                    }
                    questions.forEach(function (question) {

                        if (question['INT/NAT'] !== "yes" && question['INT/NAT'] !== "no"
                            && question['INT / NAT'] !== "yes" && question['INT / NAT'] !== "no") {
                            errors += '\nInvalid INT/NAT, must be "yes" or "no"';
                        }

                        if (langList.indexOf(question["Language"].toLowerCase()) === -1) {
                            errors += '\nInvalid Language ' + question["Language"] + ' is not by ISO';
                        }

                        const regions = question["Region"].split(";")
                        regions.forEach(function (region) {
                            if (regionList.indexOf(region.toUpperCase()) === -1) {
                                errors += '\nInvalid region ' + region + ' is not by ISO';
                            }
                        });

                        const images = [];
                        if (question['Correct Pic']) {
                            images.push(question['Correct Pic']);
                        }
                        if (question['Wrong Pic 1']) {
                            images.push(question['Wrong Pic 1']);
                        }
                        if (question['Wrong Pic 2']) {
                            images.push(question['Wrong Pic 2']);
                        }
                        if (question['Wrong Pic 3']) {
                            images.push(question['Wrong Pic 3']);
                        }
                        if (question['Correct picture 1']) {
                            images.push(question['Correct picture 1']);
                        }
                        if (question['Correct picture 2']) {
                            images.push(question['Correct picture 2']);
                        }
                        if (question['Correct picture 3']) {
                            images.push(question['Correct picture 3']);
                        }
                        if (question['Correct picture 4']) {
                            images.push(question['Correct picture 4']);
                        }
                        if (question['Question Pic']) {
                            images.push(question['Question Pic']);
                        }
                        if (question['Resolution Pic']) {
                            images.push(question['Resolution Pic']);
                        }
                        if (images.length > 0) {
                            images.forEach(function (image) {
                                const filePath = getImagePath(image);
                                if (!filePath) {
                                    errors += '\n' + image + ' is not exist';
                                }
                            });
                        }
                    });
                    next(errors);
                }
            ], function (err) {
                if (err) {
                    logger.error('httpQuestionApi.loadQuestions global failure', err);
                    if (params._fileInfo.path && fs.existsSync(params._fileInfo.path)) {
                        fs.unlinkSync(params._fileInfo.path)
                    }
                    return _apiError('question/loadQuestions', err, res, cb);
                }
                var message = _message('question/loadQuestionsResponse');
                message.setContent({ questions: params._fileInfo.filename});
                return _processData(err, message, res, cb);
            });
        } catch (e) {
            _apiError('question/loadQuestions', e, res, next);
        }
    },

}

function getImagePath(filename) {
    const nameArray = filename.split(".");
    const hasExt = (nameArray[nameArray.length - 1] === "jpg") || (nameArray[nameArray.length - 1] === "png");

    let path = `${imagesFolder}/${filename}${hasExt ? "" : ".jpg"}`;
    let finalPath;
    if (fs.existsSync(path)) {
        console.log('return true')
        finalPath = path;
    } else {
        if (!hasExt) {
            path = `${imagesFolder}/${filename}${".png"}`;
            if (fs.existsSync(path)) {
                finalPath = path;
            }
        }
    }
    return finalPath;
}

function _message(api) {
    var pm = new ProtocolMessage();
    pm.setMessage(api);
    return pm;
}

function _processData(err, data, res, next) {
    console.log('_processData');
    console.log('err', err);
    console.log('data', data);
    console.log('res', res);
    console.log('next', next);
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
        error.setError(Errors.MediaApi.FatalError);
    } else {
        error.setError(err);
    }
    var message = error.getMessageContainer();
    res.status(401).send(message);
    res.end();
}
