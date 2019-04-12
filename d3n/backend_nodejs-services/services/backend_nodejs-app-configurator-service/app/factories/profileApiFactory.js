var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var RoleService = require('./../services/roleService.js');
var TenantService = require('./../services/tenantService.js');
var ProfileService = require('./../services/profileService.js');
var ProfileSyncService = require('./../services/profileSyncService.js');
var LanguageService = require('./../services/languageService.js');
var DatabaseInstance = require('nodejs-database').getInstance(Config);
var RdbmsService = DatabaseInstance.RdbmsService;
var Profile = RdbmsService.Models.ProfileManager.Profile;
var ProfileEmail = RdbmsService.Models.ProfileManager.ProfileEmail;
var ProfilePhone = RdbmsService.Models.ProfileManager.ProfilePhone;
var ProfileHasApplication = RdbmsService.Models.ProfileManager.ProfileHasApplication;
var ProfileHasTenant = RdbmsService.Models.ProfileManager.ProfileHasTenant;
var ProfileHasRole = RdbmsService.Models.ProfileManager.ProfileHasRole;
var ProfileHasStats = RdbmsService.Models.ProfileManager.ProfileHasStats;
var ProfileHasGlobalRole = RdbmsService.Models.ProfileManager.ProfileHasGlobalRole;
var Tenant = RdbmsService.Models.Tenant.Tenant;
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var logger = require('nodejs-logger')();

var INSTANCE_MAPPINGS = {
    'profileGetModel': [
        {
            destination: '$root',
            model: Profile
        },
        {
            destination: 'applicationsIds',
            model: ProfileHasApplication,
            attribute: ProfileHasApplication.tableAttributes.applicationId
        },
        {
            destination: 'emails',
            model: ProfileEmail,
            attributes: [
                { 'email': ProfileEmail.tableAttributes.email },
                { 'verificationStatus': ProfileEmail.tableAttributes.verificationStatus }
            ]
        },
        {
            destination: 'phones',
            model: ProfilePhone,
            attributes: [
                { 'phone': ProfilePhone.tableAttributes.phone },
                { 'verificationStatus': ProfilePhone.tableAttributes.verificationStatus }
            ]
        },
        {
            destination: 'roles',
            models: [ProfileHasRole, ProfileHasGlobalRole],
            attribute: ProfileHasRole.tableAttributes.role,
            omitSearchBy: true
        }
    ],
    profileCreateModel: [
        {
            destination: '$root',
            model: Profile
        },
    ],
};
INSTANCE_MAPPINGS['profileUpdateModel'] = INSTANCE_MAPPINGS['profileCreateModel'];

