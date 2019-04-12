// Refactor promocodeManager
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            USAGE_ONCE_PER_PLAYER: 'oncePerPlayer',
            USAGE_MULTI_PER_PLAYER: 'multiPerPlayer'
        };
        return queryInterface.removeColumn(
            'promocode_campaign',
            'usage').then(function () {
            return queryInterface.removeColumn(
                'promocode_campaign',
                'numberOfUses'
            );
        }).then(function () {
            var sqlRaw ="ALTER TABLE promocode_campaign DROP FOREIGN KEY promocode_campaign_ibfk_1;";
            return queryInterface.sequelize.query(
                sqlRaw
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'promocode_campaign',
                'usedByApplicationId'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'promocode',
                'isUsed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'promocode',
                'useDate'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'promocode',
                'usedByProfileId'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'promocode',
                'usedByApplicationId'
            );
        }).then(function () {
            return queryInterface.addColumn(
                'promocode',
                'usage',
                {
                    type: Sequelize.ENUM(
                        _constants.USAGE_ONCE_PER_PLAYER,
                        _constants.USAGE_MULTI_PER_PLAYER
                    ),
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'promocode',
                'numberOfUses',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: true
                }
            );
        });
    },

    down: function (queryInterface, Sequelize) {
        return true;
    }
};