var FakeRegistryService = require('./classes/FakeRegistryService.js');
var fs = require('fs');
var logger = require('nodejs-logger')();
var ServiceClient = require('nodejs-default-client');
var ProtocolMessage = require('nodejs-protocol');

var fakeRegistryService = new FakeRegistryService({
    secure: true,
    ip: 'localhost',
    port: 4000,
    serviceName: 'fakeServiceRegistry',
    key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
    cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
    auth: {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
    } 
});

fakeRegistryService.build(function (err) {
    if(err) {
        logger.error('Error starting fake registry service', err);
        process.exit(1);
    }
    
    logger.info('Fake registry service started');
    var client = new ServiceClient({
        secure: true,
        service: {
            ip: 'localhost',
            port: 4000
        }
    });
    
    client.connect(function (err) {
        if (err) {
            logger.error('Error connecting to fake registry service', err);
            process.exit(1);
        }
        
        
        logger.info('Connected to fake registry service');
        
        var message = new ProtocolMessage();
        message.setMessage('fakeServiceRegistry/register');
        message.setSeq(1);
        message.setContent({
            serviceName: 'testService',
            uri: 'wss://localhost:1'
        });
        
        client.sendMessage(message, function () {
            
        });
        
        var message2 = new ProtocolMessage();
        message2.setMessage('fakeServiceRegistry/register');
        message2.setSeq(2);
        message2.setContent({
            serviceName: 'testService2',
            uri: 'wss://localhost:2'
        });
        client.sendMessage(message2, function () {
            
        });
        
        setTimeout(function () {
            var messageList = new ProtocolMessage();
            messageList.setMessage('fakeServiceRegistry/list');
            messageList.setSeq(3);
            messageList.setContent(null);
            client.sendMessage(messageList, function (err, response) {
                console.log(err, response.getContent());
            });
        }, 2000);
        
        
    });
});
