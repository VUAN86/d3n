var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeGame = function AerospikeGame(game) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(game);
};
AerospikeGame._namespace = Config.keyvalue.namespace;
AerospikeGame._set = 'game';
AerospikeGame._ttl = 0;

module.exports = AerospikeGame;

//Copies attributes from a parsed json or other source
AerospikeGame.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the Game in the database, overwrites existing items
AerospikeGame.prototype.save = function (callback) {
    var self = this;
    return AerospikeGame.toJSON(false, self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                return self.KeyvalueService.exec('put', {
                    model: AerospikeGame,
                    key: key,
                    value: { value: new Buffer(JSON.stringify(json)) }
                }, function (err, res) {
                    var Game = new AerospikeGame(json);
                    return _success(Game, callback);
                });
            } catch (ex) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

//Saves the Game's basic info in the database, overwrites existing items
AerospikeGame.prototype.saveBasicInfo = function (filterKey, basicInfo, callback) {
    var self = this;
    var key = _keyForBasicInfo(filterKey.key, filterKey.value, self);
    if (key) {
        try {
            return self.KeyvalueService.exec('put', {
                model: AerospikeGame,
                key: key,
                value: { value: new Buffer(JSON.stringify(basicInfo)) }
            }, function (err, res) {
                if (err) {
                    return _error(err, callback);
                }
                return _success(null, callback);
            });
        } catch (ex) {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    } else {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Removes the Game from the database
AerospikeGame.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        return self.KeyvalueService.exec('remove', {
            model: AerospikeGame,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Removes the Game's basic info from the database
AerospikeGame.prototype.removeBasicInfo = function (filterKey, callback) {
    var self = this;
    var key = _keyForBasicInfo(filterKey.key, filterKey.value, self);
    try {
        return self.KeyvalueService.exec('remove', {
            model: AerospikeGame,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

// Publish game in aerospike
AerospikeGame.publishGame = function (params, callback) {
    var basicInfo;
    var applications = _.cloneDeep(params.applications);
    params.applications = [];
    _.forEach(applications, function (application) {
        params.applications.push(application.appId);
    });
    params = _setupFilterKeys(params);
    async.series([
        // Create game entry
        function (next) {
            var Game = new AerospikeGame();
            Game.id = params.id;
            Game = _copy(Game, params);
            return Game.save(next);
        },
        // Setup game filters for every application
        function (next) {
            return async.mapSeries(applications, function (application, appNext) {
                params.appId = application.appId;
                params.handicap = application.handicap;
                return async.series([
                    // Setup filter basic info
                    function (appSerie) {
                        var GameFilter = new AerospikeGame();
                        GameFilter.id = params.id;
                        GameFilter = _copyBasicInfo(GameFilter, params);
                        return AerospikeGame.toJSON(true, GameFilter, function (err, json) {
                            if (err) {
                                return appSerie(err);
                            }
                            basicInfo = json;
                            return appSerie();
                        });
                    },
                    // Create/update game filters
                    function (appSerie) {
                        return async.mapSeries(params.filters, function (filter, filterNext) {
                            var filterKey = { key: _.keys(filter)[0], value: _.values(filter)[0] };
                            return AerospikeGame.findFilter(filterKey, params, function (err, gameFilter) {
                                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                                    return _error(err, filterNext);
                                }
                                var filterValue = gameFilter;
                                if (filterValue) {
                                    _.remove(filterValue, function (entry) {
                                        return entry.gameId.toString() === basicInfo.gameId;
                                    });
                                    filterValue.push(basicInfo);
                                } else {
                                    filterValue = [basicInfo];
                                }
                                var GameFilter = new AerospikeGame();
                                GameFilter.id = params.id;
                                GameFilter = _copyBasicInfo(GameFilter, params);
                                return GameFilter.saveBasicInfo(filterKey, filterValue, filterNext);
                            });
                        }, appSerie);
                    }
                ], appNext);
            }, next);
        },
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

// Unpublish game from aerospike
AerospikeGame.unpublishGame = function (params, callback) {
    var basicInfo;
    var applications = _.cloneDeep(params.applications);
    params.applications = [];
    _.forEach(applications, function (application) {
        params.applications.push(application.appId);
    });
    params = _setupFilterKeys(params);
    async.series([
        // Remove game entry
        function (next) {
            var Game = new AerospikeGame();
            Game.id = params.id;
            return Game.remove(next);
        },
        // Remove game filters for every application
        function (next) {
            return async.mapSeries(applications, function (application, appNext) {
                params.appId = application.appId;
                params.handicap = application.handicap;
                return async.series([
                    // Setup filter basic info
                    function (appSerie) {
                        var GameFilter = new AerospikeGame();
                        GameFilter.id = params.id;
                        _copyBasicInfo(GameFilter, params);
                        return AerospikeGame.toJSON(true, GameFilter, function (err, json) {
                            if (err) {
                                return appSerie(err);
                            }
                            basicInfo = json;
                            return appSerie();
                        });
                    },
                    // Create/update game filters
                    function (appSerie) {
                        return async.mapSeries(params.filters, function (filter, filterNext) {
                            var filterKey = { key: _.keys(filter)[0], value: _.values(filter)[0] };
                            return AerospikeGame.findFilter(filterKey, params, function (err, filterValue) {
                                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                                    return _error(err, filterNext);
                                }
                                if (!filterValue) {
                                    return filterNext();
                                }
                                _.remove(filterValue, function (entry) {
                                    return entry.gameId.toString() === basicInfo.gameId;
                                });
                                var GameFilter = new AerospikeGame();
                                GameFilter.id = params.id;
                                _copyBasicInfo(GameFilter, params);
                                if (filterValue.length > 0) {
                                    return GameFilter.saveBasicInfo(filterKey, filterValue, filterNext);
                                }
                                return GameFilter.removeBasicInfo(filterKey, filterNext);
                            });
                        }, appSerie);
                    }
                ], appNext);
            }, next);
        },
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

//Publish single winning component in aerospike
AerospikeGame.publishWinningComponent = function (params, value, callback) {
    var Game = new AerospikeGame();
    _copyWinningComponent(Game, value);
    return Game.saveBasicInfo({ key: 'winningComponent', value: params.id }, Game.winningComponent, callback);
}

// Unpublish single winning component from aerospike
AerospikeGame.unpublishWinningComponent = function (params, callback) {
    var Game = new AerospikeGame();
    return Game.removeBasicInfo({ key: 'winningComponent', value: params.id }, callback);
}

//Publish winning component list related to game in aerospike
AerospikeGame.publishWinningComponentList = function (params, value, callback) {
    var Game = new AerospikeGame();
    _copyWinningComponentList(Game, value);
    return Game.saveBasicInfo({ key: 'winningComponentList', value: params.id }, Game.winningComponentList, callback);
}

// Unpublish single winning component from aerospike
AerospikeGame.unpublishWinningComponentList = function (params, callback) {
    var Game = new AerospikeGame();
    return Game.removeBasicInfo({ key: 'winningComponentList', value: params.id }, callback);
}

AerospikeGame.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeGame();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeGame,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                try {
                    var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.value));
                    instance = instance.copy(resJson);
                    return _success(instance, callback);
                } catch (ex) {
                    return _error(Errors.DatabaseApi.NoRecordFound, callback);
                }
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeGame.findFilter = function (filterKey, params, callback) {
    try {
        var key = _keyForBasicInfo(filterKey.key, filterKey.value, params);
        var instance = new AerospikeGame();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeGame,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                try {
                    var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.value));
                    return _success(resJson, callback);
                } catch (ex) {
                    return _error(Errors.DatabaseApi.NoRecordFound, callback);
                }
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeGame.toJSON = function (basicInfo, object, callback) {
    try {
        var plain;
        if (basicInfo) {
            plain = _copyBasicInfo({}, object, false);
        } else {
            plain = _copy({}, object, false);
        }
        return _success(plain, callback);
    } catch (ex) {
        return _error(ex, callback);
    }
};

function _copy(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'gameDataModel');
    return _.assign(to, data);
}

function _copyBasicInfo(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var filter = AerospikeMapperService.map(from, 'gameFilterModel');
    return _.assign(to, filter);
}

function _copyWinningComponent(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'winningComponentDataModel');
    return _.assign(to, { winningComponent: data });
}

function _copyWinningComponentList(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'gameWinningComponentListDataModel');
    return _.assign(to, { winningComponentList: data });
}

// Setup filter keys
function _setupFilterKeys(game) {
    game.filters = [];
    game.filters.push({ 'null': null });
    game.filters.push({ 'type': game.type });
    _.forEach(game.assignedPools, function (pool) {
        game.filters.push({ 'pool': pool });
    });
    game.filters.push({ 'specialPrize': true });
    game.filters.push({ 'offline': true });
    game.filters.push({ 'free': true });
    return game;
}

function _key(params) {
    return 'game:' + params.id;
}

function _keyForBasicInfo(key, value, params) {
    if (key === 'null') {
        return 'game:tenant:' + params.tenantId + ':app:' + params.appId;
    } else if (key === 'winningComponent') {
        return 'game:winningComponent:' + value;
    } else if (key === 'winningComponentList') {
        return 'game:winningComponentList:' + value;
    }
    return 'game:tenant:' + params.tenantId + ':app:' + params.appId + ':' + key + ':' +  value;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var Game = new AerospikeGame();
        result = Game;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
