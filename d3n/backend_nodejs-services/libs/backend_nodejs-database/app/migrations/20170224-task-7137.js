// Add Tombola.shortText -> Voucher.shortTitle
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'tombola',
            'consolationPrize',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'tombola',
            'consolationPrize'
        );
    }
};