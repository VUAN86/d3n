var Gateway = require('../lib/Gateway.js'),
    // hard coded , must be discovered by monitoring service    
    instances = {
        'question': [
            {
                ip: 'localhost',
                port: 4001,
                secure: true
            },
            {
                ip: 'localhost',
                port: 4002,
                secure: true
            }
        ],
        'test': [
            {
                ip: 'localhost',
                port: 4003,
                secure: true
            }
        ]
    }
;

gateway = new Gateway({
    gateway: {
        ip: process.env.HOST || 'localhost',
        port: 4000,
        secure: true,
        key: __dirname + '/ssl-certificate/key.pem',
        cert: __dirname + '/ssl-certificate/cert.pem'
    },
    serviceInstances: instances
});

gateway.build(function (err) {
    
});
