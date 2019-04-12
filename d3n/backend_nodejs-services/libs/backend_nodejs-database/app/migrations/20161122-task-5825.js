// allow null for linkedVoucher then change name to linkedVoucherId
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'advertisement',
            'linkedVoucher',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true
            }
        ).then(queryInterface.renameColumn('advertisement', 'linkedVoucher', 'linkedVoucherId'));
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'advertisement',
            'linkedVoucherId',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false
            }
        ).then(queryInterface.renameColumn('advertisement', 'linkedVoucherId', 'linkedVoucher'));
    }
};