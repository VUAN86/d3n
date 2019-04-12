var fs = require('fs');
var jwt = require('jsonwebtoken');

var TEST_USER_ID = '9999999-9999-9999-9999-999999999999';
var MERGE_USER_ID = '2222222-2222-2222-2222-222222222222';
var DEVICE_UUID = '3333333-3333-3333-3333-333333333333';

module.exports = {
    TEST_USER_ID: TEST_USER_ID,
    MERGE_USER_ID: MERGE_USER_ID,
    DEVICE_UUID: DEVICE_UUID,
    TEST_TOKEN: _encode({ userId: TEST_USER_ID, roles: [] }),
    TEST_PROFILE: {
        userId: TEST_USER_ID,
        roles: [],
        person: {
            firstName: 'Test',
            lastName: 'Test',
            nickname: 'Test',
            birthDate: '1979-03-09',
            sex: 'M',
        },
        address: {
            street: 'Test',
            streetNumber: 'Test',
            city: 'Test',
            postalCode: 'Test',
            country: 'DE',
        },
        emails: [],
        phones: [],
        facebook: null,
        google: null,
        devices: [],
    },
    TEST_PROFILE_BLOB: {
        name: 'auth',
        value: {
            password: 'test',
            code: 1234
        }
    }
};

function _encode(payload) {
    var jwtOptions = {
        algorithm: 'RS256',
        issuer: 'F4M',
        expiresIn: 3600
    };
    var privateKey = fs.readFileSync(__dirname + '/../jwt-keys/privkey.pem', 'utf8');
    return jwt.sign(payload, privateKey, jwtOptions);
}