// Add new field Voucher.shortText
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'voucher',
            'shortText',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'voucher',
            'shortText'
        );
    }
};