var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePool = KeyvalueService.Models.AerospikePool;

describe('Pool', function () {
    describe('Pool AS check', function () {
        it('HAVE #create', function () {
            AerospikePool.should.be.have.property('create');
            AerospikePool.create.should.be.a.Function;
        });
        it('HAVE #update', function () {
            AerospikePool.should.be.have.property('update');
            AerospikePool.update.should.be.a.Function;
        });
        it('HAVE #remove', function () {
            AerospikePool.should.be.have.property('remove');
            AerospikePool.remove.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikePool.should.be.have.property('findOne');
            AerospikePool.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikePool.should.be.have.property('toJSON');
            AerospikePool.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikePool.prototype.should.be.have.property('copy');
            AerospikePool.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikePool.prototype.should.be.have.property('save');
            AerospikePool.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikePool.prototype.should.be.have.property('remove');
            AerospikePool.prototype.remove.should.be.a.Function;
        });
    });
});
