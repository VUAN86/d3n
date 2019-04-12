var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;

module.exports = {
    USER_IDS: [
        '1111-222', 
        '333-444', 
        '555-888'
    ],
    CLIENT_SESSIONS: [
        { clientId: 'asdasa3dsfsd4', gatewayURL: 'wss://localhost:80' },
        { clientId: 'asda343343dsfsd4', gatewayURL: 'wss://localhost:81' },
        { clientId: 'asda343343dsfsd4wer', gatewayURL: 'wss://localhost:82' },
        {
            clientId: 'asad45dfe54', 
            gatewayURL: 'wss://localhost:82', 
            appConfig: {
                appId: 'appIdasdas',
                deviceUUID: 'asdsdevice uuid',
                tenantId: 'thetenantid1232',
                device: {
                    a: 'a',
                    b: 'b'
                }
            }
        },
        {
            clientId: '2asad45dfe54', 
            gatewayURL: 'wss://localhost:82', 
            appConfig: {
                appId: '2appIdasdas',
                deviceUUID: '2asdsdevice uuid',
                tenantId: '2thetenantid1232',
                device: {
                    a: '2a',
                    b: '2b'
                }
            }
        }
    ],

    loadAS: function (done) {
        var self = this;
        var service = KeyvalueService.load();
        _.forEach(self.USER_IDS, function (userId) {
            service.remove(AerospikeGlobalClientSession, userId, 'userId');
        });
        service.process(function (err) {
            if (err) {
                return done(err);
            }
            return done();
        });
    },
};
