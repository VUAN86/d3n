var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var md5 = require('md5');
var crypto = require('crypto');
var jwt = require('jsonwebtoken');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeUser = KeyvalueService.Models.AerospikeUser;
var AerospikeUserToken = KeyvalueService.Models.AerospikeUserToken;
var AerospikeProfileSync = KeyvalueService.Models.AerospikeProfileSync;

var _EMAIL_TOKEN = _encode({ userId: DataIds.EMAIL_USER_ID, roles: [] });
var _PHONE_TOKEN = _encode({ userId: DataIds.PHONE_USER_ID, roles: [] });
var _FACEBOOK_TOKEN = _encode({ userId: DataIds.FACEBOOK_USER_ID, roles: [] });
var _GOOGLE_TOKEN = _encode({ userId: DataIds.GOOGLE_USER_ID, roles: [] });
var _TEST_EMAIL_TOKEN = _encode({ userId: DataIds.TEST_EMAIL_USER_ID, roles: [] });
var _TEST_PHONE_TOKEN = _encode({ userId: DataIds.TEST_PHONE_USER_ID, roles: [] });
var _TEST_FACEBOOK_TOKEN = _encode({ userId: DataIds.TEST_FACEBOOK_USER_ID, roles: [] });
var _TEST_GOOGLE_TOKEN = _encode({ userId: DataIds.TEST_GOOGLE_USER_ID, roles: [] });

