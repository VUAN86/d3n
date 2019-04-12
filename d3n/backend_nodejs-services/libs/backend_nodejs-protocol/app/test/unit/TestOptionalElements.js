var should = require('should'),
    ProtocolMessage = require('../../classes/ProtocolMessage.js');

describe('Testing protocol message container', function() {

    it('message null', function (){
        var message = new ProtocolMessage();


        should.strictEqual(message.getMessage(), null);
        should.strictEqual(message.getMessageNamespace (), '');
        should.strictEqual(message.getMessageName (), '');
    });

    it('namespace non existant', function (){
        var message = new ProtocolMessage();

        message.setMessage('ping');

        should.strictEqual(message.getMessage(), 'ping');
        should.strictEqual(message.getMessageNamespace (), '');
        should.strictEqual(message.getMessageName (), 'ping');
    });

    it('namespace existant', function (){
        var message = new ProtocolMessage();

        message.setMessage('test/ping');

        should.strictEqual(message.getMessage(), 'test/ping');
        should.strictEqual(message.getMessageNamespace (), 'test');
        should.strictEqual(message.getMessageName (), 'ping');
    });

    it('subnamespace existant', function (){
        var message = new ProtocolMessage();

        message.setMessage('test/sub/ping');

        should.strictEqual(message.getMessage(), 'test/sub/ping');
        should.strictEqual(message.getMessageNamespace (), 'test');
        should.strictEqual(message.getMessageName (), 'sub/ping');
    });

    it('optional client id', function (){
        var message = new ProtocolMessage();

        should.strictEqual(message.getClientId (), undefined);
        message.setClientId ('Client ID');
        should.strictEqual(message.getClientId (), 'Client ID');
        message.deleteClientId();
        should.strictEqual(message.getClientId (), undefined);
    });

    it('optional token', function (){
        var message = new ProtocolMessage();

        should.strictEqual(message.getToken(), undefined);
        message.setToken('Token');
        should.strictEqual(message.getToken (), 'Token');
        message.deleteToken();
        should.strictEqual(message.getToken (), undefined);
    });

    it('optional timestamp', function (){
        var message = new ProtocolMessage();

        should.strictEqual(message.getTimestamp(), undefined);
        message.setTimestamp(1469187124000);
        should.strictEqual(message.getTimestamp (), 1469187124000);
        message.deleteTimestamp();
        should.strictEqual(message.getTimestamp (), undefined);
    });

    it('set/get message container', function (){
        var message = new ProtocolMessage();

        var container = {
            message: "test",
            content: {test: "test"},
            error: null,
            ack: [1,2,3],
            seq: 1,
            clientId: "test",
            timestamp: 1562626262
        };

        message.setMessage("test");
        message.setContent({test: "test"});
        message.setError(null);
        message.setAck([1,2,3]);
        message.setSeq(1);
        message.setClientId("test");
        message.setTimestamp(1562626262);


        should.deepEqual(message.getMessageContainer(), container);

        message = new ProtocolMessage();
        message.setMessageContainer(container);

        should.deepEqual(message.getMessageContainer(), container);
    });

});