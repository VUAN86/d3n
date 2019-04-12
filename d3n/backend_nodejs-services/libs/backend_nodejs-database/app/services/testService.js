var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var logger = require('nodejs-logger')();

/**
 * Test service constructor
 * @param database
 */
var TestService = function (database) {
    this._database = database;
    this._registry = this._database.RdbmsService.Models.Test.TestRegistry;
};

/**
 * Returns test service instance
 * @param database Database instance
 * @returns
 */
TestService.getInstance = function (database) {
    if (_.isUndefined(TestService._instance)) {
        TestService._instance = new TestService(database);
    }
    return TestService._instance;
};

/**
 * Creates and maintains test suite
 * @returns Test suite object
 */
TestService.prototype.newSuite = function () {
    var self = this;
    var _models = {};
    var _set = {};
    var _order = {};
    var _count = 1;
    var _step = 100;
    var _serviceName = 'integration';
    functions = {
        forService: function (serviceName) {
            if (_.isString(serviceName) && serviceName.length > 0) {
                _serviceName = serviceName;
            }
            return functions;
        },
        setupSets: function (count) {
            if (_.isInteger(count) && count > 1) {
                _count = count;
            }
            return functions;
        },
        setupStep: function (value) {
            if (_.isInteger(value) && value > 0) {
                _step = value;
            }
            return functions;
        },
        id: function ($id, set, step) {
            if (_.isInteger($id)) {
                return $id + _step * set;
            } else if (_.isString($id)) {
                item[$key] = _step * set + '-' + $id;
            }
        },
        create: function (model, entity) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(entity)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            _models[model.name] = model;
            if (_.isUndefined(_order[model.name])) {
                _order[model.name] = _.keys(_set).length;
            }
            if (_.isUndefined(_set[model.name])) {
                _set[model.name] = [];
            }
            _set[model.name].push(entity);
            return functions;
        },
        createSeries: function (model, entities) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(entities) || !_.isArray(entities)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            _models[model.name] = model;
            if (_.isUndefined(_order[model.name])) {
                _order[model.name] = _.keys(_set).length;
            }
            if (_.isUndefined(_set[model.name])) {
                _set[model.name] = [];
            }
            var count = 0;
            _.each(entities, function (entity) {
                if (entity) {
                    _set[model.name].push(entity);
                    count++;
                }
            });
            if (count !== entities.length) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            return functions;
        },
        process: function (done) {
            var modelOrder = _.toPairs(_order);
            async.everySeries(modelOrder, function (modelIndex, next) {
                var model = _models[modelIndex[0]];
                var modelData = _set[modelIndex[0]];
                var modelSet = [];
                _.times(_count, function (index) {
                    var step = index * _step;
                    _.forEach(modelData, function (dataItem) {
                        var item = {};
                        _.forEach(dataItem, function (value, key) {
                            if (key.startsWith('$')) {
                                var $key = key.substring(1);
                                if (_.isInteger(value)) {
                                    item[$key] = value + step;
                                } else if (_.isString(value)) {
                                    item[$key] = step + '-' + value;
                                } else if (_.isArray(value)) {
                                    _.forEach(value, function (valueItem) {
                                        if (_.isInteger(value)) {
                                            valueItem = valueItem + step;
                                        } else if (_.isString(value)) {
                                            valueItem = step + '-' + valueItem;
                                        }
                                    });
                                    item[$key] = value;
                                } else {
                                    item[$key] = value;
                                }
                            } else {
                                item[key] = value;
                            }
                        });
                        modelSet.push(item);
                    });
                });
                model.bulkCreate(modelSet).then(function (record) {
                    if (process.env.DATABASE_LOGGER === 'true') {
                        logger.info(modelIndex[0] + ' created');
                    }
                    return next(null, true);
                }).catch(function (err) {
                    logger.error('TestService.process: create', err);
                    return next(err);
                });
            }, function (err) {
                if (err) {
                    return done(err);
                }
                // Save to registry
                self._registry.create({
                    serviceName: _serviceName,
                    availableSets: _count,
                    pulledSets: 0
                }).then(function (record) {
                    return done();
                }).catch(function (err) {
                    return done(err);
                });
            });
        },
        pull: function (done) {
            self._registry.findOne({
                where: { serviceName: _serviceName }
            }).then(function (record) {
                if (!record) {
                    return done(Errors.DatabaseApi.NoRecordFound);
                }
                var set = record.pulledSets;
                if (record.pulledSets >= record.availableSets) {
                    return done(Errors.DatabaseApi.NoRecordFound);
                }
                self._registry.update({ pulledSets: record.pulledSets + 1 }, { where: { serviceName: _serviceName, pulledSets: record.pulledSets } }).then(function (count) {
                    if (count[0] !== 1) {
                        return done(Errors.DatabaseApi.NoRecordFound);
                    }
                    return done(null, set);
                }).catch(function (err) {
                    return done(err);
                })
            }).catch(function (err) {
                return done(err);
            });
        }
    };
    return functions;
};

module.exports = TestService;
