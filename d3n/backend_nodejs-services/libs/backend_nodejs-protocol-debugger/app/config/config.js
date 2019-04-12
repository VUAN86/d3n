var logger = require('nodejs-logger')();

/**
 * @typedef {object} ProtocolDebuggerConfig
 * @property {string} ip
 * @property {number} port
 * @property {object} logger
 */

/**
 * @type {ProtocolDebuggerConfig}
 */
var Config = {
    ip: process.env.HOST || 'localhost',
    port: process.env.PORT || 9090,
    logger: logger
};

module.exports = Config;