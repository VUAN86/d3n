var Config = {
    dependencies: {
        voucher: [
            { entity: "advertisement", field: "linkedVoucherId", action: "deactivate", conditions: [] },
            { entity: "game", field: "resultConfiguration.specialPrizeVoucherId", action: "deactivate", conditions: [{ "resultConfiguration.specialPrize": true }] }
        ]
    }
};
module.exports = Config;