var _ = require('lodash');
var async = require('async');
var uuid = require('node-uuid');
var should = require('should');
var DataIds = require('./../config/_id.data.js');
var Data = require('./../config/psc.data.js');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var JwtUtils = require('./../../utils/jwtUtils.js');
var ProtocolMessage = require('nodejs-protocol');

describe('WS Auth API', function () {
    global.wsHelper.series().forEach(function (serie) {

        describe('[' + serie + '] ' + 'registerAnonymous API', function () {
            this.timeout(30000);
            it('[' + serie + '] ' + 'registerAnonymous email: SUCCESS', function (done) {
                global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, function (content) {
                    should.exists(content);
                    content.should.have.property('token').which.is.a.String();
                }, done);
            });
            it('[' + serie + '] ' + 'registerAnonymous phone: SUCCESS', function (done) {
                global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, function (content) {
                    should.exists(content);
                    content.should.have.property('token').which.is.a.String();
                }, done);
            });
            it('[' + serie + '] ' + 'registerAnonymous facebook: SUCCESS', function (done) {
                global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, function (content) {
                    should.exists(content);
                    content.should.have.property('token').which.is.a.String();
                }, done);
            });
            it('[' + serie + '] ' + 'registerAnonymous google: SUCCESS', function (done) {
                global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, function (content) {
                    should.exists(content);
                    content.should.have.property('token').which.is.a.String();
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'register API', function () {
            this.timeout(30000);
            it('[' + serie + '] ' + 'register email: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'register phone: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'register facebook: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerFacebook', Data.TEST_FACEBOOK_CLIENT_INFO, {
                            facebookToken: Data.TEST_FACEBOOK_USER.providerToken
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'register google: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerGoogle', Data.TEST_GOOGLE_CLIENT_INFO, {
                            googleToken: Data.TEST_GOOGLE_USER.providerToken
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'register API - user invitation by email', function () {
            it('[' + serie + '] ' + 'invite user by email: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/inviteUserByEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            emails: [Data.TEST_EMAIL_USER.email],
                            invitationText: 'Custom text',
                            invitationPerson: 'Custom person'
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.RECOVER_PASSWORD
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'register API - user invitation by email and role', function () {
            it('[' + serie + '] ' + 'invite user by email and role: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/inviteUserByEmailAndRole', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            role: 'TENANT_1_COMMUNITY',
                            profileInfo: {
                                firstName: 'Test',
                                lastName: 'Account',
                                organization: 'Ascendro'
                            }
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            code: process.env.TEST_EMAIL_VERIFICATION_CODE
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.RECOVER_PASSWORD
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'register API - register new code', function () {
            it('[' + serie + '] ' + 'register email new code: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmailNewCode', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            code: process.env.TEST_EMAIL_VERIFICATION_CODE_NEW
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'register phone new code: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerPhoneNewCode', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            code: process.env.TEST_PHONE_VERIFICATION_CODE_NEW
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'recover API', function () {
            this.timeout(30000);
            it('[' + serie + '] ' + 'recover email password (registered user): SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/recoverPasswordEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                        }, function (content) {
                            should.not.exists(content);
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/recoverPasswordConfirmEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            code: process.env.TEST_EMAIL_VERIFICATION_CODE,
                            newPassword: Data.RECOVER_PASSWORD
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.RECOVER_PASSWORD
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'recover email password (confirmed user): SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            code: process.env.TEST_EMAIL_VERIFICATION_CODE
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/recoverPasswordEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                        }, function (content) {
                            should.not.exists(content);
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/recoverPasswordConfirmEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            code: process.env.TEST_EMAIL_VERIFICATION_CODE,
                            newPassword: Data.RECOVER_PASSWORD
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.RECOVER_PASSWORD
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'recover phone password (registered user): SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/recoverPasswordPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                        }, function (content) {
                            should.not.exists(content);
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/recoverPasswordConfirmPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            code: process.env.TEST_PHONE_VERIFICATION_CODE,
                            newPassword: Data.RECOVER_PASSWORD
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.RECOVER_PASSWORD
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'recover phone password (confirmed user): SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            code: process.env.TEST_PHONE_VERIFICATION_CODE
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/recoverPasswordPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                        }, function (content) {
                            should.not.exists(content);
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/recoverPasswordConfirmPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            code: process.env.TEST_PHONE_VERIFICATION_CODE,
                            newPassword: Data.RECOVER_PASSWORD
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.RECOVER_PASSWORD
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'confirm API', function () {
            this.timeout(30000);
            it('[' + serie + '] ' + 'confirmEmail: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            code: process.env.TEST_EMAIL_VERIFICATION_CODE
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'confirmPhone: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            code: process.env.TEST_PHONE_VERIFICATION_CODE
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'auth API', function () {
            this.timeout(30000);
            it('[' + serie + '] ' + 'authEmail: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        } , next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            code: process.env.TEST_EMAIL_VERIFICATION_CODE
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'authEmail NOT_VALIDATED: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'authEmail merge: SUCCESS', function (done) {
                var firstUser, secondUser;
                var firstUserClientInfo = Data.TEST_EMAIL_CLIENT_INFO;
                var secondUserClientInfo = Data.TEST_EMAIL_NEW_CLIENT_INFO;
                return async.series([
                    // Register and confirm first user
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', firstUserClientInfo, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmEmail', firstUserClientInfo, {
                            email: Data.TEST_EMAIL_USER.email,
                            code: process.env.TEST_EMAIL_VERIFICATION_CODE
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                            firstUser = JwtUtils.decode(content.token);
                            firstUser.token = content.token;
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    // Register second user, do not confirm
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', secondUserClientInfo, {
                            email: Data.TEST_EMAIL_NEW_USER.email,
                            password: Data.TEST_EMAIL_NEW_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                            secondUser = JwtUtils.decode(content.token);
                            secondUser.token = content.token;
                        }, next);
                    },
                    // Add tenant role to first user (operate from different user)
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/setUserRole', secondUserClientInfo, {
                            userId: firstUser.payload.userId,
                            rolesToAdd: ['TENANT_1_COMMUNITY'],
                            rolesToRemove: []
                        }, function (content) {
                            should.not.exists(content);
                        }, next);
                    },
                    // Add same tenant role to second user (operate from different user)
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/setUserRole', firstUserClientInfo, {
                            userId: secondUser.payload.userId,
                            rolesToAdd: ['TENANT_1_COMMUNITY'],
                            rolesToRemove: []
                        }, function (content) {
                            should.not.exists(content);
                        }, next);
                    },
                    // Authorize as first user, use token from second user, should receive token which contains:
                    //  - first user
                    //  - single global role: REGISTERED
                    //  - single tenant role: TENANT_1_COMMUNITY
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authEmail', secondUserClientInfo, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                            var jwtData = JwtUtils.decode(content.token);
                            should(jwtData.payload.userId).be.equal(firstUser.payload.userId);
                            should(jwtData.payload.roles).be.deepEqual(['REGISTERED', 'TENANT_1_COMMUNITY']);
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'authPhone: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            code: process.env.TEST_PHONE_VERIFICATION_CODE
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'authPhone NOT_VALIDATED: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authPhone', Data.TEST_PHONE_CLIENT_INFO, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'authPhone merge: SUCCESS', function (done) {
                var firstUser, secondUser;
                var firstUserClientInfo = Data.TEST_EMAIL_CLIENT_INFO;
                var secondUserClientInfo = Data.TEST_EMAIL_NEW_CLIENT_INFO;
                return async.series([
                    // Register and confirm first user
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerPhone', firstUserClientInfo, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmPhone', firstUserClientInfo, {
                            phone: Data.TEST_PHONE_USER.phone,
                            code: process.env.TEST_PHONE_VERIFICATION_CODE
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                            firstUser = JwtUtils.decode(content.token);
                            firstUser.token = content.token;
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                    // Register second user, do not confirm
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerPhone', secondUserClientInfo, {
                            phone: Data.TEST_PHONE_NEW_USER.phone,
                            password: Data.TEST_PHONE_NEW_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                            secondUser = JwtUtils.decode(content.token);
                            secondUser.token = content.token;
                        }, next);
                    },
                    // Add tenant role to first user (operate from different user)
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/setUserRole', secondUserClientInfo, {
                            userId: firstUser.payload.userId,
                            rolesToAdd: ['TENANT_1_COMMUNITY'],
                            rolesToRemove: []
                        }, function (content) {
                            should.not.exists(content);
                        }, next);
                    },
                    // Add same tenant role to second user (operate from different user)
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/setUserRole', firstUserClientInfo, {
                            userId: secondUser.payload.userId,
                            rolesToAdd: ['TENANT_1_COMMUNITY'],
                            rolesToRemove: []
                        }, function (content) {
                            should.not.exists(content);
                        }, next);
                    },
                    // Authorize as first user, use token from second user, should receive token which contains:
                    //  - first user
                    //  - single global role: REGISTERED
                    //  - single tenant role: TENANT_1_COMMUNITY
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authPhone', secondUserClientInfo, {
                            phone: Data.TEST_PHONE_USER.phone,
                            password: Data.TEST_PHONE_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                            var jwtData = JwtUtils.decode(content.token);
                            should(jwtData.payload.userId).be.equal(firstUser.payload.userId);
                            should(jwtData.payload.roles).be.deepEqual(['REGISTERED', 'TENANT_1_COMMUNITY']);
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'authFacebook: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerFacebook', Data.TEST_FACEBOOK_CLIENT_INFO, {
                            facebookToken: Data.TEST_FACEBOOK_USER.providerToken
                        }, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authFacebook', Data.TEST_FACEBOOK_CLIENT_INFO, {
                            facebookToken: Data.TEST_FACEBOOK_USER.providerToken
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'authGoogle: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerGoogle', Data.TEST_GOOGLE_CLIENT_INFO, {
                            googleToken: Data.TEST_GOOGLE_USER.providerToken
                        }, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authGoogle', Data.TEST_GOOGLE_CLIENT_INFO, {
                            googleToken: Data.TEST_GOOGLE_USER.providerToken
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'getPublicKey SUCCESS', function (done) {
                return global.wsHelper.apiSecureSucc(serie, 'auth/getPublicKey', Data.EMAIL_CLIENT_INFO, {
                }, function (content) {
                    should.exists(content);
                    content.should.have.property('publicKey').which.is.a.String();
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'auth/refresh API', function () {
            this.timeout(30000);
            it('[' + serie + '] ' + 'refresh: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/refresh', Data.TEST_EMAIL_CLIENT_INFO, {
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                        }, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'auth/changePassword API', function () {
            this.timeout(30000);
            it('[' + serie + '] ' + 'changePassword: email SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/changePassword', Data.TEST_EMAIL_CLIENT_INFO, {
                            oldPassword: Data.TEST_EMAIL_USER.password,
                            newPassword: Data.TEST_EMAIL_USER.password + 'new',
                        }, function (content) {
                            should.not.exists(content);
                        }, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'role API', function () {
            this.timeout(30000);
            it('[' + serie + '] ' + 'setUserRole: SUCCESS', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                            var jwtData = JwtUtils.decode(content.token);
                            should(jwtData.payload.userId).be.equal(Data.TEST_EMAIL_CLIENT_INFO.profile.userId);
                            should(jwtData.payload.roles).be.deepEqual(['NOT_VALIDATED']);
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/confirmEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            code: process.env.TEST_EMAIL_VERIFICATION_CODE
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                            var jwtData = JwtUtils.decode(content.token);
                            should(jwtData.payload.userId).be.equal(Data.TEST_EMAIL_CLIENT_INFO.profile.userId);
                            should(jwtData.payload.roles).be.deepEqual(['REGISTERED']);
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/setUserRole', Data.TEST_PHONE_CLIENT_INFO, {
                            userId: Data.TEST_EMAIL_USER.userId,
                            rolesToAdd: ['ANONYMOUS', 'NOT_VALIDATED', 'REGISTERED', 'FULLY_REGISTERED', 'TENANT_1_COMMUNITY'],
                            rolesToRemove: ['TENANT_2_INTERNAL', 'TENANT_3_COMMUNITY']
                        }, function (content) {
                            should.not.exists(content);
                        }, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/authEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, function (content) {
                            should.exists(content);
                            content.should.have.property('token').which.is.a.String();
                            var jwtData = JwtUtils.decode(content.token);
                            should(jwtData.payload.userId).be.equal(Data.TEST_EMAIL_CLIENT_INFO.profile.userId);
                            should(jwtData.payload.roles).be.deepEqual(['REGISTERED', 'FULLY_REGISTERED', 'TENANT_1_COMMUNITY']);
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'setUserRole for same user: VALIDATION FAILED', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureFail(serie, 'auth/setUserRole', Data.TEST_EMAIL_CLIENT_INFO, {
                            userId: Data.TEST_EMAIL_USER.userId,
                            rolesToAdd: ['TENANT_1_COMMUNITY', 'TENANT_2_ADMIN', 'TENANT_3_INTERNAL'],
                            rolesToRemove: ['TENANT_4_INTERNAL', 'TENANT_5_COMMUNITY']
                        }, Errors.AuthApi.ValidationFailed, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'setUserRole wrong role: VALIDATION FAILED', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureFail(serie, 'auth/setUserRole', Data.TEST_EMAIL_CLIENT_INFO, {
                            userId: Data.TEST_EMAIL_USER.userId,
                            rolesToAdd: ['INTERNAL'],
                            rolesToRemove: ['TENANT_4_INTERNAL', 'TENANT_5_COMMUNITY']
                        }, Errors.AuthApi.ValidationFailed, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'addConfirmedUser API', function () {
            this.timeout(30000);
            it('[' + serie + '] ' + 'addConfirmedUser: SUCCESS for existing user', function (done) {
                global.wsHelper.initProfiles();
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerAnonymous', null, null, null, next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'auth/registerEmail', Data.TEST_EMAIL_CLIENT_INFO, {
                            email: Data.TEST_EMAIL_USER.email,
                            password: Data.TEST_EMAIL_USER.password
                        }, null, next);
                    },
                    function (next) {
                        var payload = {
                            email: Data.TEST_EMAIL_USER.email,
                            firstName: 'First-name',
                            lastName: 'Last-name',
                            phone: '123123123'
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'auth/addConfirmedUser', Data.TEST_EMAIL_CLIENT_INFO, payload, function (content) {
                            should.exists(content);
                            should.deepEqual(content.profile, {
                                roles: ["REGISTERED"],
                                person: {
                                    firstName: payload.firstName,
                                    lastName: payload.lastName
                                },
                                userId: Data.TEST_EMAIL_USER.userId,
                                emails: [{ email: payload.email, verificationStatus: 'verified' }],
                                phones: [{ phone: payload.phone, verificationStatus: 'verified' }]
                            });
                        }, next);
                    },
                ], done);
            });

            it('[' + serie + '] ' + 'addConfirmedUser: SUCCESS for new user', function (done) {
                global.wsHelper.initProfiles();
                var payload = {
                    email: Data.TEST_EMAIL_NEW_USER.email,
                    firstName: 'First-name',
                    lastName: 'Last-name',
                    phone: '123123123'
                };
                return global.wsHelper.apiSecureSucc(serie, 'auth/addConfirmedUser', Data.TEST_EMAIL_CLIENT_INFO, payload, function (content) {
                    should.exists(content);
                    should.deepEqual(content.profile, {
                        roles: ["REGISTERED"],
                        person: {
                            firstName: payload.firstName,
                            lastName: payload.lastName
                        },
                        userId: Data.TEST_EMAIL_NEW_USER.userId,
                        emails: [{ email: payload.email, verificationStatus: 'verified' }],
                        phones: [{ phone: payload.phone, verificationStatus: 'verified' }]
                    });
                }, done);
            });
        });


        describe('[' + serie + '] ' + 'generateImpersonateToken API', function () {
            this.timeout(10000);
            // Generate successfully the token
            it('[' + serie + '] ' + 'generateImpersonateToken: SUCCESS for existing user', function (done) {
                global.wsHelper.initProfiles();
                return global.wsHelper.apiSecureSucc(serie, 'auth/generateImpersonateToken', Data.TEST_EMAIL_CLIENT_INFO, {
                    email: Data.IMPERSONATE_ADMIN_USER.email,
                    tenantId: DataIds.TENANT_1_ID.toString()
                }, function (content) {
                    should.exists(content);
                    content.should.have.property('token').which.is.a.String();
                }, done);
            });

            // Fail to generate token for email address which is not admin for the given tenant
            it('[' + serie + '] ' + 'generateImpersonateToken: ERROR for user which is not ADMIN', function (done) {
                global.wsHelper.initProfiles();
                return global.wsHelper.apiSecureFail(serie, 'auth/generateImpersonateToken', Data.TEST_EMAIL_CLIENT_INFO, {
                    email: Data.IMPERSONATE_NO_ADMIN_USER.email,
                    tenantId: DataIds.TENANT_1_ID.toString()
                }, Errors.AuthApi.FatalError, done);
            });
        });
    });
});