module.exports = {
    EMAIL_TOKEN: _EMAIL_TOKEN,
    PHONE_TOKEN: _PHONE_TOKEN,
    FACEBOOK_TOKEN: _FACEBOOK_TOKEN,
    GOOGLE_TOKEN: _GOOGLE_TOKEN,
    TEST_EMAIL_TOKEN: _TEST_EMAIL_TOKEN,
    TEST_PHONE_TOKEN: _TEST_PHONE_TOKEN,
    TEST_FACEBOOK_TOKEN: _TEST_FACEBOOK_TOKEN,
    TEST_GOOGLE_TOKEN: _TEST_GOOGLE_TOKEN,
    EMAIL_PROFILE_SYNC: {
        profile: DataIds.EMAIL_USER_ID,
        randomValue: 123
    },
    PHONE_PROFILE_SYNC: {
        profile: DataIds.PHONE_USER_ID,
        randomValue: 234
    },
    FACEBOOK_PROFILE_SYNC: {
        profile: DataIds.FACEBOOK_USER_ID,
        randomValue: 345
    },
    GOOGLE_PROFILE_SYNC: {
        profile: DataIds.GOOGLE_USER_ID,
        randomValue: 456
    },
    TEST_EMAIL_PROFILE_SYNC: {
        profile: DataIds.TEST_EMAIL_USER_ID,
        randomValue: 123
    },
    TEST_PHONE_PROFILE_SYNC: {
        profile: DataIds.TEST_PHONE_USER_ID,
        randomValue: 234
    },
    TEST_FACEBOOK_PROFILE_SYNC: {
        profile: DataIds.TEST_FACEBOOK_USER_ID,
        randomValue: 345
    },
    TEST_GOOGLE_PROFILE_SYNC: {
        profile: DataIds.TEST_GOOGLE_USER_ID,
        randomValue: 456
    },    EMAIL_USER: {
        email: 'email@ascendro.de',
        phone: null,
        provider: null,
        providerToken: null,
        password: _encodePass(_md5('email'), 'email'),
        code: null,
        userId: DataIds.EMAIL_USER_ID,
        userRoles: []
    }, 
    PHONE_USER: {
        email: null,
        phone: '1234567890',
        provider: null,
        providerToken: null,
        password: _encodePass(_md5('phone'), 'phone'),
        code: null,
        userId: DataIds.PHONE_USER_ID,
        userRoles: []
    }, 
    FACEBOOK_USER: {
        email: null,
        phone: null,
        provider: 'facebook',
        providerToken: 'EAAQmqsd7XOYBABV7rMqc0JZBa5P77Hkiz3nuXjjSWxvr0FLUh2Nklv8RQO91TZBd0OZCnOhJZBWvHU7oimIoxWgTd9ag49HYgBnAIONXLhyZCeDT21ZCZCAiwPSpJs3ZA4cJWE77ZAG9JWq9ZADknsW2UgXOJOJufUjcQZD',
        password: null,
        code: null,
        userId: DataIds.FACEBOOK_USER_ID,
        userRoles: []
    }, 
    GOOGLE_USER: {
        email: null,
        phone: null,
        provider: 'google',
        providerToken: 'ya29.Ci8rA8J9faHBxjW2VWk9RwiLquVEHyIyyRZ8MhQj-vRMNJhF47-707GMwIC0_bEdQQ',
        password: null,
        code: null,
        userId: DataIds.GOOGLE_USER_ID,
        userRoles: []
    },
    TEST_EMAIL_USER: {
        email: 'test@ascendro.de',
        phone: null,
        provider: null,
        providerToken: null,
        password: _encodePass(_md5('test'), 'test'),
        code: null,
        userId: DataIds.TEST_EMAIL_USER_ID,
        userRoles: []
    },
    TEST_PHONE_USER: {
        email: null,
        phone: '0987654321',
        provider: null,
        providerToken: null,
        password: null,
        code: null,
        userId: DataIds.TEST_PHONE_USER_ID,
        userRoles: []
    },
    TEST_FACEBOOK_USER: {
        email: null,
        phone: null,
        provider: 'facebook',
        providerToken: 'test_EAAQmqsd7XOYBABV7rMqc0JZBa5P77Hkiz3nuXjjSWxvr0FLUh2Nklv8RQO91TZBd0OZCnOhJZBWvHU7oimIoxWgTd9ag49HYgBnAIONXLhyZCeDT21ZCZCAiwPSpJs3ZA4cJWE77ZAG9JWq9ZADknsW2UgXOJOJufUjcQZD',
        password: null,
        code: null,
        userId: DataIds.TEST_FACEBOOK_USER_ID,
        userRoles: []
    },
    TEST_GOOGLE_USER: {
        email: null,
        phone: null,
        provider: 'google',
        providerToken: 'test_ya29.Ci8rA8J9faHBxjW2VWk9RwiLquVEHyIyyRZ8MhQj-vRMNJhF47-707GMwIC0_bEdQQ',
        password: null,
        code: null,
        userId: DataIds.TEST_GOOGLE_USER_ID,
        userRoles: []
    },
    TEST_USER_ROLES: {
        userId: DataIds.TEST_USER_ID,
        userRoles: ['role1', 'role2', 'role3', 'role4']
    },
    EMAIL_USER_TOKEN: {
        userId: DataIds.EMAIL_USER_ID,
        token: _EMAIL_TOKEN
    }, 
    TEST_PHONE_USER_TOKEN: {
        userId: DataIds.PHONE_USER_ID,
        token: _PHONE_TOKEN
    }, 

    loadAS: function (done) {
        var self = this;
        KeyvalueService.load()
            .remove(AerospikeUser, self.EMAIL_USER, 'email')
            .remove(AerospikeUser, self.PHONE_USER, 'phone')
            .remove(AerospikeUser, self.FACEBOOK_USER, 'facebook')
            .remove(AerospikeUser, self.GOOGLE_USER, 'google')
            .remove(AerospikeUser, self.TEST_EMAIL_USER, 'test/email')
            .remove(AerospikeUser, self.TEST_PHONE_USER, 'test/phone')
            .remove(AerospikeUser, self.TEST_FACEBOOK_USER, 'test/facebook')
            .remove(AerospikeUser, self.TEST_GOOGLE_USER, 'test/google')
            .remove(AerospikeUserToken, self.EMAIL_USER_TOKEN, 'email token')
            .remove(AerospikeUserToken, self.TEST_PHONE_USER_TOKEN, 'phone token')
            .remove(AerospikeProfileSync, self.EMAIL_PROFILE_SYNC, 'email')
            .remove(AerospikeProfileSync, self.PHONE_PROFILE_SYNC, 'phone')
            .remove(AerospikeProfileSync, self.FACEBOOK_PROFILE_SYNC, 'facebook')
            .remove(AerospikeProfileSync, self.GOOGLE_PROFILE_SYNC, 'google')
            .remove(AerospikeProfileSync, self.TEST_EMAIL_PROFILE_SYNC, 'test/email')
            .remove(AerospikeProfileSync, self.TEST_PHONE_PROFILE_SYNC, 'test/phone')
            .remove(AerospikeProfileSync, self.TEST_FACEBOOK_PROFILE_SYNC, 'test/facebook')
            .remove(AerospikeProfileSync, self.TEST_GOOGLE_PROFILE_SYNC, 'test/google')
            .create(AerospikeUser, self.EMAIL_USER, 'email')
            .create(AerospikeUser, self.PHONE_USER, 'phone')
            .create(AerospikeUser, self.FACEBOOK_USER, 'facebook')
            .create(AerospikeUser, self.GOOGLE_USER, 'google')
            .create(AerospikeUserToken, self.EMAIL_USER_TOKEN, 'email token')
            .create(AerospikeProfileSync, self.EMAIL_PROFILE_SYNC, 'email')
            .create(AerospikeProfileSync, self.PHONE_PROFILE_SYNC, 'phone')
            .create(AerospikeProfileSync, self.FACEBOOK_PROFILE_SYNC, 'facebook')
            .create(AerospikeProfileSync, self.GOOGLE_PROFILE_SYNC, 'google')
            .index(AerospikeProfileSync, { bin: 'randomValue', datatype: 'numeric' })
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },
};

function _encode(payload, options, callback) {
    var jwtOptions = {
        algorithm: 'RS256',
        issuer: 'F4M',
        expiresIn: 3600
    };
    if (options && options.expiresIn) {
        jwtOptions.expiresIn = options.expiresIn;
    }
    return jwt.sign(payload, fs.readFileSync(path.join(__dirname, '..', 'jwt-keys/privkey.pem'), 'utf8'), jwtOptions, callback);
}

function _encodePass(pass, salt) {
    return crypto.createHash('sha256').update(pass + salt.toString('utf8')).digest('hex');
}

function _md5(payload) {
    return md5(payload);
}
