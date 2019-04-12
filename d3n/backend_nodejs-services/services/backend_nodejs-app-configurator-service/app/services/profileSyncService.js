var _ = require('lodash');
var async = require('async');
var uuid = require('node-uuid');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var TenantService = require('./tenantService.js');
var ProfileService = require('./profileService.js');
var LanguageService = require('./languageService.js');
var ElasticSearch = require('./elasticService.js');
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Profile = RdbmsService.Models.ProfileManager.Profile;
var ProfileEmail = RdbmsService.Models.ProfileManager.ProfileEmail;
var ProfilePhone = RdbmsService.Models.ProfileManager.ProfilePhone;
var ProfileHasApplication = RdbmsService.Models.ProfileManager.ProfileHasApplication;
var ProfileHasTenant = RdbmsService.Models.ProfileManager.ProfileHasTenant;
var ProfileHasRole = RdbmsService.Models.ProfileManager.ProfileHasRole;
var ProfileHasGlobalRole = RdbmsService.Models.ProfileManager.ProfileHasGlobalRole;
var TenantAdminAuditLog = RdbmsService.Models.Tenant.TenantAdminAuditLog;
var Tenant = RdbmsService.Models.Tenant.Tenant;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var AerospikeProfileSync = KeyvalueService.Models.AerospikeProfileSync;
var AerospikeUser = KeyvalueService.Models.AerospikeUser;
var logger = require('nodejs-logger')();

