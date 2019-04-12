var Config = {
    keyvalue: {
        hosts: process.env.AEROSPIKE_HOSTS || 'localhost:3000',
        namespace: process.env.AEROSPIKE_NAMESPACE || 'test',
        user: process.env.AEROSPIKE_USER || null,
        password: process.env.AEROSPIKE_PASSWORD || null,
        maxConnsPerNode: process.env.AEROSPIKE_MAX_CONNS_PER_NODE ? parseInt(process.env.AEROSPIKE_MAX_CONNS_PER_NODE) :  2500,
        metadata: {
            ttl: 0, // default TTL
            gen: 0
        },
        limit: 20,
        log: {
            level: 1,
            file: undefined
        },
        policies: {
            timeout: process.env.AEROSPIKE_POLICIES_TIMEOUT ? parseInt(process.env.AEROSPIKE_POLICIES_TIMEOUT) : 2000
        },
    },
};

Config.setupSchemaValidator = function (jjv, schemas) {
    jjv.addSchema('authUserIdTokenModel', schemas['auth'].definitions.authUserIdTokenModel);
    jjv.addSchema('advertisementDataModel', schemas['advertisementManager'].definitions.advertisementDataModel);
    jjv.addSchema('achievementDataModel', schemas['achievementManager'].definitions.achievementDataModel);
    jjv.addSchema('badgeDataModel', schemas['achievementManager'].definitions.badgeDataModel);
    jjv.addSchema('tenantsWithAchievementsDataModel', schemas['achievementManager'].definitions.tenantsWithAchievementsDataModel);
    jjv.addSchema('applicationDataModel', schemas['application'].definitions.applicationDataModel);
    jjv.addSchema('gameDataModel', schemas['game'].definitions.gameDataModel);
    jjv.addSchema('gameFilterModel', schemas['game'].definitions.gameFilterModel);
    jjv.addSchema('gameWinningComponentListDataModel', schemas['game'].definitions.gameWinningComponentListDataModel);
    jjv.addSchema('invoiceEndCustomerDataModel', schemas['profile'].definitions.endConsumerInvoiceModel);
    jjv.addSchema('profileSyncDataModel', schemas['profileManager'].definitions.profileSyncDataModel);
    jjv.addSchema('promocodeDataModel', schemas['promocodeManager'].definitions.promocodeDataModel);
    jjv.addSchema('promocodeCampaignDataModel', schemas['promocodeManager'].definitions.promocodeCampaignDataModel);
    jjv.addSchema('promocodeCodeDataModel', schemas['promocodeManager'].definitions.promocodeCodeDataModel);
    jjv.addSchema('promocodeClassDataModel', schemas['promocodeManager'].definitions.promocodeClassDataModel);
    jjv.addSchema('promocodeInstanceDataModel', schemas['promocodeManager'].definitions.promocodeInstanceDataModel);
    jjv.addSchema('analyticEventDataModel', schemas['question'].definitions.analyticEventDataModel);
    jjv.addSchema('poolDataModel', schemas['question'].definitions.poolDataModel);
    jjv.addSchema('poolMetaDataModel', schemas['question'].definitions.poolMetaDataModel);
    jjv.addSchema('poolIndexDataModel', schemas['question'].definitions.poolIndexDataModel);
    jjv.addSchema('poolIndexMetaDataModel', schemas['question'].definitions.poolIndexMetaDataModel);
    jjv.addSchema('poolLanguageIndexDataModel', schemas['question'].definitions.poolLanguageIndexDataModel);
    jjv.addSchema('poolLanguageIndexMetaDataModel', schemas['question'].definitions.poolLanguageIndexMetaDataModel);
    jjv.addSchema('poolListDataModel', schemas['question'].definitions.poolListDataModel);
    jjv.addSchema('tenantDataModel', schemas['tenant'].definitions.tenantDataModel);
    jjv.addSchema('vatDataModel', schemas['tenantManagementConnector'].definitions.vatDataModel);
    jjv.addSchema('tombolaDataModel', schemas['tombolaManager'].definitions.tombolaDataModel);
    jjv.addSchema('tombolaBoostModel', schemas['tombolaManager'].definitions.tombolaBoostModel);
    jjv.addSchema('voucherDataModel', schemas['voucherManager'].definitions.voucherDataModel);
    jjv.addSchema('voucherInstanceDataModel', schemas['voucherManager'].definitions.voucherInstanceDataModel);
    jjv.addSchema('winningComponentDataModel', schemas['winningManager'].definitions.winningComponentDataModel);
    jjv.addSchema('liveMessageDataModel', schemas['messageCenter'].definitions.liveMessageDataModel);
    return jjv;
};

module.exports = Config;