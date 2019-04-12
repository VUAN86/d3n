var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeProfileSync = KeyvalueService.Models.AerospikeProfileSync;

describe('ProfileSync', function () {
    describe('ProfileSync AS check', function () {
        it('HAVE #create', function () {
            AerospikeProfileSync.should.be.have.property('create');
            AerospikeProfileSync.create.should.be.a.Function;
        });
        it('HAVE #update', function () {
            AerospikeProfileSync.should.be.have.property('update');
            AerospikeProfileSync.update.should.be.a.Function;
        });
        it('HAVE #remove', function () {
            AerospikeProfileSync.should.be.have.property('remove');
            AerospikeProfileSync.remove.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeProfileSync.should.be.have.property('findOne');
            AerospikeProfileSync.findOne.should.be.a.Function;
        });
        it('HAVE #findAll', function () {
            AerospikeProfileSync.should.be.have.property('findAll');
            AerospikeProfileSync.findAll.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeProfileSync.should.be.have.property('toJSON');
            AerospikeProfileSync.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeProfileSync.prototype.should.be.have.property('copy');
            AerospikeProfileSync.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeProfileSync.prototype.should.be.have.property('save');
            AerospikeProfileSync.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeProfileSync.prototype.should.be.have.property('remove');
            AerospikeProfileSync.prototype.remove.should.be.a.Function;
        });
    });

    describe('ProfileSync AS #create', function () {
        it('test create OK', function (done) {
            AerospikeProfileSync.create(ProfileData.TEST_EMAIL_PROFILE_SYNC, function (err, profileSync) {
                if (err) return done(err);
                profileSync.should.be.an.instanceOf(AerospikeProfileSync);
                profileSync.profile.should.eql(ProfileData.TEST_EMAIL_PROFILE_SYNC.profile);
                profileSync.randomValue.should.eql(ProfileData.TEST_EMAIL_PROFILE_SYNC.randomValue);
                done();
            });
        });
    });
    
    describe('ProfileSync AS #remove', function () {
        it('test remove OK', function (done) {
            AerospikeProfileSync.create(ProfileData.TEST_EMAIL_PROFILE_SYNC, function (err, profileSync) {
                AerospikeProfileSync.remove(ProfileData.TEST_EMAIL_PROFILE_SYNC, function (err, profileSync) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
    
    describe('ProfileSync AS #findOne', function () {
        it('profile findOne OK', function (done) {
            AerospikeProfileSync.findOne(ProfileData.EMAIL_PROFILE_SYNC, function (err, profileSync) {
                if (err) return done(err);
                profileSync.should.be.an.instanceOf(AerospikeProfileSync);
                profileSync.profile.should.eql(ProfileData.EMAIL_PROFILE_SYNC.profile);
                profileSync.randomValue.should.eql(ProfileData.EMAIL_PROFILE_SYNC.randomValue);
                done();
            });
        });
        it('profile findOne FAIL', function (done) {
            AerospikeProfileSync.findOne(ProfileData.TEST_EMAIL_PROFILE_SYNC, function (err, profileSync) {
                should.exist(err);
                err.should.be.eql(Errors.DatabaseApi.NoRecordFound);
                should.not.exist(profileSync);
                done();
            });
        });
    });

    describe('ProfileSync AS #findAll', function () {
        it('profile findAll equal OK', function (done) {
            var query = {
                filters: [{
                    'equal': { 'randomValue': 123 }
                }]
            };
            AerospikeProfileSync.findAll(query, function (err, profileSyncList) {
                if (err) return done(err);
                profileSyncList.should.be.an.instanceOf(Array);
                profileSyncList.length.should.be.eql(1);
                done();
            });
        });
        it('profile findAll range OK', function (done) {
            var query = {
                filters: [{
                    'range': { 'randomValue': [0, 999] }
                }]
            };
            AerospikeProfileSync.findAll(query, function (err, profileSyncList) {
                if (err) return done(err);
                profileSyncList.should.be.an.instanceOf(Array);
                profileSyncList.length.should.be.eql(4);
                done();
            });
        });
        it('profile findAll equal FAIL', function (done) {
            var query = {
                filters: [{
                    'equal': { 'randomValue': 1 }
                }]
            };
            AerospikeProfileSync.findAll(query, function (err, profileSyncList) {
                should.exist(err);
                err.should.be.eql(Errors.DatabaseApi.NoRecordFound);
                should.not.exist(profileSyncList);
                done();
            });
        });
        it('profile findAll range FAIL', function (done) {
            var query = {
                filters: [{
                    'range': { 'randomValue': [0, 1] }
                }]
            };
            AerospikeProfileSync.findAll(query, function (err, profileSyncList) {
                should.exist(err);
                err.should.be.eql(Errors.DatabaseApi.NoRecordFound);
                should.not.exist(profileSyncList);
                done();
            });
        });
    });

    describe('ProfileSync AS #toJSON', function () {
        it('should be return json', function (done) {
            AerospikeProfileSync.findOne(ProfileData.EMAIL_PROFILE_SYNC, function (err, profileSync) {
                if (err) return done(err);
                AerospikeProfileSync.toJSON(profileSync, function (err, json) {
                    if (err) return done(err);
                    json.should.be.eql(JSON.stringify(ProfileData.EMAIL_PROFILE_SYNC));
                });
                done();
            });
        });
    });
});