var DefaultServiceStatisticsProvider = require('./DefaultServiceStatisticsProvider.js');
var async = require('async');
var _ = require('lodash');
var logger = require('nodejs-logger')();

function StatisticsMonitor(config) {
    this._service = config.service;
    
    this._sendStatisticsInterval = config.sendStatisticsInterval;
    
    // keep statistics providers
    this._providers = [];
    
    // by default add default service provider
    this.addProvider(new DefaultServiceStatisticsProvider(config.service));
    
}
var o = StatisticsMonitor.prototype;


o.addProvider = function (provider) {
    this._providers.push(provider);
};

o.generateStatistics = function (cb) {
    try {
        var self = this;
        var statistics = {};
        
        // collect statistics provided by all providers
        async.mapSeries(self._providers, function (provider, cbItem) {
            provider.generateStatistics(function (errProvider, statisticsProvider) {
                if (errProvider) {
                    return cbItem(errProvider);
                }
                
                statistics = _.assign(statistics, statisticsProvider);
                return cbItem();
            });
        }, function (err) {
            if (err) {
                logger.error('StatisticsMonitor.generateStatistics() err:', err);
                return cb(err);
            }
            return cb(false, statistics);
        });
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o.startSendStatistics = function () {
    this._sendStatistics();
};

o._sendStatistics = function () {
    logger.debug('StatisticsMonitor._sendStatistics() called');
    
    var self = this;
    function _doSendStatistics(cbSendStatistics) {
        try {
            var cbCalled = false;
            var timeoutId;
            function callCb(err, data) {
                if (cbCalled === false) {
                    cbCalled = true;
                    clearTimeout(timeoutId);
                    cbSendStatistics(err, data);
                }
            };
            
            timeoutId = setTimeout(function () {
                callCb(new Error('ERR_TIMED_OUT'));
            }, 10000);
            
            self.generateStatistics(function (err, statistics) {
                if (err) {
                    return callCb(err);
                }
                
                self._service.getServiceRegistry().pushServiceStatistics(statistics, function (errPush) {
                    if (errPush) {
                        return callCb(errPush);
                    }
                    
                    return callCb(false, statistics);
                });
            });
        } catch (e) {
            return setImmediate(callCb, e);
        }
    };
    
    _doSendStatistics(function (err, statisticsSent) {
        if (err) {
            logger.error('StatisticsMonitor._sendStatistics() error:', err);
        } else {
            logger.debug('StatisticsMonitor._sendStatistics() success:', statisticsSent);
        }
        
        setTimeout(self._sendStatistics.bind(self), self._sendStatisticsInterval);
    });
};

module.exports = StatisticsMonitor;