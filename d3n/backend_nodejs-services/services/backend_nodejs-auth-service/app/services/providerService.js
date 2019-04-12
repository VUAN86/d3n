var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ApiFactory = require('../factories/apiFactory.js');
var httpRequest = require('superagent');
var logger = require('nodejs-logger')();

module.exports = {
    /**
     * Connects to provider (Facebook/Google), verifies given user provider token and gets user profile unique ID
     * @param {String} provider Provider (facebook|google)
     * @param {String} providerToken User provider token
     * @param {Object} callback Callback
     * @returns {String} User profile unique ID
     */
    getProviderData: function (provider, providerToken, profileData, callback) {
        if (provider !== 'facebook' && provider !== 'google') {
            return ApiFactory.callbackError(Errors.AuthApi.ValidationFailed, callback);
        }
        // Don't request provider, because of provider token short life
        // It can be generated only with provider passport for short time
        if (process.env.SKIP_PROVIDER_TOKEN_VALIDATION === 'true') {
            return ApiFactory.callbackSuccess({
                uniqueId: providerToken,
                email: 'test@ascendro.de',
                providerData: {}
            }, callback);
        }
        // Test only within local development environment
        try {
            var result = {};
            var fieldData = {};
            return async.series([
                // Get unique identifier; profile data in case if additional profile data requested
                function (next) {
                    try {
                        var query = {
                            access_token: providerToken,
                            scope: Config[provider].scope
                        };
                        var queryFields = [];
                        if (Config[provider].fieldsToRequest) {
                            queryFields.push(Config[provider].fields[0]);
                        }
                        if (profileData && Config[provider].fieldsToRequest) {
                            queryFields = _.union(queryFields, Config[provider].fields);
                        }
                        if (queryFields.length > 0) {
                            query.fields = queryFields.join(',');
                        }
                        var req = httpRequest
                            .get(Config[provider].url)
                            .query(query)
                            .set('Accept', 'application/json')
                            .end(function (err, res) {
                                if (err) {
                                    logger.error('ProfileService.getProfileUniqueId [primary data]', req._data);
                                    return ApiFactory.callbackError(err, next);
                                }
                                if (!_.has(res.body, Config[provider].fields[0])) {
                                    return ApiFactory.callbackError(Errors.AuthApi.ProviderTokenNotValid, next);
                                }
                                if (profileData && !_.has(res.body, Config[provider].fields[1])) {
                                    return ApiFactory.callbackError(Errors.AuthApi.ProviderNoEmailReturned, next);
                                }
                                fieldData = _.assign(fieldData, res.body);
                                return next();
                            });
                        return req;
                    } catch (ex) {
                        return ApiFactory.callbackError(err, next);
                    }
                },
                // Retrieve additional info for google account through separate request
                function (next) {
                    try {
                        if (!Config[provider].fieldsUrl) {
                            return next();
                        }
                        var req = httpRequest
                            .get(Config[provider].fieldsUrl)
                            .query({ access_token: providerToken })
                            .set('Accept', 'application/json')
                            .end(function (err, res) {
                                if (err) {
                                    logger.error('ProfileService.getProfileUniqueId [additional data]', req._data);
                                    return ApiFactory.callbackError(err, next);
                                }
                                if (!_.has(res.body, Config[provider].fields[0])) {
                                    return ApiFactory.callbackError(Errors.AuthApi.ProviderTokenNotValid, next);
                                }
                                fieldData = _.assign(fieldData, res.body);
                                return next();
                            });
                        return req;
                    } catch (ex) {
                        return ApiFactory.callbackError(err, next);
                    }
                }
            ], function (err) {
                if (err) {
                    logger.error('ProfileService.getProfileUniqueId', err);
                    return ApiFactory.callbackError(err, callback);
                }
                result.uniqueId = fieldData[Config[provider].fields[0]];
                result.email = fieldData[Config[provider].fields[1]];
                result.providerData = fieldData;
                return ApiFactory.callbackSuccess(result, callback);
            });
        } catch (ex) {
            logger.error('ProfileService.getProfileUniqueId', ex);
            return ApiFactory.callbackError(Errors.AuthApi.ProviderServiceFatalError, callback);
        }
    }
};