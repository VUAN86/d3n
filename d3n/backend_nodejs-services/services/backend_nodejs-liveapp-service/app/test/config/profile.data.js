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
var ProfileHasTenant = RdbmsService.Models.ProfileManager.ProfileHasTenant;
var ProfileHasRole = RdbmsService.Models.ProfileManager.ProfileHasRole;
var ProfileHasStats = RdbmsService.Models.ProfileManager.ProfileHasStats;
var ProfileHasGlobalRole = RdbmsService.Models.ProfileManager.ProfileHasGlobalRole;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeProfileSync = KeyvalueService.Models.AerospikeProfileSync;

module.exports = {
    LOCAL_TOKEN: _encode({ userId: DataIds.LOCAL_USER_ID, roles: [] }),
    TEST_TOKEN: _encode({ userId: DataIds.TEST_USER_ID, roles: [] }),

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
        phones: [{ phone: '1-123456789', verificationStatus: 'notVerified' }],
        roles: [ProfileHasRole.constants().ROLE_ADMIN, ProfileHasRole.constants().ROLE_COMMUNITY, ProfileHasRole.constants().ROLE_INTERNAL, ProfileHasGlobalRole.constants().ROLE_REGISTERED],
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
        phones: [{ phone: '2-123456789', verificationStatus: 'verified' }],
        roles: [ProfileHasRole.constants().ROLE_ADMIN],
    },
    PROFILE_3: {
        userId: DataIds.GOOGLE_USER_ID,
        languageId: DataIds.LANGUAGE_DE_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        autoShare: 0,
        firstName: 'Google First Name',
        lastName: 'Google Last Name',
        nickname: 'Google Nick Name',
        birthDate: DateUtils.isoPast(),
        sex: Profile.constants().SEX_MALE,
        addressStreet: 'Facebook Street',
        addressStreetNumber: '456',
        addressCity: 'Timisoara',
        addressPostalCode: 'FB123456',
        addressCountry: 'Romania',
        $recommendedByProfileId: DataIds.LOCAL_USER_ID,
        roles: [ProfileHasRole.constants().ROLE_COMMUNITY],
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
        recommendedByProfileId: DataIds.FACEBOOK_USER_ID,
        roles: [ProfileHasRole.constants().ROLE_COMMUNITY],
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

    PROFILE_1_HAS_TENANT_1: {
        profileId: DataIds.LOCAL_USER_ID,
        tenantId: DataIds.TENANT_1_ID
    },
    PROFILE_2_HAS_TENANT_2: {
        profileId: DataIds.FACEBOOK_USER_ID,
        tenantId: DataIds.TENANT_2_ID
    },

    PROFILE_HAS_ROLE_1: {
        profileId: DataIds.LOCAL_USER_ID,
        tenantId: DataIds.TENANT_1_ID,
        role: ProfileHasRole.constants().ROLE_INTERNAL,
        status: ProfileHasRole.constants().STATUS_APPROVED
    },
    PROFILE_HAS_ROLE_2: {
        profileId: DataIds.LOCAL_USER_ID,
        tenantId: DataIds.TENANT_1_ID,
        role: ProfileHasRole.constants().ROLE_ADMIN,
        status: ProfileHasRole.constants().STATUS_APPROVED
    },
    PROFILE_HAS_ROLE_3: {
        profileId: DataIds.GOOGLE_USER_ID,
        tenantId: DataIds.TENANT_1_ID,
        role: ProfileHasRole.constants().ROLE_ADMIN,
        status: ProfileHasRole.constants().STATUS_APPROVED
    },
    PROFILE_HAS_ROLE_4: {
        profileId: DataIds.FACEBOOK_USER_ID,
        tenantId: DataIds.TENANT_1_ID,
        role: ProfileHasRole.constants().ROLE_COMMUNITY,
        status: ProfileHasRole.constants().STATUS_APPLIED
    },

    PROFILE_HAS_GLOBAL_ROLE_1: {
        profileId: DataIds.LOCAL_USER_ID,
        role: ProfileHasGlobalRole.constants().ROLE_REGISTERED,
    },

    LOCAL_PROFILE_SYNC: {
        profile: DataIds.LOCAL_USER_ID,
        randomValue: 123
    },
    FACEBOOK_PROFILE_SYNC: {
        profile: DataIds.FACEBOOK_USER_ID,
        randomValue: 345
    },
    GOOGLE_PROFILE_SYNC: {
        profile: DataIds.GOOGLE_USER_ID,
        randomValue: 456
    },

    PROFILE_HAS_STATS_1: {
        profileId: DataIds.GOOGLE_USER_ID,
        tenantId: DataIds.TENANT_1_ID,
        stat_appsInstalled: 1,
        stat_friendsInvited: 2
    },
    PROFILE_HAS_STATS_2: {
        profileId: DataIds.GOOGLE_USER_ID,
        tenantId: DataIds.TENANT_2_ID,
        stat_appsInstalled: 3,
        stat_friendsInvited: 4
    },

    loadEntities: function (done) {
        RdbmsService.load()
            .createSeries(Profile, [this.PROFILE_1, this.PROFILE_2, this.PROFILE_3])
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
            .createSeries(ProfileHasTenant, [this.PROFILE_1_HAS_TENANT_1, this.PROFILE_2_HAS_TENANT_2])
            .createSeries(ProfileHasRole, [this.PROFILE_HAS_ROLE_1, this.PROFILE_HAS_ROLE_2, this.PROFILE_HAS_ROLE_3, this.PROFILE_HAS_ROLE_4])
            .createSeries(ProfileHasStats, [this.PROFILE_HAS_STATS_1, this.PROFILE_HAS_STATS_2])
            .createSeries(ProfileHasGlobalRole, [this.PROFILE_HAS_GLOBAL_ROLE_1])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadAerospike: function (done) {
        var self = this;
        KeyvalueService.load()
            .create(AerospikeProfileSync, self.LOCAL_PROFILE_SYNC, 'email')
            .index(AerospikeProfileSync, { bin: 'randomValue', datatype: 'numeric' })
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
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
