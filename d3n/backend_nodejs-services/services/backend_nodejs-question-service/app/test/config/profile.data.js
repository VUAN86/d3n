var fs = require('fs');
var path = require('path');
var jwt = require('jsonwebtoken');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Profile = RdbmsService.Models.ProfileManager.Profile;
var ProfileEmail = RdbmsService.Models.ProfileManager.ProfileEmail;
var ProfilePhone = RdbmsService.Models.ProfileManager.ProfilePhone;
var ProfileHasApplication = RdbmsService.Models.ProfileManager.ProfileHasApplication;
var ProfileHasRole = RdbmsService.Models.ProfileManager.ProfileHasRole;
var ProfileHasGlobalRole = RdbmsService.Models.ProfileManager.ProfileHasGlobalRole;

module.exports = {
    LOCAL_TOKEN: _encode({ userId: DataIds.LOCAL_USER_ID, roles: [] }),
    TEST_TOKEN: _encode({ userId: DataIds.TEST_USER_ID, roles: [] }),
    TOKEN_ROLE_COMMUNITY: _encode({ userId: DataIds.TEST_USER_ID, roles: ['ROLE_COMMUNITY'] }),
    TOKEN_ROLE_INTERNAL: _encode({ userId: DataIds.TEST_USER_ID, roles: ['ROLE_INTERNAL'] }),
    TOKEN_ROLE_EXTERNAL: _encode({ userId: DataIds.TEST_USER_ID, roles: ['ROLE_EXTERNAL'] }),
    
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
    },
    
    PROFILE_1: {
        userId: DataIds.LOCAL_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Local First Name',
        lastName: 'Local Last Name',
        nickname: 'Local Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [{ email: 'email1@ascendro.de', verificationStatus: 'notVerified' }],
        phones: [{ phone: '1-123456789', verificationStatus: 'notVerified' }]
    },
    PROFILE_2: {
        userId: DataIds.FACEBOOK_USER_ID,
        languageId: DataIds.LANGUAGE_DE_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        autoShare: 0,
        firstName: 'Facebook First Name',
        lastName: 'Facebook Last Name',
        nickname: 'Facebook Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_FEMALE,
        addressStreet: 'Facebook Street',
        addressStreetNumber: '456',
        addressCity: 'Timisoara',
        addressPostalCode: 'FB123456',
        addressCountry: 'Romania',
        recommendedByProfileId: DataIds.LOCAL_USER_ID,
        applicationsIds: [DataIds.APPLICATION_2_ID],
        emails: [{ email: 'email2@ascendro.de', verificationStatus: 'verified' }],
        phones: [{ phone: '2-123456789', verificationStatus: 'verified' }]
    },
    PROFILE_WF_1: {
        userId: DataIds.WF_1_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Workflow First Name',
        lastName: 'Workflow Last Name',
        nickname: 'Workflow Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [],
        phones: []
    },
    PROFILE_WF_2: {
        userId: DataIds.WF_2_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Workflow First Name',
        lastName: 'Workflow Last Name',
        nickname: 'Workflow Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [],
        phones: []
    },
    PROFILE_WF_3: {
        userId: DataIds.WF_3_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Workflow First Name',
        lastName: 'Workflow Last Name',
        nickname: 'Workflow Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [],
        phones: []
    },
    PROFILE_WF_4: {
        userId: DataIds.WF_4_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Workflow First Name',
        lastName: 'Workflow Last Name',
        nickname: 'Workflow Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [],
        phones: []
    },
    PROFILE_WF_5: {
        userId: DataIds.WF_5_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Workflow First Name',
        lastName: 'Workflow Last Name',
        nickname: 'Workflow Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [],
        phones: []
    },
    PROFILE_WF_6: {
        userId: DataIds.WF_6_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Workflow First Name',
        lastName: 'Workflow Last Name',
        nickname: 'Workflow Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [],
        phones: []
    },
    PROFILE_WF_7: {
        userId: DataIds.WF_7_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Workflow First Name',
        lastName: 'Workflow Last Name',
        nickname: 'Workflow Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [],
        phones: []
    },
    PROFILE_WF_8: {
        userId: DataIds.WF_8_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Workflow First Name',
        lastName: 'Workflow Last Name',
        nickname: 'Workflow Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [],
        phones: []
    },
    PROFILE_WF_9: {
        userId: DataIds.WF_9_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Workflow First Name',
        lastName: 'Workflow Last Name',
        nickname: 'Workflow Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [],
        phones: []
    },
    PROFILE_WF_10: {
        userId: DataIds.WF_10_USER_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        autoShare: 1,
        firstName: 'Workflow First Name',
        lastName: 'Workflow Last Name',
        nickname: 'Workflow Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Local Street',
        addressStreetNumber: '123',
        addressCity: 'Iasi',
        addressPostalCode: 'LOC123456',
        addressCountry: 'Romania',
        recommendedByProfileId: null,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        emails: [],
        phones: []
    },
    PROFILE_TEST: {
        userId: DataIds.TEST_USER_ID,
        languageId: DataIds.LANGUAGE_DE_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        autoShare: 0,
        firstName: 'Test First Name',
        lastName: 'Test Last Name',
        nickname: 'Test Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_FEMALE,
        addressStreet: 'Test Street',
        addressStreetNumber: '456',
        addressCity: 'Timisoara',
        addressPostalCode: 'FB123456',
        addressCountry: 'Romania',
        recommendedByProfileId: DataIds.FACEBOOK_USER_ID
    },

    PROFILE_EMAIL_1: {
        email: 'email1@ascendro.de',
        verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_NOT_VERIFIED,
        profileId: DataIds.LOCAL_USER_ID
    },
    PROFILE_EMAIL_2: {
        email: 'email2@ascendro.de',
        verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_VERIFIED,
        profileId: DataIds.FACEBOOK_USER_ID
    },

    PROFILE_PHONE_1: {
        phone: '1-123456789',
        verificationStatus: ProfilePhone.constants().VERIFICATION_STATUS_NOT_VERIFIED,
        profileId: DataIds.LOCAL_USER_ID
    },
    PROFILE_PHONE_2: {
        phone: '2-123456789',
        verificationStatus: ProfilePhone.constants().VERIFICATION_STATUS_VERIFIED,
        profileId: DataIds.FACEBOOK_USER_ID
    },

    PROFILE_1_HAS_APPLICATION_1: {
        profileId: DataIds.LOCAL_USER_ID,
        applicationId: DataIds.APPLICATION_1_ID
    },
    PROFILE_2_HAS_APPLICATION_2: {
        profileId: DataIds.FACEBOOK_USER_ID,
        applicationId: DataIds.APPLICATION_2_ID
    },

    PROFILE_HAS_ROLE_1: {
        id: DataIds.PROFILE_HAS_ROLE_1_ID,
        profileId: DataIds.LOCAL_USER_ID,
        tenantId: DataIds.TENANT_1_ID,
        role: 'COMMUNITY',
        status: ProfileHasRole.constants().STATUS_APPLIED
    },
    PROFILE_HAS_ROLE_2: {
        id: DataIds.PROFILE_HAS_ROLE_2_ID,
        profileId: DataIds.FACEBOOK_USER_ID,
        tenantId: DataIds.TENANT_2_ID,
        role: 'INTERNAL',
        status: ProfileHasRole.constants().STATUS_APPROVED
    },

    PROFILE_HAS_GLOBAL_ROLE_1: {
        profileId: DataIds.LOCAL_USER_ID,
        role: 'REGISTERED',
    },

    loadEntities: function (done) {
        RdbmsService.load()
            .createSeries(Profile, [this.PROFILE_1, this.PROFILE_2, this.PROFILE_WF_1, this.PROFILE_WF_2, this.PROFILE_WF_3, this.PROFILE_WF_4, this.PROFILE_WF_5, this.PROFILE_WF_6, this.PROFILE_WF_7, this.PROFILE_WF_8, this.PROFILE_WF_9, this.PROFILE_WF_10])
            .createSeries(ProfileEmail, [this.PROFILE_EMAIL_1, this.PROFILE_EMAIL_2])
            .createSeries(ProfilePhone, [this.PROFILE_PHONE_1, this.PROFILE_PHONE_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadManyToMany: function (done) {
        RdbmsService.load()
            .createSeries(ProfileHasApplication, [this.PROFILE_1_HAS_APPLICATION_1, this.PROFILE_2_HAS_APPLICATION_2])
            .createSeries(ProfileHasRole, [this.PROFILE_HAS_ROLE_1, this.PROFILE_HAS_ROLE_2])
            .createSeries(ProfileHasGlobalRole, [this.PROFILE_HAS_GLOBAL_ROLE_1])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },
    
    _encodeToken: function (payload) {
        return _encode(payload);
    }
};

function _encode(payload) {
    var jwtOptions = {
        algorithm: 'RS256',
        issuer: 'F4M',
        expiresIn: 3600
    };
    var privateKey = fs.readFileSync(path.join(__dirname, '/../../config/jwt-keys/privkey.pem'), 'utf8');
    return jwt.sign(payload, privateKey, jwtOptions);
}