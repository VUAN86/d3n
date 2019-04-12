var _ = require('lodash');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeUser = KeyvalueService.Models.AerospikeUser;
var logger = require('nodejs-logger')();

module.exports = {
    ROLE_LEVELS: [
        { level: 2, role: AerospikeUser.ROLE_FULLY_REGISTERED_BANK_O18 },
        { level: 2, role: AerospikeUser.ROLE_FULLY_REGISTERED_BANK },
        { level: 2, role: AerospikeUser.ROLE_FULLY_REGISTERED },
        { level: 2, role: AerospikeUser.ROLE_REGISTERED },
        { level: 1, role: AerospikeUser.ROLE_NOT_VALIDATED, temporary: true },
        { level: 0, role: AerospikeUser.ROLE_ANONYMOUS, temporary: true }
    ],

    ROLE_DESCRIPTIONS: [
        { role: AerospikeUser.ROLE_FULLY_REGISTERED_BANK_O18, description: 'User is registered and have all required data to play games' },
        { role: AerospikeUser.ROLE_FULLY_REGISTERED_BANK, description: 'User is registered and have all required data to play games except 18+ content' },
        { role: AerospikeUser.ROLE_FULLY_REGISTERED, description: 'User is registered and have all required data to play games but no money option' },
        { role: AerospikeUser.ROLE_REGISTERED, description: 'User is registered and can play and store bonus points' },
        { role: AerospikeUser.ROLE_NOT_VALIDATED, description: 'User is registered and logged in, but not yet confirmed' },
        { role: AerospikeUser.ROLE_ANONYMOUS, description: 'Default role for new connection' },
        { role: AerospikeUser.ROLE_COMMUNITY, description: 'Tenant X based community role' },
        { role: AerospikeUser.ROLE_ADMIN, description: 'Tenant X based admin role' },
        { role: AerospikeUser.ROLE_INTERNAL, description: 'Tenant X based internal role' },
        { role: AerospikeUser.ROLE_EXTERNAL, description: 'Tenant X based external role' }
    ],

    excludeDuplicates: function (rolesFromProfile) {
        var self = this;
        _.forEach(self.ROLE_LEVELS, function (roleItem) {
            if (_.includes(rolesFromProfile, roleItem.role)) {
                var rolesToExclude = _.filter(self.ROLE_LEVELS, function (roleItemToExclude) {
                    return roleItemToExclude.level < roleItem.level && roleItemToExclude.temporary;
                });
                _.forEach(rolesToExclude, function (roleItemToExclude) {
                    _.pull(rolesFromProfile, roleItemToExclude.role);
                });
            }
        });
        var rolesUnique = _.uniq(rolesFromProfile);
        return rolesUnique;
    },

    excludeDuplicatesFromRight: function (rolesFromProfile, rolesFromToken) {
        var self = this;
        _.forEach(self.ROLE_LEVELS, function (roleItem) {
            if (_.includes(rolesFromProfile, roleItem.role)) {
                var rolesToExclude = _.filter(self.ROLE_LEVELS, function (roleItemToExclude) {
                    return roleItemToExclude.level < roleItem.level && roleItemToExclude.temporary;
                });
                _.forEach(rolesToExclude, function (roleItemToExclude) {
                    _.pull(rolesFromToken, roleItemToExclude.role);
                });
            }
        });
        var rolesByTenantToRemove = _.intersection(rolesFromProfile, rolesFromToken);
        _.forEach(rolesByTenantToRemove, function (role) {
            _.pull(rolesFromToken, role);
        });
        return rolesFromToken;
    },

    checkRolesAreValid: function (roles) {
        if (!roles || _.isEmpty(roles)) {
            return false;
        }
        var rolesToCheck = _.clone(roles);
        _.forEach(rolesToCheck, function (role) {
            if (!role || role.startsWith('TENANT_')) {
                _.pull(rolesToCheck, role);
            }
        });
        var globalRoles = _.map(this.ROLE_LEVELS, 'role');
        if (_.intersection(rolesToCheck, globalRoles).length > globalRoles.length) {
            return false;
        }
        return true;
    },

    getUserRoleLevel: function (roles) {
        var self = this;
        var level = -1;
        _.forEach(self.ROLE_LEVELS, function (roleItem) {
            if (_.includes(roles, roleItem.role)) {
                if (level === -1) {
                    level = 0;
                }
                level += roleItem.level;
            }
        });
        return level;
    },

    isAnonymousUser: function (roles) {
        var self = this;
        var level = self.getUserRoleLevel(roles);
        return level === 0;
    },

    isNotValidatedUser: function (roles) {
        var self = this;
        var level = self.getUserRoleLevel(roles);
        return level === 1;
    },

    isRegisteredUser: function (roles) {
        var self = this;
        var level = self.getUserRoleLevel(roles);
        return level >= 2;
    },

};