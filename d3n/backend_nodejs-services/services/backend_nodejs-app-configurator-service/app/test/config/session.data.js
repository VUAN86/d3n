var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Aerospike = require('nodejs-aerospike').getInstance(Config);
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var logger = require('nodejs-logger')();

module.exports = {
    CLIENT_SESSION: {
        clientId: DataIds.CLIENT_SESSION_ID,
        gatewayURL: 'wss://localhost:80',
        appConfig:   {
            appId: 'appid',
            deviceUUID: 'deviceUUID',
            tenantId: DataIds.TENANT_1_ID,
            device: {
                a: 'aa',
                b: 'bb'
            }
        }
    },

    loadAerospike: function (done) {
        return done();
    },

    cleanAerospike: function (done) {
        return done();
    },

};