module.exports = {
    /**
     * Creates or updates a profile, sync to Aerospike profile
     * @param {{}} params
     * @param {*} callback
     * @returns {*}
     */
    profileUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var mapBySchema = 'profileCreateModel';
            if (_.has(params, 'userId') && params.userId) {
                create = false;
                mapBySchema = 'profileUpdateModel';
            }
            var profile = null;
            var profileItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var internalData = {};
            var clientInstances = CrudHelper.getClientInstances(clientSession);
            var profileExist = {};
            async.series([
                // Make sure that profile exist in Aerospike and MySQL to eliminate possible inconsistency 
                // (only for profileManager/ profileUpdate):
                //  - if there is no profile in Aerospike, return ERR_ENTRY_NOT_FOUND at the end of call
                //  - if there is no profile in MySQL, first call profileSync to import or remove profile
                function (next) {
                    try {
                        if (create) {
                            return next();
                        }
                        return ProfileService.exist(profileItem.userId, clientInstances, function (err, result) {
                            if (err) {
                                logger.error('ProfileApiFactory.profileUpdate: ProfileService.exist');
                                return next(err);
                            }
                            profileExist = result;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileUpdate: ProfileService.exist', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (create || !profileExist.syncRequired) {
                            return next();
                        }
                        // Synchronize explicitly, then return ERR_ENTRY_NOT_FOUND when profile not found in aerospike
                        return ProfileSyncService.profileSync({ userIds: [profileItem.userId] }, clientInstances, function (err) {
                            if (err) {
                                return next(err);
                            }
                            if (!profileExist.aerospike) {
                                logger.error('ProfileApiFactory.profileUpdate: not found in aerospike');
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileUpdate: ProfileSyncService.profileSync', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Gather language code if needed
                function (next) {
                    try {
                        if (!_.has(profileItem, 'languageId')) {
                            return setImmediate(next, null);
                        }
                        return LanguageService.languageGet({ id: profileItem.languageId }, function (err, language) {
                            if (err) {
                                logger.error('ProfileApiFactory.profileUpdate: not found language');
                                return next(err);
                            }
                            internalData.language = language.iso;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Gather region code if needed
                function (next) {
                    try {
                        if (!_.has(profileItem, 'regionalSettingId')) {
                            return setImmediate(next, null);
                        }
                        return LanguageService.regionalSettingGet({ id: profileItem.regionalSettingId }, function (err, regionalSetting) {
                            if (err) {
                                logger.error('ProfileApiFactory.profileUpdate: not found regional setting');
                                return next(err);
                            }
                            internalData.region = regionalSetting.iso;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Update Aerospike profile
                function (next) {
                    try {
                        return ProfileService.update(clientInstances, profileItem.userId, function (profile) {
                            // Update common attributes
                            AutoMapper.mapDefined(profileItem, profile)
                                .forMember('autoShare')
                                .forMember('createDate')
                                .forMember('recommendedByProfileId', 'recommendedBy');
                            AutoMapper.mapDefined(internalData, profile)
                                .forMember('language')
                                .forMember('region')
                            // Update person attributes
                            if (!profile.person) {
                                profile.person = {};
                            }
                            AutoMapper.mapDefined(profileItem, profile.person)
                                .forMember('firstName')
                                .forMember('lastName')
                                .forMember('nickname')
                                .forMember('birthDate')
                                .forMember('sex');
                            // Update address attributes
                            if (!profile.address) {
                                profile.address = {};
                            }
                            AutoMapper.mapDefined(profileItem, profile.address)
                                .forMember('addressStreet', 'street')
                                .forMember('addressStreetNumber', 'streetNumber')
                                .forMember('addressCity', 'city')
                                .forMember('addressPostalCode', 'postalCode')
                                .forMember('addressCountry', 'country');
                        }, function (err, userId) {
                            if (err) {
                                return next(err);
                            }
                            profileItem.userId = userId;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Create/update main entity
                function (next) {
                    if (create) {
                        return Profile.create(profileItem).then(function (profile) {
                            params.userId = profile.get({ plain: true }).userId;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return Profile.update(profileItem, { where: { userId: profileItem.userId } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            params.userId = profileItem.userId;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return self.profileGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Gets a profile
     * @param {{}} params
     * @param {*} callback
     * @returns {*}
     */
    profileGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'profile';
            var mapBySchema = 'profileGetModel';
            var attributes = _.keys(Profile.attributes);
            var include = CrudHelper.include(Profile, [
                ProfileHasApplication,
                ProfileHasTenant,
                ProfileHasRole,
                ProfileHasStats,
                ProfileHasGlobalRole,
                ProfileEmail,
                ProfilePhone
            ]);
            return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                // Always include tenant usage by session tenant
                include = CrudHelper.includeWhere(include, ProfileHasTenant, { tenantId: tenantId }, true);
                // Optionally include tenant roles by session tenant
                include = CrudHelper.includeWhere(include, ProfileHasRole, { tenantId: tenantId }, false);
                include = CrudHelper.includeWhere(include, ProfileHasStats, { tenantId: tenantId }, false);
                return Profile.findOne({
                    where: { userId: params.userId },
                    attributes: attributes,
                    include: include
                }).then(function (profile) {
                    try {
                        if (!profile) {
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                        }
                        
                        profileSetStats(profile);
                        
                        var profileItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, profile);
                        response[responseItem] = profileItem;
                        return CrudHelper.callbackSuccess(response, callback);
                        
                    } catch (e) {
                        return CrudHelper.callbackError(e, callback);
                    }
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Lists profiles
     * @param {{}} params
     * @param {*} callback
     * @returns {*}
     */
    profileList: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var mapBySchema = 'profileGetModel';
            var searchBy = CrudHelper.searchBy(params, Profile);
            var orderBy = CrudHelper.orderBy(params, Profile);
            var attributes = CrudHelper.distinctAttributes(Profile.tableAttributes.userId);
            var include;
            var profileItems = [];
            var total = 0;
            var sessionTenantId;
            async.series([
                // Get existing profile data, retrieve tenant information - only if tenantFromSession=true
                function (next) {
                    try {
                        TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                            if (err) {
                                return next(err);
                            }
                            sessionTenantId = tenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.profileList: retrieving tenantId failed', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Populate total count and profile ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    var mapping = _.cloneDeep(INSTANCE_MAPPINGS);
                    var customConditions = [];
                    var customInclude = [];
                    var rootModel = _.find(mapping[mapBySchema], { destination: '$root' });
                    if (_.has(params, 'searchBy') && _.has(params.searchBy, 'roles')) {
                        // Optionally include tenant roles by session tenant (if specified by search request)
                        var tenantRoles = _.intersection(['ADMIN', 'COMMUNITY', 'EXTERNAL', 'INTERNAL'], params.searchBy.roles);
                        if (tenantRoles.length > 0) {
                            customInclude.push(ProfileHasRole);
                            customConditions.push('(`profileHasRoles`.`role` IN ("' + tenantRoles.join('","') + '") AND `profileHasRoles`.`tenantId` = ' + sessionTenantId + ' AND `profileHasRoles`.`id` IS NOT NULL)');
                        }
                        // Optionally include global roles (if specified by search request)
                        var globalRoles = _.intersection(['ANONYMOUS', 'NOT_VALIDATED', 'REGISTERED', 'FULLY_REGISTERED', 'FULLY_REGISTERED_BANK', 'FULLY_REGISTERED_BANK_O18'], params.searchBy.roles);
                        if (globalRoles.length > 0) {
                            customInclude.push(ProfileHasGlobalRole);
                            customConditions.push('`profileHasGlobalRoles`.`role` IN ("' + globalRoles.join('","') + '")');
                        }
                    }

                    let fullNamefilter="";
                    if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                        fullNamefilter=' AND (`profile`.`firstName` like "%' + params.searchBy.fulltext + '%" OR `profile`.`lastName` like "%' + params.searchBy.fulltext + '%" OR `profile`.`nickname` like "%' + params.searchBy.fulltext + '%") ';
                    }

                    // Always include tenant usage by session tenant
                    customInclude.push(ProfileHasTenant);
                    var customQuery = '(`profileHasTenants`.`tenantId` = ' + sessionTenantId + ' AND `profileHasTenants`.`id` IS NOT NULL)'+fullNamefilter;
                    // Attach custom conditions as OR statements
                    if (customConditions.length > 0) {
                        var customQueryConditions = '';
                        _.forEach(customConditions, function (condition) {
                            if (customQueryConditions.length > 0) {
                                customQueryConditions += ' OR ';
                            }
                            customQueryConditions += condition;
                        });
                        customQuery += ' AND (' + customQueryConditions + ')';
                    }
                    rootModel.customQuery = customQuery;
                    rootModel.customInclude = customInclude;
                    searchBy = CrudHelper.searchByCustom(params, Profile, mapBySchema, mapping);
                    // Build include based on updated mapping
                    include = CrudHelper.includeDefined(Profile, [
                        ProfileHasApplication,
                        ProfileHasTenant,
                        ProfileHasRole,
                        ProfileHasStats,
                        ProfileHasGlobalRole,
                        ProfileEmail,
                        ProfilePhone
                    ], params, mapBySchema, mapping);
                    if (customConditions.length === 0) {
                        // Always include tenant usage by session tenant
                        include = CrudHelper.includeWhere(include, ProfileHasTenant, { tenantId: sessionTenantId }, true);
                        // Optionally include tenant roles by session tenant (if specified by search request)
                        include = CrudHelper.includeWhere(include, ProfileHasRole, { tenantId: sessionTenantId }, false);
                        include = CrudHelper.includeWhere(include, ProfileHasStats, { tenantId: sessionTenantId }, false);
                    }
                    include = CrudHelper.includeNoAttributes(include, Profile);
                    return Profile.count({
                        where: searchBy,
                        include: include,
                        distinct: true,
                        subQuery: false
                    }).then(function (count) {
                        total = count;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate profile ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    return Profile.findAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        attributes: attributes,
                        include: include,
                        where: searchBy,
                        order: orderBy,
                        subQuery: false,
                        raw: true
                    }).then(function (profiles) {
                        searchBy = CrudHelper.searchBy(params, Profile);
                        if (!_.has(searchBy, 'userId')) {
                            var profilesIds = [];
                            _.forEach(profiles, function (profile) {
                                profilesIds.push(profile.userId);
                            });
                            searchBy = {
                                userId: { '$in': profilesIds }
                            };
                        }
                        if (_.has(params, 'searchBy') && _.has(params.searchBy, 'roles')) {
                            delete params.searchBy.roles;
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate model list with all nested model elements
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    include = CrudHelper.include(Profile, [
                        ProfileHasApplication,
                        ProfileHasTenant,
                        ProfileHasRole,
                        ProfileHasStats,
                        ProfileHasGlobalRole,
                        ProfileEmail,
                        ProfilePhone
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    // Always include tenant usage by session tenant
                    include = CrudHelper.includeWhere(include, ProfileHasTenant, { tenantId: sessionTenantId }, true);
                    // Optionally include tenant roles by session tenant (if specified by search request)
                    include = CrudHelper.includeWhere(include, ProfileHasRole, { tenantId: sessionTenantId }, false);
                    include = CrudHelper.includeWhere(include, ProfileHasStats, { tenantId: sessionTenantId }, false);
                    return Profile.findAll({
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (profiles) {
                        for(var i=0; i<profiles.length; i++) {
                            profileSetStats(profiles[i]);
                        }
                        profileItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, profiles);
                        profileItems.forEach((profile) => {
                            if ( profile.firstName && profile.lastName) {
                                profile.fullName=profile.firstName+" "+profile.lastName;
                            }
                            if (profile.nickname) {
                                profile.fullName ? (profile.fullName=profile.fullName+" "+profile.nickname) : profile.fullName=profile.nickname;
                            }
                            if (!profile.nickname && profile.emails && profile.emails[0] && profile.emails[0].email) {
                                profile.fullName=profile.emails[0].email;
                            }
                        });

                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({
                    items: profileItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Adds and removes emails to a profile
     * @param {{}} params
     * @param {*} callback
     * @returns {*}
     */
    profileAddRemoveEmails: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('profileId')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var profile = null;
        var profileEmails = [];
        var clientInstances = CrudHelper.getClientInstances(clientSession);
        var profileExist = {};
        try {
            async.series([
                // Make sure that profile exist in Aerospike and MySQL to eliminate possible inconsistency:
                //  - if there is no profile in Aerospike, return ERR_ENTRY_NOT_FOUND at the end of call
                //  - if there is no profile in MySQL, first call profileSync to import or remove profile
                //  - check emails to be added for inconsistencies, do not notify admins if they are detected
                function (next) {
                    try {
                        return ProfileService.exist(params.profileId, clientInstances, function (err, result) {
                            if (err) {
                                logger.error('ProfileApiFactory.profileAddRemoveEmails: ProfileService.exist');
                                return next(err);
                            }
                            profileExist = result;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileAddRemoveEmails: ProfileService.exist', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!profileExist.syncRequired) {
                            return next();
                        }
                        // Synchronize explicitly, then return ERR_ENTRY_NOT_FOUND when profile not found in aerospike
                        return ProfileSyncService.profileSync({ userIds: [params.profileId] }, clientInstances, function (err) {
                            if (err) {
                                return next(err);
                            }
                            if (!profileExist.aerospike) {
                                logger.error('ProfileApiFactory.profileAddRemoveEmails: not found in aerospike');
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileAddRemoveEmails: ProfileSyncService.profileSync', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!params.hasOwnProperty('adds')) {
                            return next();
                        }
                        var profile = {
                            userId: params.profileId,
                            emails: _.map(params.adds, function (addEmail) {
                                return {
                                    email: addEmail,
                                    verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_NOT_VERIFIED
                                };
                            })
                        };
                        return ProfileService.check(profile, clientInstances, false, function (err, verifiedProfile) {
                            if (err) {
                                logger.error('ProfileApiFactory.profileAddRemoveEmails: ProfileService.check');
                                return next(err);
                            }
                            if (!_.isEqual(profile, verifiedProfile)) {
                                logger.error('ProfileApiFactory.profileAddRemoveEmails: ProfileService.check inconsistencies detected');
                                return next(Errors.ProfileApi.InconsistencyDetected);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileAddRemoveEmails: ProfileService.check', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Bulk remove emails if needed
                function (next) {
                    try {
                        if (!params.hasOwnProperty('removes')) {
                            return next();
                        }
                        var removes = params.removes;
                        return ProfileEmail.destroy({ where: { profileId: params.profileId, email: { '$in': removes } } }).then(function (count) {
                            if (count !== removes.length) {
                                return next(Errors.ProfileApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Bulk create emails if needed
                function (next) {
                    if (!params.hasOwnProperty('adds')) {
                        return next();
                    }
                    var adds = [];
                    _.forEach(params.adds, function (email) {
                        adds.push({ profileId: params.profileId, email: email, verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_NOT_VERIFIED });
                    });
                    try {
                        return ProfileEmail.bulkCreate(adds).then(function (records) {
                            if (records.length !== adds.length) {
                                return next(Errors.QuestionApi.InconsistencyDetected);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Find all profile emails
                function (next) {
                    try {
                        return ProfileEmail.findAll({ where: { profileId: params.profileId } }).then(function (emails) {
                            _.forEach(emails, function (email) {
                                var emailPlain = email.get({ plain: true });
                                profileEmails.push({ email: emailPlain.email, verificationStatus: emailPlain.verificationStatus });
                            });
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Update Aerospike profile
                function (next) {
                    try {
                        return ProfileService.update(clientInstances, params.profileId, function (profile) {
                            // Update email attributes
                            profile.emails = _.unionWith(profile.emails, profileEmails, _.isEqual);
                            if (params.hasOwnProperty('removes')) {
                                _.pullAllWith(profile.emails, params.removes, function (emailEntry, emailToRemove) { return emailEntry.email === emailToRemove; });
                            }
                            return true;
                        }, function (err, userId) {
                            if (err) {
                                return next(err);
                            }
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Adds and removes phones to a profile
     * @param {{}} params
     * @param {*} callback
     * @returns {*}
     */
    profileAddRemovePhones: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('profileId')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var profile = null;
        var profilePhones = [];
        var clientInstances = CrudHelper.getClientInstances(clientSession);
        var profileExist = {};
        try {
            async.series([
                // Make sure that profile exist in Aerospike and MySQL to eliminate possible inconsistency:
                //  - if there is no profile in Aerospike, return ERR_ENTRY_NOT_FOUND at the end of call
                //  - if there is no profile in MySQL, first call profileSync to import or remove profile
                //  - check phones to be added for inconsistencies, do not notify admins if they are detected
                function (next) {
                    try {
                        return ProfileService.exist(params.profileId, clientInstances, function (err, result) {
                            if (err) {
                                logger.error('ProfileApiFactory.profileAddRemovePhones: ProfileService.exist');
                                return next(err);
                            }
                            profileExist = result;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileAddRemovePhones: ProfileService.exist', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!profileExist.syncRequired) {
                            return next();
                        }
                        // Synchronize explicitly, then return ERR_ENTRY_NOT_FOUND when profile not found in aerospike
                        return ProfileSyncService.profileSync({ userIds: [params.profileId] }, clientInstances, function (err) {
                            if (err) {
                                return next(err);
                            }
                            if (!profileExist.aerospike) {
                                logger.error('ProfileApiFactory.profileAddRemovePhones: not found in aerospike');
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileAddRemovePhones: ProfileSyncService.profileSync', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!params.hasOwnProperty('adds')) {
                            return next();
                        }
                        var profile = {
                            userId: params.profileId,
                            phones: _.map(params.adds, function (addPhone) {
                                return {
                                    phone: addPhone,
                                    verificationStatus: ProfilePhone.constants().VERIFICATION_STATUS_NOT_VERIFIED
                                };
                            })
                        };
                        return ProfileService.check(profile, clientInstances, false, function (err, verifiedProfile) {
                            if (err) {
                                logger.error('ProfileApiFactory.profileAddRemovePhones: ProfileService.check');
                                return next(err);
                            }
                            if (!_.isEqual(profile, verifiedProfile)) {
                                logger.error('ProfileApiFactory.profileAddRemovePhones: ProfileService.check inconsistencies detected');
                                return next(Errors.ProfileApi.InconsistencyDetected);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileAddRemovePhones: ProfileService.check', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Bulk remove phones if needed
                function (next) {
                    if (params.hasOwnProperty('removes')) {
                        try {
                            var removes = params.removes;
                            return ProfilePhone.destroy({ where: { profileId: params.profileId, phone: { '$in': removes } } }).then(function (count) {
                                if (count !== removes.length) {
                                    return next(Errors.ProfileApi.NoRecordFound);
                                }
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        } catch (ex) {
                            return setImmediate(next, ex);
                        }
                    } else {
                        return setImmediate(next, null);
                    }
                },
                // Bulk create phones if needed
                function (next) {
                    if (params.hasOwnProperty('adds')) {
                        var adds = [];
                        _.forEach(params.adds, function (phone) {
                            adds.push({ profileId: params.profileId, phone: phone, verificationStatus: ProfilePhone.constants().VERIFICATION_STATUS_NOT_VERIFIED });
                        });
                        try {
                            return ProfilePhone.bulkCreate(adds).then(function (records) {
                                if (records.length !== adds.length) {
                                    return next(Errors.ProfileApi.InconsistencyDetected);
                                }
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        } catch (ex) {
                            return setImmediate(next, ex);
                        }
                    } else {
                        return setImmediate(next, null);
                    }
                },
                // Find all profile phones
                function (next) {
                    try {
                        return ProfilePhone.findAll({ where: { profileId: params.profileId } }).then(function (phones) {
                            _.forEach(phones, function (phone) {
                                var phonePlain = phone.get({ plain: true });
                                profilePhones.push({ phone: phonePlain.phone, verificationStatus: phonePlain.verificationStatus });
                            });
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Update Aerospike profile
                function (next) {
                    try {
                        return ProfileService.update(clientInstances, params.profileId, function (profile) {
                            // Update phone attributes
                            profile.phones = _.unionWith(profile.phones, profilePhones, _.isEqual);
                            if (params.hasOwnProperty('removes')) {
                                _.pullAllWith(profile.phones, params.removes, function (phoneEntry, phoneToRemove) { return phoneEntry.phone === phoneToRemove; });
                            }
                            return true;
                        }, function (err, userId) {
                            if (err) {
                                return next(err);
                            }
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Validate Profile email
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    profileValidateEmail: function (params, clientSession, callback) {
        var self = this;
        var profile = null;
        var clientInstances = CrudHelper.getClientInstances(clientSession);
        var profileExist = {};
        try {
            return async.series([
                // Make sure that profile exist in Aerospike and MySQL to eliminate possible inconsistency:
                //  - if there is no profile in Aerospike, return ERR_ENTRY_NOT_FOUND at the end of call
                //  - if there is no profile in MySQL, first call profileSync to import or remove profile
                function (next) {
                    try {
                        return ProfileService.exist(params.profileId, clientInstances, function (err, result) {
                            if (err) {
                                logger.error('ProfileApiFactory.profileValidateEmail: ProfileService.exist');
                                return next(err);
                            }
                            profileExist = result;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileValidateEmail: ProfileService.exist', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!profileExist.syncRequired) {
                            return next();
                        }
                        // Synchronize explicitly, then return ERR_ENTRY_NOT_FOUND when profile not found in aerospike
                        return ProfileSyncService.profileSync({ userIds: [params.profileId] }, clientInstances, function (err) {
                            if (err) {
                                return next(err);
                            }
                            if (!profileExist.aerospike) {
                                logger.error('ProfileApiFactory.profileValidateEmail: not found in aerospike');
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileValidateEmail: ProfileSyncService.profileSync', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Ensure that at least one item not validated
                function (next) {
                    return ProfileEmail.count({
                        where: { profileId: params.profileId, verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_NOT_VERIFIED }
                    }).then(function (count) {
                        if (count === 0) {
                            return next(Errors.ProfileApi.AlreadyValidated);
                        }
                        return next();
                    }).catch(function (err) {
                        logger.error('ProfileApiFactory.profileValidateEmail: ProfileEmail.count', err);
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update profile email entity
                function (next) {
                    return ProfileEmail.update(
                        { verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_VERIFIED },
                        { where: { profileId: params.profileId, verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_NOT_VERIFIED } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileApiFactory.profileValidateEmail: ProfileEmail.update', err);
                            return CrudHelper.callbackError(err, next);
                        });
                },
                // Update Aerospike profile
                function (next) {
                    try {
                        return ProfileService.update(clientInstances, params.profileId, function (profile) {
                            // Update given email's verification status
                            _.forEach(profile.emails, function (email) {
                                email.verificationStatus = ProfileEmail.constants().VERIFICATION_STATUS_VERIFIED;
                            });
                        }, next);
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileValidateEmail: ProfileService.update', ex);
                        return setImmediate(next, ex);
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Validate Profile phone
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    profileValidatePhone: function (params, clientSession, callback) {
        var self = this;
        var profile = null;
        var clientInstances = CrudHelper.getClientInstances(clientSession);
        var profileExist = {};
        try {
            return async.series([
                // Make sure that profile exist in Aerospike and MySQL to eliminate possible inconsistency:
                //  - if there is no profile in Aerospike, return ERR_ENTRY_NOT_FOUND at the end of call
                //  - if there is no profile in MySQL, first call profileSync to import or remove profile
                function (next) {
                    try {
                        return ProfileService.exist(params.profileId, clientInstances, function (err, result) {
                            if (err) {
                                logger.error('ProfileApiFactory.profileValidatePhone: ProfileService.exist');
                                return next(err);
                            }
                            profileExist = result;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileValidatePhone: ProfileService.exist', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!profileExist.syncRequired) {
                            return next();
                        }
                        // Synchronize explicitly, then return ERR_ENTRY_NOT_FOUND when profile not found in aerospike
                        return ProfileSyncService.profileSync({ userIds: [params.profileId] }, clientInstances, function (err) {
                            if (err) {
                                return next(err);
                            }
                            if (!profileExist.aerospike) {
                                logger.error('ProfileApiFactory.profileValidatePhone: not found in aerospike');
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.profileValidatePhone: ProfileSyncService.profileSync', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Ensure that at least one item not validated
                function (next) {
                    return ProfilePhone.count({
                        where: { profileId: params.profileId, verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_NOT_VERIFIED }
                    }).then(function (count) {
                        if (count === 0) {
                            return next(Errors.ProfileApi.AlreadyValidated);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update profile email entity
                function (next) {
                    return ProfilePhone.update(
                        { verificationStatus: ProfilePhone.constants().VERIFICATION_STATUS_VERIFIED },
                        { where: { profileId: params.profileId, verificationStatus: ProfilePhone.constants().VERIFICATION_STATUS_NOT_VERIFIED } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                },
                // Update Aerospike profile
                function (next) {
                    try {
                        return ProfileService.update(clientInstances, params.profileId, function (profile) {
                            // Update email's verification status
                            _.forEach(profile.phones, function (phone) {
                                phone.verificationStatus = ProfilePhone.constants().VERIFICATION_STATUS_VERIFIED;
                            });
                        }, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Adds and removes roles to a profile
     * @param {{}} params
     * @param {*} callback
     * @returns {*}
     */
    addRemoveRolesRequest: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('profileId')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var profile = null;
        var profileRoles = [];
        var clientInstances = CrudHelper.getClientInstances(clientSession);
        try {
            async.series([
                // Make sure that profile exist in Aerospike and MySQL to eliminate possible inconsistency:
                //  - if there is no profile in Aerospike, return ERR_ENTRY_NOT_FOUND at the end of call
                //  - if there is no profile in MySQL, first call profileSync to import or remove profile
                function (next) {
                    try {
                        return ProfileService.exist(params.profileId, clientInstances, function (err, result) {
                            if (err) {
                                logger.error('ProfileApiFactory.addRemoveRolesRequest: ProfileService.exist');
                                return next(err);
                            }
                            profileExist = result;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.addRemoveRolesRequest: ProfileService.exist', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!profileExist.syncRequired) {
                            return next();
                        }
                        // Synchronize explicitly, then return ERR_ENTRY_NOT_FOUND when profile not found in aerospike
                        return ProfileSyncService.profileSync({ userIds: [params.profileId] }, clientInstances, function (err) {
                            if (err) {
                                return next(err);
                            }
                            if (!profileExist.aerospike) {
                                logger.error('ProfileApiFactory.addRemoveRolesRequest: not found in aerospike');
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.addRemoveRolesRequest: ProfileSyncService.profileSync', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Make sure that profile have not already:
                //  - applied/approved tenant roles to be added
                function (next) {
                    try {
                        if (_.isEmpty(params.adds)) {
                            return next();
                        }
                        return ProfileHasRole.count({
                            where: {
                                profileId: params.profileId,
                                tenantId: params.tenantId,
                                role: { '$in': params.adds },
                                status: ProfileHasRole.constants().STATUS_APPLIED
                            }
                        }).then(function (count) {
                            if (count > 0) {
                                return next(Errors.ProfileApi.AlreadyApplied);
                            }
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileApiFactory.addRemoveRolesRequest: ProfileHasRole.count', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.addRemoveRolesRequest: ProfileHasRole.count', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (_.isEmpty(params.adds)) {
                            return next();
                        }
                        return ProfileHasRole.count({
                            where: {
                                profileId: params.profileId,
                                tenantId: params.tenantId,
                                role: { '$in': params.adds },
                                status: ProfileHasRole.constants().STATUS_APPROVED
                            }
                        }).then(function (count) {
                            if (count > 0) {
                                return next(Errors.ProfileApi.AlreadyApproved);
                            }
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileApiFactory.addRemoveRolesRequest: ProfileHasRole.count', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.addRemoveRolesRequest: ProfileHasRole.count', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Bulk delete profile roles if needed
                function (next) {
                    if (_.isEmpty(params.removes)) {
                        return next();
                    }
                    try {
                        var removes = params.removes;
                        return ProfileHasRole.destroy({
                            where: {
                                profileId: params.profileId,
                                role: { '$in': removes }
                            }
                        }).then(function (count) {
                            if (count !== removes.length) {
                                return next(Errors.ProfileApi.InconsistencyDetected);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Bulk create profile roles if needed
                function (next) {
                    if (_.isEmpty(params.adds)) {
                        return next();
                    }
                    var adds = [];
                    _.forEach(params.adds, function (role) {
                        adds.push({
                            'tenantId': params.tenantId,
                            'profileId': params.profileId,
                            'role': role,
                            'status': ProfileHasRole.constants().STATUS_APPLIED,
                            'requestDate': _.now()
                        });
                    });
                    try {
                        return ProfileHasRole.bulkCreate(adds, {
                            updateOnDuplicate: [
                                ProfileHasRole.tableAttributes.status.field
                            ]
                        }).then(function (records) {
                            if (records.length !== adds.length) {
                                return next(Errors.ProfileApi.InconsistencyDetected);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    addRemoveRoles: function (params, message, clientSession, callback) {
        var self = this;
        var finalRoleList = [];
        var clientInstances = CrudHelper.getClientInstances(clientSession);
        try {
            async.series([
                function (next) {
                    try {
                        if (_.size(_.union(params.rolesToAdd, params.rolesToRemove)) !== _.size(params.rolesToAdd) + _.size(params.rolesToRemove)) {
                            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, next);
                        }
                        // Validation already implemented on api schema level
                        if (_.isArray(params.rolesToAdd) && params.rolesToAdd.indexOf(ProfileHasRole.constants().ROLE_ADMIN) >= 0){
                            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, next);
                        }
                        if (_.isArray(params.rolesToRemove) && params.rolesToRemove.indexOf(ProfileHasRole.constants().ROLE_ADMIN) >= 0){
                            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, next);
                        }
                        return next();
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.addRemoveRoles: validate', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Get existing profile data, retrieve tenant information
                function (next) {
                    try {
                        if (params.tenantId) {
                            return setImmediate(next);
                        }
                        return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                            if (err) {
                                return next(err);
                            }
                            params.tenantId = tenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.addRemoveRoles: retrieving tenantId failed', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Make sure that profile exist in Aerospike and MySQL to eliminate possible inconsistency:
                //  - if there is no profile in Aerospike, return ERR_ENTRY_NOT_FOUND at the end of call
                //  - if there is no profile in MySQL, first call profileSync to import or remove profile
                function (next) {
                    try {
                        return ProfileService.exist(params.profileId, clientInstances, function (err, result) {
                            if (err) {
                                logger.error('ProfileApiFactory.addRemoveRolesRequest: ProfileService.exist');
                                return next(err);
                            }
                            profileExist = result;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.addRemoveRolesRequest: ProfileService.exist', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (!profileExist.syncRequired) {
                            return next();
                        }
                        // Synchronize explicitly, then return ERR_ENTRY_NOT_FOUND when profile not found in aerospike
                        return ProfileSyncService.profileSync({ userIds: [params.profileId] }, clientInstances, function (err) {
                            if (err) {
                                return next(err);
                            }
                            if (!profileExist.aerospike) {
                                logger.error('ProfileApiFactory.addRemoveRolesRequest: not found in aerospike');
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.addRemoveRolesRequest: ProfileSyncService.profileSync', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Make sure that profile have not already:
                //  - approved tenant roles to be added
                //  - if there are roles that applied, make them approved
                function (next) {
                    try {
                        if (_.isEmpty(params.rolesToAdd)) {
                            return next();
                        }
                        return ProfileHasRole.count({
                            where: {
                                profileId: params.profileId,
                                tenantId: params.tenantId,
                                role: { '$in': params.rolesToAdd },
                                status: ProfileHasRole.constants().STATUS_APPROVED
                            }
                        }).then(function (count) {
                            if (count > 0) {
                                return next(Errors.ProfileApi.AlreadyApproved);
                            }
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileApiFactory.addRemoveRoles: ProfileHasRole.count', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.addRemoveRoles: ProfileHasRole.count', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (_.isEmpty(params.rolesToAdd)) {
                            return next();
                        }
                        return ProfileHasRole.update({
                            status: ProfileHasRole.constants().STATUS_APPROVED
                        }, {
                            where: {
                                profileId: params.profileId,
                                tenantId: params.tenantId,
                                role: { '$in': params.rolesToAdd },
                                status: ProfileHasRole.constants().STATUS_APPLIED
                            }
                        }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileApiFactory.addRemoveRoles: ProfileHasRole.update', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.addRemoveRoles: ProfileHasRole.update', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Remove Roles from database
                function (next) {
                    try {
                        if (_.isEmpty(params.rolesToRemove)) {
                            return next();
                        }
                        return ProfileHasRole.destroy({
                            where: {
                                'profileId': params.profileId,
                                'tenantId': params.tenantId,
                                'role': { '$in': params.rolesToRemove }
                            }
                        }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            logger.error('profileManagerApiFactory.addRemoveRoles: removing roles ', params, err);
                            return next(Errors.ProfileApi.InconsistencyDetected);
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.addRemoveRoles: generic error removing existing roles', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Add roles into the database
                function (next) {
                    try {
                        if (_.isEmpty(params.rolesToAdd)) {
                            return next();
                        }
                        var newProfileRoles = [];
                        _.forEach(params.rolesToAdd, function (role) {
                            newProfileRoles.push({
                                'profileId': params.profileId,
                                'tenantId': params.tenantId,
                                'role': role,
                                'status': ProfileHasRole.constants().STATUS_APPROVED,
                                'acceptanceDate': _.now()
                            });
                        });
                        return ProfileHasRole.bulkCreate(newProfileRoles, {
                            updateOnDuplicate: [
                                ProfileHasRole.tableAttributes.status.field
                            ]
                        }).then(function () {
                            return next();
                        }).catch(function (err) {
                            logger.error('profileManagerApiFactory.addRemoveRoles: creating roles ', newProfileRoles, err);
                            return next(Errors.ProfileApi.InconsistencyDetected);
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.addRemoveRoles: generic error adding a role', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Retrieve all global roles
                function (next) {
                    try {
                        return ProfileHasGlobalRole.findAll({
                            where: {
                                'profileId': params.profileId
                            }
                        }).then(function (profileGlobalRoles) {
                            _.forEach(profileGlobalRoles, function (value) {
                                finalRoleList.push(value.role);
                            });
                            return next();
                        }).catch(function (err) {
                            logger.error('profileManagerApiFactory.addRemoveRoles: listing global roles ', err);
                            return next(Errors.ProfileApi.InconsistencyDetected);
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.addRemoveRoles: generic error listing global roles', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Retrieve all the active roles, the ones which are approved
                function (next) {
                    try {
                        return ProfileHasRole.findAll({ where: {
                            'profileId': params.profileId,
                            'tenantId': params.tenantId,
                            'status': ProfileHasRole.constants().STATUS_APPROVED
                        }}).then(function (profileRoles) {
                            _.forEach(profileRoles, function(value) {
                                finalRoleList.push(value.role);
                            });
                            return next();
                        }).catch(function(err) {
                            logger.error('profileManagerApiFactory.addRemoveRoles: listing roles ', err);
                            return next(Errors.ProfileApi.InconsistencyDetected);
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.addRemoveRoles: generic error listing roles', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Call an update of the auth service to update user roles
                function (next) {
                    try {
                        var authRolesToAdd = [];
                        var authRolesToRemove = [];
                        if (_.has(params, 'rolesToAdd')) {
                            _.forEach(params.rolesToAdd, function (role) {
                                authRolesToAdd.push("TENANT_" + params.tenantId + "_" + role);
                            });
                        }
                        if (_.has(params, 'rolesToRemove')) {
                            _.forEach(params.rolesToRemove, function (role) {
                                authRolesToRemove.push("TENANT_" + params.tenantId + "_" + role);
                            });
                        }
                        return clientInstances.authServiceClientInstance.setUserRole({
                            userId: params.profileId,
                            rolesToAdd: authRolesToAdd,
                            rolesToRemove: authRolesToRemove,
                            waitResponse: true,
                            //clientInfo: clientInstances.clientInfo
                        }, next);
                    } catch(ex) {
                        logger.error('profileManagerApiFactory.addRemoveRoles: auth.setUserRole ', ex);
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({roles: finalRoleList}, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    requestCommunityAccess: function (params, message, clientSession, callback) {
        var self = this;
        var communityAccessGranted = false;
        var tenantAutoCommunityEnabled = false;
        var roles = clientSession.getRoles();
        if (RoleService.isAnonymousUser(roles)) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        if (!params.hasOwnProperty('profileId')) {
            params.profileId = clientSession.getUserId();
        }
        return async.series([
            // Get tenant ID from session
            function (next) {
                return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                    if (err) {
                        return next(err);
                    }
                    params.tenantId = tenantId;
                    return next();
                });
            },
            // Check if role already exists
            function (next) {
                var communityRole = 'TENANT_' + params.tenantId + '_' + ProfileHasRole.constants().ROLE_COMMUNITY;
                if (_.findIndex(roles, communityRole) > -1) {
                    communityAccessGranted = true;
                }
                return setImmediate(next);
            },
            // If no access granted, check if tenant has Auto Community enabled
            function (next) {
                if (!communityAccessGranted) {
                    return TenantService.hasTenantAutoCommunity(params.tenantId, function (err, result) {
                        if (err) {
                            return next(err);
                        }
                        tenantAutoCommunityEnabled = result;
                        return next();
                    });
                }
                return setImmediate(next);
            },
            // If no access granted and tenant has Open Access enabled:
            // - then perform profileManager/ addToCommunity internally
            // - otherwise create access request in MySQL database: make APPLIED role
            function (next) {
                if (!communityAccessGranted) {
                    if (tenantAutoCommunityEnabled) {
                        params.rolesToAdd = [ProfileHasRole.constants().ROLE_COMMUNITY];
                        params.rolesToRemove = [];
                        return self.addRemoveRoles(params, message, clientSession, function (err, result) {
                            if (err) {
                                return next(err);
                            }
                            roles = result.roles;
                            return next();
                        });
                    }
                    params.adds = [ProfileHasRole.constants().ROLE_COMMUNITY];
                    return self.addRemoveRolesRequest(params, clientSession, function (err) {
                        if (err) {
                            return next(err);
                        }
                        return next(Errors.ProfileApi.PrivateCommunityRequestCreated);
                    });
                }
                return setImmediate(next);
            },
        ], function (err) {
            if (err) {
                return CrudHelper.callbackError(err, callback);
            }
            return CrudHelper.callbackSuccess({ roles: roles }, callback);
        });
    },

    addToCommunity: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('profileId') || params.profileId === clientSession.getUserId()) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.tenantId = tenantId;
                return self.addToCommunityByTenantId(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    addToCommunityByTenantId: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('tenantId') || !params.hasOwnProperty('profileId') || params.profileId === clientSession.getUserId()) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var roles = clientSession.getRoles();
            var autoCommunity = false;
            async.series([
                // Check Tenant Auto Community
                function (next) {
                    return TenantService.hasTenantAutoCommunity(params.tenantId, function (err, result) {
                        if (err) {
                            return next(err);
                        }
                        autoCommunity = result;
                        return next();
                    });
                },
                // Get autoCommunity value
                function (next) {
                    if (autoCommunity) {
                        params.rolesToAdd = [ProfileHasRole.constants().ROLE_COMMUNITY];
                        params.rolesToRemove = [];
                        return self.addRemoveRoles(params, message, clientSession, function (err, result) {
                            if (err) {
                                return next(err);
                            }
                            roles = result;
                            return next();
                        });
                    } else {
                        params.adds = [ProfileHasRole.constants().ROLE_COMMUNITY];
                        return self.addRemoveRolesRequest(params, clientSession, next);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(roles, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    userRoleListByTenantId: function (params, message, clientSession, callback) {
        var self = this;
        var roles;
        try {
            async.series([
                function (next) {
                    try {
                        return ProfileHasRole.findAll({ where: {
                            'tenantId': params.tenantId,
                            'profileId': params.profileId
                        }}).then(function (dbItems) {
                            roles = _.map(dbItems, 'role');
                            return next();
                        }).catch(function(err) {
                            logger.error('profileManagerApiFactory.userRoleListByTenantId', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.userRoleListByTenantId: generic error listing roles', ex);
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({roles: roles}, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    adminList: function (params, message, clientSession, callback) {
        var self = this;
        try {
            return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.tenantId = tenantId;
                return self.adminListByTenantId(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    adminListByTenantId: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('tenantId')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        var adminList;
        try {
            async.series([
                // Retrieve the admins for the current tenant
                function (next) {
                    try {
                        return ProfileHasRole.findAll({ where: {
                            'role': ProfileHasRole.constants().ROLE_ADMIN,
                            'tenantId': params.tenantId
                        }}).then(function (admins) {
                            adminList = _.map(admins, 'profileId');
                            return next();
                        }).catch(function(err) {
                            logger.error('profileManagerApiFactory.adminListByTenantId', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.adminListByTenantId: generic error listing admin', ex);
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({admins: adminList}, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    adminAdd: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('admins') || params.admins.length === 0 || _.includes(params.admins, clientSession.getUserId())) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        var tenantAdmin;
        var clientInstances = CrudHelper.getClientInstances(clientSession);
        try {
            async.series([
                // For each potential admin make sure that profile exist in Aerospike and MySQL to eliminate possible inconsistency:
                //  - if there is no profile in Aerospike, return ERR_ENTRY_NOT_FOUND at the end of call
                //  - if there is no profile in MySQL, first call profileSync to import or remove profile
                function (next) {
                    return async.mapSeries(params.admins, function (adminId, nextAdmin) {
                        var profileExist = {};
                        return async.series([
                            function (nextSerie) {
                                try {
                                    return ProfileService.exist(adminId, clientInstances, function (err, result) {
                                        if (err) {
                                            logger.error('ProfileApiFactory.adminAdd: ProfileService.exist');
                                            return nextSerie(err);
                                        }
                                        profileExist = result;
                                        return nextSerie();
                                    });
                                } catch (ex) {
                                    logger.error('ProfileApiFactory.adminAdd: ProfileService.exist', ex);
                                    return setImmediate(nextSerie, ex);
                                }
                            },
                            function (nextSerie) {
                                try {
                                    if (!profileExist.syncRequired) {
                                        return nextSerie();
                                    }
                                    // Synchronize explicitly, then return ERR_ENTRY_NOT_FOUND when profile not found in aerospike
                                    return ProfileSyncService.profileSync({ userIds: [adminId] }, clientInstances, function (err) {
                                        if (err) {
                                            return nextSerie(err);
                                        }
                                        if (!profileExist.aerospike) {
                                            logger.error('ProfileApiFactory.adminAdd: not found in aerospike');
                                            return nextSerie(Errors.DatabaseApi.NoRecordFound);
                                        }
                                        return nextSerie();
                                    });
                                } catch (ex) {
                                    logger.error('ProfileApiFactory.adminAdd: ProfileSyncService.profileSync', ex);
                                    return setImmediate(nextSerie, ex);
                                }
                            },
                        ], nextAdmin);
                    }, next);
                },
                // Get existing profile data, retrieve tenant information
                function (next) {
                    try {
                        TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                            if (err) {
                                return next(err);
                            }
                            params.tenantId = tenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.adminAdd: retrieve tenant id', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Make sure that potential admins:
                //  - have registered profile
                //  - not already approved as admin for this tenant
                function (next) {
                    try {
                        return ProfileHasGlobalRole.count({
                            where: {
                                profileId: {
                                    '$in': params.admins
                                },
                                role: {
                                    '$in': [
                                        ProfileHasGlobalRole.constants().ROLE_ANONYMOUS,
                                        ProfileHasGlobalRole.constants().ROLE_NOT_VALIDATED
                                    ]
                                }
                            }
                        }).then(function (count) {
                            if (count > 0) {
                                return next(Errors.ProfileApi.NotVerified);
                            }
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileApiFactory.adminAdd: ProfileHasGlobalRole.count', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.adminAdd: ProfileHasGlobalRole.count', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        return ProfileHasRole.count({
                            where: {
                                profileId: {
                                    '$in': params.admins
                                },
                                tenantId: params.tenantId,
                                role: ProfileHasRole.constants().ROLE_ADMIN
                            }
                        }).then(function (count) {
                            if (count > 0) {
                                return next(Errors.ProfileApi.AlreadyApproved);
                            }
                            return next();
                        }).catch(function (err) {
                            logger.error('ProfileApiFactory.adminAdd: ProfileHasRole.count', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('ProfileApiFactory.adminAdd: ProfileHasRole.count', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Set a new tenant for this admin
                function (next) {
                    try {
                        var newAdmins = [];
                        _.forEach(params.admins, function (adminId) {
                            newAdmins.push({
                                'profileId': adminId,
                                'tenantId': params.tenantId,
                                'role': ProfileHasRole.constants().ROLE_ADMIN,
                                'status': ProfileHasRole.constants().STATUS_APPROVED,
                                'acceptanceDate': _.now()
                            });
                        });
                        return ProfileHasRole.bulkCreate(newAdmins).then(function () {
                            return next();
                        }).catch(function(err) {
                            logger.error('profileManagerApiFactory.adminAdd bulk create error', JSON.stringify(newAdmins), err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.adminAdd: generic error creating admins', JSON.stringify(newAdmins), ex);
                        return setImmediate(next, ex);
                    }
                },
                // Call an update of the auth service to update user roles
                function (next) {
                    try {
                        async.every(params.admins, function (adminId, everyAdmin) {
                            var roleToAdd = "TENANT_" + params.tenantId + "_" + ProfileHasRole.constants().ROLE_ADMIN;
                            return clientInstances.authServiceClientInstance.setUserRole({
                                userId: adminId,
                                rolesToAdd: [roleToAdd],
                                rolesToRemove: [],
                                waitResponse: true,
                                //clientInfo: clientInstances.clientInfo
                            }, everyAdmin);
                        }, function (err, result) {
                            if (err) {
                                logger.error('profileManagerApiFactory.adminAdd: auth/setUserRole', err);
                                return next(err);
                            }
                            return next();
                        });
                    } catch(ex) {
                        logger.error('profileManagerApiFactory.adminAdd: generic error auth/setUserRole ', ex);
                        return next(Errors.ProfileApi.InconsistencyDetected);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return self.adminList({}, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    adminDelete: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('admins') || params.admins.length === 0 || _.includes(params.admins, clientSession.getUserId())) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        var adminList;
        var clientInstances = CrudHelper.getClientInstances(clientSession);
        try {
            async.series([
                // For each admin make sure that profile exist in Aerospike and MySQL to eliminate possible inconsistency:
                //  - if there is no profile in Aerospike, return ERR_ENTRY_NOT_FOUND at the end of call
                //  - if there is no profile in MySQL, first call profileSync to import or remove profile
                function (next) {
                    return async.mapSeries(params.admins, function (adminId, nextAdmin) {
                        var profileExist = {};
                        return async.series([
                            function (nextSerie) {
                                try {
                                    return ProfileService.exist(adminId, clientInstances, function (err, result) {
                                        if (err) {
                                            logger.error('ProfileApiFactory.adminDelete: ProfileService.exist');
                                            return nextSerie(err);
                                        }
                                        profileExist = result;
                                        return nextSerie();
                                    });
                                } catch (ex) {
                                    logger.error('ProfileApiFactory.adminDelete: ProfileService.exist', ex);
                                    return setImmediate(nextSerie, ex);
                                }
                            },
                            function (nextSerie) {
                                try {
                                    if (!profileExist.syncRequired) {
                                        return nextSerie();
                                    }
                                    // Synchronize explicitly, then return ERR_ENTRY_NOT_FOUND when profile not found in aerospike
                                    return ProfileSyncService.profileSync({ userIds: [adminId] }, clientInstances, function (err) {
                                        if (err) {
                                            return nextSerie(err);
                                        }
                                        if (!profileExist.aerospike) {
                                            logger.error('ProfileApiFactory.adminDelete: not found in aerospike');
                                            return nextSerie(Errors.DatabaseApi.NoRecordFound);
                                        }
                                        return nextSerie();
                                    });
                                } catch (ex) {
                                    logger.error('ProfileApiFactory.adminDelete: ProfileSyncService.profileSync', ex);
                                    return setImmediate(nextSerie, ex);
                                }
                            },
                        ], nextAdmin);
                    }, next);
                },
                // Get existing profile data, retrieve tenant information
                function (next) {
                    try {
                        TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                            if (err) {
                                return next(err);
                            }
                            params.tenantId = tenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.adminDelete: retrieve tenant id', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Retrieve the admins for the current tenant
                function (next) {
                    try {
                        return ProfileHasRole.findAll({ where: {
                            'role': ProfileHasRole.constants().ROLE_ADMIN,
                            'tenantId': params.tenantId
                        }}).then(function (items) {
                            adminList = _.map(items, 'profileId');
                            return next();
                        }).catch(function(err) {
                            logger.error('profileManagerApiFactory.adminDelete find admins', err);
                            return next(Errors.ProfileApi.InconsistencyDetected);
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.adminDelete: generic error listing admin', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Validate that we are deleting less admins than existing
                function (next) {
                    try {
                        if (_.has(params, 'admins') && _.isArray(params.admins) && params.admins.length < adminList.length) {
                            return next();
                        } else {
                            logger.error('profileManagerApiFactory.adminDelete - deleting all admins');
                            return next(Errors.QuestionApi.ValidationFailed);
                        }
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.adminDelete: generic error validating deletion list', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Delete Admins
                function (next) {
                    try {
                        return ProfileHasRole.destroy({where: {
                            'profileId': { '$in': params.admins },
                            'tenantId': params.tenantId,
                            'role': ProfileHasRole.constants().ROLE_ADMIN
                        }}).then(function (count) {
                            return next();
                        }).catch(function(err) {
                            logger.error('profileManagerApiFactory.adminDelete', err);
                            return next(Errors.ProfileApi.InconsistencyDetected);
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.adminDelete: generic error removing admins', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Call an update of the auth service to update user roles
                function (next) {
                    try {
                        async.every(params.admins, function (adminId, everyAdmin) {
                            var roleToRemove = "TENANT_" + params.tenantId + "_" + ProfileHasRole.constants().ROLE_ADMIN;
                            return clientInstances.authServiceClientInstance.setUserRole({
                                userId: adminId,
                                rolesToAdd: [],
                                rolesToRemove: [roleToRemove],
                                waitResponse: true,
                                //clientInfo: clientInstances.clientInfo
                            }, everyAdmin);
                        }, function (err, result) {
                            if (err) {
                                logger.error('profileManagerApiFactory.adminDelete: auth/setUserRole', err);
                                return next(Errors.ProfileApi.InconsistencyDetected);
                            }
                            return next();
                        });
                    } catch(ex) {
                        logger.error('profileManagerApiFactory.adminDelete: auth/setUserRole ', ex);
                        return next(Errors.ProfileApi.InconsistencyDetected);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return self.adminList({}, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    inviteUser: function (params, message, clientSession, callback) {
        var self = this;
        var userAlreadyRegistered = false;
        var clientInstances = CrudHelper.getClientInstances(clientSession);
        try {
            async.series([
                // Get existing profile data, retrieve tenant information
                function (next) {
                    try {
                        TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                            if (err) {
                                return next(err);
                            }
                            params.tenantId = tenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.inviteUser: retrieve tenant id', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Call auth service to invite user and grant roles immediately. 
                // User should confirm his registration, then profile sync will be called and roles will be approved
                function (next) {
                    try {
                        var profileInfo = {
                            firstName: params.firstName,
                            lastName: params.lastName,
                            organization: params.organization
                        }; 
                        var role = 'TENANT_' + params.tenantId + '_' + params.role;
                        return clientInstances.authServiceClientInstance.inviteUserByEmailAndRole({
                            email: params.email,
                            role: role,
                            profileInfo: profileInfo,
                            waitResponse: true,
                            clientInfo: clientInstances.clientInfo
                        }, function (err) {
                            if (err === Errors.AuthApi.AlreadyRegistered) {
                                userAlreadyRegistered = true;
                                return next();
                            }
                            return next(err);
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory.inviteUser: auth.inviteUserByEmailAndRole ', ex);
                        return setImmediate(next, ex);
                    }
                },
                // In case when user already registered, get it's profile and simply add role to it
                function (next) {
                    if (!userAlreadyRegistered) {
                        return setImmediate(next);
                    }
                    return ProfileEmail.findOne({
                        where: { email: params.email, verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_VERIFIED }
                    }).then(function (profileEmail) {
                        if (!profileEmail) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        params.profileId = profileEmail.get({ plain: true }).profileId;
                        params.rolesToAdd = [params.role];
                        params.rolesToRemove = [];
                        return self.addRemoveRoles(params, message, clientSession, function (err, roles) {
                            return next(err);
                        });
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

}


function profileSetStats(profile) {
    if (! (_.has(profile.dataValues, 'profileHasStats') && profile.dataValues.profileHasStats.length) ) {
        return;
    }
    
    var stats = profile.dataValues.profileHasStats[0].dataValues;
    if (stats) {
        var excludeFields = ['id', 'profileId', 'tenantId'];
        for(var field in stats) {
            if (excludeFields.indexOf(field) >= 0) {
                continue;
            }

            profile.dataValues[field] = stats[field];
        }
    }
    delete profile.dataValues.profileHasStats;
    
};
