var _ = require('lodash');
var logger = require('nodejs-logger')();

function DefautlServiceStatisticsProvider(service) {
    this._service = service;
}

var o = DefautlServiceStatisticsProvider.prototype;

o.generateStatistics = function (cb) {
    try {
        var self = this;
        
        var statistics = {};
        
        // service name
        statistics.serviceName = self._service._serviceName;
        
        // service uri
        statistics.uri = 'ws' + (self._service.config.secure === true ? 's' : '') + '://' + self._service.config.externIp + ':' + self._service.config.port;
        
        // uptime
        statistics.uptime = parseInt(process.uptime()*1000);
        
        // memory usage
        statistics.memoryUsage = process.memoryUsage().heapUsed;
        
        // cpu usage
        statistics.cpuUsage = null;
        if (_.isFunction(process.cpuUsage)) {
            statistics.cpuUsage = process.cpuUsage().user;
        }
        
        
        var connections = self._service._connections;
        
        // check if service is connected to gateway
        statistics.countGatewayConnections = 0;
        for(var i=0; i<connections.length; i++) {
            var conn = connections[i];
            if (conn.serviceName === 'gateway' /*&& conn.wsConnection.connected*/) {
                statistics.countGatewayConnections++;
            }
        }
        
        // count connections from other services
        statistics.countConnectionsToService = connections.length;
        
        statistics.connectionsToService = [];
        for(var i=0; i<connections.length; i++) {
            var conn = connections[i];
            statistics.connectionsToService.push({
                serviceName: conn.serviceName,
                address: conn.wsConnection.requestData.remoteAddress,
                port: parseInt(conn.wsConnection.requestData.remotePort)
            });
        }        
        
        // count of sessions
        statistics.countSessionsToService = 0;
        for(var i=0; i<connections.length; i++) {
            var conn = connections[i];
            statistics.countSessionsToService += _.keys(conn.wsConnection.session.getClientSessions()).length;
        }
        
        // connections from service: to be implemented
        statistics.countConnectionsFromService = null;
        
        // to be implemented
        statistics.connectionsFromService = [];
        
        var ts = self._service.getServiceRegistry().getLastHeartbeatTimestamp();
        statistics.lastHeartbeatTimestamp = (ts === null ? null : parseInt(ts/1000));
        
        return setImmediate(cb, false, statistics);
        
    } catch (e) {
        logger.error('DefautlServiceStatisticsProvider.generateStatistics() catch error:', e);
        return setImmediate(cb, e);
    }
};

module.exports = DefautlServiceStatisticsProvider;