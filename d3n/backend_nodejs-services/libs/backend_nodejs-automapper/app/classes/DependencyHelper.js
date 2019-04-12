var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();

function DependencyHelper(config) {
    this._config = config;
}

DependencyHelper.getInstance = function (config) {
    return new DependencyHelper(config);
};

DependencyHelper.prototype.validate = function () {
    var self = this;
    _.forEach(self._config.dependencies, function (dependency) {
        _.forEach(dependency, function (entity) {
            var apiMethod = self.method(entity);
            var listMethod = self.method(entity, 'list');
        });
    });
};

DependencyHelper.prototype.method = function (dependency, overrideAction) {
    var self = this;
    if (!_.isObject(dependency)) {
        throw new Error('ERR_DEPENDENCY_VALIDATION_FAILED');
    }
    // Resolve API from dependency wrappers
    if (!self._config.serviceApi) {
        throw new Error('ERR_DEPENDENCY_VALIDATION_FAILED');
    }
    var apiClass = self._config.serviceApi + '/dependencyWrappers.js';
    try {
        require.resolve(apiClass);
    } catch (ex) {
        logger.error('DependencyHelper: resolving ' + apiClass + ' module failed', ex);
        throw new Error('ERR_DEPENDENCY_VALIDATION_FAILED');
    }
    // Resolve method
    var ApiClass = require(apiClass);
    var action = dependency.action;
    if (overrideAction) {
        action = overrideAction;
    }
    var apiMethod = _.camelCase(dependency.entity + '-' + action);
    if (!overrideAction && _.has(dependency, 'method')) {
        apiMethod = dependency.method;
    }
    var ApiMethod = ApiClass[apiMethod];
    if (!_.isFunction(ApiMethod)) {
        throw new Error('ERR_DEPENDENCY_VALIDATION_FAILED');
    }
    return ApiMethod;
};

DependencyHelper.prototype.processRecursive = function (message, clientSession, entity, action, id, processed, callback) {
    var self = this;
    // Find dependencies for base entity only with same action
    var entityDependencies = self._config.dependencies[entity];
    // Skip already processed entities to get rid of infinite loops
    _.remove(entityDependencies, function (dependency) {
        return processed.indexOf(dependency.entity) > -1 || dependency.action !== action;
    });
    processed.push(action);
    // Recursive call dependent API methods
    return async.mapSeries(entityDependencies, function (dependency, nextDependency) {
        try {
            // Find all objects that pass criteria by dependency field and conditions
            var searchBy = {};
            searchBy[dependency.field] = id;
            _.forEach(dependency.conditions, function (condition) {
                if (_.isObject(condition)) {
                    var key = _.keys(condition)[0];
                    var value = _.values(condition)[0];
                    searchBy[key] = value;
                }
            });
            var listMethod = self.method(dependency, 'list');
            return listMethod({ searchBy: searchBy }, message, clientSession, function (err, dependencyObjectList) {
                try {
                    if (err) {
                        return nextDependency(err);
                    }
                    // Process every found dependency object: itself and dependencies
                    return async.mapSeries(dependencyObjectList.items, function (dependencyObject, nextDependencyObject) {
                        return async.series([
                            // Call API method to execute action
                            function (next) {
                                var apiMethod = self.method(dependency);
                                logger.info('DependencyHelper: processing entity ' + dependency.entity + ' action ' + dependency.action + ' by ID ' + dependencyObject.id);
                                return apiMethod({ id: dependencyObject.id }, message, clientSession, next);
                            },
                            // Process recursive dependencies for current dependency object
                            function (next) {
                                return self.processRecursive(message, clientSession, dependency.entity, dependency.action, dependencyObject.id, processed, nextDependency);
                            },
                        ], nextDependencyObject);
                    }, nextDependency);
                } catch (ex) {
                    return nextDependency(ex);
                }
            });
        } catch (ex) {
            return setImmediate(nextDependency, ex);
        }
    }, callback);
};

DependencyHelper.prototype.process = function (message, clientSession, entity, action, id, callback) {
    var self = this;
    logger.info('DependencyHelper: processing of ' + entity + ' action ' + action + ' started');
    self.validate();
    return self.processRecursive(message, clientSession, entity, action, id, [], function (err) {
        if (err) {
            logger.info('DependencyHelper: processing failed with', err);
            return callback(err);
        }
        logger.info('DependencyHelper: processing finished successfully');
        return callback();
    });
};

module.exports = DependencyHelper;