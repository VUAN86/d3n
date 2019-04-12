var _constants = {
    COMMUNITY_PROMOTION_CURRENCY_MONEY: 'MONEY',
    COMMUNITY_PROMOTION_CURRENCY_CREDIT: 'CREDIT',
    COMMUNITY_PROMOTION_CURRENCY_BONUS: 'BONUS',
};

module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'workorder',
            'isCommunityPromoted',
            {
                type: Sequelize.INTEGER(1),
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'communityPromotionFromDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'communityPromotionToDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'communityPromotionCurrency',
                {
                    type: Sequelize.ENUM(
                        _constants.COMMUNITY_PROMOTION_CURRENCY_MONEY,
                        _constants.COMMUNITY_PROMOTION_CURRENCY_CREDIT,
                        _constants.COMMUNITY_PROMOTION_CURRENCY_BONUS
                    ),
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'communityPromotionAmount',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'workorder',
                'communityPromotionDescription',
                {
                    type: Sequelize.TEXT,
                    allowNull: true
                }
            );
        });
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'workorder',
            'isCommunityPromoted'
        ).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'communityPromotionFromDate'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'communityPromotionToDate'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'communityPromotionCurrency'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'communityPromotionAmount'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'communityPromotionDescription'
            );
        });
    }
};