module.exports = {
    /**
     * Converts user profiles into a format suitable for the elastic search engine
     * @param document
     * @returns {*}
     */
    profileElasticIndexConversion: function (document) {
        if (document.person) {
            document.person.searchName = "";
            if (document.showFullName) {
                if (document.person.firstName) {
                    document.person.searchName += document.person.firstName + " ";
                }
                if (document.person.lastName) {
                    document.person.searchName += document.person.lastName;
                }
            } else {
                if (document.person.nickname) {
                    document.person.searchName += document.person.nickname;
                }
            }
        }
        return document;
    },

    /**
     * Check if an admin audit log needs to be created for the given user by comparing
     * the Aerospike profile with the MySQL profile before doing the profile sync.
     * The following fields are being tracked:
     *    * email
     *    * phone
     *    * tenantId - if it has admin role for the given tenant
     *    * firstName
     *    * lastName
     */
    adminAuditLogGenerate: function (aerospikeProfile, callback) {
        try {
            var adminRoleAerospike = false, adminRoleDB = false;
            var adminRolesAerospikeList = [], adminRolesDBList = [];

            var dbProfile = {};
            var changes = {};
            var emails = [], emailsDb = [];
            var adminAuditLogs = [];

            var isAdminRole = function (role) {
                return role.startsWith('TENANT_') && role.endsWith(ProfileHasRole.constants().ROLE_ADMIN);
            }

            return async.series([
                // Get all the old profile data first
                function (next) {

                    try {
                        var sqlQuery = 'SELECT p.userId, p.firstName, p.lastName, ' +
                                    '   GROUP_CONCAT(DISTINCT pe.email SEPARATOR ",") AS emails, ' +
                                    '   GROUP_CONCAT(DISTINCT pp.phone SEPARATOR ",") AS phones, ' +
                                    '   GROUP_CONCAT(IF (phr.tenantId IS null, "", CONCAT_WS("_", "TENANT", phr.tenantId, "ADMIN")) SEPARATOR ",") AS tenantRoles ' +
                                    ' FROM profile p ' +
                                    '   LEFT JOIN profile_email pe ON pe.profileId = p.userId ' +
                                    '   LEFT JOIN profile_phone pp ON pp.profileId = p.userId ' +
                                    '   LEFT JOIN profile_has_role phr ON phr.profileId = p.userId AND phr.role = "ADMIN" ' +
                                    ' WHERE p.userId = :userId' +
                                    ' GROUP BY p.userId';

                        CrudHelper.rawQuery(sqlQuery, { userId: aerospikeProfile.userId }, function (err, data) {
                            try {
                                if (err) {
                                    logger.error("profileSyncService.adminAuditLogGenerate error retrieving profile from db", err);
                                    return next(Errors.DatabaseApi.FatalError);
                                }

                                if (_.isEmpty(data)) {
                                    logger.info("profileSyncService.adminAuditLogGenerate - profile not found", aerospikeProfile.userId);
                                    adminRoleDB = false;
                                    return setImmediate(next, false);
                                }

                                dbProfile = data.shift(); // get first row result

                                logger.info("profileSyncService.adminAuditLogGenerate db profile", dbProfile);
                                if (!_.isNull(dbProfile.emails) && dbProfile.emails.length > 0) {
                                    emailsDb = dbProfile.emails.split(',');
                                }

                                var tenantRoles = [];
                                if (!_.isNull(dbProfile.tenantRoles) && dbProfile.tenantRoles.length > 0) {
                                    tenantRoles = dbProfile.tenantRoles.split(',');
                                }

                                adminRoleDBList = _.filter(tenantRoles, isAdminRole);
                                adminRoleDB = adminRoleDBList.length > 0 ? true : false;
                                return next(false);
                            } catch (ex) {
                                logger.error("profileSyncService.adminAuditLogGenerate exception", ex, dbProfile);
                                return next(ex);
                            }
                        });
                    } catch (ex) {
                        logger.error("profileSyncService.adminAuditLogGenerate exception retriving existing profile information", ex, dbProfile);
                        return next(Errors.DatabaseApi.FatalError);
                    }
                },
                // Validate if there are admin roles setup in aerospike profile 
                function (next) {
                    try {
                        if (_.has(aerospikeProfile, 'roles')) {
                            adminRolesAerospikeList = _.filter(aerospikeProfile.roles, isAdminRole);
                            adminRoleAerospike = adminRolesAerospikeList.length > 0 ? true : false;
                        }

                        // Check if user has or had ADMIN role at one point, create trail entry only for these
                        if (adminRoleAerospike || adminRoleDB) {
                            return next(false);
                        } else {
                            logger.info("profileSyncService.adminAuditLogGenerate user to sync did not has/had admin role, skiping");
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                    } catch (ex) {
                        logger.error("profileSyncService.adminAuditLogGenerate exception checking roles");
                        return next(Errors.DatabaseApi.FatalError);
                    }
                },
                // Check for differences in profile firstName and lastName 
                function (next) {
                    try {
                        if (_.has(aerospikeProfile, 'person.firstName') && aerospikeProfile.person.firstName != dbProfile.firstName) {
                            changes['firstName'] = {
                                "old": dbProfile.firstName,
                                "new": aerospikeProfile.person.firstName
                            }
                        }
                        if (_.has(aerospikeProfile, 'person.lastName') && aerospikeProfile.person.lastName != dbProfile.firstName) {
                            changes['lastName'] = {
                                "old": dbProfile.lastName,
                                "new": aerospikeProfile.person.lastName
                            }
                        }
                        return setImmediate(next, false);
                    } catch (ex) {
                        logger.error("profileSyncService.adminAuditLogGenerate exception checking for differences for firstName and lastName", ex);
                        return next(Errors.DatabaseApi.FatalError);
                    }
                },
                // Get list of emails for which we need to create entries
                function (next) {
                    try {
                        if (_.has(aerospikeProfile, 'emails') && !_.isEmpty(aerospikeProfile.emails)) {
                            _.forEach(aerospikeProfile.emails, function (email) {
                                emails.push(email.email);
                            });
                        } else {
                            logger.error("profileSyncService.adminAuditLogGenerate aerospike profile email list is empty");
                            return setImmediate(next, Errors.DatabaseApi.FatalError);
                        }

                        _.forEach(emailsDb, function (email) {
                            emails.push(email);
                        });

                        emails = _.uniq(emails);

                        return setImmediate(next, false);
                    } catch (ex) {
                        logger.error("profileSyncService.adminAuditLogGenerate exception checking for differences for firstName and lastName", ex);
                        return next(Errors.DatabaseApi.FatalError);
                    }
                },
                function (next) {
                    if (!_.isEmpty(changes)) {
                        _.forEach(emails, function (email) {
                            adminAuditLogs.push( {
                                userId: aerospikeProfile.userId,
                                email: email,
                                createDate: _.now(),
                                items: _.clone(changes)
                            });
                        });
                    }
                    return setImmediate(next, false);
                }
            ], function (err) {
                if (err && err != Errors.DatabaseApi.NoRecordFound) {
                    logger.info("profileSyncService.adminAuditLogGenerate could not create audit trail entry", err);
                    return setImmediate(callback, false, false);
                }

                // We treat profile audit errors as non fatal
                return setImmediate(callback, false, adminAuditLogs);
            });
        } catch (ex) {
            logger.error("profileSyncService.adminAuditLogGenerate exception");
            return setImmediate(callback, false);
        }
    },

    /**
     * Synchronizes profiles from Aerospike to MySQL
     * @param {{}} params
     * @param {*} callback
     * @returns {*}
     */
    profileSync: function (params, clientInstances, callback) {
        var self = this;
        var profileSyncList = [];
        var clientsToDisconnect = [];
        try {
            // Initialize client instances if called from background processes
            if (!clientInstances) {
                // Setup media service client
                var MediaServiceClient = require('nodejs-media-service-client');
                var mediaServiceClient = new MediaServiceClient({
                    registryServiceURIs: Config.registryServiceURIs,
                    ownerServiceName: 'profileSyncService'
                });
                // Setup profile service client
                var ProfileServiceClient = require('nodejs-profile-service-client');
                var profileServiceClient = new ProfileServiceClient(null, null, 'profileSyncService');
                // Setup userMesssage service client
                var UserMessageClient = require('nodejs-user-message-client');
                var userMessageClient = new UserMessageClient({
                    registryServiceURIs: Config.registryServiceURIs,
                    ownerServiceName: 'profileSyncService'
                });
                // Put client instances
                clientInstances = {
                    clientInfo: null,
                    mediaServiceClientInstance: mediaServiceClient,
                    profileServiceClientInstance: profileServiceClient,
                    userMessageServiceClientInstance: userMessageClient
                };
                
                clientsToDisconnect = [mediaServiceClient, profileServiceClient, userMessageClient];
            }
            var elastikBulk = ElasticSearch.createBulk("profile", "profile", self.profileElasticIndexConversion);
            return async.series([
                // Read profile list from Aerospike profileSync set or get from userIds parameter when called from API
                function (serie) {
                    if (_.has(params, 'usersIds') && _.isArray(params.usersIds) && params.usersIds.length > 0) {
                        _.forEach(params.usersIds, function (userId) {
                            profileSyncList.push({ profile: userId });
                        });
                        return serie();
                    }
                    var query = {
                        filters: [{
                            'range': { 'randomValue': [0, 999] }
                        }]
                    };
                    AerospikeProfileSync.findAll(query, function (err, data) {
                        if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                            return serie(err);
                        }
                        if (err !== Errors.DatabaseApi.NoRecordFound) {
                            profileSyncList = _.cloneDeep(data);
                        }
                        return serie();
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                profileSyncList = _.uniq(profileSyncList);
                return async.everySeries(profileSyncList, function (profileSyncRecord, every) {
                    if (!profileSyncRecord.profile) {
                        return every(null, true);
                    }
                    logger.info('ProfileSyncService.profileSync: syncing ' + profileSyncRecord.profile);
                    var valid = true;
                    var userDefinedList = false;
                    if (_.has(params, 'usersIds') && _.isArray(params.usersIds) && params.usersIds.length > 0) {
                        userDefinedList = true;
                    }
                    var create = true;
                    var remove = false;
                    var profile = undefined;
                    var adminAuditLogEntries = [];
                    var profileItem = {
                        userId: profileSyncRecord.profile
                    };
                    var anonymousOrNotValidated = false;
                    return async.series([
                        // Synchronize base profile info:
                        // - get actual profile data from Aerospike, detect if profile has to be removed
                        // - check actual profile data (roles, emails, phones)
                        // - add profile to elastic search
                        // - get actual profile data from MySQL
                        // - generate admin trail data by looking at the profile differences between Aerospike and MySQL
                        // - merge base profile datsa from Aerospike to MySQL (if profile hasn't to be removed)
                        // - seek for languageId by language if passed (if profile hasn't to be removed)
                        // - seek for regionalSettingId by region if passed (if profile hasn't to be removed)
                        // - create/update base entry
                        // - update emails
                        // - update phones
                        // - update global roles
                        // - update roles only if account is anonymous or not validated, otherwise they are maintained by addRemoveRoles method
                        // - update application usage
                        // - update tenant usage
                        // create admin trail logs if any
                        // Remove profile only if:
                        // - getUserProfile got empty profile (contains no userId)
                        // Log warnings, but continue to sync only if following inconsistency detected:
                        // - no languageId exists by Aerospike profile language code
                        // - no regionalSettingId exists by Aerospike profile region code
                        // - no applicationId exists inside Aerospike profile applications object
                        // - no tenantId exists inside Aerospike profile tenants object
                        function (next) {
                            try {
                                return clientInstances.profileServiceClientInstance.getUserProfile({
                                    userId: profileSyncRecord.profile,
                                    //clientInfo: clientInstances.clientInfo
                                }, undefined, function (err, message) {
                                    if (err || message.getError()) {
                                        var errorFromProfile = err || message.getError();
                                        if ((_.isString(errorFromProfile) && errorFromProfile === Errors.ProfileApi.ValidationFailed) ||
                                            (_.isObject(errorFromProfile) && errorFromProfile.message === Errors.ProfileApi.ValidationFailed)) {
                                            // Don't skip if getProfile returned validation failed message
                                            // Send emails to all active admins
                                            logger.error('ProfileSyncService.profileSync: profile.getProfile', err || message.getError());
                                            valid = false;
                                            return next();
                                        } else if ((_.isString(errorFromProfile) && errorFromProfile === Errors.DatabaseApi.NoRecordFound) ||
                                            (_.isObject(errorFromProfile) && errorFromProfile.message === Errors.DatabaseApi.NoRecordFound)) {
                                            remove = true;
                                            return next();
                                        }
                                        logger.error('ProfileSyncService.profileSync: profile.getProfile', err || message.getError());
                                        return next(err || message.getError());
                                    }
                                    profile = message.getContent().profile;
                                    logger.debug('ProfileSyncService.profileSync: profile.getProfile', JSON.stringify(profile));
                                    if (!profile || !_.has(profile, 'userId')) {
                                        remove = true;
                                    }
                                    return next();
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: profile.getProfile', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        function (next) {
                            try {
                                if (!valid) {
                                    return ProfileService.notify(profileSyncRecord.profile, clientInstances, next);
                                }
                                if (remove) {
                                    return setImmediate(next, null);
                                }
                                return ProfileService.check(profile, clientInstances, true, function (err, verifiedProfile) {
                                    if (err) {
                                        logger.error('ProfileSyncService.profileSync: ProfileService.check');
                                        return next(err);
                                    }
                                    profile = verifiedProfile;
                                    logger.debug('ProfileSyncService.profileSync: ProfileService.check', JSON.stringify(profile));
                                    return next();
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: ProfileService.check', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                elastikBulk.push(profileSyncRecord.profile, profile);
                                if (elastikBulk.bulk.length > 100) {
                                    return ElasticSearch.addUpdateBulk(elastikBulk, function (err) {
                                        elastikBulk = ElasticSearch.createBulk("profile", "profile", self.profileElasticIndexConversion);
                                        return next(err);
                                    });
                                } else {
                                    return next();
                                }
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: ElasticSearch', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                return Profile.findOne({
                                    where: { userId: profileSyncRecord.profile }
                                }).then(function (existingProfile) {
                                    if (existingProfile) {
                                        create = false;
                                        profileItem = _.cloneDeep(existingProfile.get({ plain: true }));
                                        logger.info('ProfileSyncService.profileSync: profile to be updated');
                                    } else {
                                        logger.info('ProfileSyncService.profileSync: profile to be created');
                                    }
                                    return next();
                                }).catch(function (err) {
                                    logger.error('ProfileSyncService.profileSync: Profile.findOne');
                                    return CrudHelper.callbackError(err, next);
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: Profile.findOne', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        function (next) {
                            if (!valid) {
                                return setImmediate(next, null);
                            }
                            try {
                                return self.profileRemove(profileSyncRecord.profile, remove, clientInstances, next);
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: profileRemove', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                return self.adminAuditLogGenerate(profile, function (err, result) {
                                    if (err) {
                                        logger.error("ProfileSyncService.profileSync: adminAuditLogGenerate generation failure", err);
                                        return setImmediate(next, null);
                                    }
                                    adminAuditLogEntries = result;
                                    return setImmediate(next, null);
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: adminAuditLogGenerate', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                // Update common attributes
                                AutoMapper.mapDefined(profile, profileItem)
                                    .forMember('autoShare')
                                    .forMember('organization')
                                    .forMember('profilePhotoId', 'profilePhoto')
                                    .forMember('recommendedBy', 'recommendedByProfileId');
                                // Update person attributes
                                if (_.has(profile, 'person')) {
                                    AutoMapper.mapDefined(profile.person, profileItem)
                                        .forMember('firstName')
                                        .forMember('lastName')
                                        .forMember('nickname')
                                        .forMember('birthDate');
                                    if (_.has(profile.person, 'sex')) {
                                        var sexEnum = {
                                            'M': Profile.constants().SEX_MALE,
                                            'F': Profile.constants().SEX_FEMALE,
                                        };
                                        profileItem.sex = sexEnum[profile.person.sex];
                                    }
                                }
                                // Update address attributes
                                if (_.has(profile, 'address')) {
                                    AutoMapper.mapDefined(profile.address, profileItem)
                                        .forMember('street', 'addressStreet')
                                        .forMember('streetNumber', 'addressStreetNumber')
                                        .forMember('city', 'addressCity')
                                        .forMember('postalCode', 'addressPostalCode')
                                        .forMember('country', 'addressCountry');
                                }
                                return next();
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: merge base entry', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                if (!_.has(profile, 'language') || (_.has(profile, 'language') && !profile.language)) {
                                    return setImmediate(next, null);
                                }
                                LanguageService.languageGetByIso({ iso: profile.language }, function (err, language) {
                                    if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                                        logger.error('ProfileSyncService.profileSync: LanguageService.languageGetByIso');
                                        return next(err);
                                    }
                                    if (!language) {
                                        logger.warn('ProfileSyncService.profileSync: languageId not found by ISO code ' + profile.language);
                                        return next();
                                    }
                                    profileItem.languageId = language.id;
                                    return next();
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: seek for languageId', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                if (!_.has(profile, 'region') || (_.has(profile, 'region') && !profile.region)) {
                                    return setImmediate(next, null);
                                }
                                LanguageService.regionalSettingGetByIso({ iso: profile.region }, function (err, regionalSetting) {
                                    if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                                        logger.error('ProfileSyncService.profileSync: LanguageService.regionalSettingGetByIso');
                                        return next(err);
                                    }
                                    if (!regionalSetting) {
                                        logger.warn('ProfileSyncService.profileSync: regionalSettingId not found by ISO code ' + profile.region);
                                        return next();
                                    }
                                    profileItem.regionalSettingId = regionalSetting.id;
                                    return next();
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: seek for regionalSettingId', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                if (create) {
                                    return Profile.create(profileItem).then(function (profile) {
                                        if (!profile) {
                                            return next(Errors.DatabaseApi.NoRecordFound);
                                        }
                                        return next();
                                    }).catch(function (err) {
                                        logger.error('ProfileSyncService.profileSync: Profile.create', JSON.stringify(profileItem));
                                        return CrudHelper.callbackError(err, next);
                                    });
                                } else {
                                    return Profile.update(profileItem, { where: { userId: profileItem.userId } }).then(function (count) {
                                        if (count[0] === 0) {
                                            return next(Errors.DatabaseApi.NoRecordFound);
                                        }
                                        return next();
                                    }).catch(function (err) {
                                        logger.error('ProfileSyncService.profileSync: Profile.update', JSON.stringify(profileItem));
                                        return CrudHelper.callbackError(err, next);
                                    });
                                }
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: Profile.create/update', ex);
                                return next(ex);
                            }
                        },
                        // Synchronize emails:
                        // - always delete all for given profile id
                        // - bulk create for given profile id (if exists and profile hasn't to be removed)
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                if (!_.has(profile, 'emails')) {
                                    return setImmediate(next, null);
                                }
                                var emails = [];
                                _.forEach(profile.emails, function (email) {
                                    emails.push({ profileId: profileItem.userId, email: email.email, verificationStatus: email.verificationStatus });
                                });
                                if (emails.length === 0) {
                                    return setImmediate(next, null);
                                }
                                return ProfileEmail.bulkCreate(emails, {
                                    updateOnDuplicate: [
                                        ProfileEmail.tableAttributes.verificationStatus.field,
                                        ProfileEmail.tableAttributes.profileId.field
                                    ]
                                }).then(function (records) {
                                    if (records.length !== emails.length) {
                                        return next(Errors.ProfileApi.InconsistencyDetected);
                                    }
                                    return next();
                                }).catch(function (err) {
                                    logger.error('ProfileSyncService.profileSync: ProfileEmail.bulkCreate', JSON.stringify(emails));
                                    return CrudHelper.callbackError(err, next);
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: ProfileEmail.bulkCreate', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        // Synchronize phones:
                        // - always delete all for given profile id
                        // - bulk create for given profile id (if exists and profile hasn't to be removed)
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                if (!_.has(profile, 'phones')) {
                                    return setImmediate(next, null);
                                }
                                var phones = [];
                                _.forEach(profile.phones, function (phone) {
                                    phones.push({ profileId: profileItem.userId, phone: phone.phone, verificationStatus: phone.verificationStatus });
                                });
                                if (phones.length === 0) {
                                    return setImmediate(next, null);
                                }
                                return ProfilePhone.bulkCreate(phones, {
                                    updateOnDuplicate: [
                                        ProfilePhone.tableAttributes.verificationStatus.field,
                                        ProfilePhone.tableAttributes.profileId.field
                                    ]
                                }).then(function (records) {
                                    if (records.length !== phones.length) {
                                        return next(Errors.ProfileApi.InconsistencyDetected);
                                    }
                                    return next();
                                }).catch(function (err) {
                                    logger.error('ProfileSyncService.profileSync: ProfilePhone.bulkCreate', JSON.stringify(phones));
                                    return CrudHelper.callbackError(err, next);
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: ProfilePhone.bulkCreate', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        // Synchronize global roles: 
                        // - always delete all for given profile id
                        // - bulk create for given profile id (if exists and profile hasn't to be removed)
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                if (!_.has(profile, 'roles')) {
                                    return setImmediate(next, null);
                                }
                                var globalRoles = [];
                                _.forEach(profile.roles, function (role) {
                                    if (_.isString(role) && !role.startsWith('TENANT_')) {
                                        globalRoles.push({ profileId: profileItem.userId, role: role });
                                        if (role === AerospikeUser.ROLE_ANONYMOUS || role === AerospikeUser.ROLE_NOT_VALIDATED) {
                                            anonymousOrNotValidated = true;
                                        }
                                    }
                                });
                                if (globalRoles.length === 0) {
                                    return setImmediate(next, null);
                                }
                                return ProfileHasGlobalRole.bulkCreate(globalRoles, {
                                    updateOnDuplicate: [
                                        ProfileHasGlobalRole.tableAttributes.role.field,
                                        ProfileHasGlobalRole.tableAttributes.profileId.field
                                    ]
                                }).then(function (records) {
                                    if (records.length !== globalRoles.length) {
                                        return next(Errors.ProfileApi.InconsistencyDetected);
                                    }
                                    return next();
                                }).catch(function (err) {
                                    logger.error('ProfileSyncService.profileSync: ProfileHasGlobalRole.bulkCreate', JSON.stringify(globalRoles));
                                    return CrudHelper.callbackError(err, next);
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: ProfileHasGlobalRole.bulkCreate', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        // Synchronize roles: 
                        // - bulk create for given profile id and set applied status, only if there is anonymous account
                        // - bulk update for given profile: set applied status = approved for all roles, only if there is registered account
                        function (next) {
                            if (!valid || remove) {
                                return setImmediate(next, null);
                            }
                            if (!_.has(profile, 'roles')) {
                                return setImmediate(next, null);
                            }
                            try {
                                var roles = [];
                                _.forEach(profile.roles, function (role) {
                                    if (_.isString(role) && role.startsWith('TENANT_')) {
                                        var tenantId = parseInt(role.substring(7, role.lastIndexOf('_')));
                                        var tenantRole = role.substring(role.lastIndexOf('_') + 1);
                                        roles.push({
                                            'profileId': profileItem.userId,
                                            'tenantId': tenantId,
                                            'role': tenantRole,
                                            'status': anonymousOrNotValidated ? ProfileHasRole.constants().STATUS_APPLIED : ProfileHasRole.constants().STATUS_APPROVED,
                                            'requestDate': anonymousOrNotValidated ? _.now() : null,
                                            'acceptanceDate': anonymousOrNotValidated ? null : _.now()
                                        });
                                    }
                                });
                                if (roles.length === 0) {
                                    return setImmediate(next, null);
                                }
                                return ProfileHasRole.bulkCreate(roles, {
                                    updateOnDuplicate: [
                                        ProfileHasRole.tableAttributes.profileId.field,
                                        ProfileHasRole.tableAttributes.tenantId.field,
                                        ProfileHasRole.tableAttributes.role.field,
                                        ProfileHasRole.tableAttributes.role.status
                                    ]
                                }).then(function (records) {
                                    if (records.length !== roles.length) {
                                        return next(Errors.ProfileApi.InconsistencyDetected);
                                    }
                                    return next();
                                }).catch(function (err) {
                                    logger.error('ProfileSyncService.profileSync: ProfileHasRole.bulkCreate', JSON.stringify(profile.roles), JSON.stringify(roles));
                                    return CrudHelper.callbackError(err, next);
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: ProfileHasRole.bulkCreate', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        // Synchronize applications: 
                        // - always delete all for given profile id
                        // - bulk create for given profile id (if exists and profile hasn't to be removed)
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                if (!_.has(profile, 'applications')) {
                                    return setImmediate(next, null);
                                }
                                var applications = [];
                                _.forEach(profile.applications, function (application) {
                                    if (!_.isEmpty(application)) {
                                        applications.push({ profileId: profileItem.userId, applicationId: application });
                                    }
                                });
                                if (applications.length === 0) {
                                    return setImmediate(next, null);
                                }
                                return ProfileHasApplication.bulkCreate(applications, {
                                    updateOnDuplicate: [
                                        ProfileHasApplication.tableAttributes.profileId.field,
                                        ProfileHasApplication.tableAttributes.applicationId.field
                                    ]
                                }).then(function (records) {
                                    if (records.length !== applications.length) {
                                        return next(Errors.ProfileApi.InconsistencyDetected);
                                    }
                                    return next();
                                }).catch(function (err) {
                                    if (_.has(err, 'name') && err.name === 'SequelizeForeignKeyConstraintError') {
                                        logger.warn('ProfileSyncService.profileSync: applicationId may not exist inside ' + JSON.stringify(profile.applications));
                                        return next();
                                    }
                                    return next(err);
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: ProfileHasApplication.bulkCreate', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        // Synchronize tenant usage: 
                        // - always delete all for given profile id
                        // - bulk create for given profile id (if exists and profile hasn't to be removed)
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                if (!_.has(profile, 'tenants')) {
                                    return setImmediate(next, null);
                                }
                                var tenants = [];
                                _.forEach(profile.tenants, function (tenant) {
                                    tenants.push({ profileId: profileItem.userId, tenantId: tenant });
                                });
                                if (tenants.length === 0) {
                                    return setImmediate(next, null);
                                }
                                return ProfileHasTenant.bulkCreate(tenants, {
                                    updateOnDuplicate: [
                                        ProfileHasTenant.tableAttributes.profileId.field,
                                        ProfileHasTenant.tableAttributes.tenantId.field
                                    ]
                                }).then(function (records) {
                                    if (records.length !== tenants.length) {
                                        return next(Errors.ProfileApi.InconsistencyDetected);
                                    }
                                    return next();
                                }).catch(function (err) {
                                    if (_.has(err, 'name') && err.name === 'SequelizeForeignKeyConstraintError') {
                                        logger.warn('ProfileSyncService.profileSync: tenantId may not exist inside ' + JSON.stringify(profile.tenants));
                                        return next();
                                    }
                                    return CrudHelper.callbackError(err, next);
                                });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: ProfileHasTenant.bulkCreate', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        // Bulk create tenantAdminAuditLog entries
                        function (next) {
                            try {
                                if (!valid || remove) {
                                    return setImmediate(next, null);
                                }
                                if (_.isEmpty(adminAuditLogEntries)) {
                                    return setImmediate(next, null);
                                }
                                
                                return TenantAdminAuditLog.bulkCreate(adminAuditLogEntries, {
                                    updateOnDuplicate: [
                                        TenantAdminAuditLog.tableAttributes.userId.field,
                                        TenantAdminAuditLog.tableAttributes.email.field,
                                        TenantAdminAuditLog.tableAttributes.items.field,
                                        TenantAdminAuditLog.tableAttributes.createDate.field
                                    ]
                                }).then(function (records) {
                                        if (records.length !== adminAuditLogEntries.length) {
                                            return next(Errors.ProfileApi.InconsistencyDetected);
                                        }
                                        return next();
                                    }).catch(function (err) {
                                        logger.error('ProfileSyncService.profileSync: TenantAdminAuditLog.bulkCreate', JSON.stringify(adminAuditLogEntries));
                                        return CrudHelper.callbackError(err, next);
                                    });
                            } catch (ex) {
                                logger.error('ProfileSyncService.profileSync: TenantAdminAuditLog.bulkCreate', ex);
                                return setImmediate(next, ex);
                            }
                        },

                        // Remove profile from Aerospike profile sync queue
                        // Skip profile removal only if at least one warning is logged
                        function (next) {
                            if (userDefinedList) {
                                return setImmediate(next, null);
                            }
                            return AerospikeProfileSync.remove(profileSyncRecord, next);
                        }
                    ], function (err) {
                        if (err) {
                            logger.error('ProfileSyncService.profileSync: merge failed', err);
                        } else {
                            logger.info('ProfileSyncService.profileSync: merge done');
                        }
                        return every(null, true);
                    });
                }, function (err) {
                    // disconnect service clients
                    disconnectServiceClients(clientsToDisconnect);
                    
                    if (err) {
                        return CrudHelper.callbackError(err, callback);
                    }
                    //Updating any remaining items
                    if (elastikBulk.bulk.length > 0) {
                        return ElasticSearch.addUpdateBulk(elastikBulk, function (err) {
                            if (err) {
                                return CrudHelper.callbackError(err, callback);
                            }
                            return CrudHelper.callbackSuccess(null, callback);
                        });
                    }
                    return CrudHelper.callbackSuccess(null, callback);
                });
            });
        } catch (ex) {
            // disconnect service clients
            disconnectServiceClients(clientsToDisconnect);
            
            return CrudHelper.callbackError(ex, callback);
        }
        
        function disconnectServiceClients(clients) {
            for(var i=0; i<clients.length; i++) {
                try {
                    clients[i].disconnect();
                    logger.debug('ProfileSyncService.profileSync client disconnected', clients[i].constructor.name);
                } catch (e) {
                    logger.error('ProfileSyncService.profileSync error on service client disconnect:', e, clients[i].constructor.name);
                }
            }
        }
    },

    /**
     * Remove following MySQL entries owned by profile:
     * - user from Elastic Search engine (if removeProfile === true)
     * - emails
     * - phones
     * - global roles
     * - tenant roles (only approved ones)
     * - links to applications
     * - update profile entries where given profile is recommended by (if removeProfile === true)
     * - profile picture media entry (if removeProfile === true)
     * - profile entry (if removeProfile === true)
     * @param profileId Profile ID
     * @param removeProfile Flag to remove all profile data
     * @param clientInstances
     * @param callback
     * @returns
     */
    profileRemove: function (profileId, removeProfile, clientInstances, callback) {
        var self = this;
        try {
            if (removeProfile) {
                logger.info('ProfileSyncService.profileRemove: removing whole profile (if exists)', profileId);
            }
            return async.series([
                function (next) {
                    try {
                        if (!removeProfile) {
                            return setImmediate(next);
                        }
                        return ElasticSearch.deleteDocumentIfExists('profile', 'profile', profileId, next);
                    } catch (ex) {
                        logger.error('ProfileSyncService.profileRemove: ElasticSearch.deleteDocumentIfExists', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        return ProfileEmail.destroy({ where: { profileId: profileId } }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileSyncService.profileRemove: ProfileEmail.destroy');
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileSyncService.profileRemove: ProfileEmail.destroy', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        return ProfilePhone.destroy({ where: { profileId: profileId } }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileSyncService.profileRemove: ProfilePhone.destroy');
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileSyncService.profileRemove: ProfilePhone.destroy', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        return ProfileHasGlobalRole.destroy({ where: { profileId: profileId } }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileSyncService.profileRemove: ProfileHasGlobalRole.destroy');
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileSyncService.profileRemove: ProfileHasGlobalRole.destroy', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        var where = {
                            profileId: profileId
                        };
                        if (!removeProfile) {
                            where.status = ProfileHasRole.constants().STATUS_APPROVED;
                        }
                        return ProfileHasRole.destroy({
                            where: where
                        }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileSyncService.profileRemove: ProfileHasRole.destroy');
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileSyncService.profileRemove: ProfileHasRole.destroy', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        return ProfileHasApplication.destroy({ where: { profileId: profileId } }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileSyncService.profileRemove: ProfileHasApplication.destroy');
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileSyncService.profileRemove: ProfileHasApplication.destroy', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        return ProfileHasTenant.destroy({ where: { profileId: profileId } }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileSyncService.profileRemove: ProfileHasTenant.destroy');
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileSyncService.profileRemove: ProfileHasTenant.destroy', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!removeProfile) {
                            return setImmediate(next);
                        }
                        return Profile.update({ recommendedByProfileId: null }, { where: { recommendedByProfileId: profileId } }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileSyncService.profileRemove: Profile.update [recommendedByProfileId]');
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileSyncService.profileRemove: Profile.update [recommendedByProfileId]', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!removeProfile) {
                            return setImmediate(next);
                        }
                        return ProfileService.profileHasPhoto(profileId, function (err, result) {
                            if (err) {
                                return next(err);
                            }
                            if (result) {
                                return ProfileService.deleteProfilePicture(profileId, clientInstances, next);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileSyncService.profileRemove: Profile.update [recommendedByProfileId]', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!removeProfile) {
                            return setImmediate(next);
                        }
                        return Profile.destroy({ where: { userId: profileId } }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileSyncService.profileRemove: Profile.destroy');
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileSyncService.profileRemove: Profile.destroy', ex);
                        return setImmediate(next, ex);
                    }
                },
            ], callback);
        } catch (ex) {
            logger.error('ProfileSyncService.profileRemove', ex);
            return setImmediate(callback, ex);
        }
    }
};