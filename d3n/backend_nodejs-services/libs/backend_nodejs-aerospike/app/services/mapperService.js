var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var logger = require('nodejs-logger')();

function MapperService(config) {
    var self = this;
    self._autoMapperInstance = require('nodejs-automapper').getInstance(config);
    self._autoMapper = self._autoMapperInstance.AutoMapper;
    self._jjv = Config.setupSchemaValidator(require('jjv')(), require('nodejs-protocol-schemas').schemas);
}

MapperService.getInstance = function (config) {
    return new MapperService(config);
};

MapperService.prototype.map = function (source, model) {
    var self = this;
//    logger.debug('Aerospike model before mapping', model, JSON.stringify(source));
    var destination = self._autoMapper.mapSimpleDefinedBySchema(model, source);
//    logger.debug('Aerospike model before validation', model, JSON.stringify(destination));
    var err = self._jjv.validate(model, destination);
    if (err) {
        logger.debug('Aerospike model mapping validation ERROR', JSON.stringify(err));
        throw Errors.DatabaseApi.ValidationFailed;
    }
    return destination;
}

module.exports = MapperService;