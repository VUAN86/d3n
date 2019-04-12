var Config = require('./app/config/config.js'),
    ProtocolDebugger = require('./app/classes/ProtocolDebugger.js');

var service = new ProtocolDebugger(Config);
service.build(function (err) {
    if (err) {
        Config.logger.error('Error building NodeJS ProtocolDebuger', err);
        process.exit(1);
    }
    Config.logger.info('NodeJS Protocol Debugger started successfully');
});