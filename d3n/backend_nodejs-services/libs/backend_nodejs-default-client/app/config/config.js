var fs = require('fs');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var sslPathService = process.cwd() + '/app/config/ssl-certificate';
var sslPathModule = __dirname + '/ssl-certificate';

var sslPath; 
if (fs.existsSync(sslPathService)) {
    sslPath = sslPathService;
} else {
    sslPath = sslPathModule;
}



if (process.env.NODE_ENV === 'production' && sslPath === sslPathModule) {
    // on production allow only certificates from service folder
    logger.error('Certificates folder does not exists');
    process.exit(1);
}


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


var Config = {
    trustedCerts: trustedCerts,
    sslKey: fs.readFileSync(sslPath + '/key.pem'),
    sslCert: fs.readFileSync(sslPath + '/cert.pem')
};

module.exports = Config;

function trimCert(cert) {
    return cert.split('-----END CERTIFICATE-----')[0].replace('-----BEGIN CERTIFICATE-----', '').replace('-----END CERTIFICATE-----', '').replace(/\n/g, '');
}