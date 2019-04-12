var _ = require('lodash');
var path = require('path');
var cp = require('child_process');
var logger = require('nodejs-logger')();

var BackgroundProcessingModule = function (config) {
    var self = this;
    // Default configuration
    var _config = {
        backgroundProcesses: {
            // If true, run background processing servise and load job instances from CronMasterJob (classes/common-jobs/*.js, app/jobs/*.js) classes
            enabled: false,
            // If true, run background processing servise as Node Child Process
            asChildProcess: false,
            // If true, run GC explicitly on TICK_STARTED, prints out differences to console on TICK_COMPLETE - may take extra 20 seconds to process
            jobHeapDirrerences: false,
            // If true, exports heap snapshot first on TICK_STARTED, then second on TICK_COMPLETE
            jobHeapSnapshots: false,
        },
        memoryWatching: {
            // If true, run memory watching servise, which prints out GC state on change (state event) and monitors possible memory leaks (leak event) and then prints out heap snapshots
            enabled: false,
        },
    };
    // Merge configuration from external service
    _config = _.assign(_config, config);
    BackgroundProcessingModule.prototype.Config = _config;
    // Start background processing service
    self.start();
}

BackgroundProcessingModule.getInstance = function (config) {
    if (_.isUndefined(BackgroundProcessingModule._instance)) {
        BackgroundProcessingModule._instance = new BackgroundProcessingModule(config);
    }
    return BackgroundProcessingModule._instance;
}

BackgroundProcessingModule.prototype.start = function () {
    var self = this;
    // Initialize background processing service
    if (self.Config.backgroundProcesses.enabled) {
        if (self.Config.backgroundProcesses.childProcess) {
            var _bpService = cp.fork(path.join(__dirname, './app/services/BackgroundProcessingService.js'));
            _bpService.on('message', function (message) {
                if (_.has(message, 'started') && message.started) {
                    logger.info('Starting background processes done');
                }
            });
            _bpService.send({ start: true, config: self.Config });
            BackgroundProcessingModule.prototype.BackgroundProcessingService = _bpService;
        } else {
            var BackgroundProcessingService = require('./app/classes/BackgroundProcessingService.js');
            var _bpService = new BackgroundProcessingService(self.Config);
            _bpService.init();
            BackgroundProcessingModule.prototype.BackgroundProcessingService = _bpService;
        }
    }
    // Initialize memory watching service
    if (self.Config.memoryWatching.enabled) {
        var MemoryWatchingService = require('./app/classes/MemoryWatchingService.js');
        BackgroundProcessingModule.prototype.MemoryWatchingService = new MemoryWatchingService(self.Config);
    }
    return true;
}

BackgroundProcessingModule.prototype.shutdown = function () {
    var self = this;
    if (self.Config.backgroundProcesses.enabled) {
        if (self.Config.backgroundProcesses.childProcess && self.BackgroundProcessingService) {
            self.BackgroundProcessingService.kill();
        }
    }
    return true;
}

module.exports = BackgroundProcessingModule;