var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var Profile = Database.RdbmsService.Models.ProfileManager.Profile;
var ProfileHasRole = Database.RdbmsService.Models.ProfileManager.ProfileHasRole;
var ProfileHasGlobalRole = Database.RdbmsService.Models.ProfileManager.ProfileHasGlobalRole;
var Tenant = Database.RdbmsService.Models.Tenant.Tenant;

var ignoreProfileStatFields = [];
_.forEach(Profile.attributes, function (value, key) {
    if (key.substring(0, 5) == "stat_") {
        ignoreProfileStatFields.push(key);
    }
});

describe('WS Profile API', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {

        describe('[' + serie + '] ' + 'profileUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: profileUpdate:create', function (done) {
                var content = _.clone(ProfileData.PROFILE_TEST);
                content.userId = null;
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(ProfileData.PROFILE_TEST);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.profile, ignoreProfileStatFields.concat(['userId', 'applicationsIds', 'emails', 'phones', 'roles']));
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: profileUpdate:update', function (done) {
                var content = { userId: DataIds.LOCAL_USER_ID, languageId: DataIds.LANGUAGE_DE_ID, regionalSettingId: DataIds.REGIONAL_SETTING_2_ID, addressCity: 'London' };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.profile.languageId).be.equal(content.languageId);
                    should(responseContent.profile.regionalSettingId).be.equal(content.regionalSettingId);
                    should(responseContent.profile.addressCity).be.equal(content.addressCity);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: profileUpdate:update', function (done) {
                var content = { userId: DataIds.TEST_USER_ID, languageId: DataIds.LANGUAGE_DE_ID, regionalSettingId: DataIds.REGIONAL_SETTING_2_ID, addressCity: 'London' };
                global.wsHelper.apiSecureFail(serie, 'profileManager/profileUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'profileGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: profileGet', function (done) {
                var content = { userId: DataIds.LOCAL_USER_ID };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(ProfileData.PROFILE_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.profile, ignoreProfileStatFields);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: profileGet', function (done) {
                var content = { userId: DataIds.TEST_USER_ID };
                global.wsHelper.apiSecureFail(serie, 'profileManager/profileGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'profileList', function () {
            it('[' + serie + '] ' + 'SUCCESS: profileList all users no filter', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {},
                    orderBy: [{ field: 'userId', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent).has.property('total').which.be.equal(1);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: profileList by registered/not_validated and internal roles', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        roles: [ProfileHasRole.constants().ROLE_INTERNAL, ProfileHasGlobalRole.constants().ROLE_REGISTERED, ProfileHasGlobalRole.constants().ROLE_NOT_VALIDATED]
                    },
                    orderBy: [{ field: 'userId', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(ProfileData.PROFILE_1);
                    should(responseContent).has.property('total').which.be.equal(1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ignoreProfileStatFields);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: profileList by registered/fully_registered/fully_registered_bank roles', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        roles: [ProfileHasGlobalRole.constants().ROLE_REGISTERED, ProfileHasGlobalRole.constants().ROLE_FULLY_REGISTERED, ProfileHasGlobalRole.constants().ROLE_FULLY_REGISTERED_BANK]
                    },
                    orderBy: [{ field: 'userId', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(ProfileData.PROFILE_1);
                    should(responseContent).has.property('total').which.be.equal(1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ignoreProfileStatFields);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: profileList base', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        userId: DataIds.LOCAL_USER_ID,
                        roles: [ProfileHasRole.constants().ROLE_INTERNAL, ProfileHasGlobalRole.constants().ROLE_REGISTERED, ProfileHasGlobalRole.constants().ROLE_NOT_VALIDATED],
                        emails: [{ email: 'email1@ascendro.de', verificationStatus: 'notVerified' }],
                        phones: [{ phone: '1-123456789', verificationStatus: 'notVerified' }]
                    },
                    orderBy: [{ field: 'userId', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(ProfileData.PROFILE_1);
                    should(responseContent).has.property('total').which.be.equal(1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ignoreProfileStatFields);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: profileList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { userId: DataIds.TEST_USER_ID },
                    orderBy: [{ field: 'userId', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content).has.property('items').which.has.length(0);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: profileList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {},
                    orderBy: [{ field: 'id', direction: 'asc' }]
                };
                global.wsHelper.apiSecureFail(serie, 'profileManager/profileList', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, done);
            });
        });

        describe('[' + serie + '] ' + 'profileAddRemoveEmails', function () {
            it('[' + serie + '] ' + 'SUCCESS: profileAddRemoveEmails', function (done) {
                var content = {
                    profileId: DataIds.LOCAL_USER_ID,
                    removes: ['email1@ascendro.de'],
                    adds: ['email3@ascendro.de', 'email4@ascendro.de']
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileAddRemoveEmails', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { userId: DataIds.LOCAL_USER_ID };
                        global.wsHelper.apiSecureSucc(serie, 'profileManager/profileGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.emails).be.deepEqual(content.adds);
                        }, done);
                    }, done);
                });
            });
        });

        describe('[' + serie + '] ' + 'profileAddRemovePhones', function () {
            it('[' + serie + '] ' + 'SUCCESS: profileAddRemovePhones', function (done) {
                var content = {
                    profileId: DataIds.LOCAL_USER_ID,
                    removes: ['1-123456789'],
                    adds: ['3-123456789', '4-123456789']
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileAddRemovePhones', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { userId: DataIds.LOCAL_USER_ID };
                        global.wsHelper.apiSecureSucc(serie, 'profileManager/profileGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.phones).be.deepEqual(content.adds);
                        }, done);
                    }, done);
                });
            });
        });

        describe('[' + serie + '] ' + 'profileValidateEmail', function () {
            it('[' + serie + '] ' + 'SUCCESS: profileValidateEmail', function (done) {
                var content = { profileId: DataIds.LOCAL_USER_ID, email: 'email1@ascendro.de' };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileValidateEmail', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { userId: DataIds.LOCAL_USER_ID };
                        global.wsHelper.apiSecureSucc(serie, 'profileManager/profileGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = _.clone(ProfileData.PROFILE_1);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.profile, ignoreProfileStatFields);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_ALREADY_VALIDATED: profileValidateEmail', function (done) {
                var content = { profileId: DataIds.GOOGLE_USER_ID };
                global.wsHelper.apiSecureFail(serie, 'profileManager/profileValidateEmail', ProfileData.CLIENT_INFO_1, content, Errors.ProfileApi.AlreadyValidated, done);
            });
        });

        describe('[' + serie + '] ' + 'profileValidatePhone', function () {
            it('[' + serie + '] ' + 'SUCCESS: profileValidatePhone', function (done) {
                var content = { profileId: DataIds.LOCAL_USER_ID, phone: '1-123456789' };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileValidatePhone', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { userId: DataIds.LOCAL_USER_ID };
                        global.wsHelper.apiSecureSucc(serie, 'profileManager/profileGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = _.clone(ProfileData.PROFILE_1);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.profile, ignoreProfileStatFields);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_ALREADY_VALIDATED: profileValidatePhone', function (done) {
                var content = { profileId: DataIds.GOOGLE_USER_ID };
                global.wsHelper.apiSecureFail(serie, 'profileManager/profileValidatePhone', ProfileData.CLIENT_INFO_1, content, Errors.ProfileApi.AlreadyValidated, done);
            });
        });


        describe('[' + serie + '] ' + 'userRoleListByTenantId', function () {
            it('[' + serie + '] ' + 'get roles google user', function (done) {
                var content = {
                    profileId: ProfileData.PROFILE_HAS_ROLE_4.profileId,
                    tenantId: ProfileData.PROFILE_HAS_ROLE_4.tenantId
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/userRoleListByTenantId', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.roles.sort().should.eql([ProfileData.PROFILE_HAS_ROLE_4.role].sort());
                }, done);
            });
            it('[' + serie + '] ' + 'get roles facebook user', function (done) {
                var content = {
                    profileId: ProfileData.PROFILE_HAS_ROLE_5.profileId,
                    tenantId: ProfileData.PROFILE_HAS_ROLE_5.tenantId
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/userRoleListByTenantId', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.roles.sort().should.eql([ProfileData.PROFILE_HAS_ROLE_5.role].sort());
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'userRoleListByTenantId empty', function () {
            it('[' + serie + '] ' + 'empty by profileId', function (done) {
                var content = {
                    profileId: 'asdasdd',
                    tenantId: DataIds.TENANT_1_ID
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/userRoleListByTenantId', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.roles.should.have.length(0);
                }, done);
            });
            it('[' + serie + '] ' + 'empty by tenantId', function (done) {
                var content = {
                    profileId: DataIds.LOCAL_USER_ID,
                    tenantId: 89238432
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/userRoleListByTenantId', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.roles.should.have.length(0);
                }, done);
            });
        });


        describe('[' + serie + '] ' + 'addRemoveRoles', function () {
            it('[' + serie + '] ' + 'add roles', function (done) {
                var content = {
                    profileId: DataIds.LOCAL_USER_ID,
                    rolesToAdd: ['EXTERNAL']
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/addRemoveRoles', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.roles.sort().should.eql(['ADMIN', 'COMMUNITY', 'EXTERNAL', 'INTERNAL', 'REGISTERED'].sort());
                }, done);
            });

            it('[' + serie + '] ' + 'remove roles', function (done) {
                var content = {
                    profileId: DataIds.FACEBOOK_USER_ID,
                    rolesToRemove: ['COMMUNITY']
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/addRemoveRoles', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.roles.should.eql([]);
                }, done);
            });

            it('[' + serie + '] ' + 'add AND remove roles', function (done) {
                var content = {
                    profileId: DataIds.FACEBOOK_USER_ID,
                    rolesToRemove: ['COMMUNITY'],
                    rolesToAdd: ['INTERNAL', 'EXTERNAL']
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/addRemoveRoles', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.roles.sort().should.eql(['EXTERNAL', 'INTERNAL'].sort());
                }, done);
            });

            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION: profileManager:addRemoveRoles add ADMIN', function (done) {
                var content = { profileId: DataIds.LOCAL_USER_ID, rolesToRemove: ['ADMIN'] };
                global.wsHelper.apiSecureFail(serie, 'profileManager/addRemoveRoles', ProfileData.CLIENT_INFO_1, content, 'ERR_VALIDATION_FAILED', done);
            });

            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION: profileManager:addRemoveRoles remove ADMIN', function (done) {
                var content = { profileId: DataIds.LOCAL_USER_ID, rolesToRemove: ['ADMIN'] };
                global.wsHelper.apiSecureFail(serie, 'profileManager/addRemoveRoles', ProfileData.CLIENT_INFO_1, content, 'ERR_VALIDATION_FAILED', done);
            });

        });

        describe('[' + serie + '] ' + 'addToCommunity', function () {
            it('[' + serie + '] ' + 'to become part of the community', function (done) {
                var content = {
                    profileId: DataIds.GOOGLE_USER_ID
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/addToCommunity', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent).be.empty;
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'requestCommunityAccess', function () {
            it('[' + serie + '] ' + 'to become part of the tenant community (autoCommunity=true)', function (done) {
                return Tenant.update({ autoCommunity: 1 }, { where: { id: DataIds.TENANT_1_ID } }).then(function (count) {
                    if (count[0] === 0) {
                        return done(Errors.DatabaseApi.NoRecordFound);
                    }
                    return async.series([
                        function (next) {
                            return global.wsHelper.apiSecureSucc(serie, 'profileManager/requestCommunityAccess', ProfileData.CLIENT_INFO_1, {}, function (responseContent) {
                                should(responseContent).not.be.empty;
                                should(responseContent.roles).not.be.empty;
                                responseContent.roles.should.containEql('COMMUNITY');
                            }, next);
                        },
                        function (next) {
                            return global.wsHelper.apiSecureFail(serie, 'profileManager/requestCommunityAccess', ProfileData.CLIENT_INFO_1, {}, 'ERR_ALREADY_APPROVED', next);
                        },
                    ], done);
                }).catch(function (err) {
                    return done(err);
                });
            });
            it('[' + serie + '] ' + 'to become part of the tenant community (autoCommunity=false)', function (done) {
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureFail(serie, 'profileManager/requestCommunityAccess', ProfileData.CLIENT_INFO_1, {}, 'ERR_PRIVATE_COMMUNITY_REQUEST_CREATED', next);
                    },
                    function (next) {
                        return global.wsHelper.apiSecureFail(serie, 'profileManager/requestCommunityAccess', ProfileData.CLIENT_INFO_1, {}, 'ERR_ALREADY_APPLIED', next);
                    },
                    function (next) {
                        return Tenant.update({ autoCommunity: 1 }, { where: { id: DataIds.TENANT_1_ID } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    // APPLIED should be changed to APPROVED
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'profileManager/requestCommunityAccess', ProfileData.CLIENT_INFO_1, {}, function (responseContent) {
                            should(responseContent).not.be.empty;
                            should(responseContent.roles).not.be.empty;
                            responseContent.roles.should.containEql('COMMUNITY');
                        }, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'adminList', function () {
            it('[' + serie + '] ' + 'get current admin', function (done) {
                var content = {};
                global.wsHelper.apiSecureSucc(serie, 'profileManager/adminList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.admins.sort().should.eql([DataIds.LOCAL_USER_ID, DataIds.GOOGLE_USER_ID].sort());
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'adminListByTenantId', function () {
            it('[' + serie + '] ' + 'get tenant admins', function (done) {
                var content = {
                    tenantId: DataIds.TENANT_1_ID
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/adminListByTenantId', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.admins.length.should.be.above(0);
                }, done);
            });
        });
        
        describe('[' + serie + '] ' + 'adminListByTenantId none', function () {
            it('[' + serie + '] ' + 'get tenant admins', function (done) {
                var content = {
                    tenantId: 'asdasd'
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/adminListByTenantId', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.admins.should.have.length(0);
                }, done);
            });
        });
        
        describe('[' + serie + '] ' + 'adminAdd', function () {
            it('[' + serie + '] ' + 'add another admin', function (done) {
                var content = {'admins': [DataIds.FACEBOOK_USER_ID]};
                global.wsHelper.apiSecureSucc(serie, 'profileManager/adminAdd', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.admins.sort().should.eql([DataIds.LOCAL_USER_ID, DataIds.FACEBOOK_USER_ID, DataIds.GOOGLE_USER_ID].sort());
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'admin', function () {
            it('[' + serie + '] ' + 'ADD and DELETE', function (done) {
                var content = {'admins': [DataIds.FACEBOOK_USER_ID]};
                global.wsHelper.apiSecureSucc(serie, 'profileManager/adminAdd', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    responseContent.admins.sort().should.eql([DataIds.LOCAL_USER_ID, DataIds.FACEBOOK_USER_ID].sort());
                }, function () {
                    global.wsHelper.apiSecureSucc(serie, 'profileManager/adminDelete', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                        responseContent.admins.sort().should.eql([DataIds.LOCAL_USER_ID, DataIds.GOOGLE_USER_ID].sort());
                    }, done);
                });
            });

            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION: delete 1 of 1 admins', function (done) {
                var content = {'admins': [DataIds.LOCAL_USER_ID]};
                global.wsHelper.apiSecureFail(serie, 'profileManager/adminDelete', ProfileData.CLIENT_INFO_1, content, 'ERR_VALIDATION_FAILED', done);
            });
        });

        describe('[' + serie + '] ' + 'profileSync', function () {
            it('[' + serie + '] ' + 'SUCCESS: profileSync as API method', function (done) {
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileSync', ProfileData.CLIENT_INFO_1, null, function (responseContent) {
                    should(responseContent).be.empty;
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: profileSync as API method, pass userId', function (done) {
                var content = { usersIds: [DataIds.LOCAL_USER_ID] };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/profileSync', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent).be.empty;
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: profileSync as CronMasterJob', function (done) {
                var tickCompleteSpy = global.wsHelper.sinon.spy();
                var profileSyncJob = require('./../../jobs/profileSyncJob.js');
                profileSyncJob.on(global.wsHelper.cmaster.EVENTS.TICK_COMPLETE, tickCompleteSpy);
                profileSyncJob.start();
                setTimeout(function () {
                    should(tickCompleteSpy.called).be.true;
                    profileSyncJob.stop();
                    return done();
                }, 3000); 
            });
        });

        describe('[' + serie + '] ' + 'inviteUser', function () {
            it('[' + serie + '] ' + 'SUCCESS: inviteUser', function (done) {
                var content = {
                    email: ProfileData.PROFILE_EMAIL_1.email,
                    role: 'COMMUNITY',
                    firstName: 'Test',
                    lastName: 'Account',
                    organization: 'Ascendro'
                };
                global.wsHelper.apiSecureSucc(serie, 'profileManager/inviteUser', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent).be.empty;
                }, done);
            });
        });
    });
});
