var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var jwt = require('jsonwebtoken');
var logger = require('nodejs-logger')();
var StandardErrors = require('nodejs-errors');
var ProtocolMessage = require('nodejs-protocol');
var ejs = require('ejs');
var templatesDir = __dirname + '/../resources/templates/';
ejs.delimiter = '?';
module.exports = {
    'home': function (req, res) {
        try {
            var monitoringService = this;
            var html = '';
            var environment = req.query['env'] || 'development';
            var environmentStatistics;
            async.series([
                
                // validations
                function (next) {
                    try {
                        
                        if (_.keys(Config.envSegistryServiceURIs).indexOf(environment) === -1) {
                            return setImmediate(next, StandardErrors.ERR_VALIDATION_FAILED);
                        }
                        
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                
                // get env statistics
                function (next) {
                    try {
                        monitoringService.getEnvStatistics(environment, function (err, statistics) {
                            if (err) {
                                return next(err);
                            }
                            
                            environmentStatistics = statistics;
                            return next();
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                
                // render ejs template
                function (next) {
                    var dataStatistics = _.assign(environmentStatistics, {
                        environment: environment,
                        environments: _.keys(Config.envSegistryServiceURIs)
                    });
                    ejs.renderFile(templatesDir + 'page-home.ejs', {dataStatistics: dataStatistics}, {}, function(err, str){
                        if (err) {
                            return next(err);
                        }
                        html = str;
                        return next();
                    });                    
                }
            ], function (err) {
                if (err) {
                    return _htmlError(res, 'home', err, err.message);
                }
                return _htmlSuccess(res, html);
            });
        } catch (e) {
            _htmlError(res, 'home', e);
        }
    }
};

function _messageSuccess(res, content) {
    var m = new ProtocolMessage();
    m.setContent(content);
    
    res.send(m.getMessageContainer());
    res.end();
}

function _messageError(res, api, err) {
    logger.error('Monitoring _messageError():', api, err);
    var m = new ProtocolMessage();
    m.setContent(null);
    if (err instanceof Error) {
        m.setError(StandardErrors.ERR_FATAL_ERROR);
    } else {
        m.setError(err);
    }
    
    res.status(500).send(m.getMessageContainer());
    res.end();
}

function _htmlSuccess(res, data) {
    res.send(data);
    res.end();
}

function _htmlError(res, api, err, data) {
    logger.error('Monitoring _htmlError():', api, err);
    res.status(500).send(data || '');
    res.end();
}
