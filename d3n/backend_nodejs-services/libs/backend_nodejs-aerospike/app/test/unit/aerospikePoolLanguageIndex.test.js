var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePoolLanguageIndex = KeyvalueService.Models.AerospikePoolLanguageIndex;

describe('PoolLanguageIndex', function () {
    describe('PoolLanguageIndex AS check', function () {
        it('HAVE #create', function () {
            AerospikePoolLanguageIndex.should.be.have.property('create');
            AerospikePoolLanguageIndex.create.should.be.a.Function;
        });
        it('HAVE #update', function () {
            AerospikePoolLanguageIndex.should.be.have.property('update');
            AerospikePoolLanguageIndex.update.should.be.a.Function;
        });
        it('HAVE #remove', function () {
            AerospikePoolLanguageIndex.should.be.have.property('remove');
            AerospikePoolLanguageIndex.remove.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikePoolLanguageIndex.should.be.have.property('findOne');
            AerospikePoolLanguageIndex.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikePoolLanguageIndex.should.be.have.property('toJSON');
            AerospikePoolLanguageIndex.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikePoolLanguageIndex.prototype.should.be.have.property('copy');
            AerospikePoolLanguageIndex.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikePoolLanguageIndex.prototype.should.be.have.property('save');
            AerospikePoolLanguageIndex.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikePoolLanguageIndex.prototype.should.be.have.property('remove');
            AerospikePoolLanguageIndex.prototype.remove.should.be.a.Function;
        });
    });
});
