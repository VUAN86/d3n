var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePoolIndex = KeyvalueService.Models.AerospikePoolIndex;

describe('PoolIndex', function () {
    describe('PoolIndex AS check', function () {
        it('HAVE #create', function () {
            AerospikePoolIndex.should.be.have.property('create');
            AerospikePoolIndex.create.should.be.a.Function;
        });
        it('HAVE #update', function () {
            AerospikePoolIndex.should.be.have.property('update');
            AerospikePoolIndex.update.should.be.a.Function;
        });
        it('HAVE #remove', function () {
            AerospikePoolIndex.should.be.have.property('remove');
            AerospikePoolIndex.remove.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikePoolIndex.should.be.have.property('findOne');
            AerospikePoolIndex.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikePoolIndex.should.be.have.property('toJSON');
            AerospikePoolIndex.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikePoolIndex.prototype.should.be.have.property('copy');
            AerospikePoolIndex.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikePoolIndex.prototype.should.be.have.property('save');
            AerospikePoolIndex.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikePoolIndex.prototype.should.be.have.property('remove');
            AerospikePoolIndex.prototype.remove.should.be.a.Function;
        });
    });
});
