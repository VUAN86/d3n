// Rename Voucher.shortText -> Voucher.shortTitle
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.renameColumn(
            'voucher',
            'shortText',
            'shortTitle'
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.renameColumn(
            'voucher',
            'shortTitle',
            'shortText'
        );
    }
};