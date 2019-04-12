var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePoolMeta = KeyvalueService.Models.AerospikePoolMeta;

describe('Pool Meta', function () {
    describe('Pool Meta AS check', function () {
        it('HAVE #increment', function () {
            AerospikePoolMeta.should.be.have.property('increment');
            AerospikePoolMeta.create.should.be.a.Function;
        });
    });
});
