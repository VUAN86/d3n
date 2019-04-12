var fs = require('fs');
var jwt = require('jsonwebtoken');
var DataIds = require('./_id.data.js');

module.exports = {
    LOCAL_TOKEN: _encode({ userId: DataIds.LOCAL_USER_ID }),
    TEST_TOKEN: _encode({ userId: DataIds.TEST_USER_ID }),
    
    CLIENT_INFO_1: {
        profile: {
            userId: DataIds.LOCAL_USER_ID
        },
        
        appConfig: {
            tenantId: DataIds.TENANT_1_ID
        },
        
        ip: null
    },

    CLIENT_INFO_2: {
        profile: {
            userId: DataIds.LOCAL_USER_ID
        },
        
        appConfig: {
            tenantId: DataIds.TENANT_2_ID
        },
        
        ip: null
    },
    
    CLIENT_INFO_3: {
        profile: {
            userId: DataIds.GOOGLE_USER_ID
        },
        
        appConfig: {
            tenantId: DataIds.TENANT_1_ID
        },
        
        ip: null
    }
    
};

function _encode(payload) {
    var jwtOptions = {
        algorithm: 'RS256',
        issuer: 'F4M',
        expiresIn: 3600
    };
    var privateKey = fs.readFileSync(__dirname + '/../../jwt-keys/privkey.pem', 'utf8');
    return jwt.sign(payload, privateKey, jwtOptions);
}