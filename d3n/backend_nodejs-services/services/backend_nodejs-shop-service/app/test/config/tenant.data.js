var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Tenant = RdbmsService.Models.Tenant.Tenant;

module.exports = {
    TENANT_1: {
        id: DataIds.TENANT_1_ID,
        name: 'Tenant 1',
        autoCommunity: 0,
        logoUrl: 'https://f4m.com/logo.png',
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow(),
        status: Tenant.constants().STATUS_ACTIVE,
        address: 'Circumvalatiunii, nr.11 A',
        city: 'Timisoara',
        country: 'Romania',
        vat: '21%',
        url: 'https://f4m.com',
        email: 'test@ascendro.de',
        phone: '1234567890',
        contactPerson: 'contact person 1',
        description: 'Tenant one description'
    },

    clean: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(Tenant, [self.TENANT_1])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    load: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(Tenant, [self.TENANT_1])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};
