var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProtocolMessage = require('nodejs-protocol');
var TestData = require('./../config/psc.data.js');

describe('PSC', function () {
    describe('SUCCESS', function () {
        
        it('createProfile email SUCCESS', function (done) {
            global.wsHelper.pscValid().createProfile({ email: 'test@ascendro.de' }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/createProfileResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    userId: TestData.TEST_USER_ID
                });
                done();
            });
        });
        it('createProfile phone SUCCESS', function (done) {
            global.wsHelper.pscValid().createProfile({ phone: '1234567890' }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/createProfileResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    userId: TestData.TEST_USER_ID
                });
                done();
            });
        });
        it('createProfile facebook SUCCESS', function (done) {
            global.wsHelper.pscValid().createProfile({ facebook: 'dsfldvlcvnlvnlvn' }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/createProfileResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    userId: TestData.TEST_USER_ID
                });
                done();
            });
        });
        it('createProfile google SUCCESS', function (done) {
            global.wsHelper.pscValid().createProfile({ google: 'dflhdkghfdghfhg' }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/createProfileResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    userId: TestData.TEST_USER_ID
                });
                done();
            });
        });
        it('updateProfile SUCCESS', function (done) {
            global.wsHelper.pscValid().updateProfile({ profile: TestData.TEST_PROFILE }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/updateProfileResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    profile: TestData.TEST_PROFILE
                });
                done();
            });
        });
        it('updateUserProfile SUCCESS', function (done) {
            global.wsHelper.pscValid().updateProfile({ userId: TestData.TEST_USER_ID, profile: TestData.TEST_PROFILE }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/updateProfileResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    profile: TestData.TEST_PROFILE
                });
                done();
            });
        });
        it('getProfile SUCCESS', function (done) {
            global.wsHelper.pscValid().getProfile({userId: '3423442'}, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/getProfileResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    profile: TestData.TEST_PROFILE
                });
                done();
            });
        });
        it('getUserProfile SUCCESS', function (done) {
            global.wsHelper.pscValid().getUserProfile({ userId: TestData.TEST_USER_ID }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/getProfileResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    profile: TestData.TEST_PROFILE
                });
                done();
            });
        });
        it('deleteProfile SUCCESS', function (done) {
            global.wsHelper.pscValid().deleteProfile({userId: '34243'}, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/deleteProfileResponse');
                should.not.exists(message.getError());
                should.not.exists(message.getContent());
                done();
            });
        });
        it('mergeProfile SUCCESS', function (done) {
            global.wsHelper.pscValid().mergeProfile({ sourceUserId: TestData.TEST_USER_ID, targetUserId: TestData.MERGE_USER_ID }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/mergeProfileResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    profile: TestData.TEST_PROFILE
                });
                done();
            });
        });
        it('findByIdentifier SUCCESS', function (done) {
            global.wsHelper.pscValid().findByIdentifier({ identifierType: 'EMAIL', identifier: 'test@ascendro.de' }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/findByIdentifierResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    userId: TestData.TEST_USER_ID
                });
                done();
            });
        });
        it('getAppConfiguration SUCCESS', function (done) {
            global.wsHelper.pscValid().getAppConfiguration({appId: '1313231213', deviceUUID: TestData.DEVICE_UUID, device: {a: 'a'} }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/getAppConfigurationResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.eql({
                    appConfig: { config: 'fake' }
                });
                done();
            });
        });
        it('getProfileBlob SUCCESS', function (done) {
            global.wsHelper.pscValid().getProfileBlob({ userId: '34243', name: TestData.TEST_PROFILE_BLOB.name }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/getProfileBlobResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.deepEqual(TestData.TEST_PROFILE_BLOB);
                done();
            });
        });
        it('getUserProfileBlob SUCCESS', function (done) {
            global.wsHelper.pscValid().getUserProfileBlob({ userId: TestData.TEST_USER_ID, name: TestData.TEST_PROFILE_BLOB.name }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/getProfileBlobResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.deepEqual(TestData.TEST_PROFILE_BLOB);
                done();
            });
        });
        it('updateProfileBlob SUCCESS', function (done) {
            global.wsHelper.pscValid().updateProfileBlob({ userId: '34243', name: TestData.TEST_PROFILE_BLOB.name, value: TestData.TEST_PROFILE_BLOB.value }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/updateProfileBlobResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.deepEqual(TestData.TEST_PROFILE_BLOB);
                done();
            });
        });
        it('updateUserProfileBlob SUCCESS', function (done) {
            global.wsHelper.pscValid().updateUserProfileBlob({ userId: TestData.TEST_USER_ID, name: TestData.TEST_PROFILE_BLOB.name, value: TestData.TEST_PROFILE_BLOB.value }, TestData.TEST_TOKEN, function (err, message) {
                if (err) {
                    done(err);
                }
                should.exists(message);
                message.getMessage().should.be.eql('profile/updateProfileBlobResponse');
                should.not.exists(message.getError());
                should.exists(message.getContent());
                message.getContent().should.be.deepEqual(TestData.TEST_PROFILE_BLOB);
                done();
            });
        });
    });

    describe('VALIDATION FAIL', function () {
        it('updateProfile VALIDATION_FAILED', function (done) {
            global.wsHelper.pscValid().updateProfile(null, TestData.TEST_TOKEN, function (err, message) {
                should.not.exists(message);
                should.exists(err);
                err.message.should.be.eql(Errors.PscApi.ValidationFailed);
                done();
            });
        });
        it('updateUserProfile VALIDATION_FAILED', function (done) {
            global.wsHelper.pscValid().updateProfile(null, TestData.TEST_TOKEN, function (err, message) {
                should.not.exists(message);
                should.exists(err);
                err.message.should.be.eql(Errors.PscApi.ValidationFailed);
                done();
            });
        });
        it('getUserProfile VALIDATION_FAILED', function (done) {
            global.wsHelper.pscValid().getUserProfile(null, TestData.TEST_TOKEN, function (err, message) {
                should.not.exists(message);
                should.exists(err);
                err.message.should.be.eql(Errors.PscApi.ValidationFailed);
                done();
            });
        });
        it('mergeProfile VALIDATION_FAILED', function (done) {
            global.wsHelper.pscValid().mergeProfile(null, TestData.TEST_TOKEN, function (err, message) {
                should.not.exists(message);
                should.exists(err);
                err.message.should.be.eql(Errors.PscApi.ValidationFailed);
                done();
            });
        });
        it('findByIdentifier VALIDATION_FAILED', function (done) {
            global.wsHelper.pscValid().findByIdentifier(null, TestData.TEST_TOKEN, function (err, message) {
                should.not.exists(message);
                should.exists(err);
                err.message.should.be.eql(Errors.PscApi.ValidationFailed);
                done();
            });
        });
        it('getAppConfiguration VALIDATION_FAILED', function (done) {
            global.wsHelper.pscValid().getAppConfiguration(null, TestData.TEST_TOKEN, function (err, message) {
                should.not.exists(message);
                should.exists(err);
                err.message.should.be.eql(Errors.PscApi.ValidationFailed);
                done();
            });
        });
        it('getUserProfileBlob VALIDATION_FAILED', function (done) {
            global.wsHelper.pscValid().getUserProfileBlob(null, TestData.TEST_TOKEN, function (err, message) {
                should.not.exists(message);
                should.exists(err);
                err.message.should.be.eql(Errors.PscApi.ValidationFailed);
                done();
            });
        });
        it('updateUserProfileBlob VALIDATION_FAILED', function (done) {
            global.wsHelper.pscValid().updateUserProfileBlob(null, TestData.TEST_TOKEN, function (err, message) {
                should.not.exists(message);
                should.exists(err);
                err.message.should.be.eql(Errors.PscApi.ValidationFailed);
                done();
            });
        });
    });
});
