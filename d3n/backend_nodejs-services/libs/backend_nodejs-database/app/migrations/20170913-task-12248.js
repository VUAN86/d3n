// Add isActivatedManually flags
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'isPromotion',
            {
                type: Sequelize.INTEGER(1),
                allowNull: true,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'game',
                'promotionStartDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'promotionEndDate',
                {
                    type: Sequelize.DATE,
                    allowNull: true
                }
            );
        });     
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('game', 'isPromotion').then(function () {
            queryInterface.removeColumn('game', 'promotionStartDate');
        }).then(function () {
            queryInterface.removeColumn('game', 'promotionEndDate');
        });
    }
};