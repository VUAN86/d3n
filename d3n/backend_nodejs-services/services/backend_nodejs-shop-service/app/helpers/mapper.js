var _ = require('lodash');
var logger = require('nodejs-logger')();
var Mappings = require('./../config/mappings.js');

module.exports = Mapper;
    
/**
 * Create a generic mapper based on a mapping name
 * @param mappingName name of mapping
 */
function Mapper(mappingName) {
    this.mapping = {
        mappings: _getMappings(mappingName),
        name: mappingName
    };
};

/**
 * Map object
 * @param src source object
 * @return mapped object
 */
Mapper.prototype.get = function (src) {
    _validate(src, this.mapping);
    var dest = _map(src, this.mapping.mappings);
    return dest;
};

/**
 * Get corresponding mapping for a parameter
 * @param param parameter name
 * @param isReversed true if source is provided and return destination, false for other way around
 * @return mapped parameter name
 */
Mapper.prototype.getMapping = function (param, isReversed = false) {
    if (isReversed) {
        for (var key in this.mapping.mappings) {
            if (this.mapping.mappings.hasOwnProperty(key) && this.mapping.mappings[key] === param) {
                return key;
            }
        }
    } else {
        if (_.has(this.mapping.mappings, param)) {
            return this.mapping.mappings.param;
        }
    }
    var err = "property not found: " + param;
    logger.error("Shop: error mapping field", err);
    throw err;
};

function _getMappings(mappingName) {
    if (!(mappingName && _.has(Mappings, mappingName))) {
        var err = "missing mapping " + (mappingName ? mappingName : "name");
        logger.error("Shop: error mapping object", err);
        throw err;
    }

    return _.get(Mappings, mappingName);
}

function _validate (src, mapping) {
    if (!_.isObject(src)) {
        var err = "missing response object for " + mapping.name;
        logger.error("Shop: error validating response ", err);
        throw err;
    }
    var invalidProperties = [];
    var mappingName = mapping.name;
    var mappings = mapping.mappings;
    _.forOwn(mappings, function(value, key) {
        if (!_.has(src, key)) {
            invalidProperties.push(key);
        }
    });

    if (invalidProperties.length) {
        var err = "missing properties from response " + mappingName + ": " + invalidProperties.join(",");
        logger.error("Shop: error validating response ", err);
        throw err;
    }
}

function _map (src, mappings) {
    var dest = {};
    _.forOwn(mappings, function(value, key) {
        dest[value] = src[key];
        //temporary - oxstock is string, but number when 0
        if (key == 'oxstock') dest[value] = "" + src[key];
    });

    return dest;
}
