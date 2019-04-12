var should = require('should');
var ProtocolMessage = require('../../classes/ProtocolMessage.js');

describe('Testing API security validation', function() {
    it('access application/applicationGet with required role', function() {
        var message = new ProtocolMessage();
        message.setMessage('application/applicationGet');

        should(message.validateSecurity(['REGISTERED', 'TENANT_1_ADMIN'], 1)).be.true();
    });

    // Security check as per issue #6145
    it('access profileManager/profileGet having a single permission from the required list', function() {
        var message = new ProtocolMessage();
        message.setMessage('profileManager/profileGet');

        should(message.validateSecurity(['REGISTERED', 'TENANT_1_EXTERNAL'], 1)).be.true();
    });

    it('cannon access application/applicationGet with invalid tenant ADMIN role', function() {
        var message = new ProtocolMessage();
        message.setMessage('application/applicationGet');

        should(message.validateSecurity(['REGISTERED', 'ADMIN'], 1)).be.false();
    });

    it('cannot access application/applicationGet without required role', function() {
        var message = new ProtocolMessage();
        message.setMessage('application/applicationGet');

        should(message.validateSecurity(['REGISTERED', 'TENANT_1_COMMUNITY'], 1)).be.false();
    });

    it('cannot access application/applicationGet without no user role specified', function() {
        var message = new ProtocolMessage();
        message.setMessage('application/applicationGet');

        should(message.validateSecurity('', 1)).be.false();
    });
});

describe('Testing private APIs', function() {
    it('not private', function() {
        var message = new ProtocolMessage();
        message.setMessage('auth/inviteUserByEmailAndRole');

        should(message.isPrivate()).be.false();
    });
    it('private', function() {
        var message = new ProtocolMessage();
        message.setMessage('auth/addConfirmedUser');

        should(message.isPrivate()).be.true();
    });
    
});
