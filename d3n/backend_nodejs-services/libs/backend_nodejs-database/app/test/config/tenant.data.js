var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('./../../../index.js').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Tenant = RdbmsService.Models.Tenant.Tenant;
var TenantContract = RdbmsService.Models.Tenant.TenantContract;

module.exports = {
    TENANT_1: {
        $id: DataIds.TENANT_1_ID,
        $name: 'First tenant',
        url: 'https://f4m.com',
        autoCommunity: 0,
        logoUrl: 'https://f4m.com/logo.png',
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow(),
        status: Tenant.constants().STATUS_ACTIVE,
        address: 'Circumvalatiunii, nr.11 A',
        city: 'Timisoara',
        country: 'Romania',
        vat: '21%',
        email: 'test@ascendro.de',
        phone: '1234567890',
        contactFirstName: 'First',
        contactLastName: 'Tenant',
        description: 'Tenant one description'
    },
    TENANT_2: {
        $id: DataIds.TENANT_2_ID,
        $name: 'Second Tenant',
        url: 'https://f4m.com',
        autoCommunity: 0,
        logoUrl: 'https://f4m.com/logo.png',
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow(),
        status: Tenant.constants().STATUS_ACTIVE,
        address: 'Circumvalatiunii, nr.11 A',
        city: 'Timisoara',
        country: 'Romania',
        vat: '21%',
        email: 'test@ascendro.de',
        phone: '1234567890',
        contactFirstName: 'Second',
        contactLastName: 'Tenant',
        description: 'Tenant two description'
    },

    TENANT_1_CONTRACT_1: {
        $id: DataIds.TENANT_CONTRACT_1_ID,
        tenantId: DataIds.TENANT_1_ID,
        name: "The inactive contract",
        type: "monthly",
        status: TenantContract.constants().STATUS_INACTIVE,
        action: TenantContract.constants().ACTION_NONE,
        content: "A description for this contract",
        startDate: DateUtils.isoPast(1000*3600*24*60),
        endDate: DateUtils.isoNow(1000*3600*24*30),
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow()
    },

    TENANT_1_CONTRACT_2: {
        $id: DataIds.TENANT_CONTRACT_2_ID,
        tenantId: DataIds.TENANT_1_ID,
        name: "Current active contract",
        type: "monthly",
        status: TenantContract.constants().STATUS_ACTIVE,
        action: TenantContract.constants().ACTION_RENEW,
        content: "The active contract",
        startDate: DateUtils.isoPast(1000*3600*24*15),
        endDate: DateUtils.isoFuture(1000*3600*24*15),
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow()
    },


    loadManyToMany: function (testSet) {
        return testSet
            .createSeries(TenantContract, [this.TENANT_1_CONTRACT_1, this.TENANT_1_CONTRACT_2])
    },


    load: function (testSet) {
        return testSet
            .createSeries(Tenant, [this.TENANT_1, this.TENANT_2])
    }
};