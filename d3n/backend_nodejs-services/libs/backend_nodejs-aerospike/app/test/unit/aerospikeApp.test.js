var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var AppData = require('./../config/app.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeApp = KeyvalueService.Models.AerospikeApp;

describe('Application', function () {
    describe('Application AS check', function () {
        it('HAVE #create', function () {
            AerospikeApp.should.be.have.property('create');
            AerospikeApp.create.should.be.a.Function;
        });
        it('HAVE #update', function () {
            AerospikeApp.should.be.have.property('update');
            AerospikeApp.update.should.be.a.Function;
        });
        it('HAVE #remove', function () {
            AerospikeApp.should.be.have.property('remove');
            AerospikeApp.remove.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeApp.should.be.have.property('findOne');
            AerospikeApp.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeApp.should.be.have.property('toJSON');
            AerospikeApp.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeApp.prototype.should.be.have.property('copy');
            AerospikeApp.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeApp.prototype.should.be.have.property('save');
            AerospikeApp.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeApp.prototype.should.be.have.property('remove');
            AerospikeApp.prototype.remove.should.be.a.Function;
        });
    });

    describe('Application AS #remove', function () {
        it('test remove OK', function (done) {
            AerospikeApp.create(AppData.APP_1, function (err, app) {
                AerospikeApp.remove(AppData.APP_1, function (err, app) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
});
