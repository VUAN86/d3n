var _ = require('lodash');
var logger = require('nodejs-logger')();

function NewrelicMetrics(config) {
    this._config = config || {};
    
    this._sendMetricsInterval = this._config.sendMetricsInterval || process.env.NEWRELIC_SEND_METRICS_INTERVAL || 5000;
    
    this._cleanupInterval = this._config.cleanupInterval || process.env.NEWRELIC_CLEANUP_INTERVAL || 1000*60*30; // run clean up every 30 minutes
    
    this._cleanupOlderThan = this._config.cleanupOlderThan || 1000*60*30; // remove metrics older than 30 minutes
    
    this._timeStart = {};
    
    this._metricsValuesBuffer = {};
    
    if (global.serviceNewrelic) {
        setTimeout(this._sendMetrics.bind(this), this._sendMetricsInterval);
        setTimeout(this._cleanup.bind(this), this._cleanupInterval);
    }
}

var o = NewrelicMetrics.prototype;


o._cleanup = function () {
    try {
        
        var idsToDelete = [];
        for(var id in this._timeStart) {
            if ((this._timeStart[id] + this._cleanupOlderThan) < Date.now()) {
                idsToDelete.push(id);
            }
        }
        
        for(var i=0; i<idsToDelete.length; i++) {
            delete this._timeStart[idsToDelete[i]];
        }
        
        logger.debug('NewrelicMetrics._cleanup() success:', idsToDelete);
        setTimeout(this._cleanup.bind(this), this._cleanupInterval);
    } catch (e) {
        setTimeout(this._cleanup.bind(this), this._cleanupInterval);
        logger.error('NewrelicMetrics._cleanup() error:', e);
    }
};

o._sendMetrics = function () {
    try {
        var self = this;
        var newrelic = self._getNewrelic();
        
        for(var metricName in self._metricsValuesBuffer) {
            var metrics = self._metricsValuesBuffer[metricName];
            if (!metrics.length) {
                continue;
            }
            
            var value = {
                count: metrics.length,
                total: _.sum(metrics),
                min: _.min(metrics),
                max: _.max(metrics),
                sumOfSquares: _.sumBy(metrics, function (val) {
                    return val*val;
                })
            };
            
            newrelic.recordMetric(metricName, value);
            logger.debug('Metrics._sendMetrics recordMetric:', metricName, value);
        }
        
        self._metricsValuesBuffer = {};
        
        logger.debug('Metrics._sendMetrics metrics sent succesfully');
        setTimeout(self._sendMetrics.bind(self), self._sendMetricsInterval);
        
    } catch (e) {
        logger.error('Metrics._sendMetrics error:', e);
        setTimeout(self._sendMetrics.bind(self), self._sendMetricsInterval);
    }
};

o._getNewrelic = function () {
    return global.serviceNewrelic || null;
};

o._metricNameByMessage = function(message) {
    return 'Custom/ApiHandler/' + message.getMessage().replace(/Response$/, '').replace(/\//g, '.');
};

o._generateRecordId = function(metricName, seq, clientId) {
    return metricName + '_seq_' + seq + '_client_' + clientId;
};

o.apiHandlerStart = function (message) {
    try {
        var self = this;
        var newrelic = self._getNewrelic();
        
        if (!newrelic) {
            return;
        }
        
        var metricName = self._metricNameByMessage(message);
        var clientId = message.getClientId() || 'noclient';
        var recordId = self._generateRecordId(metricName, message.getSeq(), clientId);
        
        self.startRecord(recordId, metricName);
        
    } catch (e) {
        logger.error('Metrics.apiHandlerStart() error on handling:', e, JSON.stringify(message));
    }
};

o.apiHandlerEnd = function (message) {
    try {
        var self = this;
        var newrelic = self._getNewrelic();
        
        if (!newrelic) {
            return;
        }
        
        var metricName = self._metricNameByMessage(message);

        var ack = message.getAck();
        if (!_.isArray(ack)) {
            logger.debug('Metrics.apiHandlerEnd() SEND ',JSON.stringify(message.messageContainer.message,'  without ack '));
            return;
        }
        
        var clientId = message.getClientId() || 'noclient';
        for(var i=0; i<ack.length; i++) {
            var recordId = self._generateRecordId(metricName, ack[i], clientId);
            self.endRecord(recordId, metricName);
        }
        
    } catch (e) {
        logger.error('Metrics.apiHandlerEnd() error on handling:', e, JSON.stringify(message));
    }
};


o.startRecord = function (recordId, metricName) {
    try {
        var self = this;
        var newrelic = self._getNewrelic();
        
        if (!newrelic) {
            return;
        }
        
        if (!_.has(self._metricsValuesBuffer, metricName)) {
            self._metricsValuesBuffer[metricName] = [];
        }
        
        if (_.has(self._timeStart, recordId)) {
            logger.error('Metrics.startRecord() recordId="',recordId,'" already exists, metricName="', metricName,'"');
            return;
        }
        
        self._timeStart[recordId] = Date.now();
        
    } catch (e) {
        logger.error('Metrics.startRecord() error on handling:', e, recordId, metricName);
    }
};

o.endRecord = function (recordId, metricName) {
    try {
        var self = this;
        var newrelic = self._getNewrelic();
        
        if (!newrelic) {
            return;
        }
        
        if (!_.has(self._timeStart, recordId)) {
            logger.error('Metrics.endRecord() recordId not started:', recordId, metricName);
            return;
        }

        if (!_.has(self._metricsValuesBuffer, metricName)) {
            self._metricsValuesBuffer[metricName] = [];
        }
        self._metricsValuesBuffer[metricName].push(Date.now()-self._timeStart[recordId]);

        delete self._timeStart[recordId];
        
    } catch (e) {
        logger.error('Metrics.endRecord() error on handling:', e, recordId, metricName);
    }
};


module.exports = NewrelicMetrics;