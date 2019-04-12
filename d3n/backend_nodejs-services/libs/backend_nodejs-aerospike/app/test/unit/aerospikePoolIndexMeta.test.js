var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePoolIndexMeta = KeyvalueService.Models.AerospikePoolIndexMeta;

describe('Pool Index Meta', function () {
    describe('Pool Index Meta AS check', function () {
        it('HAVE #increment', function () {
            AerospikePoolIndexMeta.should.be.have.property('increment');
            AerospikePoolIndexMeta.create.should.be.a.Function;
        });
    });
});
