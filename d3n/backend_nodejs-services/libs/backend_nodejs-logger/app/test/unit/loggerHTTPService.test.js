var fs = require('fs'),
    tempfile = require('tempfile'),
    should = require('should');

var logger = null;
var port = 9615;
var http = require('http');
var server = null;
var receivedMessage = null;

describe('Logger HTTP', function () {
    before(function (done) {
        server = http.createServer(function (req, res) {
            receivedMessage = "";
            req.on('data', function (chunk) {
                receivedMessage += chunk;
            });
            req.on('end', function () {
                res.writeHead(200, {'Content-Type': 'text/plain'});
                res.end("");
            });
        }).listen(port, function() {
            // 1: Development + Default Config with http server
            process.env.LOG_LEVEL = undefined;
            process.env.LOG_FILE = undefined;
            process.env.ENV = 'development';
            process.env.LOG_SERVER = '127.0.0.1';
            process.env.LOG_SERVER_PORT = port;
            logger = require('./../../services/loggerService.js')();
            
            done();
        });
    });

    after(function(done) {
        process.env.LOG_SERVER = undefined;
        process.env.LOG_SERVER_PORT = undefined;

        // Close the server
        server.close(function () { console.log('Server closed!'); });

        done();
    });

    it('HTTP Logger available and configured', function (done) {
        should.exists(logger);
        logger.transports.console.level.should.be.eql('debug');
        logger.transports.console.colorize.should.be.eql(true);
        logger.transports.console.timestamp.should.be.eql(true);
        should.exists(logger.transports.http);
        logger.transports.http.level.should.be.eql('debug');
        should.not.exists(logger.transports.http.colorize);
        should.not.exists(logger.transports.http.timestamp);
        done();
    });

    it('HTTP Logger log error message', function (done) {
        var errorMessage = "Testing Error Message - don't worry about the log entry.";

        var errorHandler =  function (err) {
            receivedMessage = null;
            logger.removeListener('logging', loggingHandler);
            logger.removeListener('error', errorHandler);
            
            should.fail(err, "no error");
            done();
        };

        var loggingHandler = function (transport, level, msg, meta) {
            try {
                // [msg] and [meta] have now been logged at [level] to [transport]
                should.exist(transport);
                should.exist(transport.host);
                should.exist(transport.port);
                should.exist(transport.path);
                transport.host.should.be.eql('127.0.0.1');
                transport.port.should.be.eql(port+"");
                transport.path.should.be.eql('/winstonLog');
                level.should.be.eql('error');
                msg.should.be.eql(errorMessage);

                should(receivedMessage).not.null();

                receivedMessage = JSON.parse(receivedMessage);

                should.exist(receivedMessage.method);
                receivedMessage.method.should.be.eql('collect');
                should.exist(receivedMessage.params);
                should.exist(receivedMessage.params.level);
                should.exist(receivedMessage.params.message);
                receivedMessage.params.level.should.be.eql('error');
                receivedMessage.params.message.should.be.eql(errorMessage);

                done();
            } catch(e) {
                done(e);
            } finally {
                receivedMessage = null;
                logger.removeListener('logging', loggingHandler);
                logger.removeListener('error', errorHandler);
            }
        };

        logger.error(errorMessage);
        logger.on('logging', loggingHandler);
        logger.on('error', errorHandler);
    });

    it('HTTP Logger log info message', function (done) {
        var errorMessage = "Testing Info Message - don't worry about the log entry.{test:42t2}<br>Test";

        var errorHandler =  function (err) {
            receivedMessage = null;
            logger.removeListener('logging', loggingHandler);
            logger.removeListener('error', errorHandler);

            should.fail(err, "no error");
            done();
        };

        var loggingHandler = function (transport, level, msg, meta) {
            try {
                // [msg] and [meta] have now been logged at [level] to [transport]
                should.exist(transport);
                should.exist(transport.host);
                should.exist(transport.port);
                should.exist(transport.path);
                transport.host.should.be.eql('127.0.0.1');
                transport.port.should.be.eql(port+"");
                transport.path.should.be.eql('/winstonLog');
                level.should.be.eql('info');
                msg.should.be.eql(errorMessage);

                should(receivedMessage).not.null();

                receivedMessage = JSON.parse(receivedMessage);

                should.exist(receivedMessage.method);
                receivedMessage.method.should.be.eql('collect');
                should.exist(receivedMessage.params);
                should.exist(receivedMessage.params.level);
                should.exist(receivedMessage.params.message);
                receivedMessage.params.level.should.be.eql('info');
                receivedMessage.params.message.should.be.eql(errorMessage);

                done();
            } catch(e) {
                done(e);
            } finally {
                receivedMessage = null;
                logger.removeListener('logging', loggingHandler);
                logger.removeListener('error', errorHandler);
            }
        };

        logger.info(errorMessage);
        logger.on('logging', loggingHandler);
        logger.on('error', errorHandler);
    });

    it('HTTP Logger log info json message', function (done) {
        var errorMessage = JSON.stringify({
            timestamp: new Date().getTime(),
            signatur: '127.0.0.1:'+port,
            type: 'service',
            action: 'created',
            name: 'Test Service'
        });

        var errorHandler =  function (err) {
            receivedMessage = null;
            logger.removeListener('logging', loggingHandler);
            logger.removeListener('error', errorHandler);

            should.fail(err, "no error");
            done();
        };

        var loggingHandler = function (transport, level, msg, meta) {
            try {
                // [msg] and [meta] have now been logged at [level] to [transport]
                should.exist(transport);
                should.exist(transport.host);
                should.exist(transport.port);
                should.exist(transport.path);
                transport.host.should.be.eql('127.0.0.1');
                transport.port.should.be.eql(port+"");
                transport.path.should.be.eql('/winstonLog');
                level.should.be.eql('info');
                msg.should.be.eql(errorMessage);

                should(receivedMessage).not.null();

                receivedMessage = JSON.parse(receivedMessage);

                should.exist(receivedMessage.method);
                receivedMessage.method.should.be.eql('collect');
                should.exist(receivedMessage.params);
                should.exist(receivedMessage.params.level);
                should.exist(receivedMessage.params.message);
                receivedMessage.params.level.should.be.eql('info');
                receivedMessage.params.message.should.be.eql(errorMessage);

                errorMessage = JSON.parse(errorMessage);
                should.exist(errorMessage.timestamp);
                should.exist(errorMessage.signatur);
                should.exist(errorMessage.type);
                should.exist(errorMessage.action);
                should.exist(errorMessage.name);
                errorMessage.signatur.should.be.eql('127.0.0.1:'+port);
                errorMessage.type.should.be.eql('service');
                errorMessage.action.should.be.eql('created');
                errorMessage.name.should.be.eql('Test Service');

                done();
            } catch(e) {
                done(e);
            } finally {
                receivedMessage = null;
                logger.removeListener('logging', loggingHandler);
                logger.removeListener('error', errorHandler);
            }
        };

        logger.info(errorMessage);
        logger.on('logging', loggingHandler);
        logger.on('error', errorHandler);
    });
});
