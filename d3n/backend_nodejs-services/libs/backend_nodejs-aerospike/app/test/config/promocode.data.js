var DataIds = require('./_id.data.js');

module.exports = {
    PROMOCODE_CAMPAIGN_1: {
        id: 1,
        name: 'Promocode Campaign 1',
        description: 'Promocode Campaign 1: two nonunique promocodes used twice',
        startDate: '2017-01-05T09:00:00Z',
        endDate: '2017-11-28T20:00:00Z',
        appIds: [1, 2],
        classes: [
            { code: 'PROMOCODE_NONUNIQUE', number: 2, isUnique: false, isQRCode: true },
        ],
        promocodes: [
            {
                code: 'PROMOCODE_NONUNIQUE',
                class: { code: 'PROMOCODE_NONUNIQUE', number: 2, isUnique: false, isQRCode: true },
                numberOfUses: 1,
                expirationDate: '2017-11-28T20:00:00Z',
                generationDate: '2017-01-10T08:00:00Z',
                moneyValue: 50,
                creditValue: 0,
                bonuspointsValue: 0,
                promocodeCampaignId: 1
            }
        ]
    },
    PROMOCODE_CAMPAIGN_2: {
        id: 1,
        name: 'Promocode Campaign 2',
        description: 'Promocode Campaign 2: two unique promocodes',
        startDate: '2017-01-06T09:00:00Z',
        endDate: '2017-11-29T20:00:00Z',
        appIds: [1, 2, 3],
        classes: [
            { code: 'PROMOCODE_UNIQUE', number: 2, isUnique: true, isQRCode: false }
        ],
        promocodes: [
            {
                code: 'PROMOCODE_UNIQUE',
                class: { code: 'PROMOCODE_UNIQUE', number: 2, isUnique: true, isQRCode: false },
                numberOfUses: 2,
                expirationDate: '2017-10-29T20:00:00Z',
                generationDate: '2017-02-10T08:00:00Z',
                moneyValue: 55,
                creditValue: 0,
                bonuspointsValue: 0,
                promocodeCampaignId: 2
            }
        ]
    }
}
