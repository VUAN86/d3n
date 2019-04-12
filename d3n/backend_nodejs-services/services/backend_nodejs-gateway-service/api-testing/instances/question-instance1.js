var Service = require('nodejs-default-service'),
    fs = require('fs'),
    numCPUs = require('os').cpus().length
;

var service = new Service({
    serviceName: 'question',
    secure: true,
    port: 4001,
    ip: 'localhost',
    key: fs.readFileSync(__dirname + '/../ssl-certificate/key.pem', 'utf8'),
    cert: fs.readFileSync(__dirname + '/../ssl-certificate/cert.pem', 'utf8'),
    auth: {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/../jwt-keys/pubkey.pem', 'utf8')
    }
});

/*
service.messageHandlers['ping'] = function (connection, message, cb) {
    return setImmediate(cb, {
            'm': message.m + 'Response',
            'cid': message.cid,
            'c': {'evenMoreData': 'pong data from question service'},
            'ack': [message.seq]
    });
};
*/

service.build(function () {
});