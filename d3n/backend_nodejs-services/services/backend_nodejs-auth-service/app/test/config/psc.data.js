var fs = require('fs');
var DataIds = require('./../config/_id.data.js');
var JwtUtils = require('./../../utils/jwtUtils.js');

var TEST_USER_ID = '1111111-1111-1111-1111-111111111111';
var MERGE_USER_ID = '2222222-2222-2222-2222-222222222222';
var DEVICE_UUID = '3333333-3333-3333-3333-333333333333';

module.exports = {
    TEST_USER_ID: TEST_USER_ID,
    MERGE_USER_ID: MERGE_USER_ID,
    DEVICE_UUID: DEVICE_UUID,
    TEST_TOKEN: JwtUtils.encode({ userId: TEST_USER_ID, roles: [] }),
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
    },
    EMAIL_TOKEN: JwtUtils.encode({ userId: DataIds.EMAIL_USER_ID, roles: [] }),
    PHONE_TOKEN: JwtUtils.encode({ userId: DataIds.PHONE_USER_ID, roles: [] }),
    FACEBOOK_TOKEN: JwtUtils.encode({ userId: DataIds.FACEBOOK_USER_ID, roles: [] }),
    GOOGLE_TOKEN: JwtUtils.encode({ userId: DataIds.GOOGLE_USER_ID, roles: [] }),
    
    TEST_EMAIL_TOKEN: JwtUtils.encode({ userId: DataIds.TEST_EMAIL_USER_ID, roles: [] }),
    TEST_PHONE_TOKEN: JwtUtils.encode({ userId: DataIds.TEST_PHONE_USER_ID, roles: [] }),
    TEST_FACEBOOK_TOKEN: JwtUtils.encode({ userId: DataIds.TEST_FACEBOOK_USER_ID, roles: [] }),
    TEST_GOOGLE_TOKEN: JwtUtils.encode({ userId: DataIds.TEST_GOOGLE_USER_ID, roles: [] }),
    
    EMAIL_CLIENT_INFO: {
        profile: {
            userId: DataIds.EMAIL_USER_ID
        },
        
        appConfig: {
            tenantId: '1'
        },
        
        ip: null
    },
    PHONE_CLIENT_INFO: {
        profile: {
            userId: DataIds.PHONE_USER_ID
        },
        
        appConfig: {
            tenantId: '1'
        },
        
        ip: null
    },
    
    TEST_EMAIL_CLIENT_INFO: {
        profile: {
            userId: DataIds.TEST_EMAIL_USER_ID
        },
        
        appConfig: {
            tenantId: '1',
            appId: '1'
        },
        
        ip: null
    },
    
    TEST_EMAIL_NEW_CLIENT_INFO: {
        profile: {
            userId: DataIds.TEST_EMAIL_NEW_USER_ID
        },

        appConfig: {
            tenantId: '1'
        },

        ip: null
    },

    TEST_PHONE_CLIENT_INFO: {
        profile: {
            userId: DataIds.TEST_PHONE_USER_ID
        },
        
        appConfig: {
            tenantId: '1',
            appId: '1'
        },
        
        ip: null
    },
    
    TEST_PHONE_NEW_CLIENT_INFO: {
        profile: {
            userId: DataIds.TEST_PHONE_NEW_USER_ID
        },

        appConfig: {
            tenantId: '1'
        },

        ip: null
    },

    TEST_FACEBOOK_CLIENT_INFO: {
        profile: {
            userId: DataIds.TEST_FACEBOOK_USER_ID
        },
        
        appConfig: {
            tenantId: '1'
        },
        
        ip: null
    },
    
    TEST_GOOGLE_CLIENT_INFO: {
        profile: {
            userId: DataIds.TEST_GOOGLE_USER_ID
        },
        
        appConfig: {
            tenantId: '1'
        },
        
        ip: null
    },
    
    
    EMAIL_USER: {
        email: 'email@ascendro.de',
        phone: null,
        provider: null,
        providerToken: null,
        password: JwtUtils.encodePassWithFixedSalt('email'),
        code: null,
        userId: DataIds.EMAIL_USER_ID,
        userRoles: []
    }, 
    PHONE_USER: {
        email: null,
        phone: '1234567890',
        provider: null,
        providerToken: null,
        password: JwtUtils.encodePassWithFixedSalt('phone'),
        code: null,
        userId: DataIds.PHONE_USER_ID,
        userRoles: []
    }, 
    FACEBOOK_USER: {
        email: null,
        phone: null,
        provider: 'facebook',
        providerToken: 'EAAUUbL3PNhwBADs83OlZCsPqnGmcGFaLHILitd3R6kfUvSszwYT9V2ddaSZCd0iK4oKfyTkQ0Tdv7QZBW8hhjJ8b2kg8jH0MFxITXKrQoQLGwS2QfMPKzzNNdx5jauVEpqxJphiVNfS4spwASeT5cJraRAdTwE55lpsUDOzJAZDZD',
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
        password: JwtUtils.encodePassWithFixedSalt('emailtest'),
        code: null,
        userId: DataIds.TEST_EMAIL_USER_ID,
        userRoles: []
    },
    TEST_PHONE_USER: {
        email: null,
        phone: '0987654321',
        provider: null,
        providerToken: null,
        password: JwtUtils.encodePassWithFixedSalt('phonetest'),
        code: null,
        userId: DataIds.TEST_PHONE_USER_ID,
        userRoles: []
    },
    TEST_FACEBOOK_USER: {
        email: null,
        phone: null,
        provider: 'facebook',
        providerToken: 'EAAUUbL3PNhwBADs83OlZCsPqnGmcGFaLHILitd3R6kfUvSszwYT9V2ddaSZCd0iK4oKfyTkQ0Tdv7QZBW8hhjJ8b2kg8jH0MFxITXKrQoQLGwS2QfMPKzzNNdx5jauVEpqxJphiVNfS4spwASeT5cJraRAdTwE55lpsUDOzJAZDZD',
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
    TEST_EMAIL_NEW_USER: {
        email: 'new-test-user@ascendro.de',
        phone: null,
        provider: null,
        providerToken: null,
        password: JwtUtils.encodePassWithFixedSalt('emailtest'),
        code: null,
        userId: DataIds.TEST_EMAIL_NEW_USER_ID,
        userRoles: []
    },
    IMPERSONATE_NO_ADMIN_USER: {
        email: 'impersonate-no-admin@ascendro.de',
        phone: null,
        provider: null,
        password: JwtUtils.encodePassWithFixedSalt('emailtest'),
        code: null, 
        userId: DataIds.IMPERSONATE_NO_ADMIN_USER,
        userRoles: ['REGISTERED', 'TENANT_' + DataIds.TENANT_TEST_ID + '_ADMIN']
    },
    IMPERSONATE_ADMIN_USER: {
        email: 'impersonate-admin@ascendro.de',
        phone: null,
        provider: null,
        password: JwtUtils.encodePassWithFixedSalt('emailtest'),
        code: null, 
        userId: DataIds.IMPERSONATE_ADMIN_USER,
        userRoles: ['REGISTERED', 'TENANT_' + DataIds.TENANT_1_ID + '_ADMIN']
    },
    TEST_PHONE_NEW_USER: {
        email: null,
        phone: '12345-67890',
        provider: null,
        providerToken: null,
        password: JwtUtils.encodePassWithFixedSalt('phonetest'),
        code: null,
        userId: DataIds.TEST_PHONE_NEW_USER_ID,
        userRoles: []
    },
    TEST_USER_ROLES: {
        userId: DataIds.TEST_USER_ID,
        userRoles: ['role1', 'role2', 'role3', 'role4']
    },

    RECOVER_PASSWORD: JwtUtils.encodePassWithFixedSalt(process.env.TEST_RECOVER_PASSWORD),
};