var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeTaskLock = KeyvalueService.Models.AerospikeTaskLock;

module.exports = {
    TASK_IDS: [
        'check-tenant-money-2017-12-05', 
        'profile-sync2017-12-05T10:10:00', 
        'voucher-check-2017-12-05'
    ]
};
