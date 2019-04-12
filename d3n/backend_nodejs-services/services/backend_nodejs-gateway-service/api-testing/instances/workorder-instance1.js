var Service = require('nodejs-default-service'),
    fs = require('fs'),
    numCPUs = require('os').cpus().length,
    ProtocolMessage = require('nodejs-protocol')
;

knownConnections = {};

var service = new Service({
    serviceName: 'test',
    secure: true,
    port: 4003,
    ip: 'localhost',
    key: fs.readFileSync(__dirname + '/../ssl-certificate/key.pem', 'utf8'),
    cert: fs.readFileSync(__dirname + '/../ssl-certificate/cert.pem', 'utf8'),
    auth: {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/../jwt-keys/pubkey.pem', 'utf8')
    }
});


service.messageHandlers['ping'] = function (connection, message, cb) {
        
        var resMessage = new ProtocolMessage(Object.assign({}, message.getMessageContainer()));
        
        resMessage.setMessage(message.getMessage() + 'Response');
        resMessage.setContent(message.getContent());
		resMessage.setSeq(null);
        resMessage.setAck([message.getSeq()]);
        resMessage.setError(null);
        
        return setImmediate(cb, resMessage);
};

service.messageHandlers['enter'] = function (connection, message, cb) {
        
        var resMessage = new ProtocolMessage();
        
		
        resMessage.setMessage(message.getMessage() + 'Response');
        resMessage.setContent(null);
		resMessage.setSeq(null);
        resMessage.setAck([message.getSeq()]);
        resMessage.setError(null);
        
        knownConnections[""+message.getClientId()+""] = {
			clientId : message.getClientId(),
			socket : connection
		};
		
        return setImmediate(cb, resMessage);
};

service.messageHandlers['sendMessage'] = function (connection, message, cb) {
        
        var resMessage = new ProtocolMessage();
        resMessage.setMessage(message.getMessage() + 'Response');
        resMessage.setContent(null);
		resMessage.setSeq(null);
        resMessage.setAck([message.getSeq()]);
        resMessage.setError(null);
		
		var content = message.getContent();
		
		/*
		@TODO:
		The protocol message should hold a valid message object already on instance creation (constructor).
		It makes no sense to have to call all set methods first.
		*/
		var outgoingMessage = new ProtocolMessage();
		outgoingMessage.setMessage("messageReceived");
        outgoingMessage.setContent(content);
		outgoingMessage.setSeq(null);
        outgoingMessage.setAck(null);
        outgoingMessage.setError(null);
        
		for (var prop in knownConnections) {
		  if( knownConnections.hasOwnProperty( prop ) ) {
			  /*
			  @TODO: 
			  According to the Document F4M Backend Architecture, there should be already methods provided from the base service implementation, to send messages to one or a group of clients.
			  This is visible in Page 6 "Event Broadcast System". Same goes for the "Data Store". This needs to be implemented.
			  */
			  outgoingMessage.setClientId(knownConnections[prop].clientId);
			  knownConnections[prop].socket.sendUTF(JSON.stringify(outgoingMessage.getMessageContainer()));
		  } 
		}
		
        return setImmediate(cb, resMessage);
};


service.build(function () {
});