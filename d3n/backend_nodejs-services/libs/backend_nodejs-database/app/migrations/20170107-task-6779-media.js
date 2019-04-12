// Update media.namespace
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            NAMESPACE_APP: 'app',
            NAMESPACE_LANGUAGE: 'language',
            NAMESPACE_VOUCHER: 'voucher',
            NAMESPACE_VOUCHER_BIG: 'voucherBig',
            NAMESPACE_PROFILE: 'profile',
            NAMESPACE_QUESTION: 'question',
            NAMESPACE_ANSWER: 'answer',
            NAMESPACE_GAME: 'game',
            NAMESPACE_GAME_TYPE: 'gameType',
            NAMESPACE_AD: 'ad',
            NAMESPACE_TOMBOLA: 'tombola',
            NAMESPACE_TOMBOLA_BUNDLES: 'tombolaBundles',
            NAMESPACE_PROMO: 'promo',
            NAMESPACE_POOLS: 'pools'
        };
        return queryInterface.changeColumn(
            'media',
            'namespace',
            {
                type: Sequelize.ENUM(
                    _constants.NAMESPACE_APP,
                    _constants.NAMESPACE_LANGUAGE,
                    _constants.NAMESPACE_VOUCHER,
                    _constants.NAMESPACE_VOUCHER_BIG,
                    _constants.NAMESPACE_PROFILE,
                    _constants.NAMESPACE_QUESTION,
                    _constants.NAMESPACE_ANSWER,
                    _constants.NAMESPACE_GAME,
                    _constants.NAMESPACE_GAME_TYPE,
                    _constants.NAMESPACE_AD,
                    _constants.NAMESPACE_TOMBOLA,
                    _constants.NAMESPACE_TOMBOLA_BUNDLES,
                    _constants.NAMESPACE_PROMO,
                    _constants.NAMESPACE_POOLS
                ),
                allowNull: false
            }
        )
    },

    down: function (queryInterface, Sequelize) {
        var _constants = {
            NAMESPACE_APP: 'app',
            NAMESPACE_QUESTION: 'question',
            NAMESPACE_VOUCHER: 'voucher',
            NAMESPACE_PROFILE: 'profile'
        };
        return queryInterface.changeColumn(
            'media',
            'namespace',
            {
                type: Sequelize.ENUM(
                    _constants.NAMESPACE_APP,
                    _constants.NAMESPACE_QUESTION,
                    _constants.NAMESPACE_VOUCHER,
                    _constants.NAMESPACE_PROFILE
                ),
                allowNull: false
            }
        );
    }
};