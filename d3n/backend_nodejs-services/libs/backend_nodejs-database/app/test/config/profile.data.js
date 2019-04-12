var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('./../../../index.js').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Profile = RdbmsService.Models.ProfileManager.Profile;
var ProfileEmail = RdbmsService.Models.ProfileManager.ProfileEmail;
var ProfilePhone = RdbmsService.Models.ProfileManager.ProfilePhone;
var ProfileHasApplication = RdbmsService.Models.ProfileManager.ProfileHasApplication;
var ProfileHasRole = RdbmsService.Models.ProfileManager.ProfileHasRole;
var ProfileHasGlobalRole = RdbmsService.Models.ProfileManager.ProfileHasGlobalRole;

module.exports = {
    PROFILE_1: {
        $userId: DataIds.LOCAL_USER_ID,
        $languageId: DataIds.LANGUAGE_EN_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
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
        $recommendedByProfileId: null
    },
    PROFILE_2: {
        $userId: DataIds.FACEBOOK_USER_ID,
        $languageId: DataIds.LANGUAGE_DE_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
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
        $recommendedByProfileId: DataIds.LOCAL_USER_ID
    },

    PROFILE_EMAIL_1: {
        $email: 'email1@ascendro.de',
        verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_NOT_VERIFIED,
        $profileId: DataIds.LOCAL_USER_ID
    },
    PROFILE_EMAIL_2: {
        $email: 'email2@ascendro.de',
        verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_VERIFIED,
        $profileId: DataIds.FACEBOOK_USER_ID
    },

    PROFILE_PHONE_1: {
        $phone: '1-123456789',
        verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_NOT_VERIFIED,
        $profileId: DataIds.LOCAL_USER_ID
    },
    PROFILE_PHONE_2: {
        $phone: '2-123456789',
        verificationStatus: ProfileEmail.constants().VERIFICATION_STATUS_VERIFIED,
        $profileId: DataIds.FACEBOOK_USER_ID
    },

    PROFILE_1_HAS_APPLICATION_1: {
        $profileId: DataIds.LOCAL_USER_ID,
        $applicationId: DataIds.APPLICATION_1_ID
    },
    PROFILE_2_HAS_APPLICATION_2: {
        $profileId: DataIds.FACEBOOK_USER_ID,
        $applicationId: DataIds.APPLICATION_2_ID
    },

    PROFILE_HAS_ROLE_1: {
        $id: DataIds.PROFILE_HAS_ROLE_1_ID,
        $profileId: DataIds.LOCAL_USER_ID,
        $tenantId: DataIds.TENANT_1_ID,
        role: ProfileHasRole.constants().ROLE_ADMIN,
        status: ProfileHasRole.constants().STATUS_APPROVED
    },
    PROFILE_HAS_ROLE_2: {
        $id: DataIds.PROFILE_HAS_ROLE_2_ID,
        $profileId: DataIds.FACEBOOK_USER_ID,
        $tenantId: DataIds.TENANT_2_ID,
        role: ProfileHasRole.constants().ROLE_COMMUNITY,
        status: ProfileHasRole.constants().STATUS_APPLIED
    },

    PROFILE_HAS_GLOBAL_ROLE_1: {
        $id: DataIds.PROFILE_HAS_ROLE_1_ID,
        $profileId: DataIds.LOCAL_USER_ID,
        role: ProfileHasGlobalRole.constants().ROLE_ANONYMOUS,
    },

    loadEntities: function (testSet) {
        return testSet
            .createSeries(Profile, [this.PROFILE_1, this.PROFILE_2])
            .createSeries(ProfileEmail, [this.PROFILE_EMAIL_1, this.PROFILE_EMAIL_2])
            .createSeries(ProfilePhone, [this.PROFILE_PHONE_1, this.PROFILE_PHONE_2])
    },

    loadManyToMany: function (testSet) {
        return testSet
            .createSeries(ProfileHasApplication, [this.PROFILE_1_HAS_APPLICATION_1, this.PROFILE_2_HAS_APPLICATION_2])
            .createSeries(ProfileHasRole, [this.PROFILE_HAS_ROLE_1, this.PROFILE_HAS_ROLE_2])
            .createSeries(ProfileHasGlobalRole, [this.PROFILE_HAS_GLOBAL_ROLE_1])
    }
};
