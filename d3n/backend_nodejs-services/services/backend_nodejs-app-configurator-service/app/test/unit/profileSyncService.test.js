var should = require('should');
var Config = require('./../../config/config.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var profileSyncService = require('./../../services/profileSyncService.js');
var Data = require('./../config/profile.data.js');
var DataIds = require('./../config/_id.data.js');

describe('Profile sync service - admin audit log generation', function () {
    this.timeout(20000);
    
    it('SUCCESS: admin audit log create', function (done) {
        var aerospikeProfile = {
            "userId": DataIds.LOCAL_USER_ID,
            "roles": "TENANT_1_ADMIN",
            "emails": [ { "email": "test@ascendro.de"}, { "email": Data.PROFILE_1.emails[0].email}],
            "person": {
                "firstName": "New First Name",
                "lastName": "New Last Name"
            }
        }
        var result = [{
                userId: Data.PROFILE_1.userId,
                email: "test@ascendro.de",
                items: {
                    "firstName": {
                        "old": Data.PROFILE_1.firstName,
                        "new": aerospikeProfile.person.firstName
                    },
                    "lastName": {
                        "old": Data.PROFILE_1.lastName,
                        "new": aerospikeProfile.person.lastName
                    }
                }
            }, {
                userId: Data.PROFILE_1.userId,
                email: Data.PROFILE_1.emails[0].email,
                items: {
                    "firstName": {
                        "old": Data.PROFILE_1.firstName,
                        "new": aerospikeProfile.person.firstName
                    },
                    "lastName": {
                        "old": Data.PROFILE_1.lastName,
                        "new": aerospikeProfile.person.lastName
                    }
                }
            }];

        profileSyncService.adminAuditLogGenerate(aerospikeProfile, function(err, resp) {
            should(err).be.false();
            ShouldHelper.deepEqualList(result, resp, ['id', 'createDate']);
            done();
        });
    });
});