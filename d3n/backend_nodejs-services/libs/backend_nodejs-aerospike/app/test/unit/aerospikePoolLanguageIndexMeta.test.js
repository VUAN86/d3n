var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePoolLanguageIndexMeta = KeyvalueService.Models.AerospikePoolLanguageIndexMeta;

describe('Pool Language Index Meta', function () {
    describe('Pool Language Index Meta AS check', function () {
        it('HAVE #increment', function () {
            AerospikePoolLanguageIndexMeta.should.be.have.property('increment');
            AerospikePoolLanguageIndexMeta.create.should.be.a.Function;
        });
    });
});
