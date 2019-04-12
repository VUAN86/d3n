var async = require('async');
var logger = require('nodejs-logger')();

function DatabasesStatisticsProvider(config) {
    this._config = config;
};

var o = DatabasesStatisticsProvider.prototype;

o.generateStatistics = function (cb) {
    try {
        var self = this;
        var connectionsToDb = {
            "mysql": "NA",
            "aerospike": "NA",
            "elastic": "NA",
            "spark": "NA"
        };
        async.series([
            // check mysql/sequelize connection
            function (next) {
                try {
                    if (!self._config.mysqlInstance) {
                        return setImmediate(next);
                    }
                    self._config.mysqlInstance.RdbmsService._storage
                        .authenticate()
                        .then(function() {
                            connectionsToDb.mysql = 'OK';
                            return next();
                        })
                        .catch(function (err) {
                            logger.error('DatabasesStatisticsProvider.generateStatistics() mysql error:', err);
                            connectionsToDb.mysql = 'NOK';
                            return next();
                        });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // check aerospike
            function (next) {
                try {
                    if (!self._config.aerospikeConnectionModel) {
                        return setImmediate(next);
                    }
                    
                    return self._config.aerospikeConnectionModel.getStatus(function (err, status) {
                        if (err) {
                            return next(err);
                        }
                        connectionsToDb.aerospike = status;
                        return next();
                    });

                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // check elasticsearch
            function (next) {
                try {
                    if (!self._config.elasticsearchService) {
                        return setImmediate(next);
                    }
                    
                    self._config.elasticsearchService.getConnectionStatus(function(err, status) {
                        if (err) {
                            return next(err);
                        }
                        connectionsToDb.elastic = status;
                        return next();
                    });
                    
                    
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
            
        ], function (err) {
            if (err) {
                logger.error('DatabasesStatisticsProvider.generateStatistics() error:', err);
                return cb(err);
            }
            return cb(false, {
                connectionsToDb: connectionsToDb
            });
        });
        
    } catch (e) {
        logger.error('DatabasesStatisticsProvider.generateStatistics() trycatch error:', e);
        return setImmediate(cb, e);
    }
};

module.exports = DatabasesStatisticsProvider;