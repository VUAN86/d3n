var fs = require('fs'),
    tempfile = require('tempfile'),
    should = require('should');

var loggers = {};

describe('Logger', function () {
    before(function (done) {
        // 1: Development + Default Config
        process.env.LOG_LEVEL = undefined;
        process.env.ENV = 'development';
        loggers['DEV'] = require('./../../services/loggerService.js')();
        // 2: Development + External Config
        process.env.LOG_LEVEL = undefined;
        process.env.ENV = 'development';
        loggers['DEV_XC'] = require('./../../services/loggerService.js')({
            timestamp: false,
            level: 'warn'
        });
        // 3: Development + LOG_LEVEL + Default Config
        process.env.LOG_LEVEL = 'verbose';
        process.env.ENV = 'development';
        loggers['DEV_LL'] = require('./../../services/loggerService.js')();
        // 4: Development + LOG_LEVEL + External Config
        process.env.LOG_LEVEL = 'verbose';
        process.env.ENV = 'development';
        loggers['DEV_LL_XC'] = require('./../../services/loggerService.js')({
            timestamp: false,
            level: 'warn'
        });
        // 5: Production + Default Config
        process.env.LOG_LEVEL = undefined;
        process.env.ENV = 'production';
        loggers['PROD'] = require('./../../services/loggerService.js')();
        // 6: Production + External Config
        process.env.LOG_LEVEL = undefined;
        process.env.ENV = 'production';
        loggers['PROD_XC'] = require('./../../services/loggerService.js')({
            timestamp: false,
            level: 'warn'
        });
        // 7: Production + LOG_LEVEL + Default Config
        process.env.LOG_LEVEL = 'verbose';
        process.env.ENV = 'production';
        loggers['PROD_LL'] = require('./../../services/loggerService.js')();
        // 8: Production + LOG_LEVEL + External Config
        process.env.LOG_LEVEL = 'verbose';
        process.env.ENV = 'production';
        loggers['PROD_LL_XC'] = require('./../../services/loggerService.js')({
            timestamp: false,
            level: 'warn'
        });
        // 9: Development + File
        process.env.LOG_FILE = tempfile('test.log');
        process.env.LOG_LEVEL = 'verbose';
        process.env.ENV = 'development';
        loggers['FILE'] = require('./../../services/loggerService.js')();
        done();
    });

    it('DEV: Default Config', function (done) {
        should.exists(loggers['DEV']);
        loggers['DEV'].transports.console.level.should.be.eql('debug');
        loggers['DEV'].transports.console.colorize.should.be.eql(true);
        loggers['DEV'].transports.console.timestamp.should.be.eql(true);
        done();
    });
    it('DEV: External Config', function (done) {
        should.exists(loggers['DEV_XC']);
        loggers['DEV_XC'].transports.console.level.should.be.eql('warn');
        loggers['DEV_XC'].transports.console.colorize.should.be.eql(true);
        loggers['DEV_XC'].transports.console.timestamp.should.be.eql(false);
        done();
    });
    it('DEV: LOG_LEVEL+Default Config', function (done) {
        should.exists(loggers['DEV_LL']);
        loggers['DEV_LL'].transports.console.level.should.be.eql('verbose');
        loggers['DEV_LL'].transports.console.colorize.should.be.eql(true);
        loggers['DEV_LL'].transports.console.timestamp.should.be.eql(true);
        done();
    });
    it('DEV: LOG_LEVEL+External Config', function (done) {
        should.exists(loggers['DEV_LL_XC']);
        loggers['DEV_LL_XC'].transports.console.level.should.be.eql('warn');
        loggers['DEV_LL_XC'].transports.console.colorize.should.be.eql(true);
        loggers['DEV_LL_XC'].transports.console.timestamp.should.be.eql(false);
        done();
    });
    it('PROD: Default Config', function (done) {
        should.exists(loggers['PROD']);
        loggers['PROD'].transports.console.level.should.be.eql('info');
        loggers['PROD'].transports.console.colorize.should.be.eql(true);
        loggers['PROD'].transports.console.timestamp.should.be.eql(true);
        done();
    });
    it('PROD: External Config', function (done) {
        should.exists(loggers['PROD_XC']);
        loggers['PROD_XC'].transports.console.level.should.be.eql('warn');
        loggers['PROD_XC'].transports.console.colorize.should.be.eql(true);
        loggers['PROD_XC'].transports.console.timestamp.should.be.eql(false);
        done();
    });
    it('PROD: LOG_LEVEL+Default Config', function (done) {
        should.exists(loggers['PROD_LL']);
        loggers['PROD_LL'].transports.console.level.should.be.eql('verbose');
        loggers['PROD_LL'].transports.console.colorize.should.be.eql(true);
        loggers['PROD_LL'].transports.console.timestamp.should.be.eql(true);
        done();
    });
    it('PROD: LOG_LEVEL+External Config', function (done) {
        should.exists(loggers['PROD_LL_XC']);
        loggers['PROD_LL_XC'].transports.console.level.should.be.eql('warn');
        loggers['PROD_LL_XC'].transports.console.colorize.should.be.eql(true);
        loggers['PROD_LL_XC'].transports.console.timestamp.should.be.eql(false);
        done();
    });
    it('Console + File', function (done) {
        try {
            should.exists(loggers['FILE']);
            loggers['FILE'].info('test', { seriously: true }, function (err, level, msg, meta) {
                fs.stat(process.env.LOG_FILE, function (err, stats) {
                    if (err) {
                        return done(err);
                    }
                    if (stats.size === 0) {
                        return done(false);
                    }
                    done();
                });
            });
        } catch (ex) {
            done(ex);
        }
    });
    it('Integration', function (done) {
        try {
            loggers['DEV'].info('test');
            loggers['DEV_XC'].info('test');
            loggers['DEV_LL'].info('test');
            loggers['DEV_LL_XC'].info('test');
            loggers['PROD'].info('test');
            loggers['PROD_XC'].info('test');
            loggers['PROD_LL'].info('test');
            loggers['PROD_LL_XC'].info('test');
            done();
        } catch (ex) {
            done(ex);
        }
    });
    it('No HTTP Logger present', function (done) {
        try {
            should.not.exist(loggers['DEV'].transports.http);
            should.not.exist(loggers['DEV_XC'].transports.http);
            should.not.exist(loggers['DEV_LL'].transports.http);
            should.not.exist(loggers['DEV_LL_XC'].transports.http);
            should.not.exist(loggers['PROD'].transports.http);
            should.not.exist(loggers['PROD_XC'].transports.http);
            should.not.exist(loggers['PROD_LL'].transports.http);
            should.not.exist(loggers['PROD_LL_XC'].transports.http);
            done();
        } catch (ex) {
            done(ex);
        }
    });
});
