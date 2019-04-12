/**
 * Default Config object for logger
 * @constructor
 */
var Config = function () {
    this.timestamp = true;
    this.level = _logLevel();
    this.colorize = true;
};

module.exports = Config;

/**
 * Determines log level of logger based on the environment or direct log level setting
 * @returns {string} determined log level
 * @private
 */
function _logLevel() {
    var level = process.env.ENV === 'production' ? 'info' : 'debug';
    if (process.env.LOG_LEVEL && process.env.LOG_LEVEL !== 'undefined' && process.env.LOG_LEVEL !== 'null') {
        level = process.env.LOG_LEVEL;
    }
    return level;
}