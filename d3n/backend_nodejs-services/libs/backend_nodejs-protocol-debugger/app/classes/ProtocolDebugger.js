;
var express = require('express'),
    bodyParser = require('body-parser'),
    http = require('http'),
    WebSocketServer = require('websocket').server,
    winston = require("winston"),
    uuid = require('uuid'),
    fs = require('fs');

/**
 * @type ProtocolDebugger
 * @param {ProtocolDebuggerConfig} config
 * @constructor
 */
var ProtocolDebugger = function(config) {
    /**
     * @type {ProtocolDebuggerConfig}
     * @private
     */
    this._config = config;

    /**
     * @type {object|undefined} HTTP Server instance to listen to incomming connections
     * @private
     */
    this._httpInstance = undefined;

    /**
     * @type {object} Express Server instance to listen to incomming connections
     * @private
     */
    this._expressInstance = express();
    this._expressInstance.use(bodyParser.json());
    this._expressInstance.use(bodyParser.urlencoded({ extended: true }));
    this._expressInstance.use(express.static('app/web'));
    this._expressInstance.post('/winstonLog',  this._winstonLog.bind(this) );
    this._expressInstance.get('/getLogFiles', this._getLogFiles.bind(this) );

    /**
     * @type {object|undefined} Websocket Server instance to listen to incomming connections
     * @private
     */
    this._wsInstance = undefined;

    /**
     * @type {winston.Logger}
     * @private
     */
    this._rotationalLogger = new (winston.Logger)({
        transports: [
            new (winston.transports.Console)({
                timestamp : true,
                colorize : true
            })
        ]
    });

    this._rotationalLogger.add(require('winston-daily-rotate-file'), {
        filename: __dirname + '/../web/logs/protocol.log',
        colorize: false,
        timestamp: true,
        json: true,
        maxsize: 1024*1024*1024,
        maxFiles: 5,
        zippedArchive: false,
        datePattern: 'yyyy-MM-dd HH-mm ',
        prepend: true
    });

    /**
     * @type {Object.<string,Object>}
     * @private
     */
    this._clients = {};

};

var o = ProtocolDebugger.prototype;

/**
 * Handles request to get a list of log files
 * @param req
 * @param res
 * @private
 */
o._getLogFiles = function(req, res) {
    var self = this;
    fs.readdir( __dirname + "/../web/logs", function (err, files) {
        if (!err) {
            res.writeHead(200, {'Content-Type': 'text/plain'});

            res.end(JSON.stringify({files: files, config: self._config}));
        } else {
            res.writeHead(500, {'Content-Type': 'text/plain'});
            self._config.logger.error(err);
            res.end("Error while trying to receive file list.");
        }
    });
};


/**
 * Handles request from http logger
 * @param req
 * @param res
 * @private
 */
o._winstonLog = function(req, res) {
    var srcAdress = req.connection.remoteAddress;
    var message = null;
    try {
        if (req.body.identification) {
            message = req.body;
        } else if (req.body.params.message) {
            message = JSON.parse(req.body.params.message);
        }
        if (message) {
            if (message.identification) {
                message.identification = srcAdress + "P" + message.identification;
            }
            this._rotationalLogger.info(message);

            this._broadcastWebsocket(JSON.stringify(message));
        }

        res.writeHead(200, {'Content-Type': 'text/plain'});
        res.end("");
    } catch (e) {
        if (res) {
            //Not what we are searching...
            res.writeHead(400, {'Content-Type': 'text/plain'});
            res.end("");
        }
    }
};

/**
 * Sends string to all connected websockets.
 * @param {string} messageString
 * @private
 */
o._broadcastWebsocket = function(messageString) {
    for (var uuid in this._clients) {
        if (this._clients.hasOwnProperty(uuid)) {
            this._clients[uuid].sendUTF(messageString);
        }
    }
};

/**
 * Handle the request. Create connection session, set message handler, handle connection close event
 * @param {object} request
 * @private
 */
o._onWebSocketRequestHandler = function (request) {
    var self = this;
    var connection = request.accept(null, request.origin);
    connection.uuid = uuid.v4();

    self._clients[connection.uuid] = connection;

    self._config.logger.info("Client connected.");

    connection.on('message', function(message) {
        self._config.logger.info(message.utf8Data);
    });

    connection.on('close', function() {
        delete self._clients[connection.uuid];
        self._config.logger.info("Client disconnected.");
    });
};

/**
 * Build the service. Create http server, set on request handler
 * @param {Function} cb
 */
o.build = function(cb) {
    //Close existing connections if exist
    this.shutdown();

    var server = http.createServer(this._expressInstance);
    this._httpInstance = server.listen(this._config.port, this._config.ip, cb);

    this._wsInstance = new WebSocketServer({
        httpServer: server,
        autoAcceptConnections: false
    });
    this._wsInstance.on('request', this._onWebSocketRequestHandler.bind(this));
};

/**
 * Shutting down service, closing all connections
 */
o.shutdown = function () {
    var self = this;
    if (self._httpInstance) {
        self._httpInstance.close();
        self._httpInstance = undefined;
    }

    if (self._wsInstance) {
        self._wsInstance.shutDown();
        self._wsInstance = undefined;
    }
};


module.exports=ProtocolDebugger;