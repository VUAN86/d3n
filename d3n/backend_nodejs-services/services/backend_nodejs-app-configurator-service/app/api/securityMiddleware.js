var _ = require('lodash');
var auth = require('basic-auth');
var logger = require('nodejs-logger')();

/**
 * These express middleware functions are being used by the tenant management
 * CRUD service to validate that the request is comming from the appropriate
 * machine.
 * There are two checks performed:
 *   * www-basic-auth 
 *   * ip address check - allow connection from a given IP address. Connections
 *                        via proxy services is not being handled only direct.
 */
module.exports  = {

    basicAuth: function(req, res, next) {
        try {
            var credentials = auth(req);

            if (_.isUndefined(credentials) || 
                credentials.name !== req.app.locals.tenantManagementConfiguration.username ||
                credentials.pass !== req.app.locals.tenantManagementConfiguration.password) {
                    logger.error("securityMiddleware:basicAuth could not authenticate user with credentials", 
                        credentials);
                    res.set('WWW-Authenticate', 'Basic realm="Tenant Manager"')
                    res.status(401).end('Unauthorized');
            } else {
                next();
            }
        } catch (e) {
            logger.error('securityMiddleware:basicAuth exception', e);
            res.status(401).end('Unauthorized');
        }
    },

    IPCheck: function(req, res, next) {
        try {
            // Not dealing with proxy and other settings, assuming direct client to server communication
            var clientIP = req.ip;
            // Eliminating IPv6 header from it:
            clientIP = clientIP.replace('::ffff:', '');
            if (-1 === req.app.locals.tenantManagementConfiguration.clientIP.indexOf(clientIP)) {
                logger.error("securityMiddleware:IPCheck ip not allowed", clientIP, 
                    "allowed IP addresses", JSON.stringify(req.app.locals.tenantManagementConfiguration.clientIP));
                res.status(403).end('Forbidden');
            } else {
                next();
            }
        } catch (e) {
            logger.error('securityMiddleware:IPCheck exception', e)
            res.status(403).end('Forbidden');
        }
    }
}