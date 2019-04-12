var _ = require('lodash');

module.exports = {
    ROLE_LEVELS: [
        { level: 2, role: 'FULLY_REGISTERED_BANK_O18' },
        { level: 2, role: 'FULLY_REGISTERED_BANK' },
        { level: 2, role: 'FULLY_REGISTERED' },
        { level: 2, role: 'REGISTERED' },
        { level: 1, role: 'NOT_VALIDATED', temporary: true },
        { level: 0, role: 'ANONYMOUS', temporary: true }
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