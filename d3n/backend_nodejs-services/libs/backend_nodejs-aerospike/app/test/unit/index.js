var DataLoader = require('./../config/index.js');

before(function (done) {
    try {
        DataLoader.loadBefore(function (err, res) {
            if (err) {
                return done(err);
            }
            done();
        });
    } catch (ex) {
        return done(ex);
    }
});

beforeEach(function (done) {
    try {
        DataLoader.loadBeforeEach(function (err, res) {
            if (err) {
                return done(err);
            }
            done();
        });
    } catch (ex) {
        return done(ex);
    }
});

require('./aerospikeConnection.test.js');
require('./aerospikeUserToken.test.js');
require('./aerospikeGlobalClientSession.test.js');
require('./aerospikeProfileSync.test.js');
require('./aerospikeAchievement.test.js');
require('./aerospikeAchievementList.test.js');
require('./aerospikeAdvertisementCounter.test.js');
require('./aerospikeApp.test.js');
require('./aerospikeBadge.test.js');
require('./aerospikeBadgeList.test.js');
require('./aerospikeGame.test.js');
require('./aerospikePool.test.js');
require('./aerospikePoolMeta.test.js');
require('./aerospikePoolIndex.test.js');
require('./aerospikePoolIndexMeta.test.js');
require('./aerospikePoolLanguageIndex.test.js');
require('./aerospikePoolLanguageIndexMeta.test.js');
require('./aerospikeVoucher.test.js');
require('./aerospikeVoucherCounter.test.js');
require('./aerospikeVoucherList.test.js');
require('./aerospikeTenant.test.js');
require('./aerospikeTombola.test.js');
require('./aerospikeTombolaList.test.js');
require('./aerospikePromocodeCampaign.test.js');
require('./aerospikePromocodeCampaignList.test.js');
require('./aerospikePromocodeClass.test.js');
require('./aerospikePromocodeCounter.test.js');
require('./aerospikeTaskLock.js');
