var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeConnection = KeyvalueService.Models.AerospikeConnection;

describe('Connection', function () {
    describe('Connection AS check', function () {
        it('HAVE #getStatus', function () {
            AerospikeConnection.should.be.have.property('getStatus');
            AerospikeConnection.getStatus.should.be.a.Function;
        });
    });

    describe('Connection AS #getStatus', function () {
        it('test getStatus OK', function (done) {
            AerospikeConnection.getStatus(function (err, status) {
                if (err) return done(err);
                should(status).be.equal('OK');
                done();
            });
        });
    });
});
