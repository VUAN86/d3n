var _ = require('lodash');
var url = require('url');

module.exports = {
    
    parseURIs: function (URIs) {
        try {
            if (!_.isString(URIs)) {
                return [];
            }

            var hostsConfig = [];
            _.each(URIs.split(','), function (uri) {
                var hostConfig = url.parse(uri);
                hostsConfig.push({
                    service: {
                        ip: hostConfig.hostname,
                        port: hostConfig.port
                    },
                    secure: hostConfig.protocol === 'wss:'
                });
            });

            return hostsConfig;
            
        } catch (e) {
            return e;
        }
    },
    
    toWSUri: function (secure, ip, port) {
        return 'ws' + (secure === true ? 's' : '') + '://' + ip + ':' + port;
    }
};