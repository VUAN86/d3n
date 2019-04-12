var Gateway = require('../../classes/Gateway.js'),
    workerConfig = JSON.parse(process.argv[2]),
    ProtocolMessage = require('nodejs-protocol')
;
//console.log(workerConfig); proces.exit()
var gateway = new Gateway({
    gateway: workerConfig.gateway,
    registryServiceURIs: workerConfig.registryServiceURIs,
    serviceInstances: workerConfig.serviceInstances
});



gateway.messageDispatcher.profileServiceClient.getUserProfile = function (params, token, cb) {
    var m = new ProtocolMessage();
    var profile = {
        userId: '1235454',
        roles: ['EXTERNAL'],
        language: 'en',
        handicap: 1.2,
        emails: ['asda@gmail.com'],
        phones: []
    };
    
    m.setContent({
        profile: profile
    });
    
    return cb(false, m);
};

gateway.build(function (err) {
    process.send({
        success: err ? false : true
    });
});