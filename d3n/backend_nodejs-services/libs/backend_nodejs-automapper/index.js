var _ = require('lodash');
var Config = require('./app/config/config.js');
var AutoMapper = require('./app/classes/AutoMapper.js');
var CrudHelper = require('./app/classes/CrudHelper.js');
var ShouldHelper = require('./app/classes/ShouldHelper.js');
var DependencyHelper = require('./app/classes/DependencyHelper.js');

var AutoMapperService = function (config) {
    var _config = { limit: 20, entryBlockTimeSeconds: 2 * 60 * 60, dependencies: Config.dependencies, serviceApi: null };
    if (config && _.has(config, 'rdbms') && _.has(config.rdbms, 'limit')) {
        _config.limit = config.rdbms.limit;
    }
    if (config && _.has(config, 'entryBlockTimeSeconds')) {
        _config.entryBlockTimeSeconds = config.entryBlockTimeSeconds;
    }
    if (config && _.has(config, 'dependencies')) {
        _config.dependencies = config.dependencies;
    }
    if (config && _.has(config, 'serviceApi')) {
        _config.serviceApi = config.serviceApi;
    }
    AutoMapperService.prototype.AutoMapper = AutoMapper.getInstance(_config);
    AutoMapperService.prototype.CrudHelper = CrudHelper.getInstance(_config);
    AutoMapperService.prototype.ShouldHelper = ShouldHelper.getInstance(_config);
    AutoMapperService.prototype.DependencyHelper = DependencyHelper.getInstance(_config);
    AutoMapperService.prototype.Config = _config;
}

AutoMapperService.getInstance = function (config) {
    if (_.isUndefined(AutoMapperService._instance)) {
        AutoMapperService._instance = new AutoMapperService(config);
    }
    return AutoMapperService._instance;
}

module.exports = AutoMapperService;