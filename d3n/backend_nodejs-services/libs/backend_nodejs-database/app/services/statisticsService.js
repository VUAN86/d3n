var events = require('events');
var StatisticsEvents = require('./../factories/statisticsEvents.js');
var _ = require('lodash');
var Config = require('./../config/config.js');
var Statistics = require('./../config/statistics.js');
var logger = require('nodejs-logger')();

var StatisticsService = function (sequelize) {
    var self = this;

    self._sequelize = sequelize;
    self._collect = true;

    StatisticsEvents.on('update_statistics', function(ids, from, ignoreMappingErrors) {
        if (self._collect && Config.statisticsEnabled) {
            var sqlList = Statistics[from];
            if (sqlList) {
                _.forOwn(sqlList, function(sql, to) {
                    if (_.has(ids, to)) {
                        self._sequelize.query(sql, {
                            replacements: { ids: ids[to] },
                            type: self._sequelize.QueryTypes.UPDATE 
                        }).then(function(){
                            logger.debug("StatisticsService: update " + to +
                                " statistics from " + from + ":", JSON.stringify(ids[to]));
                        }).catch(function (err) {
                            logger.error("StatisticsService: " + to + " update error ", err);
                        });
                    } else if (!ignoreMappingErrors) {
                        throw new Error('StatisticsService: unable to update ' + to +
                            ' statistics from ' + from + '\n\tmissing field name "' + to + '" from:' +
                            JSON.stringify(ids));
                    }
                })
            } else {
                logger.error("StatisticsService: missing query files for " + from);
            }
        }
    });
}

/**
 * Returns statistics listener service instance with sequelize connection defined
 * @returns
 */
StatisticsService.getInstance = function (sequelize) {
    if (_.isUndefined(StatisticsService._instance)) {
        StatisticsService._instance = new StatisticsService(sequelize);
    }
    return StatisticsService._instance;
};

/**
 * Disable the statistics collection on demand don't forget to re-enable it
 * once you are done.
 */
StatisticsService.prototype.disable = function () {
    var self = this;
    self._collect = false;
}

/**
 * Re-enable statistics collection once work on DB has been done
 */
StatisticsService.prototype.enable = function () {
    var self = this;
    self._collect = true;
}

/**
 * Emit 'update_statistics' event
 * Context contains name of table triggering the event and name of field containing id
 */
StatisticsService.prototype.handleEvent = function (instances) {
    if (!_.isArray(instances)) instances = [instances];
    if (instances.length) {
        //prevent unsupported bulk operation to trigger - fail-safe
        if (_.isFunction(instances[0].get)) {
            var ids = getIds(instances, this.fieldNames);
            StatisticsEvents.emit('update_statistics', ids, this.tableName);
        } else {
            if (instances[0].individualHooks) {
                logger.info("StatisticsService: unsupported " + instances[0].type + " for " +
                    this.tableName + " will be handled individually");
            } else {
                logger.error("StatisticsService: unsupported " + instances[0].type + " for " +
                    this.tableName + " with no individual handling enabled");
            }
        }
    }
}

module.exports = StatisticsService;

function getIds(instances, fieldNames) {
    var items = _.map(instances, function(instance) {
        return instance.get({'plain': true});
    })
    var ids = _.mapValues(fieldNames, function(fieldName) {
        return _.map(items, fieldName);
    });
    return ids;
}