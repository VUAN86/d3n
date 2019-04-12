var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var RoleService = require('./roleService.js');
var Database = require('nodejs-database').getInstance(Config);
var Profile = Database.RdbmsService.Models.ProfileManager.Profile;
var ProfileEmail = Database.RdbmsService.Models.ProfileManager.ProfileEmail;
var ProfilePhone = Database.RdbmsService.Models.ProfileManager.ProfilePhone;
var ProfileHasRole = Database.RdbmsService.Models.ProfileManager.ProfileHasRole;
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var logger = require('nodejs-logger')();

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client');
var S3ClientInstance = S3Client.getInstance(_.merge(S3Client.Config, { amazon_s3: { bucket: Config.blob.bucket } }));

module.exports = {
    cdnStorage: S3ClientInstance.Storage,

    /**
     * Perform check if profile exist in both Aerospike and MySQL by given userId and returned userId is the same
     * If there is entry only in on place (Aerospike or MySQL), then perform synchorization explicitly
     * @param {string} profileId Profile ID
     * @param {Object} clientInstances Java Profile Service client instance
     * @param {callback} callback Callback function
     */
    exist: function (profileId, clientInstances, callback) {
        var result = {
            aerospike: true,
            mysql: true,
            syncRequired: false
        };
        return async.series([
            // Make sure that profile exist in Aerospike
            function (next) {
                try {
                    return clientInstances.profileServiceClientInstance.getUserProfile({
                        userId: profileId,
                        //clientInfo: clientInstances.clientInfo
                    }, undefined, function (err, message) {
                        if (err || message.getError()) {
                            logger.error('ProfileService.exist: getUserProfile');
                            return next(err);
                        }
                        var aerospikeProfile = message.getContent().profile;
                        if (!aerospikeProfile || !_.has(aerospikeProfile, 'userId') || aerospikeProfile.userId !== profileId) {
                            result.aerospike = false;
                        }
                        return next();
                    });
                } catch (ex) {
                    logger.error('ProfileService.exist: getUserProfile', ex);
                    return next(ex);
                }
            },
            // Make sure that profile exist in MySQL
            function (next) {
                try {
                    return Profile.findOne({
                        where: { userId: profileId }
                    }).then(function (mysqlProfile) {
                        if (!mysqlProfile || mysqlProfile.userId !== profileId) {
                            result.mysql = false;
                        }
                        return next();
                    }).catch(function (err) {
                        logger.error('ProfileService.profileSync: Profile.findOne');
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (ex) {
                    logger.error('ProfileService.exist: Profile.findOne', ex);
                    return setImmediate(callback, ex);
                }
            },
        ], function (err) {
            if (err) {
                return setImmediate(callback, err);
            }
            result.syncRequired = !((result.aerospike && result.mysql) || (!result.aerospike && !result.mysql));
            return setImmediate(callback, null, result);
        });
    },

    /**
     * Perform data inconsistency checks for roles/emails/phones actual of particular profile
     * Notify admins if there are any inconsistencies found
     * @param {Object} profile Profile data, should contain userId, may contain emails and/or phones
     * @param {Object} clientInstances Java Profile Service client instance
     * @param {boolean} callback Email to admins when inconsistencies are detected
     * @param {callback} callback Callback function
     */
    check: function (profile, clientInstances, notify, callback) {
        var self = this;
        if (!profile || !_.has(profile, 'userId')) {
            return setImmediate(callback, null, profile);
        }
        var resultProfile = _.cloneDeep(profile);
        var inconsistencies = [];
        var profileAdmins = [];
        var profileTenants = [];
        return async.series([
            // Check emails
            function (next) {
                if (!_.has(profile, 'roles')) {
                    return next();
                }
                resultProfile.roles = RoleService.excludeDuplicates(profile.roles);
                return next();
            },
            // Check emails
            function (next) {
                if (!_.has(profile, 'emails')) {
                    return next();
                }
                return async.mapSeries(profile.emails, function (emailEntry, nextEmail) {
                    try {
                        // Profile service: call findByIdentifier
                        return clientInstances.profileServiceClientInstance.findByIdentifier({
                            identifierType: 'EMAIL',
                            identifier: emailEntry.email,
                            //clientInfo: clientInstances.clientInfo
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    logger.error('ProfileService.check: findByIdentifier [EMAIL]', err || message.getError());
                                    return next(err || message.getError());
                                }
                                if (!_.has(message.getContent(), 'userId') || message.getContent().userId === profile.userId) {
                                    return nextEmail();
                                }
                                _.pull(resultProfile.emails, emailEntry);
                                var inconsistency = { profileId: profile.userId, assignedProfileId: message.getContent().userId, subject: emailEntry.email };
                                logger.error('ProfileService.check: findByIdentifier [EMAIL] inconsistency', inconsistency);
                                inconsistencies.push(inconsistency);
                                return nextEmail();
                            } catch (ex) {
                                logger.error('ProfileService.check: findByIdentifier [EMAIL]', ex);
                                return nextEmail(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('ProfileService.check: profile.findByIdentifier [EMAIL]', ex);
                        return nextEmail(Errors.AuthApi.NoInstanceAvailable);
                    }
                }, next);
            },
            // Check phones
            function (next) {
                if (!_.has(profile, 'phones')) {
                    return next();
                }
                return async.mapSeries(profile.phones, function (phoneEntry, nextPhone) {
                    try {
                        // Profile service: call findByIdentifier
                        return clientInstances.profileServiceClientInstance.findByIdentifier({
                            identifierType: 'PHONE',
                            identifier: phoneEntry.phone,
                            //clientInfo: clientInstances.clientInfo
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    logger.error('ProfileService.check: findByIdentifier [PHONE]', err || message.getError());
                                    return next(err || message.getError());
                                }
                                if (!_.has(message.getContent(), 'userId') || message.getContent().userId === profile.userId) {
                                    return nextPhone();
                                }
                                _.pull(resultProfile.phones, phoneEntry);
                                var inconsistency = { profileId: profile.userId, assignedProfileId: message.getContent().userId, subject: phoneEntry.phone };
                                logger.error('ProfileService.check: findByIdentifier [PHONE] inconsistency', inconsistency);
                                inconsistencies.push(inconsistency);
                                return nextPhone();
                            } catch (ex) {
                                logger.error('ProfileService.check: findByIdentifier [PHONE]', ex);
                                return nextPhone(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('ProfileService.check: profile.findByIdentifier [PHONE]', ex);
                        return nextPhone(Errors.AuthApi.NoInstanceAvailable);
                    }
                }, next);
            },
            // Notify admins about found inconsistencies:
            //  - get admin list by all profile's tenants, map to every inconsistency
            //  - put as email parameters: profile's userId, assigned userId (or null if not found) and invalid entry
            function (next) {
                if (inconsistencies.length === 0 || !notify) {
                    return next();
                }
                return self.getAdminTenantList(function (err, activeAdmins) {
                    if (err) {
                        logger.error('ProfileService.notify: getAdminTenantList', err);
                        return next(err);
                    }
                    if (profile.tenants && _.isArray(profile.tenants)) {
                        _.forEach(profile.tenants, function (tenant) {
                            if (!_.isNaN(tenant) && !_.isEmpty(tenant)) {
                                var tenantId = parseInt(tenant);
                                profileTenants.push(tenantId);
                                _.forEach(activeAdmins, function (activeAdmin) {
                                    if (activeAdmin.tenantId === tenantId) {
                                        profileAdmins.push(activeAdmin.profileId);
                                    }
                                });
                            }
                        });
                    }
                    if (profileTenants.length === 0) {
                        _.forEach(activeAdmins, function (activeAdmin) {
                            profileAdmins.push(activeAdmin.profileId);
                        });
                    }
                    profileAdmins = _.uniq(profileAdmins);
                    profileTenants = _.uniq(profileTenants);
                    if (profileAdmins.length === 0) {
                        logger.warn('ProfileService.getAdminTenantList: no active admins found for tenants', profileTenants);
                    }
                    return next();
                });
            },
            function (next) {
                if (inconsistencies.length === 0 || !notify) {
                    return next();
                }
                try {
                    return ProfileHasRole.findAll({
                        where: {
                            role: ProfileHasRole.constants().ROLE_ADMIN,
                            tenantId: { '$in': profileTenants }
                        }
                    }).then(function (admins) {
                        _.forEach(admins, function (admin) {
                            var adminItem = admin.get({ plain: true });
                            profileAdmins.push(adminItem.profileId);
                        });
                        return next();
                    }).catch(function (err) {
                        logger.error('ProfileService.check: ProfileHasRole.findAll', err);
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (ex) {
                    logger.error('ProfileService.check: ProfileHasRole.findAll', ex);
                    return next(ex);
                }
            },
            function (next) {
                if (inconsistencies.length === 0 || !notify) {
                    return next();
                }
                var inconsistencyEmails = [];
                _.forEach(inconsistencies, function (inconsistency) {
                    _.forEach(profileAdmins, function (profileAdmin) {
                        inconsistencyEmails.push({
                            adminId: profileAdmin,
                            profileId: inconsistency.profileId,
                            assignedProfileId: inconsistency.assignedProfileId,
                            subject: inconsistency.subject,
                        });
                    });
                });
                return async.mapSeries(inconsistencyEmails, function (inconsistency, nextInconsistency) {
                    try {
                        var sendEmailParams = {
                            userId: inconsistency.adminId,
                            address: null,
                            subject: Config.userMessage.profileManagerProfileSyncCheckEmail.subject,
                            message: Config.userMessage.profileManagerProfileSyncCheckEmail.message,
                            subject_parameters: null,
                            message_parameters: [inconsistency.profileId, inconsistency.assignedProfileId, inconsistency.subject],
                            waitResponse: Config.umWaitForResponse
                        };
                        clientInstances.userMessageServiceClientInstance.sendEmail(sendEmailParams, function (err, message) {
                            if (err || (message && message.getError())) {
                                logger.error('ProfileService.check: userMessage.sendEmail', err || message.getError());
                                return nextInconsistency();
                            }
                            return nextInconsistency();
                        });
                    } catch (ex) {
                        logger.error('ProfileService.check: userMessage.sendEmail', ex);
                        return nextInconsistency(Errors.AuthApi.NoInstanceAvailable);
                    }
                }, next);
            }
        ], function (err) {
            if (err) {
                return setImmediate(callback, err);
            }
            return setImmediate(callback, null, resultProfile);
        });
    },

    /**
     * Notify admins if there are any inconsistencies for profile found
     * @param {Object} profileId Profile ID
     * @param {Object} clientInstances Java Profile Service client instance
     * @param {callback} callback Callback function
     */
    notify: function (profileId, clientInstances, callback) {
        var self = this;
        var profileAdmins = [];
        return async.series([
            // Notify admins about found inconsistencies:
            //  - get admin list
            //  - put as email parameters: profile's userId
            function (next) {
                return self.getAdminTenantList(function (err, activeAdmins) {
                    if (err) {
                        logger.error('ProfileService.notify: getAdminTenantList', err);
                        return next(err);
                    }
                    _.forEach(activeAdmins, function (activeAdmin) {
                        profileAdmins.push(activeAdmin.profileId);
                    });
                    profileAdmins = _.uniq(profileAdmins);
                    return next();
                });
            },
            function (next) {
                return async.mapSeries(profileAdmins, function (adminId, nextAdmin) {
                    try {
                        var sendEmailParams = {
                            userId: adminId,
                            address: null,
                            subject: Config.userMessage.profileManagerProfileSyncGetEmail.subject,
                            message: Config.userMessage.profileManagerProfileSyncGetEmail.message,
                            subject_parameters: null,
                            message_parameters: [profileId],
                            waitResponse: Config.umWaitForResponse
                        };
                        clientInstances.userMessageServiceClientInstance.sendEmail(sendEmailParams, function (err, message) {
                            if (err || (message && message.getError())) {
                                logger.error('ProfileService.notify: userMessage.sendEmail', err || message.getError());
                                return nextAdmin();
                            }
                            return nextAdmin();
                        });
                    } catch (ex) {
                        logger.error('ProfileService.notify: userMessage.sendEmail', ex);
                        return nextAdmin(Errors.AuthApi.NoInstanceAvailable);
                    }
                }, next);
            }
        ], callback);
    },

    /**
     * 
     * @param {Object} clientInstances - Java Profile Service client instances
     * @param {string} userId - User ID
     * @param {Function} updateFunction External synchronous function to update profile data
     * @param {callback} callback Callback function
     */
    update: function (clientInstances, userId, updateFunction, callback) {
        var profile = null;
        async.series([
            // Create Aerospike profile if needed
            function (next) {
                if (userId) {
                    return next();
                }
                try {
                    return clientInstances.profileServiceClientInstance.createProfile({
                        //clientInfo: clientInstances.clientInfo
                    }, undefined, function (err, message) {
                        try {
                            if (err || message.getError()) {
                                return next(err || message.getError());
                            }
                            userId = message.getContent().userId;
                            return next();
                        } catch (ex) {
                            return next(Errors.QuestionApi.FatalError);
                        }
                    });
                } catch (ex) {
                    return setImmediate(next, Errors.QuestionApi.NoInstanceAvailable);
                }
            },
            // Get Aerospike profile data
            function (next) {
                try {
                    // Profile service: call getProfile
                    return clientInstances.profileServiceClientInstance.getUserProfile({
                        userId: userId,
                        //clientInfo: clientInstances.clientInfo
                    }, undefined, function (err, message) {
                        try {
                            if (err || message.getError()) {
                                return next(err || message.getError());
                            }
                            profile = message.getContent().profile;
                            return next();
                        } catch (ex) {
                            return next(Errors.QuestionApi.FatalError);
                        }
                    });
                } catch (ex) {
                    return setImmediate(next, Errors.QuestionApi.NoInstanceAvailable);
                }
            },
            // Update profile data
            function (next) {
                try {
                    if (_.isFunction(updateFunction)) {
                        updateFunction(profile);
                    }
                    return next();
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            },
            // Update Aerospike profile
            function (next) {
                try {
                    return clientInstances.profileServiceClientInstance.updateUserProfile({
                        userId: userId,
                        profile: profile,
                        //clientInfo: clientInstances.clientInfo
                    }, undefined, function (err) {
                        return next(err);
                    });
                } catch (ex) {
                    return setImmediate(next, Errors.QuestionApi.NoInstanceAvailable);
                }
            },
        ], function (err) {
            if (err) {
                return setImmediate(callback, err);
            }
            return setImmediate(callback, null, userId);
        });
    }, 

    /**
     * Calls Media service to delete profile picture from S3
     * @param userId Profile ID
     * @param clientInstances
     * @param callback
     */
    deleteProfilePicture: function (userId, clientInstances, callback) {
        var self = this;
        try {
            var params = {
                userId: userId,
                action: 'delete',
                waitResponse: false
            };
            return clientInstances.mediaServiceClientInstance.updateProfilePicture(params, function (err, message) {
                if (err || (message && message.getError())) {
                    // If any error, log and simply skip picture setup
                    logger.error('ProfileService.uploadPicture: updateProfilePicture error', picture, err || (message && message.getError()));
                }
                return setImmediate(callback);
            });
        } catch (ex) {
            logger.error('ProfileService.uploadPicture', picture, ex);
            return setImmediate(callback, ex);
        }
    },

    profileHasPhoto: function (userId, callback) {
        var self = this;
        try {
            return self.cdnStorage.listObjects({
                Prefix: 'profile/' + userId + '.jpg'
            }, function (err, result) {
                if (err) {
                    // If any error, log err and skip
                    logger.error('ProfileService.profileHasPhoto: error', err);
                    return callback(null, false);
                }
                return callback(null, result.Contents.length && result.Contents.length > 0);
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    getAdminTenantList: function (callback) {
        return ProfileHasRole.findAll({
            where: {
                role: ProfileHasRole.constants().ROLE_ADMIN
            }
        }).then(function (admins) {
            activeAdmins = [];
            _.forEach(admins, function (admin) {
                var adminPlain = admin.get({ plain: true });
                activeAdmins.push({ profileId: adminPlain.profileId, tenantId: adminPlain.tenantId });
            });
            if (activeAdmins.length === 0) {
                logger.warn('ProfileService.getAdminTenantList: no active admins found');
            }
            return setImmediate(callback, null, activeAdmins);
        }).catch(function (err) {
            return CrudHelper.callbackError(err, callback);
        });
    },

    /**
     * Check if user is not already setup as tenant admin, if no set it up.
     * This api call is used by the adminConnector which handles tenant admins
     * for more than one tenant.
     */
    setTenantAdmin: function(AuthServiceClient, userId, tenantId, callback) {
        var tenantAdmin = null;

        async.series([
            // Check if user is setup as admin for this tenant
            function (next) {
                try {
                    ProfileHasRole.findOne({
                        where: {
                            'profileId': userId,
                            'tenantId': tenantId,
                            'role': ProfileHasRole.constants().ROLE_ADMIN
                        }
                    }).then(function(tenantAdminUser) {
                        tenantAdmin = tenantAdminUser;
                        return next(null);
                    }).catch(function(err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (ex) {
                    return setImmediate(next, err);
                };
            },

            // If no tenantAdminUser, create one
            function (next) {
                try {
                    if (!tenantAdmin) {
                        var admin = ProfileHasRole.build({
                            profileId: userId,
                            tenantId: tenantId,
                            role: ProfileHasRole.constants().ROLE_ADMIN,
                            status: ProfileHasRole.constants().STATUS_APPROVED,
                            acceptanceDate: _.now()
                        });

                        return admin.save()
                            .then(function(newTenantAdmin) {
                                if (!newTenantAdmin) {
                                    return setImmediate(next, Errors.QuestionApi.FatalError);
                                }
                                tenantAdmin = newTenantAdmin;
                                return next();
                            })
                            .catch(function(err) {
                                return CrudHelper.callbackError(err, next);
                            });
                    } else {
                        return next(false);
                    }
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            },  
            // Setup role in auth service 
            function (next) {
                return AuthServiceClient.setUserRole({
                    userId: userId,
                    rolesToAdd: ["TENANT_" + tenantId + "_" + ProfileHasRole.constants().ROLE_ADMIN], 
                    rolesToRemove: [], 
                    waitResponse: true,
                    clientInfo: null
                }, next);
            }
        ], function (err) {
            if (err) {
                return setImmediate(callback, err);
            }
            return setImmediate(callback, null, userId);
        });
    },


    /**
     * Delete admin role for a user across multiple tenants at once.
     * 
     * @param {} AuthServiceClient 
     * @param {string} userId
     * @param {array} tenantIds
     * @param {boolean} allTenants
     * @param {function} callback
     * 
     */
    deleteTenantAdmin: function(AuthServiceClient, userId, tenantIds, allTenants, callback) {
        try {
            var tenantAdmin = null;
            var filter = {
                profileId: userId,
                role: ProfileHasRole.constants().ROLE_ADMIN
            }

            if (false === allTenants && !_.isEmpty(tenantIds)) {
                filter['tenantId'] = { '$in': tenantIds };
            }

            async.series([
                // Check if user is setup as admin for this tenant
                function (next) {
                    try {
                        ProfileHasRole.findAll({ where: filter }).then(function(result) {
                            tenantIds = _.map(result, 'tenantId');
                            return next(null);
                        }).catch(function(err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, err);
                    };
                },

                // Delete Admins in MySQL
                function (next) {
                    try {
                        return ProfileHasRole.destroy({where: filter}).then(function (count) {
                            return next();
                        }).catch(function(err) {
                            logger.error('profileService.adminTenantDelete', err);
                            return next(Errors.ProfileApi.InconsistencyDetected);
                        });
                    } catch (ex) {
                        logger.error('profileService.deleteTenantAdmin: exception removing tenant admins', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Delete roles via auth service as well
                function (next) {
                    var roles = _.map(tenantIds, function (tenantId) {
                        return "TENANT_" + tenantId + "_" + ProfileHasRole.constants().ROLE_ADMIN;
                    });
                    return AuthServiceClient.setUserRole({
                        userId: userId,
                        rolesToAdd: [],
                        rolesToRemove: roles,
                        waitResponse: true,
                        clientInfo: null
                    }, next);
                }
            ], function (err) {
                if (err) {
                    return setImmediate(callback, err);
                }
                return setImmediate(callback, null, userId);
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    /**
     * Check if user is not already setup as tenant admin, if no set it up.
     * This api call is used by the adminConnector which handles tenant admins
     * for more than one tenant.
     */
    addTenantAdminUser: function(AuthServiceClient, adminItem, callback) {
        var emailEntry = false;

        async.series([
            // Check if user is setup as admin for this tenant
            function (next) {
                try {
                    // User should confirm his registration, then profile sync will be called and roles will be approved
                    var profileInfo = {
                        firstName: adminItem.firstName,
                        lastName: adminItem.lastName,
                        organization: '' // Organization not defined
                    };
                    return AuthServiceClient.inviteUserByEmailAndRole({
                        email: adminItem.email,
                        role: 'TENANT_' + adminItem.tenantId + '_' + ProfileHasRole.constants().ROLE_ADMIN,
                        profileInfo: profileInfo,
                        waitResponse: true,
                        clientInfo: null
                    }, function (err) {
                        if (err && err !== Errors.AuthApi.AlreadyRegistered) {
                            return setImmediate(next, err);
                        }
                        return setImmediate(next);
                    });
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            },
            // Retrieve tenant admin user after the invitation call
            function (next) {
                try {
                    return ProfileEmail.findOne({where: {email: adminItem.email}})
                        .then(function(profileEmail) {
                            if (!profileEmail) {
                                return setImmediate(next, Errors.QuestionApi.FatalError);
                            }
                            emailEntry = profileEmail;
                            return setImmediate(next);
                        })
                        .catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            }
        ], function (err) {
            if (err) {
                return setImmediate(callback, err);
            }
            return setImmediate(callback, null, emailEntry);
        });
    }
};
