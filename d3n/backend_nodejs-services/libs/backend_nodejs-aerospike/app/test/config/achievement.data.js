module.exports = {
    ACHIEVEMENT_1: {
        id: 1,
        name: 'Achievement one',
        description: 'Description 1',
        status: 'inactive',
        imageId: 'test.jpg',
        isTimeLimit: 1,
        timePeriod: 100,
        isReward: 1,
        bonusPointsReward: 15,
        creditReward: 0,
        paymentMultiplier: 1.1,
        accessRules: "[\"PRO\"]",
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: '2016-11-28T20:48:52Z',
        tenantId: 1
    },
    ACHIEVEMENT_2: {
        id: 2,
        name: 'Achievement two',
        description: 'Description 2',
        status: 'inactive',
        imageId: 'test.jpg',
        isTimeLimit: 1,
        timePeriod: 200,
        isReward: 1,
        bonusPointsReward: 0,
        creditReward: 3,
        paymentMultiplier: 1.1,
        accessRules: "[\"PREMIUM\"]",
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: '2016-11-28T20:48:52Z',
        tenantId: 2
    },

    BADGE_1: {
        id: 1,
        name: 'Badge one',
        description: 'Description 1',
        type: 'Game',
        status: 'inactive',
        time: 'Permanent',
        timePeriod: 0,
        imageId: 'test.jpg',
        usage: 'Single',
        isReward: 1,
        bonusPointsReward: 15,
        creditReward: 0,
        paymentMultiplier: 1.1,
        accessRules: "[\"PRO\"]",
        earningRules: "[{\"rule\":\"RULE_QUESTION_TRANSLATED\",\"limit\":\100},{\"rule\":\"RULE_GAME_WON_GAMES\",\"limit\":\10}]",
        battleRules: [2],
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: '2016-11-28T20:48:52Z',
        tenantId: 1
    },
    BADGE_2: {
        id: 2,
        name: 'Badge two',
        description: 'Description 2',
        type: 'Battle',
        status: 'inactive',
        time: 'Temporary',
        timePeriod: 100,
        imageId: 'test.jpg',
        usage: 'Multiple',
        isReward: 1,
        bonusPointsReward: 0,
        creditReward: 10,
        paymentMultiplier: 1.1,
        accessRules: "[\"PREMIUM\"]",
        earningRules: "[{\"rule\":\"RULE_QUESTION_TRANSLATED\",\"limit\":\200},{\"rule\":\"RULE_GAME_WON_GAMES\",\"limit\":\20}]",
        battleRules: [1],
        messaging: "[\"SMS\",\"EMAIL\"]",
        createDate: '2016-11-28T20:48:52Z',
        tenantId: 2
    },
}
