var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeUserToken = KeyvalueService.Models.AerospikeUserToken;

describe('User', function () {
    describe('User Token AS check', function () {
        it('HAVE #create', function () {
            AerospikeUserToken.should.be.have.property('create');
            AerospikeUserToken.create.should.be.a.Function;
        });
        it('HAVE #update', function () {
            AerospikeUserToken.should.be.have.property('update');
            AerospikeUserToken.update.should.be.a.Function;
        });
        it('HAVE #remove', function () {
            AerospikeUserToken.should.be.have.property('remove');
            AerospikeUserToken.remove.should.be.a.Function;
        });
        it('HAVE #validate', function () {
            AerospikeUserToken.should.be.have.property('validate');
            AerospikeUserToken.validate.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeUserToken.should.be.have.property('findOne');
            AerospikeUserToken.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeUserToken.should.be.have.property('toJSON');
            AerospikeUserToken.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeUserToken.prototype.should.be.have.property('copy');
            AerospikeUserToken.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeUserToken.prototype.should.be.have.property('save');
            AerospikeUserToken.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeUserToken.prototype.should.be.have.property('remove');
            AerospikeUserToken.prototype.remove.should.be.a.Function;
        });
    });

    describe('User Token AS #create', function () {
        it('test create OK', function (done) {
            AerospikeUserToken.create(ProfileData.TEST_PHONE_USER_TOKEN, function (err, userToken) {
                setImmediate(function (done) {
                    var content = { userId: ProfileData.TEST_PHONE_USER_TOKEN.userId };
                    AerospikeUserToken.findOne(content, function (err, userToken) {
                        if (err) return done(err);
                        userToken.should.be.an.instanceOf(AerospikeUserToken);
                        userToken.userId.should.be.eql(ProfileData.TEST_PHONE_USER_TOKEN.userId);
                        userToken.token.should.be.eql(ProfileData.TEST_PHONE_USER_TOKEN.token);
                        done();
                    });
                }, done);
            });
        });
    });
    
    describe('User Token AS #remove', function () {
        it('test remove OK', function (done) {
            AerospikeUserToken.create(ProfileData.TEST_PHONE_USER_TOKEN, function (err, userToken) {
                AerospikeUserToken.remove(ProfileData.TEST_PHONE_USER_TOKEN, function (err, userToken) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
    
    describe('User Token AS #findOne', function () {
        it('email findOne OK', function (done) {
            var content = { userId: ProfileData.EMAIL_USER_TOKEN.userId };
            AerospikeUserToken.findOne(content, function (err, userToken) {
                if (err) return done(err);
                userToken.should.be.an.instanceOf(AerospikeUserToken);
                userToken.userId.should.be.eql(ProfileData.EMAIL_USER_TOKEN.userId);
                userToken.token.should.be.eql(ProfileData.EMAIL_USER_TOKEN.token);
                done();
            });
        });
        it('email findOne FAIL', function (done) {
            var content = { userId: '9999999-9999-9999-9999-999999999999' };
            AerospikeUserToken.findOne(content, function (err, userToken) {
                should.exist(err);
                err.should.be.eql(Errors.DatabaseApi.NoRecordFound);
                should.not.exist(userToken);
                done();
            });
        });
    });

    describe('User Token AS #validate', function () {
        it('email token validate OK', function (done) {
            var userData = _.clone(ProfileData.EMAIL_USER_TOKEN);
            AerospikeUserToken.validate(userData, function (err, userToken) {
                if (err) return done(err);
                userToken.should.be.an.instanceOf(AerospikeUserToken);
                userToken.userId.should.be.eql(ProfileData.EMAIL_USER_TOKEN.userId);
                userToken.token.should.be.eql(ProfileData.EMAIL_USER_TOKEN.token);
                done();
            });
        });
    });
    
    describe('User Token AS #toJSON', function () {
        it('should be return json', function (done) {
            AerospikeUserToken.findOne(ProfileData.EMAIL_USER_TOKEN, function (err, userToken) {
                if (err) return done(err);
                AerospikeUserToken.toJSON(userToken, function (err, json) {
                    if (err) return done(err);
                    json.should.be.eql(JSON.stringify(ProfileData.EMAIL_USER_TOKEN));
                });
                done();
            });
        });
    });
});