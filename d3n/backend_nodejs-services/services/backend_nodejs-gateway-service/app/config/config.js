var fs = require('fs');
var logger = require('nodejs-logger')();
var config = {
    gateway: {
        ip: process.env.HOST || 'localhost',
        port: process.env.PORT ? parseInt(process.env.PORT) : 4100,
        secure: process.env.SECURE ? (process.env.SECURE === 'true') : true,
        externIp: process.env.EXTERN_IP,
        externUrl: process.env.EXTERN_URL,
        key: __dirname + '/ssl-certificate/key.pem',
        cert: __dirname + '/ssl-certificate/cert.pem',
        auth: {
            algorithm: 'RS256',
            publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8'),
            privateKey: fs.readFileSync(__dirname + '/jwt-keys/privkey.pem', 'utf8')
        }
    },
    
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000',
    
    registryServiceURIsNode: process.env.REGISTRY_SERVICE_URIS_NODE || 'wss://localhost:5000',
    
    
    connectMaxTime: process.env.CONNECT_MAX_TIME || 1000*30,
    
    reconnectMaxTime: process.env.RECONNECT_MAX_TIME || 1000*60*5, // 5 minutes
    
    /*
    connectMaxTime: 3000,
    
    reconnectMaxTime: 3000, // 5 minutes
    */
    
    checkCert: (process.env.CHECK_CERT ? (process.env.CHECK_CERT === 'true') : true)
};

config.gateway.keyAsString = fs.readFileSync(config.gateway.key, 'utf8');
config.gateway.certAsString = fs.readFileSync(config.gateway.cert, 'utf8');

var incrementPortCnt = 0;
config.incrementPort = function () {
    var inc = incrementPortCnt*10;
    incrementPortCnt++;
    return inc;
};

var sslPathService = process.cwd() + '/app/config/ssl-certificate';
var sslPath = sslPathService;

var trustedCerts = [];

/*
// add service certificate as trusted certificate
trustedCerts.push(trimCert(fs.readFileSync(sslPath + '/cert.pem', 'utf8')));
*/

// add from list of trusted certificates
var certFiles = fs.readdirSync(sslPath + '/trusted-certs');
for(var i=0; i<certFiles.length; i++) {
    var content = fs.readFileSync(sslPath + '/trusted-certs/' + certFiles[i], 'utf8');
    trustedCerts.push(trimCert(content));
}


config.trustedCerts = trustedCerts;

module.exports = config;

function trimCert(cert) {
    return cert.split('-----END CERTIFICATE-----')[0].replace('-----BEGIN CERTIFICATE-----', '').replace('-----END CERTIFICATE-----', '').replace(/\n/g, '');
}