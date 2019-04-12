var _ = require('lodash');
var logger = require('nodejs-logger')();
var protocolSchemas = require('nodejs-protocol-schemas');

/**
 * The Security module checks the user roles against the APi permissions setup
 *
 * @constructor
 * @param {object} schemas Swagger schemas
 * @param {object} roles   Roles to permission mapping configuration
 */
function Security() {
    this.schemas = protocolSchemas.schemas;
    this.roles = protocolSchemas.roles;
    this.privateApis = null;
}

var o = Security.prototype;

/**
 * Map an array of role strings to an array of permissions. Duplicate entries
 * are removed returning only the unique permissions available for the user
 * having those roles.
 *
 * @param  {array} userRoles An array of role strings to map to permissions
 * @param  {integer} tenantId The id of the tenant
 * @return {array}       Return an array of permissions that map to those roles
 */
o.getRolesPermissions = function (userRoles, tenantId) {
    var rolesConfiguration = this.roles;
    var permissions = [];
    var tenantRoles = {};
    var i;
    var tenantConfiguredRoles = ['ADMIN', 'COMMUNITY', 'INTERNAL', 'EXTERNAL'];

    for (i=0; i < tenantConfiguredRoles.length; i++) {
        tenantRoles['TENANT_' + tenantId + '_' + tenantConfiguredRoles[i]] = tenantConfiguredRoles[i];
    }

    _.forEach(userRoles, function (value) {
        // Check for tenant roles
        if (tenantConfiguredRoles.indexOf(value) === -1) {
            // Map tenant roles
            var roleToMap = value;
            if (_.has(tenantRoles, value)) {
                roleToMap = tenantRoles[value];
            }

            if (_.has(rolesConfiguration, roleToMap)) {
                permissions = _.concat(permissions, rolesConfiguration[roleToMap]);
            } else {
                logger.debug("Security:getRolesPermissions - role not found", JSON.stringify(value));
            }
        } else {
            logger.debug("Security:getRolesPermissions - invalid tenant role", JSON.stringify(value));
        }
    });

    return _.uniq(permissions);
};

/**
 * Get the x-permissions configuration from swagger schema for a given api path.
 * @param  {string} apiPath Api URL/path for which we want the permission config
 * @return {array}          An array of permissions required for this api
 */
o.getApiPermission = function (apiPath) {
    var definition = [];
    // Get schema from schemas definitions
    _.forEach(this.schemas, function (schema) {
        if (_.has(schema, 'paths') && _.has(schema.paths, apiPath)) {
            _.forIn (schema.paths[apiPath], function (value, key) {
                if (_.has(value, 'x-permissions')) {
                    definition = _.clone(value['x-permissions']);
                }
                return false;
            });
        }
    });

    if (_.isEmpty(definition)) {
        logger.debug("Security:findPathPermission - no permission set for", apiPath);
    }

    return definition;
};


/**
 * Get private APIs. Private APIs are cached on first call.
 * @return {array} An array of private APIs
 */
o.getPrivateApis = function () {
    var self = this;
    if (_.isArray(self.privateApis)) {
        return self.privateApis;
    }
    
    self.privateApis = [];
    
    for(var f in self.schemas) {
        var schemaFile = self.schemas[f];
        
        if (!_.has(schemaFile, 'paths')) {
            continue;
        }
        
        var apis = schemaFile['paths'];
        for(var api in apis) {
            for(var verb in apis[api]) {
                if (apis[api][verb]['x-private'] === true) {
                    self.privateApis.push(api);
                }
            }
        }
    }
    
    return self.privateApis;
};


/**
 * Check if a given array of user roles can access this api endpoint.
 * @param  {array} role       Array of user roles
 * @param  {integer} tenantId Id of the tenant
 * @param  {string} apiPath   Api endpoint
 * @return {boolean}          Return true if api is accessible for the user
 */
o.roleCanAccessApi = function (role, tenantId, apiPath) {
    var requiredPermissions = this.getApiPermission(apiPath);
    var userPermissions = this.getRolesPermissions(role, tenantId);

    if (requiredPermissions.length > 0) {
        var commonPermissions = _.intersection(requiredPermissions, userPermissions);
        // Security check update per issue #6145
        if (commonPermissions.length > 0) {
            return true;
        }
        logger.debug("Security:roleCanAccessApi - not enough permissions to access path",
            apiPath, "required permissions", JSON.stringify(requiredPermissions),
            "user permissions", JSON.stringify(userPermissions),
            "common permissions", JSON.stringify(commonPermissions));
        return false;
    }
    return true;
}

module.exports = Security;
