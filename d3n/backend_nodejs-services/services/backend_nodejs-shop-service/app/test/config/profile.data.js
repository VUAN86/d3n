var fs = require('fs');
var path = require('path');
var jwt = require('jsonwebtoken');
var DataIds = require('./_id.data.js');

module.exports = {
    
    TEST_TOKEN: _encode({ userId: DataIds.USER_ID, roles: [] }),
    
    CLIENT_INFO: {
        profile: {
            userId: DataIds.USER_ID
        },
        
        appConfig: {
            tenantId: DataIds.TENANT_ID
        },
        
        ip: null
    },

    TEST_PROFILE: {
        userId: DataIds.USER_ID,
        person: {
            firstName: 'Test First Name',
            lastName: 'Test Last Name'
        },
        address: {
            street: 'Local Street',
            streetNumber: '123',
            city: 'Iasi',
            postalCode: 'LOC123456',
            country: 'RO'
        },
        emails: [
            {
                email: "a@a.aaa"
            }
        ]
    },

    TEST_PROFILE_BLOB: {
        userId: DataIds.USER_ID,
        name: 'auth',
        value: {
            password: 'test',
            code: 1234
        }
    },

    SHIPPING_ADDRESS: {
        firstName: "First name test",
        lastName: "Last name test",
        street: "Street test",
        streetNumber: "1A",
        city: "City test",
        country: DataIds.COUNTRY_ISO_ID,
        zip: "Zip test"
    },

    SHIPPING_ADDRESS_INVALID_COUNTRY: {
        firstName: "First name test",
        lastName: "Last name test",
        street: "Street test",
        streetNumber: "1A",
        city: "City test",
        country: DataIds.COUNTRY_ISO_INVALID_ID,
        zip: "Zip test"
    },

    SHIPPING_ADDRESS_MISSING_NAME: {
        street: "Street test",
        streetNumber: "1A",
        city: "City test",
        country: DataIds.COUNTRY_ISO_ID,
        zip: "Zip test"
    },
}

function _encode(payload) {
    var jwtOptions = {
        algorithm: 'RS256',
        issuer: 'F4M',
        expiresIn: 3600
    };
    var privateKey = fs.readFileSync(path.join(__dirname, '/../../config/jwt-keys/privkey.pem'), 'utf8');
    var response = jwt.sign(payload, privateKey, jwtOptions);
    return response;
}