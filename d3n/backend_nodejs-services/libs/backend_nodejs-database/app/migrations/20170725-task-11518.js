var _constants = {
    NAMESPACE_APP: 'app',
    NAMESPACE_LANGUAGE: 'language',      // not used, only original resolution
    NAMESPACE_VOUCHER: 'voucher',
    NAMESPACE_VOUCHER_BIG: 'voucherBig',
    NAMESPACE_QUESTION: 'question',
    NAMESPACE_ANSWER: 'answer',
    NAMESPACE_GAME: 'game',
    NAMESPACE_GAME_TYPE: 'gameType',
    NAMESPACE_AD: 'ad',
    NAMESPACE_TOMBOLA: 'tombola',
    NAMESPACE_TOMBOLA_BUNDLES: 'tombolaBundles',
    NAMESPACE_PROMO: 'promo',
    NAMESPACE_POOLS: 'pools',
    NAMESPACE_TENANT_LOGO: 'tenantLogo',
    NAMESPACE_ACHIEVEMENT: 'achievement',
    NAMESPACE_BADGE: 'badge',
    NAMESPACE_ICON: 'icon',

    /*
     * Profile and Group images are not stored in mysql but directly by
     * their userId an groupId
     */
    NAMESPACE_PROFILE: 'profile',
    NAMESPACE_GROUP: 'group'
};

module.exports = {
    up: function (queryInterface, Sequelize) {
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
                    _constants.NAMESPACE_POOLS,
                    _constants.NAMESPACE_TENANT_LOGO,
                    _constants.NAMESPACE_GROUP,
                    _constants.NAMESPACE_ACHIEVEMENT,
                    _constants.NAMESPACE_BADGE,
                    _constants.NAMESPACE_ICON
                ),
                allowNull: false,
                comment: "Namespace identifier"
            }
        );
    },

    down: function (queryInterface, Sequelize) {
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
                    _constants.NAMESPACE_POOLS,
                    _constants.NAMESPACE_TENANT_LOGO,
                    _constants.NAMESPACE_GROUP,
                    _constants.NAMESPACE_ACHIEVEMENT,
                    _constants.NAMESPACE_BADGE
                ),
                allowNull: false,
                comment: "Namespace identifier"
            }
        );
    }
};