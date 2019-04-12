var _ = require('lodash'),
    url = require('url');


/**
 * @typedef {object} ServiceRegistryClient.Config.Element
 * @property {{ip: string, port: number}} service
 * @property {boolean} secure
 */

/**
 * @typedef {ServiceRegistryClient.Config.Element[]} ServiceRegistryClient.Config
 */

var Config = {
    fakeHost: {
        service: {
            ip: 'localhost',
            port: process.env.PORT || '9200'
        },
        secure: false,
        key: undefined,
        cert: undefined
    },
    
    SERVICE_REGISTRY_NAME: 'serviceRegistry'
};

Config.hosts = function () {
    var hosts = 'ws' + (Config.fakeHost.secure ? 's' : '') + '://' + Config.fakeHost.service.ip + ':' + Config.fakeHost.service.port,
        config = [];
    if (process.env.REGISTRY_SERVICE_URIS && process.env.REGISTRY_SERVICE_URIS !== 'undefined' && process.env.REGISTRY_SERVICE_URIS !== 'null') {
        hosts = process.env.REGISTRY_SERVICE_URIS;
    }
    hosts = hosts.split(',');
    _.each(hosts, function (host) {
        var hostConfig = url.parse(host);
        config.push({
            service: {
                ip: hostConfig.hostname,
                port: hostConfig.port
            },
            secure: hostConfig.protocol === 'wss:'
        });
    });
    return config;
};

Config.fakeHostURI = function () {
    return 'ws' + (Config.fakeHost.secure ? 's' : '') + '://' + Config.fakeHost.service.ip + ':' + Config.fakeHost.service.port;
};

module.exports = Config;