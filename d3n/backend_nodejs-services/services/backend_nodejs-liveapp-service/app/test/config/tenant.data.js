var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Tenant = RdbmsService.Models.Tenant.Tenant;
var TenantAuditLog = RdbmsService.Models.Tenant.TenantAuditLog;
var TenantContract = RdbmsService.Models.Tenant.TenantContract;
var TenantInvoice = RdbmsService.Models.Tenant.TenantInvoice;
var TenantContractAuditLog = RdbmsService.Models.Tenant.TenantContractAuditLog;
var TenantAdminAuditLog = RdbmsService.Models.Tenant.TenantAdminAuditLog;

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
        contactFirstName: 'First',
        contactLastName: 'Tenant',
        description: 'Tenant one description',
        currency: 'USD'
    },
    TENANT_2: {
        id: DataIds.TENANT_2_ID,
        name: 'Tenant 2',
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
        contactFirstName: 'Second',
        contactLastName: 'Tenant',
        description: 'Tenant two description',
        currency: 'USD'
    },
    TENANT_TEST: {
        id: DataIds.TENANT_TEST_ID,
        name: 'Tenant Test',
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
        contactFirstName: 'Test',
        contactLastName: 'Tenant',
        description: 'Tenant test description',
        currency: 'USD'
    },
    TENANT_1_CONTRACT_1: {
        id: DataIds.TENANT_CONTRACT_1_ID,
        tenantId: DataIds.TENANT_1_ID,
        type: 'Monthly Contract',
        name: 'First Contract',
        status: 'active',
        action: 'none',
        content: 'Monthly contract from the client',
        startDate: DateUtils.isoPast(1000 * 3600 * 24 * 15),
        endDate: DateUtils.isoFuture(1000 * 3600 * 34 * 15),
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow()
    },
    TENANT_1_CONTRACT_2: {
        id: DataIds.TENANT_CONTRACT_2_ID,
        tenantId: DataIds.TENANT_1_ID,
        type: 'Monthly Contract',
        name: 'First Contract',
        status: 'inactive',
        action: 'none',
        content: 'Monthly contract from the client',
        startDate: DateUtils.isoPast(1000 * 3600 * 24 * 45),
        endDate: DateUtils.isoPast(1000 * 3600 * 34 * 15),
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow()
    },
    TENANT_1_CONTRACT_TEST: {
        id: DataIds.TENANT_CONTRACT_TEST_ID,
        tenantId: DataIds.TENANT_1_ID,
        type: 'Monthly Contract',
        name: 'First Contract',
        status: 'inactive',
        action: 'none',
        content: 'Monthly contract from the client',
        startDate: DateUtils.isoPast(1000 * 3600 * 24 * 45),
        endDate: DateUtils.isoPast(1000 * 3600 * 34 * 15),
        createDate: DateUtils.isoNow(),
        updateDate: DateUtils.isoNow()
    },

    TENANT_1_INVOICE_1: {
        id: DataIds.TENANT_INVOICE_1_ID,
        tenantId: DataIds.TENANT_1_ID,
        number: 'invoice001',
        url: 'http://domain.com/invoice001',
        type: TenantInvoice.constants().TYPE_CREDIT,
        date: DateUtils.isoNow(),
        paymentDate: DateUtils.isoNow(),
        invoiceDetails: [
            { type: 'x', amount: 1.1 },
            { type: 'y', amount: 2.1 }
        ],
        totalNet: 1.23,
        status: 'paid',
        totalVat: 44.55,
        vatAppliedPercentage: 19,
        totalGross: 1234.34
    },
    TENANT_1_INVOICE_2: {
        id: DataIds.TENANT_INVOICE_2_ID,
        tenantId: DataIds.TENANT_1_ID,
        number: 'invoice002',
        url: 'http://domain.com/invoice002',
        type: TenantInvoice.constants().TYPE_DEBIT,
        date: DateUtils.isoNow(),
        paymentDate: DateUtils.isoNow(),
        invoiceDetails: [
            { type: 'x', amount: 1.1 },
            { type: 'y', amount: 2.1 }
        ],
        totalNet: 1.23,
        status: 'paid',
        totalVat: 44.55,
        vatAppliedPercentage: 19,
        totalGross: 1234.34
    },
    TENANT_1_INVOICE_TEST: {
        id: DataIds.TENANT_INVOICE_TEST_ID,
        tenantId: DataIds.TENANT_1_ID,
        number: 'invoiceTest',
        url: 'http://domain.com/invoiceTest',
        type: TenantInvoice.constants().TYPE_DEBIT,
        date: DateUtils.isoNow(),
        paymentDate: DateUtils.isoNow(),
        invoiceDetails: [
            {type: 'x', amount: 1.1},
            {type: 'y', amount: 2.1}
        ],
        totalNet: 1.23,
        status: 'paid',
        totalVat: 44.55,
        vatAppliedPercentage: 19,
        totalGross: 1234.34
    },

    TENANT_1_INVOICE_END_CUSTOMER_TEST: {
        id: DataIds.TENANT_INVOICE_END_CUSTOMER_TEST_ID,
        tenantId: DataIds.TENANT_1_ID,
        profileId: DataIds.LOCAL_USER_ID,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoNow(),
        invoiceSeriesPrefix: 'prefixTest',
        invoiceSeriesNumber: '444',
        jackpotUnitsNumber: 1,
        jackpotNetAmount: 1.1,
        jackpotVatAmount: 1.2,
        jackpotTotalAmount: 1.3,
        sportBettingUnitsNumber: 2,
        sportBettingNetAmount: 2.1,
        sportBettingVatAmount: 2.2,
        sportBettingTotalAmount: 2.3,
        tournamentUnitsNumber: 3,
        tournamentNetAmount: 3.1,
        tournamentVatAmount: 3.2,
        tournamentTotalAmount: 3.3,
        duelUnitsNumber: 4,
        duelNetAmount: 4.1,
        duelVatAmount: 4.2,
        duelTotalAmount: 4.3,
        creditShopUnitsNumber: 5,
        creditShopNetAmount: 5.1,
        creditShopVatAmount: 5.2,
        creditShopTotalAmount: 5.3,
        premiumGamesUnitsNumber: 6,
        premiumGamesNetAmount: 6.1,
        premiumGamesVatAmount: 6.2,
        premiumGamesTotalAmount: 6.3,
        totalInvoicedAmount: 9.1,
        totalVatPercent: 9.2,
        totalVatAmount: 9.3,
        totalInvoicedAmountWithVat: 9.4,
        cdnPdfUrl: 'http://domain.com/invoiceTest',
        totalTransactionsCount: 100,
        createdAt: DateUtils.isoNow()
    },

    TENANT_1_AUDIT_LOG_1: {
        id: DataIds.TENANT_AUDIT_LOG_1_ID,
        tenantId: DataIds.TENANT_1_ID,
        type: "dataChange",
        items: {"name": "changed"},
        creatorProfileId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
    },

    TENANT_1_AUDIT_LOG_2: {
        id: DataIds.TENANT_AUDIT_LOG_2_ID,
        tenantId: DataIds.TENANT_1_ID,
        type: "dataChange",
        items: {"description": "second"},
        creatorProfileId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
    },

    CONTRACT_1_AUDIT_LOG_1: {
        id: DataIds.TENANT_CONTRACT_AUDIT_LOG_1_ID,
        contractId: DataIds.TENANT_CONTRACT_1_ID,
        type: 'none',
        items: { "description": "changed"},
        creatorProfileId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
    },

    CONTRACT_1_AUDIT_LOG_2: {
        id: DataIds.TENANT_CONTRACT_AUDIT_LOG_2_ID,
        contractId: DataIds.TENANT_CONTRACT_1_ID,
        type: 'holdRequest',
        items: { "description": "changed"},
        creatorProfileId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
    },

    TENANT_ADMIN_1_AUDIT_LOG_1: {
        id: DataIds.TENANT_ADMIN_AUDIT_LOG_1_ID,
        email: 'test@ascendro.de',
        userId: DataIds.LOCAL_USER_ID,
        items: { 'firstName': { 'new': 'Upgraded', 'old': 'Old Name'}},
        createDate: DateUtils.isoNow()
    },

    TENANT_ADMIN_1_AUDIT_LOG_2: {
        id: DataIds.TENANT_ADMIN_AUDIT_LOG_2_ID,
        email: 'test@ascendro.de',
        userId: DataIds.LOCAL_USER_ID,
        items: { 'lastName': { 'new': 'Upgraded', 'old': 'Old Name'}},
        createDate: DateUtils.isoNow()
    },

    TENANT_CONFIG_1: {
		"tenantId": DataIds.TENANT_1_ID.toString(),
		"apiId": "02B3E684-78A8-42F3-8EC8-8FE245FC1178",
		"mainCurrency" : "EUR",
		"exchangeRates": [
			{
				"fromCurrency" : "EUR",
				"toCurrency" : "CREDIT",
				"fromAmount" : "5",
				"toAmount" : "10.00"
			},
			{
				"fromCurrency" : "EUR",
				"toCurrency" : "CREDIT",
				"fromAmount" : "10",
				"toAmount" : "30.00"
			},
			{
				"fromCurrency" : "EUR",
				"toCurrency" : "CREDIT",
				"fromAmount" : "20",
				"toAmount" : "100.00"
			},
			{
				"fromCurrency" : "EUR",
				"toCurrency" : "BONUS",
				"fromAmount" : "5.00",
				"toAmount" : "100"
			}
		]
	},
    VAT_1: {
        country: 'RO',
        percent: 20,
        startDate: '2017-08-01T00:00:00Z'
    },
    
    VAT_2: {
        country: 'RO',
        percent: 19,
        startDate: '2018-01-01T00:00:00Z'
    },
    
    
    cleanClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(TenantContractAuditLog, [self.CONTRACT_1_AUDIT_LOG_1, self.CONTRACT_1_AUDIT_LOG_2])
            .removeSeries(TenantContract, [self.TENANT_1_CONTRACT_1, self.TENANT_1_CONTRACT_2])
            .removeSeries(TenantInvoice, [self.TENANT_1_INVOICE_1, self.TENANT_1_INVOICE_2])
            .removeSeries(TenantAuditLog, [self.TENANT_1_AUDIT_LOG_1, self.TENANT_1_AUDIT_LOG_2])
            .removeSeries(TenantAdminAuditLog, [self.TENANT_ADMIN_1_AUDIT_LOG_1, self.TENANT_ADMIN_1_AUDIT_LOG_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(TenantInvoice, [self.TENANT_1_INVOICE_1, self.TENANT_1_INVOICE_2])
            .createSeries(TenantContract, [self.TENANT_1_CONTRACT_1, self.TENANT_1_CONTRACT_2])
            .createSeries(TenantAuditLog, [self.TENANT_1_AUDIT_LOG_1, self.TENANT_1_AUDIT_LOG_2])
            .createSeries(TenantAdminAuditLog, [self.TENANT_ADMIN_1_AUDIT_LOG_1, self.TENANT_ADMIN_1_AUDIT_LOG_2])
            .createSeries(TenantContractAuditLog, [self.CONTRACT_1_AUDIT_LOG_1, self.CONTRACT_1_AUDIT_LOG_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    clean: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(Tenant, [self.TENANT_1, self.TENANT_2, self.TENANT_TEST])
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
            .createSeries(Tenant, [self.TENANT_1, self.TENANT_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};
